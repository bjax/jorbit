//
// StateVector.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-04-28 by Bruce Jackson, bruce@jaxfam.org
package org.jaxfam.orbit;

/**
 * Subclasses SystemVector to define an object to be used as the state and state
 * derivative vectors associated with a DynamicSystem. Basically stores 'length'
 * number of state values in an array of doubles.
 * Provides basic manipulation methods.
 * @author Bruce Jackson
 */
public class StateVector extends SystemVector {

    /**
     * Constructor
     * @param length of new SystemVector
     */
    StateVector(int length) {
        super(length);
    }

    /**
     * Copy constructor; makes a new StateVector from the provided one
     * @param in the StateVector to copy
     */
    public StateVector(StateVector in) {
        super(in);
    }

    /**
     * Class method to multiply a StateVector by a factor
     * @param x the StateVector to be scaled
     * @param factor the multiplication factor
     * @return a new StateVector containing the scaled input vector values
     */
    public static StateVector mult(StateVector x, Double factor) {
        StateVector out = new StateVector(x);
        for (int i = 0; i < x.length(); i++) {
            out.set(i, x.get(i) * factor);
        }
        return out;
    }

    /**
     * Class method to add two vectors element-by-element
     * @param x first vector
     * @param y second vector
     * @return a new vector containing the sum of the two vectors
     */
    public static StateVector add(StateVector x, StateVector y) {
        if (x.length() != y.length()) {
            throw new IndexOutOfBoundsException(
                    "Attempting to add state vectors of different length");
        }
        StateVector out = new StateVector(x);
        for (int i = 0; i < x.length(); i++) {
            out.set(i, x.get(i) + y.get(i));
        }
        return out;
    }
}
