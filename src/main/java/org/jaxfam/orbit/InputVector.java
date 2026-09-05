//
// InputVector.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-04-28 by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;


/**
 * Subclasses SystemVector to define an object to be used as the input vector 
 * associated with a DynamicSystem. Basically stores 'length'
 * number of input values in an array of doubles. 
 * Provides basic manipulation methods.
 * @author Bruce Jackson
 */
public class InputVector extends SystemVector {

    /**
     * Constructor
     * @param length of new InputVector
     */
    InputVector(int length) {
        super(length);
    }

    /**
     * Copy constructor; makes a new InputVector from the provided one
     * @param in the InputVector to copy
     */
     public InputVector( InputVector in ) {
        super( in );
    }

    /**
     * Class method to multiply a InputVector by a factor
     * @param x the InputVector to be scaled
     * @param factor the multiplication factor
     * @return a new StateVector containing the scaled input vector values
     */
    public static InputVector mult(InputVector x, Double factor) {
        InputVector out = new InputVector(x);
        for (int i = 0; i < x.length(); i++) {
            out.set(i, x.get(i)*factor);
        }
        return out;
    }

    /**
     * Class method to add two vectors element-by-element
     * @param x first vector
     * @param y second vector
     * @return a new vector containing the sum of the two vectors
     */
    public static InputVector add(InputVector x, InputVector y) {
        if (x.length() != y.length())
            throw new IndexOutOfBoundsException(
                    "Attempting to add state vectors of different length");
        InputVector out = new InputVector(x);
        for (int i = 0; i < x.length(); i++) {
            out.set(i, x.get(i) + y.get(i));
        }
        return out;
    }

}
