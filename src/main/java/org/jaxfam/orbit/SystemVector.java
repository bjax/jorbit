//
// SystemVector.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-04-28 by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;

/**
 * Defines an object to be used as the input, output, state and state derivative
 * vectors associated with a DynamicSystem. Basically stores 'length' number of
 * values in an array of doubles. Provides basic manipulation methods.
 * @author Bruce Jackson
 */
public class SystemVector {

    private double[] x; /** the state values */

    /**
     * Constructor; instantiates the vector of defined length
     * @param length How long to make the vector
     */
    SystemVector(int length) {
        x = new double[length];
    }

    /**
     * Copy constructor: builds a deep copy of the argument
     * @param in SystemVector to copy
     */
    public SystemVector( SystemVector in ) {
        x = new double[in.length()];
        for (int i=0;i<in.length(); i++) {
            this.x[i] = in.get(i);
        }
    }

    /**
     * Fetch the length of the vector
     * @return number of values contained herein
     */
    public int length() { return x.length; }

    /**
     * Sets the value of an element at a given offset from the beginning of the
     * SystemVector (0-based)
     * @param offset a 0-based offset from start of the vector
     * @param value  the double value to store at that offset
     */
    void set(int offset, double value) {
        x[offset] = value;
    }

    /**
     * Returns the value of an element at a given offset from the beginning of the
     * SystemVector (0-based)
     * @param offset a 0-based offset from start of the vector
     * @return the double value of the offset+1th value
     */
    double get(int offset) {
        return x[offset];
    }

    /**
     * Multiplies all the values by the given factor
     * @param factor the scaling factor to apply to each value
     */
    public void scale(double factor) {
        for (int i = 0; i < x.length; i++ ) {
            x[i] = x[i]*factor;
        }
    }

    /**
     * Adds the values of the provided SystemVector to our corresponding
     * values (element by element)
     * @param in the SystemVector to add to our values
     */
    public void plus( SystemVector in ) {
        if (x.length != in.length())
            throw new IndexOutOfBoundsException("Attempting to add state vectors of different length");
        for (int i = 0; i < x.length; i++) {
            x[i] = x[i] + in.get(i);
        }
    }

    /**
     * Class method to multiply a vector by a factor
     * @param x the vector to be scaled
     * @param factor the multiplication factor
     * @return a new vector containing the scaled input vector values
     */
    public static SystemVector mult(SystemVector x, Double factor) {
        SystemVector out = new SystemVector(x);
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
    public static SystemVector add(SystemVector x, SystemVector y) {
        if (x.length() != y.length())
            throw new IndexOutOfBoundsException(
                    "Attempting to add state vectors of different length");
        SystemVector out = new SystemVector(x);
        for (int i = 0; i < x.length(); i++) {
            out.set(i, x.get(i) + y.get(i));
        }
        return out;
    }

}
