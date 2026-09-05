//
// DynamicSystem.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-04-28 by Bruce Jackson, bruce@jaxfam.org

package org.jaxfam.orbit;

/**
 * A DynamicSystem can calculate continous state derivatives for a given set of
 * state variables and input variables. It can be reset to some
 * internally-recorded initial conditions, and can be moved to any arbitary
 * other set of initial conditions
 * 
 * @author Bruce Jackson
 */
public interface DynamicSys {

    /**
     * Gets the state vector from a dynamic system
     * @return state vector with initial conditions
     */
    StateVector getIC();

    /**
     * Calculates the state derivatives for the system
     * @param u InputVector with current input values
     * @param x StateVector gives desired state for derivatives
     * @return StateVector containing time-rate-of-change of state values
     */
    StateVector getStateDeriv( InputVector u, StateVector x );

    /**
     * Load the given state vector into the system
     * @param x desired starting state vector
     */
    void loadStateVector( StateVector x );

    /**
     * Get the current states of the system
     * @return the current StateVector
     */
    StateVector getStateVector();

}
