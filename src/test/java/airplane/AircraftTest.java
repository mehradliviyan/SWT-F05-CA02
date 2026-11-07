
package airplane;

import airplane.components.Engine;
import airplane.components.FuelSystem;
import airplane.components.LandingGear;
import airplane.components.NavigationSystem;
import airplane.exceptions.FlightException;
import airplane.models.FlightStatus;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class AircraftTest {

    // Dependencies
    private Engine engine;
    private FuelSystem fuelSystem;
    private LandingGear landingGear;
    private NavigationSystem navigationSystem;

    // System Under Test
    private Aircraft aircraft;


    // ==========================================
    // JUNIT LIFECYCLE
    // ==========================================

    @BeforeAll
    static void beforeAll() {
        System.out.println("========== BEFORE ALL ==========");
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("---------- BEFORE EACH ----------");

        engine = mock(Engine.class);
        fuelSystem = mock(FuelSystem.class);
        landingGear = mock(LandingGear.class);
        navigationSystem = mock(NavigationSystem.class);

        aircraft = new Aircraft(
                engine,
                fuelSystem,
                landingGear,
                navigationSystem
        );
    }




    // ==========================================
    // TEST HELPERS
    // ==========================================

    private void stubSuccessfulPreconditions() {
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(true);
    }

    private void prepareAircraftForFlight() {
        stubSuccessfulPreconditions();

        aircraft.prepareForTakeoff();
        aircraft.takeOff();
    }


    // ==========================================
    // INITIAL STATE
    // ==========================================


    @Test
    void newAircraft_startsInGroundedState() {

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );
    }

    @Test
    void constructor_whenEngineIsNull_throwsNullPointerException() {

        assertThrows(
                NullPointerException.class,
                () -> new Aircraft(
                        null,
                        fuelSystem,
                        landingGear,
                        navigationSystem
                )
        );
    }


    // ==========================================
    // PREPARE FOR TAKEOFF
    // ==========================================

    @Test
    void prepareForTakeoff_whenAllPreconditionsAreMet_setsStatusToReady() {

        // Arrange
        stubSuccessfulPreconditions();

        // Act
        aircraft.prepareForTakeoff();

        // Assert
        assertEquals(
                FlightStatus.READY,
                aircraft.getStatus()
        );

        verify(engine, times(1)).isOperational();
        verify(fuelSystem, times(1)).hasEnoughFuel();
        verify(navigationSystem, times(1)).isRouteAvailable();
    }

    @Test
    void prepareForTakeoff_whenEngineIsNotOperational_throwsFlightException() {

        // Arrange
        when(engine.isOperational()).thenReturn(false);

        // Act & Assert
        FlightException exception = assertThrows(
                FlightException.class,
                () -> aircraft.prepareForTakeoff()
        );

        assertTrue(
                exception.getMessage().contains("engine is not operational")
        );

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );

        verifyNoInteractions(fuelSystem, navigationSystem);
    }

    @Test
    void prepareForTakeoff_whenFuelIsInsufficient_throwsFlightException() {

        // Arrange
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(false);

        // Act & Assert
        FlightException exception = assertThrows(
                FlightException.class,
                () -> aircraft.prepareForTakeoff()
        );

        assertTrue(
                exception.getMessage().contains("insufficient fuel")
        );

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );

        verifyNoInteractions(navigationSystem);
    }

    @Test
    void prepareForTakeoff_whenRouteIsUnavailable_throwsFlightException() {

        // Arrange
        when(engine.isOperational()).thenReturn(true);
        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(false);

        // Act & Assert
        FlightException exception = assertThrows(
                FlightException.class,
                () -> aircraft.prepareForTakeoff()
        );

        assertTrue(
                exception.getMessage().contains("route is unavailable")
        );

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );
    }

    @Test
    void prepareForTakeoff_whenEngineHealthCheckThrows_propagatesException() {

        // Arrange
        IllegalStateException failure =
                new IllegalStateException("Engine sensor unavailable");

        when(engine.isOperational()).thenThrow(failure);

        // Act
        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> aircraft.prepareForTakeoff()
        );

        // Assert
        assertSame(failure, actual);

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );
    }


    // ==========================================
    // TAKE OFF
    // ==========================================

    @Test
    void takeOff_whenAircraftIsReady_startsEngineRetractsGearAndSetsInFlight() {

        // Arrange
        stubSuccessfulPreconditions();
        aircraft.prepareForTakeoff();

        // Explicit void-method stubbing for demonstration.
        // Mockito mocks do nothing by default for void methods.
        doNothing().when(landingGear).retract();

        // Act
        aircraft.takeOff();

        // Assert
        assertEquals(
                FlightStatus.IN_FLIGHT,
                aircraft.getStatus()
        );

        verify(engine, times(1)).start();
        verify(landingGear, times(1)).retract();
    }

    @Test
    void takeOff_whenEngineStartThrows_setsAbortedAndWrapsCause() {

        // Arrange
        stubSuccessfulPreconditions();
        aircraft.prepareForTakeoff();

        IllegalStateException failure =
                new IllegalStateException("Engine failed to start");

        doThrow(failure).when(engine).start();

        // Act
        FlightException actual = assertThrows(
                FlightException.class,
                () -> aircraft.takeOff()
        );

        // Assert
        assertEquals(
                FlightStatus.ABORTED,
                aircraft.getStatus()
        );

        assertSame(failure, actual.getCause());

        verify(landingGear, times(0)).retract();
    }

    @Test
    void takeOff_whenLandingGearRetractionThrows_setsAbortedAndWrapsCause() {

        // Arrange
        stubSuccessfulPreconditions();
        aircraft.prepareForTakeoff();

        IllegalStateException failure =
                new IllegalStateException("Landing gear jammed");

        doThrow(failure).when(landingGear).retract();

        // Act
        FlightException actual = assertThrows(
                FlightException.class,
                () -> aircraft.takeOff()
        );

        // Assert
        assertEquals(
                FlightStatus.ABORTED,
                aircraft.getStatus()
        );

        assertSame(failure, actual.getCause());

        verify(engine, times(1)).start();
    }

    @Test
    void takeOff_whenAircraftIsNotReady_throwsFlightException() {

        // Act & Assert
        FlightException exception = assertThrows(
                FlightException.class,
                () -> aircraft.takeOff()
        );

        assertTrue(
                exception.getMessage().contains("GROUNDED")
        );

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );

        verifyNoInteractions(engine, landingGear);
    }


    // ==========================================
    // CRUISE
    // ==========================================

    @Test
    void cruise_whenFuelAmountIsValid_consumesFuelAndRemainsInFlight() {

        // Arrange
        prepareAircraftForFlight();

        double fuelAmount = 25.0;

        // Act
        aircraft.cruise(fuelAmount);

        // Assert
        assertEquals(
                FlightStatus.IN_FLIGHT,
                aircraft.getStatus()
        );

        verify(fuelSystem, times(1)).consumeFuel(fuelAmount);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -5.0})
    void cruise_whenFuelAmountIsNotPositive_throwsIllegalArgumentException(
            double amount
    ) {

        // Arrange
        prepareAircraftForFlight();

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> aircraft.cruise(amount)
        );

        assertEquals(
                FlightStatus.IN_FLIGHT,
                aircraft.getStatus()
        );

        verify(fuelSystem, times(0)).consumeFuel(anyDouble());
    }

    @Test
    void cruise_whenAircraftIsNotInFlight_throwsFlightException() {

        // Act & Assert
        FlightException exception = assertThrows(
                FlightException.class,
                () -> aircraft.cruise(10.0)
        );

        assertTrue(
                exception.getMessage().contains("GROUNDED")
        );

        verifyNoInteractions(engine, fuelSystem, landingGear);
    }

    @Test
    void cruise_whenEngineIsNoLongerOperational_performsEmergencyLanding() {

        // Arrange
        prepareAircraftForFlight();

        when(engine.isOperational()).thenReturn(false);

        // Act
        aircraft.cruise(10.0);

        // Assert
        assertEquals(
                FlightStatus.LANDED,
                aircraft.getStatus()
        );

        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();

        verify(fuelSystem, times(0)).consumeFuel(anyDouble());
    }

    @Test
    void cruise_whenFuelConsumptionThrows_performsEmergencyLandingAndThrowsFlightException() {

        // Arrange
        prepareAircraftForFlight();

        IllegalStateException failure =
                new IllegalStateException("Not enough fuel");

        doThrow(failure)
                .when(fuelSystem)
                .consumeFuel(anyDouble());

        // Act
        FlightException actual = assertThrows(
                FlightException.class,
                () -> aircraft.cruise(100.0)
        );

        // Assert
        assertEquals(FlightStatus.LANDED, aircraft.getStatus());
        assertSame(failure, actual.getCause());

        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
    }


    // ==========================================
    // NORMAL LANDING
    // ==========================================

    @Test
    void land_whenAircraftIsInFlight_deploysGearStopsEngineAndSetsLanded() {

        // Arrange
        prepareAircraftForFlight();

        // Act
        aircraft.land();

        // Assert
        assertEquals(
                FlightStatus.LANDED,
                aircraft.getStatus()
        );

        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
    }


    // ==========================================
    // EMERGENCY LANDING
    // ==========================================

    @Test
    void emergencyLand_whenAircraftIsInFlight_deploysGearStopsEngineAndSetsLanded() {

        // Arrange
        prepareAircraftForFlight();

        // Act
        aircraft.emergencyLand();

        // Assert
        assertEquals(
                FlightStatus.LANDED,
                aircraft.getStatus()
        );

        verify(landingGear, times(1)).deploy();
        verify(engine, times(1)).stop();
    }

    @Test
    void emergencyLand_whenAircraftIsNotInFlight_throwsFlightException() {

        assertThrows(
                FlightException.class,
                () -> aircraft.emergencyLand()
        );

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );

        verifyNoInteractions(engine, landingGear);
    }

    @Test
    void land_whenAircraftIsNotInFlight_throwsFlightException() {

        // Act & Assert
        FlightException exception = assertThrows(
                FlightException.class,
                () -> aircraft.land()
        );

        assertTrue(
                exception.getMessage().contains("GROUNDED")
        );

        assertEquals(
                FlightStatus.GROUNDED,
                aircraft.getStatus()
        );

        verifyNoInteractions(engine, landingGear);
    }


    // ==========================================
    // REPEATED FLIGHT
    // ==========================================

    @Test
    void prepareForTakeoff_afterLanding_canPrepareForAnotherFlight() {

        // Arrange
        prepareAircraftForFlight();
        aircraft.land();

        // Act
        aircraft.prepareForTakeoff();

        // Assert
        assertEquals(
                FlightStatus.READY,
                aircraft.getStatus()
        );
    }


    // ==========================================
    // SPY DEMONSTRATION
    // ==========================================

    @Test
    void takeOff_withRealEngineSpy_invokesRealStartBehavior() {

        // Arrange
        Engine realEngineSpy = spy(new Engine(true));

        when(fuelSystem.hasEnoughFuel()).thenReturn(true);
        when(navigationSystem.isRouteAvailable()).thenReturn(true);

        Aircraft aircraftWithSpy = new Aircraft(
                realEngineSpy,
                fuelSystem,
                landingGear,
                navigationSystem
        );

        aircraftWithSpy.prepareForTakeoff();

        // Act
        aircraftWithSpy.takeOff();

        // Assert
        assertTrue(realEngineSpy.isRunning());

        verify(realEngineSpy, times(1)).start();

        assertEquals(
                FlightStatus.IN_FLIGHT,
                aircraftWithSpy.getStatus()
        );
    }


    // ==========================================
    // RESET DEMONSTRATION
    // ==========================================

    @Test
    void reset_clearsPreviousStubbingAndInteractionHistory() {

        // Arrange
        when(engine.isOperational()).thenReturn(true);

        assertTrue(engine.isOperational());
        verify(engine).isOperational();

        // Act
        reset(engine);

        // Assert
        verifyNoInteractions(engine);

        // Unstubbed boolean methods return false by default.
        assertFalse(engine.isOperational());
    }

    @AfterEach
    void afterEach() {
        System.out.println("---------- AFTER EACH ----------");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("========== AFTER ALL ==========");
    }
}
