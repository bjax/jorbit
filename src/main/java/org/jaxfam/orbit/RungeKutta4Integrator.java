//
// RungeKutta4Integrator.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-05-03 by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;

/**
 * Provides an integration utility for use with a DynamicSystem.
 * This class performs the Runge-Kutta 4-th order integration
 * with four calls to the system's getStateDeriv() method per step.
 *
 * @author Bruce Jackson
 */
public class RungeKutta4Integrator extends Integrator {

    public RungeKutta4Integrator( DynamicSys system ) {
        super(system);
    }

    /**
     * The integrate method moves the system forward by deltaT seconds but
     * requires four calls to the system's getStateDeriv() method to do so.
     * @param u  InputVector
     * @param deltaT step size, sec
     * @return newly-updated  StateVector
     */
    @Override
    public StateVector integrate(InputVector u, Double deltaT) {

        // get initial state derivatives for present state
        StateVector X_0 = new StateVector(sys.getStateVector());
        StateVector X_dot_0 = sys.getStateDeriv(u, X_0);

        // advance state vector half-way to end of frame, using original slope, and get new derivatives
        StateVector X_1 = StateVector.add(X_0, StateVector.mult(X_dot_0, 0.5*deltaT));
        StateVector X_dot_1 = sys.getStateDeriv(u, X_1);

        // advance state vector half-way to end of frame, using middle slope, and get new derivatives
        StateVector X_2 = StateVector.add(X_0, StateVector.mult(X_dot_1, 0.5*deltaT));
        StateVector X_dot_2 = sys.getStateDeriv(u, X_2);

        // advance state vector end of frame, using second middle slope, and get new derivatives
        StateVector X_3 = StateVector.add(X_0, StateVector.mult(X_dot_2, deltaT));
        StateVector X_dot_3 = sys.getStateDeriv(u, X_3);

        // get the weighted average of the four slopes: 1/6*(xd0 + 2*xd1 + 2*xd2 + xd3)
        StateVector X_dot_avg = StateVector.mult(
                StateVector.add(
                    StateVector.add(X_dot_0,
                        StateVector.mult(X_dot_1, 2.0)
                    ),
                    StateVector.add(X_dot_3,
                        StateVector.mult(X_dot_2, 2.0)
                    )
                 ),
                1/6.0);

        // propagate state vector by average over the whole step
        StateVector X_new = StateVector.add(X_0, StateVector.mult(X_dot_avg, deltaT));

        return X_new;
    }
}