//
// IntegratorBenchmark.java
//
// Part of the orbital mechanics demonstrator program
//
// 2026 Written to compare Integrator implementations on a fixed, reproducible scenario
//

package org.jaxfam.orbit;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * A standard, reproducible benchmark for comparing Integrator implementations: builds an
 * identical RandomSystem initial condition (fixed seed) for each integrator in turn, propagates
 * it the same number of steps, and reports both numerical drift (total energy conservation,
 * which the true continuous system would hold exactly constant) and wall-clock performance.
 * <p>
 * The scenario's parameters deliberately match Orbit's own interactive RandomSystem defaults
 * (see Orbit.SCENARIO_RANDOM's prompts) rather than an artificial stress case, so the numbers
 * this produces describe integrator behavior on the scenario a user would actually run - only
 * with a fixed seed in place of wall-clock-seeded randomness, so every run is identical and
 * results are comparable across code changes.
 * <p>
 * STEP_COUNT (2000) is deliberately kept below the first collision/merge event under this exact
 * seed - empirically found at step 3000 for RK2/RK4/RKK4 (Euler1's much larger local error keeps
 * it away from the same close encounter entirely, at least within 5000 steps) - so every
 * integrator is still propagating the same 200-body system throughout. See README.txt's
 * Benchmarking section for why this matters: once body count diverges between two integrators'
 * independent trajectories, their total-energy figures are no longer describing the same
 * physical system and a direct numerical comparison stops being meaningful. main() still reports
 * body count and flags any mismatch, in case this margin is ever eroded by a future change.
 * <p>
 * Run via {@code ./mvnw compile exec:exec -Dexec.mainClass=org.jaxfam.orbit.IntegratorBenchmark}.
 *
 * @author Bruce Jackson
 */
public class IntegratorBenchmark {

    // fixed benchmark configuration - see class javadoc for why these particular values
    static final long   SEED         = 42L;
    static final double DT           = 1200.0;
    static final int    NUM_BODIES   = 200;
    static final int    PATH_LENGTH  = 200;
    static final double DIST_SHAPE   = -1.0;
    static final double MIN_RADIUS   = 0.001;
    static final double MAX_RADIUS   = 50.0;
    static final double MAX_V_IC     = 1000.0;
    static final double DENSITY      = 2.0;
    static final double MAX_SYSTEM_R = 3.0;
    static final int    STEP_COUNT   = 2_000;

    /** one entry per Integrator implementation being compared */
    record IntegratorSpec(String name, Function<OrbitalSystem, Integrator> factory) {}

    static final List<IntegratorSpec> INTEGRATORS = List.of(
            new IntegratorSpec("Euler1", Euler1Integrator::new),
            new IntegratorSpec("RK2",    RungeKutta2Integrator::new),
            new IntegratorSpec("RK4",    RungeKutta4Integrator::new),
            new IntegratorSpec("RKK4",   RungeKuttaKahan4Integrator::new)
    );

    static RandomSystem buildStandardScenario() {
        RandomSystem system = new RandomSystem(DT, NUM_BODIES, PATH_LENGTH, DIST_SHAPE,
                MIN_RADIUS, MAX_RADIUS, MAX_V_IC, DENSITY, MAX_SYSTEM_R, new Random(SEED));
        OrbitalSystem.dt = DT;
        return system;
    }

    /** Total mechanical energy (kinetic + gravitational potential) of the system - the true
     * continuous N-body system conserves this exactly, so any drift in it is purely a numerical
     * integration artifact. */
    static double totalEnergy(OrbitalSystem system) {
        double G = OrbitalSystem.Kgravity;
        List<Body> bodies = system.bodies;
        double ke = 0.0;
        for (Body b : bodies) {
            double speed2 = b.getU()*b.getU() + b.getV()*b.getV();
            ke += 0.5 * b.getMass() * speed2;
        }
        double pe = 0.0;
        for (int i = 0; i < bodies.size(); i++) {
            for (int j = i+1; j < bodies.size(); j++) {
                Body bi = bodies.get(i);
                Body bj = bodies.get(j);
                double dx = bi.getX() - bj.getX();
                double dy = bi.getY() - bj.getY();
                double r = Math.sqrt(dx*dx + dy*dy);
                pe -= G * bi.getMass() * bj.getMass() / r;
            }
        }
        return ke + pe;
    }

    static class Result {
        String name;
        int initialBodyCount, finalBodyCount;
        double e0, eFinal;
        double elapsedMs;
    }

    static Result run(IntegratorSpec spec) {
        RandomSystem system = buildStandardScenario();
        system.integrator = spec.factory().apply(system);

        Result result = new Result();
        result.name = spec.name();
        result.initialBodyCount = system.bodies.size();
        result.e0 = totalEnergy(system);

        long t0 = System.nanoTime();
        for (int i = 0; i < STEP_COUNT; i++) {
            system.Propagate();
        }
        long t1 = System.nanoTime();

        result.finalBodyCount = system.bodies.size();
        result.eFinal = totalEnergy(system);
        result.elapsedMs = (t1 - t0) / 1e6;
        return result;
    }

    public static void main(String[] args) {
        System.out.printf("IntegratorBenchmark: seed=%d dt=%.1f numBodies=%d steps=%d%n%n",
                SEED, DT, NUM_BODIES, STEP_COUNT);
        System.out.printf("%-8s %6s %6s %22s %14s %10s%n",
                "name", "N0", "Nfinal", "final energy", "relDrift", "ms");

        for (IntegratorSpec spec : INTEGRATORS) {
            Result r = run(spec);
            double relDrift = (r.eFinal - r.e0) / Math.abs(r.e0);
            System.out.printf("%-8s %6d %6d %22.10e %14.6e %10.1f%s%n",
                    r.name, r.initialBodyCount, r.finalBodyCount, r.eFinal, relDrift, r.elapsedMs,
                    (r.finalBodyCount != r.initialBodyCount)
                            ? "  *** body count changed - see README.txt caveat ***" : "");
        }
    }
}
