package org.jaxfam.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Body.estimatePeriod() against synthetic points placed exactly on a true
 * Kepler orbit (solving Kepler's equation for each sample), including a notably
 * eccentric case whose angular rate is far from constant - a naive swept-ANGLE
 * average-rate estimate would be systematically wrong there, unlike the swept-AREA
 * (Kepler's second law) approach this method actually uses.
 */
public class PeriodEstimateTest {

    /**
     * A body with points placed exactly on a Kepler ellipse of the given shape, with the
     * focus at the origin and periapsis on the +X axis (angle 0) - the standard
     * focus-at-origin parametrization, via Newton-Raphson solution of Kepler's equation
     * (M = E - e sin E) at each sample time.
     * @param semiMajor      semi-major axis, m
     * @param eccentricity   0 (circle) to just under 1
     * @param period         true orbital period, seconds
     * @param fractionOfOrbit how much of one period the recorded arc covers (t=0 at periapsis)
     * @param numPoints      number of trail points to place, evenly spaced in time
     */
    private static Body bodyOnKeplerOrbit(double semiMajor, double eccentricity,
            double period, double fractionOfOrbit, int numPoints) {
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), numPoints + 10);
        double semiMinor = semiMajor * Math.sqrt(1.0 - eccentricity*eccentricity);
        for (int i = 0; i < numPoints; i++) {
            double frac = (double) i / (numPoints - 1) * fractionOfOrbit;
            double t = frac * period;
            double meanAnomaly = 2.0 * Math.PI * frac;

            double E = meanAnomaly;
            for (int iter = 0; iter < 50; iter++) {
                E -= (E - eccentricity*Math.sin(E) - meanAnomaly) / (1.0 - eccentricity*Math.cos(E));
            }

            double x = semiMajor * (Math.cos(E) - eccentricity); // focus at origin
            double y = semiMinor * Math.sin(E);
            body.setX(x);
            body.setY(y);
            body.recordPosition(t);
        }
        return body;
    }

    // Segment areas are computed from straight chords between sampled points, not the
    // true curved arc, so each segment under-counts its swept area by O(dTheta^2) -
    // negligible with enough points, but not with just a handful. These tests use dense
    // sampling specifically so that bias is small enough to demonstrate the underlying
    // formula converges to the true period, rather than picking a tolerance loose enough
    // to hide it.

    @Test
    public void testEstimatesPeriodForEccentricOrbit() {
        double semiMajor = 5.0e9, eccentricity = 0.6, truePeriod = 3.0e7;
        Body body = bodyOnKeplerOrbit(semiMajor, eccentricity, truePeriod, 0.9, 1000);

        Ellipse fit = body.fitEllipse();
        assertEquals(semiMajor, fit.semiMajor(), 1e-4 * semiMajor);

        double estimated = body.estimatePeriod(fit);
        assertEquals(truePeriod, estimated, 1e-3 * truePeriod);
    }

    @Test
    public void testEstimatesPeriodForFullEccentricOrbit() {
        double semiMajor = 2.0e9, eccentricity = 0.8, truePeriod = 1.0e6;
        Body body = bodyOnKeplerOrbit(semiMajor, eccentricity, truePeriod, 1.0, 1500);

        Ellipse fit = body.fitEllipse();
        double estimated = body.estimatePeriod(fit);
        assertEquals(truePeriod, estimated, 1e-3 * truePeriod);
    }

    @Test
    public void testEstimatesPeriodForCircularOrbit() {
        double semiMajor = 4.0e9, truePeriod = 8.64e5;
        Body body = bodyOnKeplerOrbit(semiMajor, 0.0, truePeriod, 0.75, 300);

        Ellipse fit = body.fitEllipse();
        double estimated = body.estimatePeriod(fit);
        assertEquals(truePeriod, estimated, 1e-3 * truePeriod);
    }

    @Test
    public void testThrowsWithoutEnoughTrailHistory() {
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 10);
        body.recordPosition(0);
        body.recordPosition(1);
        Ellipse placeholder = new Ellipse(0.0, 0.0, 1.0e9, 1.0e9, 0.0);
        assertThrows(IllegalStateException.class, () -> body.estimatePeriod(placeholder));
    }

    @Test
    public void testThrowsForNegligibleSweptArea() {
        // purely radial "motion" - every point on the same ray from the origin, so the
        // swept area relative to it (both candidate foci, for this circular placeholder
        // ellipse, coincide at the origin) is exactly zero, not just small
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 10);
        body.setX(1.0e9); body.setY(0.0); body.recordPosition(0);
        body.setX(1.2e9); body.setY(0.0); body.recordPosition(1);
        body.setX(1.5e9); body.setY(0.0); body.recordPosition(2);
        Ellipse placeholder = new Ellipse(0.0, 0.0, 1.0e9, 1.0e9, 0.0);
        assertThrows(IllegalStateException.class, () -> body.estimatePeriod(placeholder));
    }
}
