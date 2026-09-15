package org.jaxfam.orbit;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the parallel force-sum path OrbitalSystem.sumForces() switches to above
 * PARALLEL_FORCE_SUM_THRESHOLD bodies: Body.addForceFrom() (one-sided, used by the parallel
 * path) against Body.addForces() (the original two-sided, sequential-only method) directly, and
 * a full Propagate()-driven smoke test at real scale (sumForces() itself is private, so this is
 * the actual public entry point that exercises it).
 */
public class ParallelForceSumTest {

    double savedDt;

    @BeforeEach
    public void saveDt() {
        savedDt = OrbitalSystem.dt;
    }

    @AfterEach
    public void restoreDt() {
        OrbitalSystem.dt = savedDt;
    }

    // Body's constructor takes X/Y in AU (converted to meters internally, X = AU2m * X_ic_AU)
    // and radius in Earth radii (radius_m = radius_re * earthRadius) - both helpers below take
    // those same units directly so the fixture's distances/radii are easy to reason about.
    private static Body body(double xAU, double yAU, double radiusEarthRadii) {
        return new Body(xAU, yAU, 0.0, 0.0, radiusEarthRadii, 1.0, Color.white(), 10);
    }

    @Test
    public void testAddForceFromMatchesAddForcesPairwise() {
        // four bodies, non-axis-aligned, no two at the same distance - two of them (0 and 1)
        // deliberately close enough to trigger collision detection, the rest not: 1 Earth
        // radius each gives sumRadii ~1.276e7 m (~8.5e-5 AU), and they're placed 5e-5 AU apart
        Body[] sequential = {
                body(0.0, 0.0, 1.0),
                body(5.0e-5, 0.0, 1.0),
                body(3.0, 2.0, 1.0),
                body(-4.0, 1.0, 2.0),
        };
        Body[] parallel = {
                body(0.0, 0.0, 1.0),
                body(5.0e-5, 0.0, 1.0),
                body(3.0, 2.0, 1.0),
                body(-4.0, 1.0, 2.0),
        };

        // sequential: addForces() once per unordered pair (i<j), as OrbitalSystem.sumForces()'s
        // sequential branch does
        for (int i = 0; i < sequential.length; i++) {
            for (int j = i + 1; j < sequential.length; j++) {
                Body.addForces(sequential[i], sequential[j]);
            }
        }

        // parallel-style: addForceFrom() once per ORDERED pair (both directions), as the
        // parallel branch does - each body accumulating onto itself only
        for (int i = 0; i < parallel.length; i++) {
            for (int j = 0; j < parallel.length; j++) {
                if (i != j) {
                    parallel[i].addForceFrom(parallel[j]);
                }
            }
        }

        double pct = 1e-12;
        for (int i = 0; i < sequential.length; i++) {
            assertEquals(sequential[i].getXForce(), parallel[i].getXForce(),
                    pct * Math.abs(sequential[i].getXForce()), "body " + i + " X force");
            assertEquals(sequential[i].getYForce(), parallel[i].getYForce(),
                    pct * Math.abs(sequential[i].getYForce()), "body " + i + " Y force");
        }

        // collision detection: body 0 should have body 1 in its joinList either way
        assertNotNull(sequential[0].joinList);
        assertTrue(sequential[0].joinList.contains(sequential[1]));
        assertNotNull(parallel[0].joinList);
        assertTrue(parallel[0].joinList.contains(parallel[1]));

        // bodies 2 and 3 were never close to anything - no joinList on either version
        assertNull(sequential[2].joinList);
        assertNull(parallel[2].joinList);
        assertNull(sequential[3].joinList);
        assertNull(parallel[3].joinList);
    }

    @Test
    public void testParallelForceSumProducesFiniteResultsAtScale() {
        // comfortably above PARALLEL_FORCE_SUM_THRESHOLD (500), so Propagate() exercises the
        // parallel branch throughout its internal RK4 sub-steps
        int numBodies = 600;
        RandomSystem system = new RandomSystem(1200.0, numBodies, 10, -1.0, 0.1, 5.0, 500.0, 2.0,
                3.0, new Random(7));

        for (int step = 0; step < 5; step++) {
            system.Propagate();
        }

        assertTrue(system.bodies.size() <= numBodies, "collisions can only reduce body count");
        assertTrue(system.bodies.size() > 0, "shouldn't have culled/merged everything away");
        for (Body b : system.bodies) {
            assertTrue(Double.isFinite(b.getX()), "X must stay finite");
            assertTrue(Double.isFinite(b.getY()), "Y must stay finite");
            assertTrue(Double.isFinite(b.getU()), "U must stay finite");
            assertTrue(Double.isFinite(b.getV()), "V must stay finite");
        }
    }
}
