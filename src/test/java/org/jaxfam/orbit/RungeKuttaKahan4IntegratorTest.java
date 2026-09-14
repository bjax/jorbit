package org.jaxfam.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies RungeKuttaKahan4Integrator.kahanAdd(), the Kahan-compensated summation used for the
 * final X_0 + dt*X_dot_avg update - the one addition in integrate() whose rounding error
 * actually accumulates over a long propagation, since every other StateVector built during a
 * single integrate() call is discarded at the end of that call.
 */
public class RungeKuttaKahan4IntegratorTest {

    @Test
    public void testKahanAddAccumulatesBelowUlpIncrementsThatPlainSummationDrops() {
        // sys is never touched by kahanAdd(), only by integrate()
        RungeKuttaKahan4Integrator integrator = new RungeKuttaKahan4Integrator(null);

        // smaller than ulp(1.0) (~2.22e-16): "plainSum += increment" rounds right back to the
        // unchanged value every single time, so a naive running total makes zero progress
        double increment = 1.0e-16;
        int steps = 100_000;

        StateVector kahanSum = new StateVector(1);
        kahanSum.set(0, 1.0);
        StateVector delta = new StateVector(1);
        delta.set(0, increment);

        double plainSum = 1.0;
        for (int i = 0; i < steps; i++) {
            kahanSum = integrator.kahanAdd(kahanSum, delta);
            plainSum = plainSum + increment;
        }

        double expected = 1.0 + steps * increment;

        assertEquals(1.0, plainSum, 0.0, "plain summation should have made no progress at all");
        assertEquals(expected, kahanSum.get(0), 1e-12 * expected,
                "Kahan-compensated summation should recover the true accumulated total");
    }

    @Test
    public void testKahanAddMatchesPlainAdditionWhenNoPrecisionIsLost() {
        RungeKuttaKahan4Integrator integrator = new RungeKuttaKahan4Integrator(null);

        StateVector base = new StateVector(3);
        base.set(0, 10.0);
        base.set(1, -5.0);
        base.set(2, 0.0);

        StateVector increment = new StateVector(3);
        increment.set(0, 2.5);
        increment.set(1, 1.5);
        increment.set(2, 3.0);

        StateVector result = integrator.kahanAdd(base, increment);

        assertEquals(12.5, result.get(0), 1e-15);
        assertEquals(-3.5, result.get(1), 1e-15);
        assertEquals(3.0, result.get(2), 1e-15);
    }

    @Test
    public void testKahanAddResetsCompensationWhenVectorLengthChanges() {
        RungeKuttaKahan4Integrator integrator = new RungeKuttaKahan4Integrator(null);

        StateVector base3 = new StateVector(3);
        StateVector inc3 = new StateVector(3);
        inc3.set(0, 1.0);
        integrator.kahanAdd(base3, inc3); // establishes a length-3 compensation array

        // a differently-sized vector (e.g. after a body is culled or merges in a collision)
        // must not throw, or silently reuse stale compensation values from the wrong-length array
        StateVector base2 = new StateVector(2);
        StateVector inc2 = new StateVector(2);
        inc2.set(0, 5.0);
        inc2.set(1, 7.0);
        StateVector result = integrator.kahanAdd(base2, inc2);

        assertEquals(5.0, result.get(0), 1e-15);
        assertEquals(7.0, result.get(1), 1e-15);
    }
}
