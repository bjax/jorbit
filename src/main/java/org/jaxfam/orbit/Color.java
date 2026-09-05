//
// Color.java
//
// Part of the orbital mechanics demonstrator program
//
// 2011-05-04 Written by Bruce Jackson, bruce@jaxfam.org


package org.jaxfam.orbit;

/**
 * Color specification for OpenGL drawing
 * @author Bruce Jackson, bruce@jaxfam.org
 */
public class Color {
    private float r, g, b;


    /**
     * Constructor for Color
     * @param red  Red component     (0.0 - 1.0)
     * @param green Green component  (0.0 - 1.0)
     * @param blue Blue component    (0.0 - 1.0)
     */
    public Color( float red, float green, float blue ) {
        assert(red >= 0.0);
        assert(red <= 1.0);
        assert(green >= 0.0);
        assert(green <= 1.0);
        assert(blue >= 0.0);
        assert(blue <= 1.0);
        r = red;
        g = green;
        b = blue;
    }

    /**
     * Accessor for red component
     * @return red color component (0.0 - 1.0)
     */
    float r() { return r; }

    /**
     * Accessor for green component
     * @return green color component (0.0 - 1.0)
     */
    float g() { return g; }

    /**
     * Accessor for blue component
     * @return red blue component (0.0 - 1.0)
     */
    float b() { return b; }

    /**
     * Static method defines indicated color
     * @return black color
     */
    static public Color black() {
        return new Color((float) 0.0, (float) 0.0, (float) 0.0);
    }

    /**
     * Static method defines indicated color
     * @return white color
     */
    static public Color white() {
        return new Color((float) 1.0, (float) 1.0, (float) 1.0);
    }

    /**
     * Static method defines indicated color
     * @return gray color
     */
    static public Color gray() {
        return new Color((float) 0.5, (float) 0.5, (float) 0.5);
    }

    /**
     * Static method defines indicated color
     * @return red color
     */
    static public Color red() {
        return new Color((float) 1.0, (float) 0.0, (float) 0.0);
    }

    /**
     * Static method defines indicated color
     * @return green color
     */
    static public Color green() {
        return new Color((float) 0.0, (float) 1.0, (float) 0.0);
    }

    /**
     * Static method defines indicated color
     * @return blue color
     */
    static public Color blue() {
        return new Color((float) 0.0, (float) 0.0, (float) 1.0);
    }

    /**
     * Static method defines indicated color
     * @return pink color
     */
    static public Color pink() {
        return new Color((float) 0.737255, (float) 0.560784, (float) 0.560784);
    }

    /**
     * Static method defines indicated color
     * @return yellow color
     */
    static public Color yellow() {
        return new Color((float) 1.0, (float) 1.0, (float) 0.0 );
    }

    /**
     * Static method defines indicated color
     * @return cyan color
     */
    static public Color cyan() {
        return new Color((float) 0.0, (float) 1.0, (float) 1.0 );
    }

    /**
     * Static method defines indicated color
     * @return magenta color
     */
    static public Color magenta() {
        return new Color((float) 1.0, (float) 0.0, (float) 1.0 );
    }

     /**
     * Static method defines indicated color
     * @return orange color
     */
   static public Color orange() {
        return new Color((float) 1.0, (float) 0.5, (float) 0.0 );
    }

     /**
     * Static method defines indicated color
     * @return darkGray color
     */
   static public Color darkGray() {
        return new Color((float) 0.25, (float) 0.25, (float) 0.25);
    }

    /**
     * Static method defines indicated color
     * @return lightBlue color
     */
    static public Color lightBlue() {
        return new Color((float) 0.4, (float) 0.7, (float) 1.0);
    }

    /**
     * Static method defines indicated color
     * @return lightRed color
     */
    static public Color lightRed() {
        return new Color((float) 1.0, (float) 0.4, (float) 0.4);
    }

}
