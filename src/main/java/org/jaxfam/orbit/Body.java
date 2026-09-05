//
// SolarSystem.java
//
// Part of the orbital mechanics demonstrator program
//
// Originally written 2001 by Bruce Jackson, bruce@jaxfam.org
// Ported to Mac OSX Project Builder 020327 EBJ
// Ported to NetBeans 2011-04-21 EBJ
// Moved this class out of SolarSystem.java 2011-04-28 EBJ
package org.jaxfam.orbit;

import java.util.ArrayList;
import static org.lwjgl.opengl.GL11.*;

public class Body implements DynamicSys {


    /* in the following definitions, CM is Center of Mass */
    private StateVector IC;     /** initial conditions: X, Y, U, V, mass_kg       */
    double X;                   /** inertial position of body CM in the X axis, m */
    double Y;                   /** inertial position of body CM in the Y axis, m */
    double mass_kg;             /** mass of body, kg */
    double xForce;              /** sum of all forces in the X direction, N */
    double yForce;              /** sum of all forces in the Y direction, N */
    double U;                   /** inertial velocity of body CM in x axis, m/s */
    double V;                   /** inertial velocity of body CM in Y axis, m/s */
    double radius_m;              /** radius of body, m */
    double dens_kg_m3;          /** density kg/m^3 */
    Color  color;               /** color of disk */
    private Color origColor;    /** color assigned at construction; restored by
                                     updateOrbitColor() when not elliptical    */
    TrailingPath trail;         /** time history of CM position */
    ArrayList<Body> joinList;   /** list of impacting bodies (else null) */
    boolean absorbed;           /** flag indicates we've been absorbed by another body */
    boolean isHighlighted;      /** set/cleared for every body at once by V's "highlight all"
                                     toggle when no body is selected (Orbit.toggleHighlight()) */
    boolean selected;           /** sticky, single-selection: set by clicking this body, cleared
                                     by clicking empty space or by V while a body is selected    */
    boolean hovered;            /** transient, single-hover: set while the mouse cursor is over
                                     this body, cleared as soon as it moves off                 */
    private String name;        /** optional display name, e.g. "Earth"; null if unnamed */

    final double DEG_TO_RAD = 3.14159265/180.0;  /** Convert degrees to radians */
    static int NUM_STATES = 5; /** X, Y, U, V, mass_kg */


    /**
     * Fully-specified constructor with all fields
     * @param X_ic_AU     Initial position of center-of-mass (CM) in X axis
     * @param Y_ic_AU     Initial position of center-of-mass (CM) in & axis
     * @param U_ic        Initial inertial velocity of center-of-mass (CM) in X direction
     * @param V_ic        Initial inertial velocity of center-of-mass (CM) in Y direction
     * @param radius_re   Radius of sphere in earth radii
     * @param dens_g_cm3  Density of planet, g/cm^3
     * @param theColor    Color of body sphere
     * @param path_length How many previous positions to remember
     */
    public Body(
            double X_ic_AU,
            double Y_ic_AU,
            double U_ic,
            double V_ic,
            double radius_re, 
            double dens_g_cm3,
            Color theColor,
            int path_length ) {
        absorbed   = false;
        joinList   = null;
        X          = SolarSystem.AU2m * X_ic_AU;
        Y          = SolarSystem.AU2m * Y_ic_AU;
        U          = U_ic;
        V          = V_ic;
        radius_m   = radius_re*OrbitalSystem.earthRadius;
        xForce     = 0.;
        yForce     = 0.;
        dens_kg_m3 = dens_g_cm3*1000.0;
        color      = theColor;
        origColor  = theColor;
        trail      = null;
        if (path_length > 1) {
            trail      = new TrailingPath( path_length );
        }
        this.calculateMass();

        // save initial conditions
        this.saveToIC();
    }

    /**
     * Solar System constructor
     * @param orbital_radius_AU  Size of orbit in astronomical units
     * @param mass_me            Mass of body in Earth masses
     * @param radius_re          Radius of sphere in Earth radii
     * @param theColor           Color of sphere
     * @param pathLength         How many previous positions to remember
     */
    public Body(double orbital_radius_AU, double mass_me, double radius_re,
            Color theColor, int pathLength) //, double planet_radius_re)
    {
        // for this constructor, calculate density from mass and radius
        this(0., -orbital_radius_AU,
                0., 0.,
                radius_re, 1.0,
                theColor, pathLength);
        this.mass_kg = mass_me*OrbitalSystem.earthMass;
        this.calculateDensity();
    }

    /**
     * Access function for this body's optional display name
     * @return the name, or null if unnamed
     */
    public String getName() {
        return name;
    }

