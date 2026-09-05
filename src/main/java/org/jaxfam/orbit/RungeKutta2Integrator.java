//
// RungeKutta4Integrator.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-05-03 by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;

/**
 * Provides an integration utility for use with a DynamicSystem.
 * This class performs the Kutta 2nd order integration
 * with two calls to the system's getStateDeriv() method per step.
 *
 * @author Bruce Jackson
 */
public class RungeKutta2Integrator extends Integrator {

    public RungeKutta2Integrator( DynamicSys system ) {
        super(system);
    }

    /**
     * The integrate method moves the system forward by deltaT seconds but
     * requires two calls to the system's getStateDeriv() method to do so.
     * @param u  InputVector
     * @param deltaT step size, sec
     * @return newly-updated  StateVector
     */
    @Override
    public StateVector integrate(InputVector u, Double deltaT) {

        // get initial state derivatives for present state
        StateVector X_0 = new StateVector(sys.getStateVector());
        StateVector X_dot_0 = sys.getStateDeriv(u, X_0);

        // advance state vector to end of frame and get new derivatives
        StateVector X_1 = StateVector.add(X_0, StateVector.mult(X_dot_0, deltaT));
        StateVector X_dot_1 = sys.getStateDeriv(u, X_1);

        // get the average of the two end slopes
        StateVector X_dot_avg = StateVector.mult(StateVector.add(X_dot_0,X_dot_1), 0.5);

        // propagate state vector by average over the whole step
        StateVector X_new = StateVector.add(X_0, StateVector.mult(X_dot_avg, deltaT));
                
        return X_new;
    }
}