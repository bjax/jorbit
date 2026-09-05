//
// Euler1Integrator.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-05-03 by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;

/**
 * Provides an integration utility for use with a DynamicSystem.
 * This class performs the Euler 1st order integration
 * with one call to the system's getStateDeriv() method per step;
 * however, stability and accuracy suffer as a result.
 *
 * @author Bruce Jackson
 */
public class Euler1Integrator extends Integrator {

    public Euler1Integrator( DynamicSys system ) {
        super(system);
    }

    /**
     * The integrate method moves the system forward by deltaT seconds with
     * only one call to the system's getStateDeriv() method to do so.
     * @param u  InputVector
     * @param deltaT step size, sec
     * @return newly-updated  StateVector
     */
    @Override
    public StateVector integrate(InputVector u, Double deltaT) {
        StateVector X0 = new StateVector(sys.getStateVector());
        StateVector xd = sys.getStateDeriv(u, X0);
        StateVector X1 = StateVector.add( X0, StateVector.mult( xd, deltaT ));
        return X1;
    }

}