    /**
     * Sets this body's optional display name
     * @param name the name, e.g. "Earth" (null to clear it)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Access function for X
     * @return X inertial position of center-of-mass, m
     */
    public double getX() {
        return this.X;
    }

    /**
     * Access function for Y
     * @return Y inertial position of center-of-mass, m
     */
    public double getY() {
        return this.Y;
    }

    /**
     * Get total inertial position from center of frame
     * @return R distance from center of frame, meters
     */

    public double getR() {
        return Math.sqrt(Math.pow(X,2.0) + Math.pow(Y,2.0));
    }

    /**
     * Get total inertial distance from center of frame in AU
     * @return R_AU distance from center of frame, AU
     */
    public double getR_AU() {
        return getR()/OrbitalSystem.AU2m;
    }

    /**
     * Access function for X velocity
     * @return X-axis inertial velocity, m/s
     */
    public double getU() {
        return this.U;
    }

    /**
     * Access function for Y velocity
     * @return Y-axis inertial velocity, m/s
     */
    public double getV() {
        return this.V;
    }

    /**
     * Setter function for X position
     * @param x Inertial X-axis position, m
     */
    public void setX(double x) {
        this.X = x;
    }

    /**
     * Setter function for Y position
     * @param y Inertial Y-axis position, m
     */
    public void setY(double y) {
        this.Y = y;
    }

    /**
     * Setter function for X-axis inertial velocity
     * @param u X-axis inertial velocity, m/s
     */
    public void setU(double u) {
        this.U = u;
    }

    /**
     * Setter function for Y-axis inertial velocity
     * @param v Y-axis inertial velocity, m/s
     */
    public void setV(double v) {
        this.V = v;
    }

    /**
     * Access for X force
     * @return X-axis force
     */
    public double getXForce() {
        return this.xForce;
    }

    /**
     * Access for Y force
     * @return Y-axis force
     */
    public double getYForce() {
        return this.yForce;
    }

    /**
     * Calculate X-axis inertial acceleration
     * @return X-axis inertial acceleration, m/s/s
     */
    public double getUdot() {
        return this.xForce / this.mass_kg;
    }

    /**
     * Calculate Y-axis inertial acceleration
     * @return Y-axis inertial acceleration, m/s/s/
     */
    public double getVdot() {
        return this.yForce / this.mass_kg;
    }

    /**
     * Access function for body mass
     * @return body mass, kg
     */
    public double getMass() {
        return this.mass_kg;
    }

    /**
     * Sets the color of the planet
     * @param theColor
     */
    public void setColor(Color theColor) {
        this.color = theColor;
    }

    /**
     * Access function
     * @return the Color of the planet
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Access function
     * @return radius of the planet
     */
    public double getRadius() {
        return radius_m;
    }

    /**
     * Access function
     * @return density of the planet, g/cm^3 (water = 1.0)
     */
    public double getDensity_g_cm3() {
        return dens_kg_m3 / 1000.0;
    }

    /**
     * Zeros the force summation values
     */
    public void resetForces() {
        this.xForce = 0.;
        this.yForce = 0.;
    }

    /**
     * Sets X and Y inertial velocities (U and V) so the body
     * is in a circular orbit about the indicated central body
     * @param centerMass the body about which to circularize the orbit
     */
    public void circularizeAbout(Body centerMass) {
        double xDelta = this.getX() - centerMass.getX();
        double yDelta = this.getY() - centerMass.getY();
        double R = java.lang.Math.sqrt(xDelta * xDelta + yDelta * yDelta);
        double Vel_ic = 0.;
        this.U = centerMass.getU();
        this.V = centerMass.getV();
        if (R > 0.) {
            Vel_ic = java.lang.Math.sqrt(SolarSystem.Kgravity * centerMass.getMass() / R);

            this.U = this.U - Vel_ic * yDelta / R;
            this.V = this.V - Vel_ic * xDelta / R;
        }
    }

    /**
     * Add the effective gravitational 'force' components between two bodies
     * @param body1 first of two bodies to sum gravitational attraction
     * @param body2 the other body to determine the 'force' to add
     */
    public static void addForces( Body body1, Body body2 ) {

        double deltaX = body2.getX() - body1.getX();
        double deltaY = body2.getY() - body1.getY();

        double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        double sumRadii = body1.getRadius() + body2.getRadius();
        if (dist < sumRadii) { // close enough to clump together?
            dist = sumRadii;
            if (body1.joinList == null)
                body1.joinList = new ArrayList<>();
            // only add once to join list; this gets called in multi-step integrator
            // more times than the joining function in OrbitalSystem.Propagate()
            // we don't absorb encroaching bodies until this runtime loop finishes
            if (!body1.joinList.contains( body2 )) {
                body1.joinList.add( body2 );
            }
        }

        // gravitational law
        double Force = OrbitalSystem.Kgravity * body1.getMass() * body2.getMass()
                / (dist * dist);

        double FX = Force * deltaX / dist;
        double FY = Force * deltaY / dist;

        body1.addXForce(  FX );
        body1.addYForce(  FY );

        // save time - add reverse to other body - only works if we call a pair once
        body2.addXForce( -FX );
        body2.addYForce( -FY );
    }


