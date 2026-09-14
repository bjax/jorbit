//
// RungeKuttaKahan4Integrator.java
//
// Part of the orbital mechanics demonstrator program
//
// 2026 Written as a Kahan-compensated variant of RungeKutta4Integrator
//

package org.jaxfam.orbit;

/**
 * A variant of RungeKutta4Integrator that uses Kahan (compensated) summation for the final
 * X_0 + dt*X_dot_avg update each step - the one addition per call whose rounding error actually
 * accumulates over a long propagation, since every other StateVector built during a single
 * integrate() call (X_1, X_2, X_3, X_dot_avg) is transient and discarded at the end of that
 * call. The low-order bits lost each time that final sum is rounded back into a single double
 * are kept in this.compensation and folded into the next call's addition instead of simply
 * being dropped, at the cost of a few extra flops per state-vector element per step.
 *
 * @author Bruce Jackson
 */
public class RungeKuttaKahan4Integrator extends Integrator {

    /**
     * Per-element Kahan compensation, carried across successive integrate() calls (one per
     * Propagate()). This must be an instance field rather than a local: a RungeKuttaKahan4Integrator
     * is created once per OrbitalSystem and lives for the system's whole lifetime, so this is the
     * one place that can actually accumulate compensation across an entire propagation. Lazily
     * (re)sized to zero if the state vector's length changes, e.g. after a body is culled or two
     * bodies merge in a collision - at that point there's no meaningful compensation to carry
     * forward anyway.
     */
    private double[] compensation;

    public RungeKuttaKahan4Integrator( DynamicSys system ) {
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

        // propagate state vector by average over the whole step, using Kahan-compensated
        // summation so the rounding error lost each step is fed back into the next one instead
        // of simply discarded
        StateVector X_new = kahanAdd(X_0, StateVector.mult(X_dot_avg, deltaT));

        return X_new;
    }

    /**
     * Adds increment to base element-by-element using Kahan (compensated) summation: the
     * low-order bits lost when rounding each element's sum back into a single double are kept
     * in this.compensation and folded into the next call's addition for that element, rather
     * than being discarded every time.
     * @param base the current state vector (unmodified)
     * @param increment the delta to add to it (unmodified)
     * @return a new StateVector holding base + increment, compensated
     */
    StateVector kahanAdd(StateVector base, StateVector increment) {
        int n = base.length();
        if (compensation == null || compensation.length != n) {
            compensation = new double[n]; // zero-initialized; also handles a change in body count
        }
        StateVector out = new StateVector(base);
        for (int i = 0; i < n; i++) {
            double y = increment.get(i) - compensation[i];
            double baseI = base.get(i);
            double t = baseI + y;
            compensation[i] = (t - baseI) - y;
            out.set(i, t);
        }
        return out;
    }
}
