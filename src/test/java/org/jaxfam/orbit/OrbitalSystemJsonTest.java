package org.jaxfam.orbit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests OrbitalSystem.saveToFile()/loadFromFile() (schema/orbital-system.schema.json)
 * round-trip fidelity.
 */
public class OrbitalSystemJsonTest {

    static final double pct = 1e-12;

    // OrbitalSystem.dt is a static field shared across the whole test JVM; save/restore
    // it so setting it here doesn't leak into other test classes (which rely on its
    // default value of 0.0)
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
    public void testRoundTrip(@TempDir File tempDir) throws IOException {
        OrbitalSystem original = new OrbitalSystem();
        OrbitalSystem.dt = 1234.5;
        original.pathLength = 50;
        original.trailDecimation = 3;
        original.enforce_R_limit = true;
        original.maxR = 7.5;
        original.colorByOrbitShape = true;
        original.bodies = new ArrayList<>();

        Body named = new Body(1.5, -2.5, 100.0, -200.0, 2.0, 5.5, new Color(1f, 0.5f, 0.25f), 50);
        named.setName("Earth");
        Body unnamed = new Body(-3.0, 4.0, -10.0, 20.0, 0.5, 1.0, new Color(0f, 0f, 1f), 50);

        original.bodies.add(named);
        original.bodies.add(unnamed);

        File file = new File(tempDir, ".JOS.json");
        original.saveToFile(file);

        OrbitalSystem loaded = OrbitalSystem.loadFromFile(file);

        assertEquals(1234.5, OrbitalSystem.dt, pct);
        assertEquals(50, loaded.pathLength);
        assertEquals(3, loaded.trailDecimation);
        assertTrue(loaded.enforce_R_limit);
        assertEquals(7.5, loaded.maxR, pct);
        assertTrue(loaded.colorByOrbitShape);
        assertEquals(2, loaded.bodies.size());

        Body loadedNamed = loaded.bodies.get(0);
        assertEquals("Earth", loadedNamed.getName());
        assertEquals(named.getX(), loadedNamed.getX(), pct * OrbitalSystem.AU2m);
        assertEquals(named.getY(), loadedNamed.getY(), pct * OrbitalSystem.AU2m);
        assertEquals(named.getU(), loadedNamed.getU(), pct * Math.abs(named.getU()));
        assertEquals(named.getV(), loadedNamed.getV(), pct * Math.abs(named.getV()));
        assertEquals(named.getRadius(), loadedNamed.getRadius(), pct * named.getRadius());
        assertEquals(named.getDensity_g_cm3(), loadedNamed.getDensity_g_cm3(),
                pct * named.getDensity_g_cm3());
        assertEquals(named.getColor().r(), loadedNamed.getColor().r(), 1e-6f);
        assertEquals(named.getColor().g(), loadedNamed.getColor().g(), 1e-6f);
        assertEquals(named.getColor().b(), loadedNamed.getColor().b(), 1e-6f);

        Body loadedUnnamed = loaded.bodies.get(1);
        assertNull(loadedUnnamed.getName());
        assertEquals(unnamed.getX(), loadedUnnamed.getX(), pct * OrbitalSystem.AU2m);

        // loadFromFile() suggests the first body as a default view center - actual interactive
        // view-centering is Orbit's own concern now, not OrbitalSystem's (see
        // OrbitalSystem.defaultCenteredBody's javadoc)
        assertSame(loadedNamed, loaded.defaultCenteredBody);
    }

    @Test
    public void testUnnamedBodyOmitsNameKey(@TempDir File tempDir) throws IOException {
        OrbitalSystem system = new OrbitalSystem();
        OrbitalSystem.dt = 1.0;
        system.bodies = new ArrayList<>();
        system.bodies.add(new Body(0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Color.white(), 10));

        File file = new File(tempDir, ".JOS.json");
        system.saveToFile(file);

        JSONObject root = new JSONObject(Files.readString(file.toPath()));
        JSONObject body = root.getJSONArray("bodies").getJSONObject(0);
        assertFalse(body.has("name"));
    }

    @Test
    public void testMissingColorByOrbitShapeKeyDefaultsFalse(@TempDir File tempDir) throws IOException {
        // a hand-written or pre-existing save file from before colorByOrbitShape was added to
        // the schema won't have this key at all; loading it must not throw, and must default
        // to false (matching every scenario except RandomSystem, which explicitly opts in)
        String json = "{\"dt\": 1.0, \"bodies\": [{"
                + "\"positionAU\": [0.0, 0.0], \"velocityMS\": [0.0, 0.0],"
                + "\"radiusEarthRadii\": 1.0, \"densityGCm3\": 1.0, \"color\": [1.0, 1.0, 1.0]"
                + "}]}";
        File file = new File(tempDir, ".JOS.json");
        Files.writeString(file.toPath(), json);

        OrbitalSystem loaded = OrbitalSystem.loadFromFile(file);
        assertFalse(loaded.colorByOrbitShape);
    }

    @Test
    public void testLoadMissingFileThrows(@TempDir File tempDir) {
        File missing = new File(tempDir, "does-not-exist.json");
        assertThrows(NoSuchFileException.class, () -> OrbitalSystem.loadFromFile(missing));
    }
}