    /**
     * Draw the body and it's trail relative to some central body
     * @param drawTrail if true, draw the trail (skipped regardless if this body has none,
     *                   e.g. one constructed/loaded with a trail length of 0 or 1)
     * @param minR minimum radius of sphere to allow
     * @param centeredBody the central body about which to draw
     */
    public void glRender( boolean drawTrail, float minR, Body centeredBody ) {
        float x;
        float y;
        float x0 = (float) X;
        float y0 = (float) Y;
        boolean highlighted = isHighlighted || selected || hovered;

        TrailingPath centralBodyPath = null;

        if (centeredBody != null) {
            x0 -= (float) centeredBody.getX();
            y0 -= (float) centeredBody.getY();
            centralBodyPath = centeredBody.getTrailingPath();
        }

        if (drawTrail && trail != null) {
            if (highlighted) {
                glColor3f(color.r()*0.9f, color.g()*0.9f, color.b()*0.9f);
            } else {
                glColor3f(color.r()*0.3f, color.g()*0.3f, color.b()*0.3f);
            }
            glBegin(GL_LINE_STRIP);
            int trailLength = trail.length();
            for(int i=0; i < trailLength; i++) {
                x = (float) trail.getX(i);
                y = (float) trail.getY(i);
                if (centralBodyPath != null) {
                    x -= centralBodyPath.getX(i);
                    y -= centralBodyPath.getY(i);
                } else {
                    if (centeredBody != null) {
                        // body has no path; use it's current position
                        x -= centeredBody.getX();
                        y -= centeredBody.getY();
                    }
                }
                glVertex2f( x, y );
            }
            glVertex2f ( x0, y0 );
            glEnd();
        }

        glColor3f(color.r(), color.g(), color.b());

        glPushMatrix();
        glTranslatef((float) x0, (float) y0, (float) 0.0);

        float R = (float) radius_m;
        if (highlighted)
            minR = 4.0f*minR;
        if (R < minR) {
            R = minR;
        }
        drawDisk(R, 24);
        glPopMatrix();
    }

    /**
     * Draws a filled disk of the given radius centered on the current origin,
     * replacing the GLU Disk primitive (removed in LWJGL 3's util package).
     * @param radius radius of the disk, in the current coordinate units
     * @param slices number of triangles used to approximate the circle
     */
    private static void drawDisk(float radius, int slices) {
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(0f, 0f);
        for (int i = 0; i <= slices; i++) {
            double angle = 2.0 * Math.PI * i / slices;
            glVertex2f(radius * (float) Math.cos(angle), radius * (float) Math.sin(angle));
        }
        glEnd();
    }

    /**
     * Saves current state into the initial condition state vector
     */
    public void saveToIC() {
        IC = new StateVector(NUM_STATES);
        IC.set(0, X);
        IC.set(1, Y);
        IC.set(2, U);
        IC.set(3, V);
        IC.set(4, mass_kg);
    }

    /**
     * puts body back at initial conditions; zeros out
     * trailing path
     */
    public void resetToIC() {
        X = IC.get(0);
        Y = IC.get(1);
        U = IC.get(2);
        V = IC.get(3);
        mass_kg = IC.get(4);
        calculateRadius();
        if (trail != null) {
            trail = new TrailingPath( trail.length() );
        }
    }


    /**
     * Intended to return the initial conditions
     * @return a StateVector containing the initial conditions
     */
    public StateVector getIC() {
        return IC;
    }

    /**
     * Calculate the state derivative vector (accelerations and velocities) for
     * the given input and state vectors
     * @param u input vector about which to obtain state derivatives
     * @param x state vector about which to obtain state derivatives
     * @return state derivatives in a state vector (Xdot, Ydot, Udot, Vdot)
     */
    public StateVector getStateDeriv(InputVector u, StateVector x) {
        // our parent should have loaded the state vector and called sumForces()
        StateVector xd = new StateVector(NUM_STATES);
        xd.set(0, this.getU());
        xd.set(1, this.getV());
        xd.set(2, this.getUdot());
        xd.set(3, this.getVdot());
        xd.set(4, 0.0); // mass_kg
        return xd;
    }

    /**
     * Sets the states of the body (velocity and position) to a given set of values
     * @param x The StateVector to load
     */
    public void loadStateVector(StateVector x) {
        this.loadStateVector(x, 0);
    }

