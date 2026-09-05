package org.jaxfam.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Body.fitEllipse() against synthetic points placed exactly on a known
 * ellipse - since the points are noise-free, the least-squares fit should recover
 * the original parameters essentially exactly.
 */
public class EllipseFitTest {

    private static Body bodyOnEllipse(double centerX, double centerY,
            double semiMajor, double semiMinor, double angle, int numPoints) {
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), numPoints + 10);
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        for (int i = 0; i < numPoints; i++) {
            double t = 2.0 * Math.PI * i / numPoints;
            double localX = semiMajor * Math.cos(t);
            double localY = semiMinor * Math.sin(t);
            double x = centerX + localX * cosA - localY * sinA;
            double y = centerY + localX * sinA + localY * cosA;
            body.setX(x);
            body.setY(y);
            body.recordPosition(i);
        }
        return body;
    }

    /** cos/sin of 2*angle, so angle and angle+pi (the same physical ellipse) compare equal. */
    private static void assertSameAxisDirection(double expected, double actual) {
        assertEquals(Math.cos(2 * expected), Math.cos(2 * actual), 1e-6);
        assertEquals(Math.sin(2 * expected), Math.sin(2 * actual), 1e-6);
    }

    @Test
    public void testRecoversAxisAlignedEllipse() {
        double cx = 3.0e9, cy = -2.0e9, a = 5.0e9, b = 2.0e9, angle = 0.0;
        Body body = bodyOnEllipse(cx, cy, a, b, angle, 30);

        Ellipse fit = body.fitEllipse();

        assertEquals(cx, fit.centerX(), 1e-3 * a);
        assertEquals(cy, fit.centerY(), 1e-3 * a);
        assertEquals(a, fit.semiMajor(), 1e-3 * a);
        assertEquals(b, fit.semiMinor(), 1e-3 * a);
        assertSameAxisDirection(angle, fit.angle());
    }

    @Test
    public void testRecoversRotatedEllipse() {
        double cx = -1.0e9, cy = 4.0e9, a = 6.0e9, b = 1.5e9, angle = 0.7;
        Body body = bodyOnEllipse(cx, cy, a, b, angle, 40);

        Ellipse fit = body.fitEllipse();

        assertEquals(cx, fit.centerX(), 1e-3 * a);
        assertEquals(cy, fit.centerY(), 1e-3 * a);
        assertEquals(a, fit.semiMajor(), 1e-3 * a);
        assertEquals(b, fit.semiMinor(), 1e-3 * a);
        assertSameAxisDirection(angle, fit.angle());
    }

    @Test
    public void testSemiMajorAlwaysAtLeastSemiMinor() {
        // constructed with the "wrong" axis order (first arg smaller) to confirm
        // fitEllipse() normalizes semiMajor >= semiMinor, adjusting angle to match
        double cx = 0.0, cy = 0.0, small = 1.0e9, large = 4.0e9, angle = 1.2;
        Body body = bodyOnEllipse(cx, cy, small, large, angle, 30);

        Ellipse fit = body.fitEllipse();

        assertTrue(fit.semiMajor() >= fit.semiMinor());
        assertEquals(large, fit.semiMajor(), 1e-3 * large);
        assertEquals(small, fit.semiMinor(), 1e-3 * large);
        // the true major axis here is the "small"-labeled parameter rotated an extra 90 degrees
        assertSameAxisDirection(angle + Math.PI / 2.0, fit.angle());
    }

    @Test
    public void testThrowsWithoutEnoughTrailHistory() {
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 10);
        body.recordPosition(0);
        body.recordPosition(1);
        assertThrows(IllegalStateException.class, body::fitEllipse);
    }

    @Test
    public void testThrowsForHyperbolicTrail() {
        // points on a hyperbola (x^2 - y^2 = const), not an ellipse
        Body body = new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 20);
        double[] xs = {2, 3, 4, 5, 6, 7};
        for (int i = 0; i < xs.length; i++) {
            double x = xs[i] * 1e9;
            double y = Math.sqrt(x*x - 1e18);
            body.setX(x);
            body.setY(y);
            body.recordPosition(i);
        }
        assertThrows(IllegalStateException.class, body::fitEllipse);
    }
}
