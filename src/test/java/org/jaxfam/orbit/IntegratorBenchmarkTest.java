package org.jaxfam.orbit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sanity-checks the pieces IntegratorBenchmark's numerical comparison depends on: that its fixed
 * seed actually produces a reproducible initial condition (the whole point of using a fixed seed
 * rather than RandomSystem's normal wall-clock-seeded default), and that totalEnergy() computes
 * what it's supposed to.
 */
public class IntegratorBenchmarkTest {

    double savedDt;

    @BeforeEach
    public void saveDt() {
        savedDt = OrbitalSystem.dt;
    }

    @AfterEach
    public void restoreDt() {
        OrbitalSystem.dt = savedDt;
    }

    @Test
    public void testStandardScenarioIsReproducible() {
        RandomSystem a = IntegratorBenchmark.buildStandardScenario();
        RandomSystem b = IntegratorBenchmark.buildStandardScenario();

        assertEquals(a.bodies.size(), b.bodies.size());
        for (int i = 0; i < a.bodies.size(); i++) {
            Body bodyA = a.bodies.get(i);
            Body bodyB = b.bodies.get(i);
            assertEquals(bodyA.getX(), bodyB.getX(), 0.0, "body " + i + " X");
            assertEquals(bodyA.getY(), bodyB.getY(), 0.0, "body " + i + " Y");
            assertEquals(bodyA.getU(), bodyB.getU(), 0.0, "body " + i + " U");
            assertEquals(bodyA.getV(), bodyB.getV(), 0.0, "body " + i + " V");
            assertEquals(bodyA.getMass(), bodyB.getMass(), 0.0, "body " + i + " mass");
            assertEquals(bodyA.getName(), bodyB.getName(), "body " + i + " name");
        }
        assertEquals(IntegratorBenchmark.totalEnergy(a), IntegratorBenchmark.totalEnergy(b), 0.0);
    }

    @Test
    public void testTotalEnergyMatchesHandComputedTwoBodyCase() {
        OrbitalSystem system = new OrbitalSystem();
        system.maxR = 10.0;
        system.bodies = new java.util.ArrayList<>(2);

        Body b1 = new Body(-1.0, 0.0, 100.0, 0.0, 1.0, 1.0, Color.white(), 10);
        Body b2 = new Body(1.0, 0.0, -50.0, 0.0, 2.0, 1.0, Color.white(), 10);
        system.bodies.add(b1);
        system.bodies.add(b2);

        double ke = 0.5*b1.getMass()*100.0*100.0 + 0.5*b2.getMass()*50.0*50.0;
        double r = Math.hypot(b1.getX()-b2.getX(), b1.getY()-b2.getY());
        double pe = -OrbitalSystem.Kgravity * b1.getMass() * b2.getMass() / r;
        double expected = ke + pe;

        assertEquals(expected, IntegratorBenchmark.totalEnergy(system), 1e-6*Math.abs(expected));
    }
}
