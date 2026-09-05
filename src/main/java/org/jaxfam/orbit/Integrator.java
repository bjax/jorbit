//
// Integrator.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-05-03 by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;

/**
 * Provides an integration utility for use with a DynamicSystem.
 * This abstract class requires a subclass to define a concrete integrate()
 * method.
 *
 * @author Bruce Jackson
 */
abstract public class Integrator {

    protected DynamicSys sys;   /** the DynamicSystem we're to integrate */

    public Integrator( DynamicSys system ) {
        sys = system;
    }

    /**
     * The integrate method moves the system forward by deltaT seconds by
     * calling the DynamicSystem's getStateDeriv() method, sometimes multiple
     * times, per frame.
     * @param u  InputVector
     * @param deltaT step size, sec
     * @return newly-updated  StateVector
     */
    abstract public StateVector integrate( InputVector u, Double deltaT );

}
