package org.jaxfam.orbit;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies SimulationEngine's threading mechanics in isolation - no GLFW/OpenGL involved, just
 * an OrbitalSystem with zero bodies (Propagate() is well-defined and cheap on an empty system;
 * these tests care about the engine's command-queue/publish/pause/shutdown behavior, not physics
 * correctness, which CircularizeTest/OrbitalSystemTest/etc. already cover).
 */
public class SimulationEngineTest {

    // OrbitalSystem.dt is a static field shared across the whole test JVM - see
    // OrbitalSystemJsonTest's comment for why this needs saving/restoring
    double savedDt;
    SimulationEngine<Double> engine;

    @BeforeEach
    public void saveDt() {
        savedDt = OrbitalSystem.dt;
    }

    @AfterEach
    public void restoreDtAndStopEngine() throws InterruptedException {
        OrbitalSystem.dt = savedDt;
        if (engine != null) {
            engine.stop();
            engine.awaitStop(2000);
        }
    }

    private static OrbitalSystem emptySystem() {
        OrbitalSystem system = new OrbitalSystem();
        system.maxR = 10.0;
        system.bodies = new ArrayList<>();
        return system;
    }

    /** Polls until the given condition holds or the timeout elapses, failing the test if it
        never does - the only reliable way to observe genuinely asynchronous state. */
    private static void awaitTrue(String message, long timeoutMillis, java.util.function.BooleanSupplier condition)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(5);
        }
        assertTrue(condition.getAsBoolean(), message);
    }

    @Test
    public void testSnapshotIsAvailableImmediatelyOnConstruction() {
        OrbitalSystem system = emptySystem();
        system.time = 42.0;
        engine = new SimulationEngine<>(system, sys -> sys.time);

        // latest() must work even before start() is ever called
        assertEquals(42.0, engine.latest());
    }

    @Test
    public void testPhysicsAdvancesTimeWhileRunning() throws InterruptedException {
        OrbitalSystem.dt = 100.0;
        OrbitalSystem system = emptySystem();
        engine = new SimulationEngine<>(system, sys -> sys.time);
        engine.setRunning(true);
        engine.start();

        awaitTrue("time should advance past zero once physics starts running",
                2000, () -> engine.latest() > 0.0);
    }

    @Test
    public void testPausedEngineDoesNotAdvancePhysicsButStillAppliesCommands() throws InterruptedException {
        OrbitalSystem.dt = 100.0;
        OrbitalSystem system = emptySystem();
        engine = new SimulationEngine<>(system, sys -> sys.time);
        // never call setRunning(true) - engine starts paused

        AtomicInteger commandsApplied = new AtomicInteger(0);
        engine.start();
        engine.submit(sys -> {
            commandsApplied.incrementAndGet();
            return sys;
        });

        awaitTrue("submitted command should be applied even while paused",
                2000, () -> commandsApplied.get() > 0);

        // give the (paused) engine a further moment to have run several more idle cycles, to
        // make sure none of them advanced time
        Thread.sleep(100);
        assertEquals(0.0, engine.latest(), "paused engine must not advance simulation time");
    }

    @Test
    public void testCommandCanReplaceTheWholeSystem() throws InterruptedException {
        OrbitalSystem original = emptySystem();
        original.time = 1.0;
        engine = new SimulationEngine<>(original, sys -> sys.time);

        OrbitalSystem replacement = emptySystem();
        replacement.time = 999.0;
        engine.start();
        engine.submit(sys -> replacement);

        awaitTrue("snapshot should reflect the replaced system, not the original",
                2000, () -> engine.latest() == 999.0);
    }

    @Test
    public void testStopActuallyTerminatesThePhysicsThread() throws InterruptedException {
        OrbitalSystem.dt = 100.0;
        OrbitalSystem system = emptySystem();
        engine = new SimulationEngine<>(system, sys -> sys.time);
        engine.setRunning(true);
        engine.start();

        awaitTrue("engine should be running before we try to stop it",
                2000, () -> engine.latest() > 0.0);

        engine.stop();
        long start = System.currentTimeMillis();
        engine.awaitStop(2000);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 2000, "thread should have exited well before the join timeout, took " + elapsed + "ms");
    }
}
