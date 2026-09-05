/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jaxfam.orbit;

import java.util.ArrayList;
/**
 * Used for debugging; not linked into program
 * @author Bruce Jackson
 */
class TempSystem extends OrbitalSystem {

    /**
     * Default constructor
     */
    public TempSystem() {

        super();

        maxR = 3.0; /** maximum distance from [0,0] allowed without culling */
        bodies = new ArrayList<>(2);
        bodies.add(new Body(    0.0,  // X_IC_AU
                                0.0,  // Y_IC_AU
                                0.0,  // U_IC_m/s
                                0.0,  // V_ic_m/s
                                10.0,  // radius, earth radii
                                2.0,  // density, g/cm3
                                Color.yellow(),
                                1000  // path length
                                ));

        bodies.add(new Body(    0.0,  // X_IC_AU
                                -0.01,  // Y_IC_AU
                                0.0,  // U_IC_m/s
                                0.0,  // V_ic_m/s
                                5.0,  // radius, earth radii
                                2.0,  // density, g/cm3
                                Color.blue(),
                                1000  // path length
                                ));

        dt = 500.0;
        trailDecimation = 2; /* how many points to skip when remembering path */
        centeredBody = bodies.get(0);
    }
}