    /**
     * Sets the states of the body (velocity and position) to a given set of values,
     * with an offset into the given StateVector. Sequence is X, Y, U, V, mass
     * @param x the StateVector to load
     * @param offset offset to our states
     */
    public void loadStateVector(StateVector x, int offset ) {
        this.setX(x.get(offset  ));
        this.setY(x.get(offset+1));
        this.setU(x.get(offset+2));
        this.setV(x.get(offset+3));
        mass_kg = x.get(offset+4);
        calculateRadius();
    }

    /**
     * Returns the current state vector
     * @return StateVector (order is X, Y, U, V)
     */
    public StateVector getStateVector() {
        StateVector x = new StateVector(NUM_STATES);
        x.set(0, this.getX());
        x.set(1, this.getY());
        x.set(2, this.getU());
        x.set(3, this.getV());
        x.set(4, mass_kg);
        return x;
    }

    /**
     * Save our current inertial X and Y in our TrailingPath. Does nothing if this
     * body has no trail (e.g. one constructed/loaded with a trail length of 0 or 1).
     * @param t Time stamp (sec) to associate with current point
     */
    public void recordPosition(double t) {
        if (trail != null) {
            trail.addPt(t, getX(), getY());
        }
    }

    /**
     * Obtain the TrailingPath for this body
     * @return the TrailingPath record for this body
     */
    public TrailingPath getTrailingPath() {
        return this.trail;
    }

    /**
     * Fits the general conic Ax^2 + Bxy + Cy^2 + Dx + Ey + F = 0 through five
     * points sampled (evenly spaced) from this body's recorded trail, and
     * returns its discriminant B^2 - 4AC. This classifies the shape of the
     * path purely from observed positions, with no need to know the central
     * body's mass:
     * <pre>
     *   discriminant &lt; 0   ellipse (circle if also A == C and B == 0)
     *   discriminant == 0   parabola
     *   discriminant &gt; 0   hyperbola
     * </pre>
     * Five points determine a conic only up to an unknown common scale
     * factor, but that factor's square is always non-negative, so the SIGN
     * of the returned value is meaningful even though its magnitude isn't.
     * This assumes the recorded motion is actually following a conic (a true
     * two-body/inverse-square trajectory); points perturbed by other bodies
     * will not fit one exactly.
     * @return B^2 - 4AC for the conic through five points spread across the
     *         recorded trail
     * @throws IllegalStateException if fewer than five positions have been
     *         recorded in the trail
     */
    public double findDiscriminant() {
        if (trail == null || trail.length() < 5) {
            throw new IllegalStateException(
                    "Need at least 5 recorded trail points to fit a conic");
        }

        // each point gives one row [x^2, xy, y^2, x, y, 1] of the homogeneous
        // system M*(A,B,C,D,E,F)^T = 0; only A, B and C are needed here
        double[][] rows = new double[5][6];
        int lastIndex = trail.length() - 1;
        for (int i = 0; i < 5; i++) {
            int index = (int) Math.round(i * lastIndex / 4.0); // spread evenly across the trail
            double x = trail.getX(index);
            double y = trail.getY(index);
            rows[i][0] = x*x;
            rows[i][1] = x*y;
            rows[i][2] = y*y;
            rows[i][3] = x;
            rows[i][4] = y;
            rows[i][5] = 1.0;
        }

        // the null-space vector (A,B,C,D,E,F) is given, up to a common scale,
        // by the signed 5x5 minors of the 5x6 matrix above (same trick as the
        // 2D cross product / 3D scalar triple product, generalized)
        double a =  minorDeterminant(rows, 0);
        double b = -minorDeterminant(rows, 1);
        double c =  minorDeterminant(rows, 2);

        return b*b - 4.0*a*c;
    }

    /**
     * Determinant of the 5x5 matrix formed by deleting one column from a
     * 5x6 matrix.
     */
    private static double minorDeterminant(double[][] rows, int skipColumn) {
        double[][] minor = new double[5][5];
        for (int r = 0; r < 5; r++) {
            int col = 0;
            for (int c = 0; c < 6; c++) {
                if (c == skipColumn) continue;
                minor[r][col++] = rows[r][c];
            }
        }
        return determinant(minor);
    }

