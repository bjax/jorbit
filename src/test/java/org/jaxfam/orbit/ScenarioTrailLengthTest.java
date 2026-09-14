package org.jaxfam.orbit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards against the class of bug found in TriaxSystem: a scenario subclass that hardcodes a
 * trail length literal directly into each Body(...) constructor call but never mirrors it onto
 * OrbitalSystem's own pathLength field. saveToFile() serializes pathLength (not anything read
 * off an individual Body), so any mismatch is invisible until a save/reload round trip silently
 * rebuilds every body with the wrong (usually zero) trail capacity - exactly what happened after
 * pressing 'S' then 'R' in the Triax scenario. Checked here directly against each body's actual
 * TrailingPath capacity, for every scenario reachable from the startup prompt, so a future
 * scenario subclass that repeats the same mistake fails a test instead of only failing silently
 * after a save/reload.
 */
public class ScenarioTrailLengthTest {

    // OrbitalSystem.dt is a static field shared across the whole test JVM; SolarSystem's
    // constructor (used below) sets it as a side effect, so save/restore it here the same way
    // OrbitalSystemJsonTest does - otherwise it leaks into unrelated test classes that rely on
    // its default value of 0.0.
    double savedDt;

    @BeforeEach
    public void saveDt() {
        savedDt = OrbitalSystem.dt;
    }

    @AfterEach
    public void restoreDt() {
        OrbitalSystem.dt = savedDt;
    }

    private static void assertPathLengthMatchesBodyTrailCapacity(OrbitalSystem system) {
        assertTrue(system.pathLength > 1,
                "test scenarios are expected to be constructed with a real trail length");
        for (Body body : system.bodies) {
            TrailingPath trail = body.getTrailingPath();
            assertNotNull(trail, "body's trail must be allocated when pathLength > 1 "
                    + "(system.pathLength=" + system.pathLength + ")");
            assertEquals(system.pathLength, trail.MAX_LENGTH,
                    "body's actual trail capacity must match OrbitalSystem.pathLength, "
                    + "since that's the only value saveToFile()/loadFromFile() round-trips");
        }
    }

    @Test
    public void testSolarSystemPathLengthMatchesBodyTrails() {
        assertPathLengthMatchesBodyTrailCapacity(new SolarSystem(3600.0, 500));
    }

    @Test
    public void testRandomSystemPathLengthMatchesBodyTrails() {
        assertPathLengthMatchesBodyTrailCapacity(
                new RandomSystem(3600.0, 20, 300, 0.0, 0.5, 2.0, 1000.0, 1.0, 5.0));
    }

    @Test
    public void testTriaxSystemPathLengthMatchesBodyTrails() {
        assertPathLengthMatchesBodyTrailCapacity(new TriaxSystem());
    }
}
