package org.jaxfam.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Body methods that must tolerate a body with no
 * TrailingPath (path_length 0 or 1, e.g. one loaded from a hand-edited
 * OrbitalSystem JSON file with "trailLength": 0) rather than throwing a
 * NullPointerException.
 */
public class BodyTest {

    @Test
    public void testRecordPositionNoTrailDoesNotThrow() {
        Body body = new Body(1.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 0);
        assertDoesNotThrow(() -> body.recordPosition(0.0));
    }

    @Test
    public void testResetToICNoTrailDoesNotThrow() {
        Body body = new Body(1.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 0);
        body.setX(5.0);
        body.setY(6.0);
        body.setU(7.0);
        body.setV(8.0);
        assertDoesNotThrow(body::resetToIC);
        assertEquals(OrbitalSystem.AU2m, body.getX(), 1e-6 * OrbitalSystem.AU2m);
        assertEquals(0.0, body.getY(), 1e-6);
        assertEquals(0.0, body.getU(), 1e-6);
        assertEquals(0.0, body.getV(), 1e-6);
    }
}