    /**
     * Determinant of a square matrix, via recursive cofactor expansion along
     * the first row. Fine for the small (5x5) matrices used here; not
     * intended for general-purpose or performance-critical use.
     */
    private static double determinant(double[][] m) {
        int n = m.length;
        if (n == 1) {
            return m[0][0];
        }
        double det = 0.0;
        for (int col = 0; col < n; col++) {
            double[][] sub = new double[n-1][n-1];
            for (int r = 1; r < n; r++) {
                int subCol = 0;
                for (int c = 0; c < n; c++) {
                    if (c == col) continue;
                    sub[r-1][subCol++] = m[r][c];
                }
            }
            double sign = (col % 2 == 0) ? 1.0 : -1.0;
            det += sign * m[0][col] * determinant(sub);
        }
        return det;
    }

    /**
     * Fits the general conic Ax^2 + Bxy + Cy^2 + Dx + Ey + F = 0 to every
     * point currently recorded in this body's trail, in an algebraic
     * least-squares sense, and returns its discriminant B^2 - 4AC. Unlike
     * {@link #findDiscriminant()}, which fits exactly through 5 points, this
     * uses all recorded points, so it averages out perturbations from other
     * bodies (N-body noise) rather than fitting them exactly. Classified the
     * same way as findDiscriminant():
     * <pre>
     *   discriminant &lt; 0   ellipse (circle if also A == C and B == 0)
     *   discriminant == 0   parabola
     *   discriminant &gt; 0   hyperbola
     * </pre>
     * F is normalized to -1 (valid unless the fitted conic happens to pass
     * through the origin, which won't happen for a real, non-colliding
     * orbit), turning the equation into Ax^2+Bxy+Cy^2+Dx+Ey = 1 per point -
     * an overdetermined linear system, solved via the normal equations. With
     * exactly 5 points this reduces to the same exact fit as
     * {@link #findDiscriminant()}.
     * @return B^2 - 4AC for the least-squares conic fit through the trail
     * @throws IllegalStateException if fewer than five positions have been
     *         recorded in the trail, or if the recorded points are too
     *         degenerate (e.g. collinear) to determine a conic
     */
    public double findDiscriminantLeastSquares() {
        double[] coeffs = fitConicCoefficients();
        double a = coeffs[0];
        double b = coeffs[1];
        double c = coeffs[2];
        return b*b - 4.0*a*c;
    }

