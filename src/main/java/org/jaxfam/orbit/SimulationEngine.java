//
// SimulationEngine.java
//
// Part of the orbital mechanics demonstrator program
//
// 2026 Written to decouple Propagate() from Orbit's GLFW-bound render/input loop
//

package org.jaxfam.orbit;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Runs OrbitalSystem.Propagate() on a dedicated background thread, decoupled from Orbit's
 * render/input loop - which GLFW/Cocoa forces onto the main thread (see CLAUDE.md's
 * -XstartOnFirstThread section) and can't be moved. Physics runs continuously, as fast as
 * Propagate() allows, independent of vsync; the render thread is meant to read whatever
 * snapshot was most recently published via {@link #latest()} rather than ever touching the live
 * OrbitalSystem or its Body objects directly, so mouse/keyboard handling and rendering stay
 * responsive no matter how expensive a single step is - Propagate() is O(n^2) in body count, so
 * a run with thousands of bodies can take a large fraction of a second per step.
 * <p>
 * Mutations that would otherwise happen directly on the render thread (save/reload/circularize/
 * pause/speed) instead go through {@link #submit(UnaryOperator)}, a command queue drained
 * between steps - never mid-{@code Propagate()} - so the physics thread's OrbitalSystem is never
 * read and written by two threads at once. A command receives the current OrbitalSystem and
 * returns the one to use going forward: a plain mutation returns the same (now-mutated)
 * reference; a full replacement (e.g. a reload) returns a different one.
 * <p>
 * {@code S}, the published snapshot type, is caller-defined and opaque to this class - built by
 * a caller-supplied {@code snapshotBuilder} every time a step (or a drained command) completes.
 * Kept generic and decoupled from any Orbit/rendering-specific type so the threading mechanics
 * here can be tested with plain JUnit, without GLFW or OpenGL.
 *
 * @param <S> the snapshot type published to {@link #latest()}
 */
class SimulationEngine<S> {

    /** how long the physics loop sleeps between iterations while paused and idle, so it doesn't
        busy-spin a full CPU core doing nothing - irrelevant while actually running, since a real
        Propagate() call itself takes real time */
    private static final long IDLE_SLEEP_MILLIS = 15;

    private final Queue<UnaryOperator<OrbitalSystem>> commands = new ConcurrentLinkedQueue<>();
    private final AtomicReference<S> latestSnapshot;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Function<OrbitalSystem, S> snapshotBuilder;

    /** touched only by the physics thread once start() has been called */
    private OrbitalSystem system;
    private Thread thread;

    SimulationEngine(OrbitalSystem initialSystem, Function<OrbitalSystem, S> snapshotBuilder) {
        this.system = initialSystem;
        this.snapshotBuilder = snapshotBuilder;
        this.latestSnapshot = new AtomicReference<>(snapshotBuilder.apply(initialSystem));
    }

    /** Starts the background physics thread. No-op if already started. */
    void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this::runLoop, "orbit-physics");
        thread.setDaemon(true);
        thread.start();
    }

    /** Signals the physics loop to stop after its current step/command drain completes. */
    void stop() {
        shutdown.set(true);
    }

    /** Blocks until the physics thread has actually exited, or the timeout elapses. */
    void awaitStop(long timeoutMillis) throws InterruptedException {
        if (thread != null) {
            thread.join(timeoutMillis);
        }
    }

    /** Whether Propagate() is being called every cycle - P's toggle target. */
    void setRunning(boolean value) {
        running.set(value);
    }

    boolean isRunning() {
        return running.get();
    }

    /**
     * Queues a mutation (or full replacement) of the live OrbitalSystem, applied on the physics
     * thread between steps - never concurrently with Propagate() itself. The command receives
     * the current system and returns the one to use going forward.
     */
    void submit(UnaryOperator<OrbitalSystem> command) {
        commands.add(command);
    }

    /** The most recently published snapshot - safe to call from any thread, including
        immediately after construction, before start() has ever been called. */
    S latest() {
        return latestSnapshot.get();
    }

    private void runLoop() {
        while (!shutdown.get()) {
            boolean didWork = drainCommands();
            if (running.get()) {
                system.Propagate();
                didWork = true;
            }
            if (didWork) {
                latestSnapshot.set(snapshotBuilder.apply(system));
            } else {
                sleepQuietly(IDLE_SLEEP_MILLIS);
            }
        }
    }

    /** @return true if at least one command was applied (so the caller knows to publish a
        fresh snapshot even though Propagate() wasn't called this cycle, e.g. while paused) */
    private boolean drainCommands() {
        boolean applied = false;
        UnaryOperator<OrbitalSystem> command;
        while ((command = commands.poll()) != null) {
            system = command.apply(system);
            applied = true;
        }
        return applied;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
