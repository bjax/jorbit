//
// TriaxSystem.java
//
// Part of the orbital mechanics demonstrator program
//
// 2026 Written for the startup scenario-selection prompt
//

package org.jaxfam.orbit;

import java.util.ArrayList;

/**
 * The "Triax" scenario, offered as a choice at startup: three equal-mass
 * bodies arranged in an equilateral triangle, each moving perpendicular to
 * its radius vector at a matching speed - a Lagrange three-body
 * relative-equilibrium configuration, so the triangle rotates rigidly about
 * the origin rather than the bodies flying apart or collapsing together.
 * @author Bruce Jackson
 */
class TriaxSystem extends OrbitalSystem {

    /**
     * Default constructor
     */
    public TriaxSystem() {

        super();

        maxR = 3.0; /** maximum distance from [0,0] allowed without culling */
        // set on the system itself (not just passed to each Body below) so saveToFile()/
        // loadFromFile() round-trip it correctly - previously this was only ever passed as a
        // literal to each Body constructor, leaving OrbitalSystem's own pathLength at its
        // default of 0, so a save always wrote "trailLength": 0 and a subsequent reload (R)
        // rebuilt every body with trail length 0 - i.e. no trail at all - even though the
        // original (pre-reload) bodies had real 1000-point trails
        pathLength = 1000;
        bodies = new ArrayList<>(3);
	double cos30 = Math.sqrt(3.0)/2.0;
        bodies.add(new Body(    0.,       // X_IC_AU
                                0.01,     // Y_IC_AU
                             -3*1000.0,   // U_IC_m/s
                                0,        // V_ic_m/s
                                5.0,      // radius, earth radii
                                2.0,      // density, g/cm3
                                Color.yellow(),
                                pathLength
                                ));

        bodies.add(new Body(cos30/100.0,  // X_IC_AU
                               -0.005,    // Y_IC_AU
                                3*500,    // U_IC_m/s
                         3*cos30*1000,    // V_ic_m/s
                                5.0,      // radius, earth radii
                                2.0,      // density, g/cm3
                                Color.blue(),
                                pathLength
                                ));

        bodies.add(new Body(-cos30/100.0, // X_IC_AU
                               -0.005,    // Y_IC_AU
                                3*500,    // U_IC_m/s
                         -3*cos30*1000,   // V_ic_m/s
                                5.0,      // radius, earth radii
                                2.0,      // density, g/cm3
                                Color.red(),
                                pathLength
                                ));

        dt = 3000.0;
        trailDecimation = 2; /* how many points to skip when remembering path */
        centeredBody = bodies.get(0);

        // the triangle's vertices sit exactly 0.01 AU from the origin (see the Y_IC_AU/cos30
        // terms above); Orbit.initial_m2pix (tuned for AU-scale systems like SolarSystem) would
        // render that as a barely-visible few-pixel cluster, so pick a default zoom that instead
        // puts each vertex a comfortable distance from screen center
        double triangleRadiusAU = 0.01;
        double desiredVertexPixelRadius = 250.0;
        defaultM2pix = desiredVertexPixelRadius / (triangleRadiusAU * AU2m);

        defaultHighViz = true;
    }
}
