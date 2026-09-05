//
// TrailingPath.java
//
// Part of the orbital mechanics demonstrator program
//
// 2011-05-05 Written by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;

/**
 * Circular buffer to record past values of X and Y vs. time
 * @author Bruce Jackson
 */
public class TrailingPath {

    int MAX_LENGTH;         /** maximum length of the buffer   */
    double[] t_, x_, y_;    /** the arrays for data storage    */
    int start_index;        /** offset to oldest data point    */
    int next_index;         /** offset to where data goes next */
    int last_index;         /** offset to previous data point  */
    boolean wrapped;        /** flag to indicate we've wrapped at least once */

    /**
     * Constructor
     * @param capacity defines the buffer size
     */
    public TrailingPath(int capacity) {
        MAX_LENGTH = capacity;
        t_ = new double[MAX_LENGTH];
        x_ = new double[MAX_LENGTH];
        y_ = new double[MAX_LENGTH];
        start_index = 0;
        next_index = 0;
        last_index = MAX_LENGTH;
        wrapped = false;
    }

    /**
     * Store away the provided data as the freshest data in the buffer,
     * wrapping and overwriting older data if necessary.
     * @param t time stamp, seconds
     * @param x X position, meters
     * @param y Y position, meters
     */
    public void addPt(double t, double x, double y) {
        t_[next_index] = t;
        x_[next_index] = x;
        y_[next_index] = y;
        last_index = next_index;
        next_index++;
        if (next_index >= MAX_LENGTH ) {
            next_index = 0;
        }
        if (wrapped) { // we've wrapped around
            start_index++;
        }
        if (next_index <= start_index) { // detect first wrap
            wrapped = true;
        }
        if (start_index >= MAX_LENGTH ) {
            start_index = 0;
        }
    }

    /**
     * Returns the number of points stored
     * @return the number of stored points, up to MAX_LENGTH
     */
    public int length() {
        int len = next_index;
        if (wrapped) { //
            len = MAX_LENGTH;
        }
        return len;
    }

    /**
     * Local method that converts an absolute index (365th point)
     * into the correct position in the possibly wrapped, and probably shorter,
     * circular buffers.
     * @param index absolute index since point 0
     * @return offset into our local array
     */
    protected int getOffset( int index ) {
        if (index >= length())
            throw new IndexOutOfBoundsException();
        int offset = index;
        if (wrapped) {
            offset = index + start_index;
            if (offset >= MAX_LENGTH) {
                offset -= MAX_LENGTH;
            }
        }
        return offset;
    }

    /**
     * Fetches the time stamp for the given absolute index
     * @param index absolute index since point 0
     * @return value of time for record number index
     */
    public double getT( int index ) {
        return t_[getOffset(index)];
    }

    /**
     * Fetches the X position for the given absolute index
     * @param index absolute index since point 0
     * @return value of X for record number index
     */
    public double getX( int index ) {
        return x_[getOffset(index)];
    }

    /**
     * Fetches the Y position for the given absolute index
     * @param index absolute index since point 0
     * @return value of Y for record number index
     */
    public double getY( int index ) {
        return y_[getOffset(index)];
    }

}