    /**
     * Fits the general conic Ax^2 + Bxy + Cy^2 + Dx + Ey + F = 0 (F normalized to -1)
     * to every point currently recorded in this body's trail, in an algebraic
     * least-squares sense - the shared fit behind both
     * {@link #findDiscriminantLeastSquares()} and {@link #fitEllipse()}. See
     * {@link #findDiscriminantLeastSquares()}'s javadoc for the method (normal
     * equations over an overdetermined Ax^2+Bxy+Cy^2+Dx+Ey=1 system).
     * @return {A, B, C, D, E} (F is fixed at -1, not returned)
     * @throws IllegalStateException if fewer than five positions have been
     *         recorded in the trail, or if the recorded points are too
     *         degenerate (e.g. collinear) to determine a conic
     */
    private double[] fitConicCoefficients() {
        if (trail == null || trail.length() < 5) {
            throw new IllegalStateException(
                    "Need at least 5 recorded trail points to fit a conic");
        }

        int n = trail.length();
        double[][] w = new double[n][5]; // columns: x^2, xy, y^2, x, y
        for (int i = 0; i < n; i++) {
            double x = trail.getX(i);
            double y = trail.getY(i);
            w[i][0] = x*x;
            w[i][1] = x*y;
            w[i][2] = y*y;
            w[i][3] = x;
            w[i][4] = y;
        }

        // normal equations (W^T W) z = W^T b, with b = all-ones (F normalized to -1)
        double[][] wtw = new double[5][5];
        double[] wtb = new double[5];
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                double sum = 0.0;
                for (int i = 0; i < n; i++) {
                    sum += w[i][r] * w[i][c];
                }
                wtw[r][c] = sum;
            }
            double sum = 0.0;
            for (int i = 0; i < n; i++) {
                sum += w[i][r]; // * b[i], and b[i] == 1
            }
            wtb[r] = sum;
        }

        return solveLinearSystem(wtw, wtb);
    }

    /**
     * Fits an ellipse to this body's currently recorded trail (via {@link
     * #fitConicCoefficients()}) and reduces the general conic to standard form:
     * center, semi-major/minor axis lengths, and rotation angle. Recomputed from
     * scratch each call against whatever the trail currently holds, so calling
     * this once per frame - the intended use - yields an ellipse that continually
     * reshapes as the trail grows and the orbit is perturbed by other bodies,
     * rather than a one-time fit locked to the orbit's shape at some past moment.
     * <p>
     * The reduction: the center solves the conic's gradient to zero; the
     * rotation angle {@code atan2(B, A-C)/2} is the one that eliminates the xy
     * cross-term when the axes are substituted in; the two resulting
     * coefficients along those rotated axes give the semi-axis lengths once
     * divided into the conic's value at the center. Standard analytic geometry
     * for reducing a conic to canonical form - see e.g. any conic sections
     * reference for the derivation.
     * @return the fitted ellipse
     * @throws IllegalStateException if fewer than five trail points are
     *         recorded, the points are too degenerate to fit a conic (see
     *         {@link #fitConicCoefficients()}), the fitted conic isn't
     *         currently an ellipse (discriminant &gt;= 0), or the fit is too
     *         numerically degenerate to reduce to a real ellipse
     */
    public Ellipse fitEllipse() {
        double[] coeffs = fitConicCoefficients();
        double a = coeffs[0];
        double b = coeffs[1];
        double c = coeffs[2];
        double d = coeffs[3];
        double e = coeffs[4];
        final double f = -1.0;

        double discriminant = b*b - 4.0*a*c;
        if (discriminant >= 0.0) {
            throw new IllegalStateException(
                    "Trail does not currently fit an ellipse (discriminant " + discriminant + ")");
        }

        // center: where the conic's gradient (2Ax+By+D, Bx+2Cy+E) is zero
        double denom = 4.0*a*c - b*b; // = -discriminant, positive here
        double x0 = (b*e - 2.0*c*d) / denom;
        double y0 = (b*d - 2.0*a*e) / denom;

        // value of the (translated) conic at the center
        double fc = a*x0*x0 + b*x0*y0 + c*y0*y0 + d*x0 + e*y0 + f;

        // rotation eliminating the xy cross-term, and the conic's coefficients
        // along that axis and its perpendicular (see fitEllipse()'s javadoc)
        double theta = 0.5 * Math.atan2(b, a - c);
        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);
        double lambda1 = a*cosT*cosT + b*sinT*cosT + c*sinT*sinT;
        double lambda2 = a*sinT*sinT - b*sinT*cosT + c*cosT*cosT;

        double semiA = Math.sqrt(-fc / lambda1);
        double semiB = Math.sqrt(-fc / lambda2);
        if (!Double.isFinite(semiA) || !Double.isFinite(semiB) || semiA <= 0.0 || semiB <= 0.0) {
            throw new IllegalStateException("Ellipse fit is too numerically degenerate to use");
        }

        // keep semiMajor as the longer axis, adjusting theta to match
        if (semiA < semiB) {
            double tmp = semiA; semiA = semiB; semiB = tmp;
            theta += Math.PI / 2.0;
        }

        return new Ellipse(x0, y0, semiA, semiB, theta);
    }

    /**
     * Estimates this body's orbital period from its recorded trail and an already-fitted
     * ellipse (e.g. from {@link #fitEllipse()}), via Kepler's second law: a real orbit
     * sweeps equal areas, relative to its focus, in equal times. Swept AREA accrues at a
     * genuinely constant rate even for an eccentric orbit (faster near closest approach,
     * slower near farthest, but always the same area per unit time) - unlike swept angle,
     * which would only give an average-rate approximation, biased for any orbit that
     * isn't a circle.
     * <p>
     * The ellipse's center isn't its focus. Of its two candidate foci - offset from the
     * center by {@code c = sqrt(semiMajor^2 - semiMinor^2)} along the major axis - only
     * one is physically real (the attracting body sits there); the other is empty space.
     * Rather than requiring the caller to say which, this computes the area-sweep rate
     * relative to both and keeps whichever is more nearly constant (lowest variance)
     * across the trail's recorded segments: that's exactly what Kepler's second law
     * predicts for the true focus and not for the empty one, so which is real is
     * determined from the same purely observational trail data as the rest of this
     * orbit-shape-fitting family - no need to know where any body's mass actually is.
     * Once the real focus is picked, total swept area divided by total elapsed time gives
     * the areal velocity, and dividing that into the whole ellipse's area
     * ({@code pi * semiMajor * semiMinor}) gives the period.
     * @param ellipse the already-fitted ellipse (its center and axes) to derive foci from
     * @return the estimated orbital period, seconds
     * @throws IllegalStateException if fewer than three trail points are recorded, or the
     *         recorded points don't yet show enough swept area to extrapolate from
     */
    public double estimatePeriod(Ellipse ellipse) {
        if (trail == null || trail.length() < 3) {
            throw new IllegalStateException(
                    "Need at least 3 recorded trail points to estimate a period");
        }

        double semiMajor = ellipse.semiMajor();
        double semiMinor = ellipse.semiMinor();
        double c = Math.sqrt(semiMajor*semiMajor - semiMinor*semiMinor);
        double cosT = Math.cos(ellipse.angle());
        double sinT = Math.sin(ellipse.angle());

        SweptSegments segsFocus1 = sweptAreaSegments(
                ellipse.centerX() + c*cosT, ellipse.centerY() + c*sinT);
        SweptSegments segsFocus2 = sweptAreaSegments(
                ellipse.centerX() - c*cosT, ellipse.centerY() - c*sinT);
        SweptSegments segs = (sweptAreaRateVariance(segsFocus1) <= sweptAreaRateVariance(segsFocus2))
                ? segsFocus1 : segsFocus2;

        double totalArea = 0.0;
        double totalTime = 0.0;
        for (int i = 0; i < segs.areas().length; i++) {
            totalArea += segs.areas()[i];
            totalTime += segs.dts()[i];
        }
        if (totalArea <= 0.0 || totalTime <= 0.0) {
            throw new IllegalStateException("Not enough swept area recorded to estimate a period");
        }

        double ellipseArea = Math.PI * semiMajor * semiMinor;
        double arealVelocity = totalArea / totalTime;
        return ellipseArea / arealVelocity;
    }

    /** Per-segment (consecutive trail point pair) swept areas and time steps. */
    private record SweptSegments(double[] areas, double[] dts) {}

    /**
     * Swept area (relative to the given candidate focus) and elapsed time for every
     * consecutive pair of points in the trail - the raw ingredients {@link
     * #estimatePeriod(Ellipse)} needs both to judge which of the two candidate foci is
     * real (via {@link #sweptAreaRateVariance}) and, for the winning one, to total up
     * into an overall areal velocity.
     */
    private SweptSegments sweptAreaSegments(double focusX, double focusY) {
        int n = trail.length();
        double[] areas = new double[n - 1];
        double[] dts = new double[n - 1];
        double prevX = trail.getX(0) - focusX;
        double prevY = trail.getY(0) - focusY;
        double prevT = trail.getT(0);
        for (int i = 1; i < n; i++) {
            double x = trail.getX(i) - focusX;
            double y = trail.getY(i) - focusY;
            double t = trail.getT(i);
            // signed area of the triangle (focus, previous point, this point), via the
            // usual 2D cross product; magnitude only matters here, so just take the abs
            areas[i - 1] = 0.5 * Math.abs(prevX*y - x*prevY);
            dts[i - 1] = t - prevT;
            prevX = x;
            prevY = y;
            prevT = t;
        }
        return new SweptSegments(areas, dts);
    }

    /**
     * Variance of area-sweep rate (area/time) across a set of segments - low for a focus
     * where Kepler's second law holds (the real one), high for one where it doesn't (the
     * empty one). See {@link #estimatePeriod(Ellipse)}.
     */
    private static double sweptAreaRateVariance(SweptSegments segs) {
        int n = segs.areas().length;
        double[] rates = new double[n];
        double mean = 0.0;
        for (int i = 0; i < n; i++) {
            rates[i] = (segs.dts()[i] != 0.0) ? segs.areas()[i] / segs.dts()[i] : 0.0;
            mean += rates[i];
        }
        mean /= n;
        double variance = 0.0;
        for (double rate : rates) {
            variance += (rate - mean) * (rate - mean);
        }
        return variance / n;
    }

    /**
     * Solves the NxN linear system Ax = b via Gaussian elimination with
     * partial pivoting. Fine for the small (5x5) systems used here; not
     * intended for general-purpose or performance-critical use.
     */
    private static double[] solveLinearSystem(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int r = 0; r < n; r++) {
            System.arraycopy(a[r], 0, m[r], 0, n);
            m[r][n] = b[r];
        }

        for (int col = 0; col < n; col++) {
            // partial pivot: swap in the row with the largest entry in this column
            int pivotRow = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > Math.abs(m[pivotRow][col])) {
                    pivotRow = r;
                }
            }
            double[] tmp = m[col];
            m[col] = m[pivotRow];
            m[pivotRow] = tmp;

            if (m[col][col] == 0.0) {
                throw new IllegalStateException(
                        "Singular system: trail points do not determine a unique conic");
            }

            for (int r = col + 1; r < n; r++) {
                double factor = m[r][col] / m[col][col];
                for (int c = col; c <= n; c++) {
                    m[r][c] -= factor * m[col][c];
                }
            }
        }

        double[] x = new double[n];
        for (int r = n - 1; r >= 0; r--) {
            double sum = m[r][n];
            for (int c = r + 1; c < n; c++) {
                sum -= m[r][c] * x[c];
            }
            x[r] = sum / m[r][r];
        }
        return x;
    }

    /**
     * Recolors this body light blue if its currently recorded trail fits an
     * elliptical orbit, or light red if it fits a hyperbolic one (via
     * {@link #findDiscriminantLeastSquares()}); otherwise restores its
     * originally assigned color - including while there isn't yet enough
     * trail history to classify, on the knife-edge parabolic case
     * (discriminant exactly 0), or if the recorded points are too
     * degenerate (e.g. collinear) to fit a conic at all. Intended to be
     * called once per simulation step so the coloring tracks the orbit's
     * classification live as it evolves.
     */
    void updateOrbitColor() {
        if (trail == null || trail.length() < 5) {
            return;
        }
        try {
            double discriminant = findDiscriminantLeastSquares();
            if (discriminant < 0.0) {
                color = Color.lightBlue();
            } else if (discriminant > 0.0) {
                color = Color.lightRed();
            } else {
                color = origColor;
            }
        } catch (IllegalStateException ex) {
            // degenerate trail data this step (e.g. collinear points); leave color as-is
        }
    }

    /**
     * Get momentum in the X axis
     * @return X axis momentum
     */
    public double getXMomentum() {
        return U*mass_kg;
    }

    /**
     * Get momentum in the Y axis
     * @return Y axis momentum
     */
    public double getYMomentum() {
        return V*mass_kg;
    }

    /**
     * Add additional force in X axis
     * @param additionalXForce
     */
    void addXForce(double additionalXForce) {
        this.xForce += additionalXForce;
    }

    /**
     * Add additional force in Y axis
     * @param additionalYForce
     */
    void addYForce(double additionalYForce) {
        this.yForce += additionalYForce;
    }

    /**
     * Add additional mass
     * @param additionalMass
     */
    void addMass(double additionalMass) {
        this.mass_kg += additionalMass;
        this.calculateRadius();
    }

    /**
     * Absorbs another body into ourself: also
     *   - conserves linear momentum
     *   - shifts position to center-of-mass of two bodies
     *   - sums the net gravitational force of both masses
     *   - marks the other body as 'absorbed' to prevent further consideration
     *   - puts the other body on the deletion list
     * @param otherBody the body to absorb
     * @param deletionList list of bodies to be deleted later
     *
     * Deletion is deferred so the calling routine won't modify the bodies list
     * while this absorption is being consummated
     */
    public void absorb(Body otherBody, ArrayList<Body> deletionList) {

        // we shouldn't see any interlopers that have been absorbed into other
        // bodies; crash if we do

        assert ( !otherBody.getAbsorbedFlag() );

        // make sure we get absorbed by only one other body
        if ( !deletionList.contains(otherBody) ) {

            // mark the new body as being absorbed
            otherBody.setAbsorbedFlag();

            // use trailing path of larger body
            if (otherBody.getMass() > this.mass_kg)
                this.trail = otherBody.getTrailingPath();

            // add forces from other body to this one
            addXForce( otherBody.getXForce() );
            addYForce( otherBody.getYForce() );

            // calculate joined moments
            double xm = mass_kg * X + otherBody.getMass() * otherBody.getX();
            double ym = mass_kg * Y + otherBody.getMass() * otherBody.getY();

            // calculate joined momentum
            double hx = this.getXMomentum() + otherBody.getXMomentum();
            double hy = this.getYMomentum() + otherBody.getYMomentum();

            // sum the masses and adjust our radius accordingly
            addMass( otherBody.getMass() );

            // set combined velocity from conservation of momentum
            setU( hx / mass_kg );
            setV( hy / mass_kg );

            // move to the center of the new mass
            setX( xm / mass_kg );
            setY( ym / mass_kg );

            // mark the other body for deletion
            deletionList.add(otherBody);
        }
    }


    /**
     * Remove the join list once it's been handled
     */
    void deleteJoinList() {
        joinList = null;
    }

    /**
     * Adjust radius to match density, mass
     */
    private void calculateRadius() {
        double volume_m3 = mass_kg/dens_kg_m3;
        // V = (4/3)*PI*r^3
        double radius3 = (3.0*volume_m3)/(4.0*Math.PI);
        radius_m = Math.pow(radius3,(1.0/3.0));
    }

    /**
     * Adjust mass to match density & radius
     */
    private void calculateMass() {
        double volume_m3 = (4.0/3.0)*Math.PI*Math.pow(radius_m,3.0);
        mass_kg = dens_kg_m3*volume_m3;
    }

    /**
     * Adjust density to match mass and radius
     */
    private void calculateDensity() {
        double volume_m3  = (4.0/3.0)*Math.PI
                *Math.pow(radius_m,3.0);
        dens_kg_m3 = mass_kg/volume_m3;
    }

    /**
     * Have we been absorbed by another body?
     * @return absorbed flags
     */
    public boolean getAbsorbedFlag() {
        return absorbed;
    }

    /**
     * Set the absorbed flag to true
     */
    public void setAbsorbedFlag() {
        absorbed = true;
    }

}
