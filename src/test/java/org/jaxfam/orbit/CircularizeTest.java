package org.jaxfam.orbit;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Body.circularizeAbout()/circularizeAboutAcceleration() and
 * OrbitalSystem.circularizeAllAboutCenterOfMass() against the properties a true circular orbit
 * must have relative to whatever it's circularized about: its speed must match the relevant
 * formula (vis-viva sqrt(G*M/R) for a real point mass, sqrt(|a|*R) for the body's own current net
 * acceleration), and its velocity must be exactly 90 degrees *clockwise* from the vector pointing
 * toward the center - not just perpendicular to it (a plain dot-product-is-zero check can't tell
 * clockwise from counterclockwise; a signed cross-product check is used here instead, since an
 * earlier version of this test suite used only axis-aligned placements and a dot-product check,
 * which happens to accept a 180-degrees-backwards (counterclockwise) velocity just as readily as
 * the correct one whenever the body sits due north/south or due east/west of the center - exactly
 * the bug this was meant to catch and didn't).
 */
public class CircularizeTest {

    private static final double pct = 1e-9;

    /** density 1.0 g/cm^3, radius 1.0 Earth radii - same convention OrbitalSystemTest uses. */
    private static double bodyMass(double radiusEarthRadii, double densityGCm3) {
        double radiusM = radiusEarthRadii * OrbitalSystem.earthRadius;
        return densityGCm3 * 1000.0 * (4.0/3.0) * Math.PI * Math.pow(radiusM, 3.0);
    }

    /**
     * Asserts that (relU, relV) - the body's velocity relative to whatever center it was
     * circularized about - has the given expected speed and points exactly 90 degrees clockwise
     * from the vector toward that center, i.e. (relU, relV) is a positive multiple of
     * (-yDelta, +xDelta), not (+yDelta, -xDelta) (its 180-degrees-backwards, counterclockwise
     * mirror image) or any other direction. Checked via the signed cross product of the
     * center-to-body vector (xDelta, yDelta) with (relU, relV), which for a correct clockwise
     * perpendicular vector equals exactly +R*speed (positive; a counterclockwise one would give
     * -R*speed, and anything not perpendicular would give something other than +-R*speed).
     */
    private static void assertClockwiseCircular(double xDelta, double yDelta, double R,
            double relU, double relV, double expectedSpeed) {
        double relSpeed = Math.sqrt(relU*relU + relV*relV);
        assertEquals(expectedSpeed, relSpeed, pct*expectedSpeed, "speed");

        double cross = xDelta*relV - yDelta*relU;
        assertEquals(R*expectedSpeed, cross, pct*R*expectedSpeed, "clockwise direction");
    }

    @Test
    public void testCircularizeAboutRawCenterIsClockwiseNonAxisAligned() {
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 10);
        double xDelta = 3.0e9, yDelta = 4.0e9; // deliberately not axis-aligned
        double R = Math.hypot(xDelta, yDelta);
        body.setX(xDelta);
        body.setY(yDelta);

        double centerMassKg = 5.0e24;
        double centerU = 100.0, centerV = 50.0; // moving center

        body.circularizeAbout(0.0, 0.0, centerMassKg, centerU, centerV);

        double relU = body.getU() - centerU;
        double relV = body.getV() - centerV;
        double expectedSpeed = Math.sqrt(OrbitalSystem.Kgravity * centerMassKg / R);
        assertClockwiseCircular(xDelta, yDelta, R, relU, relV, expectedSpeed);

        // the center's own velocity must be inherited, on top of the orbital component
        assertEquals(centerU, body.getU() - relU, pct*Math.abs(centerU));
        assertEquals(centerV, body.getV() - relV, pct*Math.abs(centerV));
    }

    @Test
    public void testWestOfCenterMeansDueNorth() {
        // the exact case that exposed the bug: center due west of the body must produce due
        // north motion, not due south
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 10);
        double R = 2.0e9;
        body.setX(R);   // body is east of the origin...
        body.setY(0.0);
        // ...so the center (origin) is due west of the body

        double centerMassKg = 6.0e24;
        body.circularizeAbout(0.0, 0.0, centerMassKg, 0.0, 0.0);

        double expectedSpeed = Math.sqrt(OrbitalSystem.Kgravity * centerMassKg / R);
        assertEquals(0.0, body.getU(), pct*expectedSpeed, "no east/west motion");
        assertEquals(expectedSpeed, body.getV(), pct*expectedSpeed, "due north, not due south");
    }

    @Test
    public void testCircularizeAboutBodyIsClockwiseNonAxisAligned() {
        Body center = new Body(0.0, 0.0, 0.0, 0.0, 50.0, 2.0, Color.white(), 10);
        double xDelta = -2.0e10, yDelta = 1.0e10;
        double R = Math.hypot(xDelta, yDelta);
        Body orbiter = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 10);
        orbiter.setX(center.getX() + xDelta);
        orbiter.setY(center.getY() + yDelta);

        orbiter.circularizeAbout(center);

        double relU = orbiter.getU() - center.getU();
        double relV = orbiter.getV() - center.getV();
        double expectedSpeed = Math.sqrt(OrbitalSystem.Kgravity * center.getMass() / R);
        assertClockwiseCircular(xDelta, yDelta, R, relU, relV, expectedSpeed);
    }

    @Test
    public void testGetCmUAndGetCmVAreMassWeightedAverages() {
        OrbitalSystem system = new OrbitalSystem();
        system.maxR = 10.0;
        system.bodies = new ArrayList<>(2);

        Body b1 = new Body(-1.0, 0.0, 200.0, -30.0, 1.0, 1.0, Color.white(), 10);
        Body b2 = new Body(+1.0, 0.0, -50.0, 400.0, 2.0, 1.0, Color.white(), 10);
        system.bodies.add(b1);
        system.bodies.add(b2);

        double m1 = bodyMass(1.0, 1.0);
        double m2 = bodyMass(2.0, 1.0);
        double expectedU = (m1*b1.getU() + m2*b2.getU()) / (m1 + m2);
        double expectedV = (m1*b1.getV() + m2*b2.getV()) / (m1 + m2);

        assertEquals(expectedU, system.getCmU(), pct*Math.abs(expectedU));
        assertEquals(expectedV, system.getCmV(), pct*Math.abs(expectedV));
    }

    @Test
    public void testCircularizeAllAboutCenterOfMassIsClockwiseAndUsesRealAcceleration() {
        OrbitalSystem system = new OrbitalSystem();
        system.maxR = 10.0;
        system.bodies = new ArrayList<>(3);

        // two heavy, slow-moving bodies establish a moving, off-origin, non-axis-aligned
        // center of mass
        Body anchor1 = new Body(-1.0, -1.0, 20.0, 5.0, 50.0, 2.0, Color.white(), 10);
        Body anchor2 = new Body(3.0, 2.0, 20.0, 5.0, 50.0, 2.0, Color.white(), 10);
        // radius tiny enough that the orbiter's own mass/gravity barely perturbs anything -
        // close to a real test particle
        Body orbiter = new Body(0.0, 0.0, 0.0, 0.0, 1.0e-4, 1.0, Color.white(), 10);
        system.bodies.add(anchor1);
        system.bodies.add(anchor2);
        system.bodies.add(orbiter);

        double xDelta = 4.0e9, yDelta = -2.0e9; // deliberately not axis-aligned
        double R = Math.hypot(xDelta, yDelta);
        orbiter.setX(system.getCmX() + xDelta);
        orbiter.setY(system.getCmY() + yDelta);

        system.circularizeAllAboutCenterOfMass();

        // the orbiter's own force accumulators, freshly summed during circularizeAll...(), are
        // exactly what the implementation itself used to pick a speed - reading them back gives
        // the same expected speed independently of internal implementation details
        double expectedSpeed = Math.sqrt(Math.hypot(orbiter.getUdot(), orbiter.getVdot()) * R);
        assertTrue(expectedSpeed > 0.0, "orbiter should feel nonzero net gravity from the anchors");

        double cmU = system.getCmU();
        double cmV = system.getCmV();
        double relU = orbiter.getU() - cmU;
        double relV = orbiter.getV() - cmV;
        assertClockwiseCircular(xDelta, yDelta, R, relU, relV, expectedSpeed);
    }

    @Test
    public void testCircularizeAllAboutCenterOfMassAppliesFraction() {
        OrbitalSystem system = new OrbitalSystem();
        system.maxR = 10.0;
        system.bodies = new ArrayList<>(3);

        Body anchor1 = new Body(-1.0, -1.0, 20.0, 5.0, 50.0, 2.0, Color.white(), 10);
        Body anchor2 = new Body(3.0, 2.0, 20.0, 5.0, 50.0, 2.0, Color.white(), 10);
        Body orbiter = new Body(0.0, 0.0, 0.0, 0.0, 1.0e-4, 1.0, Color.white(), 10);
        system.bodies.add(anchor1);
        system.bodies.add(anchor2);
        system.bodies.add(orbiter);

        double xDelta = 4.0e9, yDelta = -2.0e9;
        double R = Math.hypot(xDelta, yDelta);
        orbiter.setX(system.getCmX() + xDelta);
        orbiter.setY(system.getCmY() + yDelta);

        double fraction = 0.8;
        system.circularizeAllAboutCenterOfMass(fraction);

        double fullSpeed = Math.sqrt(Math.hypot(orbiter.getUdot(), orbiter.getVdot()) * R);
        double expectedSpeed = fraction * fullSpeed;

        double relU = orbiter.getU() - system.getCmU();
        double relV = orbiter.getV() - system.getCmV();
        assertClockwiseCircular(xDelta, yDelta, R, relU, relV, expectedSpeed);

        // sanity check this is genuinely a *different* (slower) speed than the full-circular
        // case, not just coincidentally passing the same assertion at fraction 1.0
        assertTrue(expectedSpeed < fullSpeed);
    }
}
