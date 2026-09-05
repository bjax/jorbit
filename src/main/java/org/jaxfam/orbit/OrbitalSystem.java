//
// OrbitalSystem.java
//
// Part of the orbital mechanics demonstrator program
//
// Originally written 2001 by Bruce Jackson, bruce@jaxfam.org
// Ported to Mac OSX Project Builder 020327 EBJ
// Ported to NetBeans 2011-04-21 EBJ
// Factored this parent class out of SolarSystem.java 2011-04-28 EBJ

package org.jaxfam.orbit;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;

/**
 * Provides a parent class for a set of bodies in space that are attracted to
 * one another by gravity alone. Implements the DynamicSystem interface so that
 * the OrbitalSystem can be propagated through time with a selected integration
 * method.
 * @author Bruce Jackson
 */
public class OrbitalSystem implements DynamicSys {

    static final double Kgravity = 6.672e-11; 	/** G in  N m^2 kg^-2         */
    static final double AU2m = 1.496E11;        /** Conversion factor AU to m */
    static final double SolMass = 332800.;      /** in earth masses           */
    static final double earthMass = 5.976E24;   /** in kg, used for scaling   */
    static final double earthRadius = 6378000.0;/** in m, used for scaling    */
    static final double minDist = 10.0;         /** how close can bodies get  */
                                                /** in m, used for scaling    */
    static final double MINUTE = 60.0;          /** secs per minute           */
    static final double HOUR   = 60.0*MINUTE;   /** secs per hour             */
    static final double DAY    = 24.0*HOUR;     /** secs per day              */
    static final double YEAR   = 365.25*DAY;    /** secs per year             */
    public static double dt;                    /** time step, sec            */

    int pathLength;            /** the length of recorded position arrays     */

    double maxR;               /** maximum initial radius, AU */
    boolean enforce_R_limit;   /** should we cull escapees?   */

    ArrayList<Body> bodies;    /** the planets and stars are stored here */
    Body centeredBody;         /** central body in view (if null, [0,0]) */
    double time;               /** current time for model    */
    int trailDecimation;       /** space between trail pts   */
    Integrator integrator;     /** integrator object         */

    double defaultM2pix = Orbit.initial_m2pix; /** zoom (pixels per meter) Orbit starts at when
                                                    this system is first chosen at startup;
                                                    subclasses may override to fit their own
                                                    scale (e.g. a system spanning fractions of an
                                                    AU needs a much tighter default than one
                                                    spanning several AU)                          */
    boolean defaultHighViz;    /** if true, every body starts with isHighlighted already set, as
                                    if V had been pressed right after this system was chosen      */
    boolean colorByOrbitShape; /** if true, Propagate() recolors every body each step via
                                    Body.updateOrbitColor() (light blue while its trail currently
                                    fits an ellipse, light red while it fits a hyperbola, else its
                                    originally assigned color) - off by default, since it would
                                    otherwise override a system's own intentional body colors, e.g.
                                    SolarSystem's traditional per-planet colors                    */
    boolean defaultShowHistogram; /** if true, the size histogram overlay starts shown, as if H
                                       had been pressed right after this system was chosen        */

    /**
     * Constructor; selects the integration method
     */
    public OrbitalSystem() {
        integrator = new RungeKutta4Integrator(this);
        time = 0.0;
        pathLength = 0;
    }

    /**
     * Advance all elements one time step; cull bodies that have exceeded
     * distance limit (TBD - calculate escape velocity). Records time as the
     * first element of the associated StateVector
     */
    public void Propagate() {
        sumForces();        // call each body and sum attractions to all others
        handleImpacts();    // combine bodies that have collided (marked previously)

        // perform integration of updated state vector
        StateVector nextState = integrator.integrate(null, OrbitalSystem.dt);

        // have each body update it's internal state vector
        this.loadStateVector(nextState);

        // cull bodies that have 'escaped' system in two passes
        // first one identifies 'escapees'; second removes them from list
        if (enforce_R_limit) {
            ArrayList<Body> culled = new ArrayList<>(5);
            // first pass - look for outliers/escapees
            for (Body body : bodies) {
                double R = body.getR_AU();
                if (R > 3.0*maxR) {
                    culled.add(body);
                }
            }
            // second pass - remove them from list
            bodies.removeAll(culled);
        }

        // elliptical/hyperbolic orbit-shape recoloring, per-scenario opt-in (see colorByOrbitShape's
        // javadoc for why this isn't unconditional)
        if (colorByOrbitShape) {
            for (Body body : bodies) {
                body.updateOrbitColor();
            }
        }
    }

    /**
     * Return the initial condition state vector
     * @return a StateVector with initial conditions
     */
    public StateVector getIC() {
        StateVector x = new StateVector(Body.NUM_STATES * bodies.size() + 1);
        int i = 0;
        x.set(0, time);
        for (Body body : bodies) {
            for (int j = 0; j < Body.NUM_STATES; j++) {
                x.set(Body.NUM_STATES * i + j + 1, 0.0);
            }
            StateVector littleX = body.getIC();
            for (int j = 0; j < Body.NUM_STATES; j++) {
                x.set(Body.NUM_STATES * i + j + 1, littleX.get(j));
            }
            i++;
        }
        return x;
    }

    /**
     * Reset the system to initial conditions
     */
    public void resetToIC() {
        time = 0.0;
        for (Body b : bodies) {
            b.resetToIC();
        }
    }

    /**
     * Serializes this system's current state - integration parameters plus every
     * body's position, velocity, size, density, color, and optional name - as JSON
     * matching schema/orbital-system.schema.json, and writes it to the given file.
     * Bodies' positions/velocities are whatever they currently are, not necessarily
     * their original initial conditions, so this can be used to save either: call it
     * right after construction to capture initial conditions, or at any later point
     * to checkpoint the system as it currently stands.
     * @param file file to write
     * @throws IOException if the file can't be written
     */
    public void saveToFile(File file) throws IOException {
        JSONObject root = new JSONObject();
        root.put("dt", dt);
        root.put("trailLength", pathLength);
        root.put("trailDecimation", trailDecimation);
        root.put("enforceRadiusLimit", enforce_R_limit);
        root.put("maxRadiusAU", maxR);

        JSONArray bodyArray = new JSONArray();
        for (Body body : bodies) {
            JSONObject b = new JSONObject();
            if (body.getName() != null) {
                b.put("name", body.getName());
            }
            b.put("positionAU", new JSONArray(new double[] {
                    body.getX() / AU2m, body.getY() / AU2m }));
            b.put("velocityMS", new JSONArray(new double[] {
                    body.getU(), body.getV() }));
            b.put("radiusEarthRadii", body.getRadius() / earthRadius);
            b.put("densityGCm3", body.getDensity_g_cm3());
            Color c = body.getColor();
            b.put("color", new JSONArray(new double[] { c.r(), c.g(), c.b() }));
            bodyArray.put(b);
        }
        root.put("bodies", bodyArray);

        Files.writeString(file.toPath(), root.toString(2));
    }

    /**
     * Reads a JSON file matching schema/orbital-system.schema.json (as written by
     * {@link #saveToFile}) and builds a new OrbitalSystem from it. The returned
     * system is centered on its first body by default; callers that want a
     * different view (e.g. Orbit always recenters on the origin) should set that
     * afterward.
     * @param file file to read
     * @return a new OrbitalSystem matching the file's contents
     * @throws IOException if the file can't be read
     * @throws org.json.JSONException if the file's contents don't match the schema
     */
    public static OrbitalSystem loadFromFile(File file) throws IOException {
        String text = Files.readString(file.toPath());
        JSONObject root = new JSONObject(text);

        OrbitalSystem system = new OrbitalSystem();
        dt = root.getDouble("dt");
        system.pathLength = root.optInt("trailLength", 200);
        system.trailDecimation = root.optInt("trailDecimation", 2);
        system.enforce_R_limit = root.optBoolean("enforceRadiusLimit", false);
        system.maxR = root.optDouble("maxRadiusAU", 1000.0);

        JSONArray bodyArray = root.getJSONArray("bodies");
        system.bodies = new ArrayList<>(bodyArray.length());
        for (int i = 0; i < bodyArray.length(); i++) {
            JSONObject b = bodyArray.getJSONObject(i);
            JSONArray pos = b.getJSONArray("positionAU");
            JSONArray vel = b.getJSONArray("velocityMS");
            JSONArray col = b.getJSONArray("color");
            Body body = new Body(
                    pos.getDouble(0), pos.getDouble(1),
                    vel.getDouble(0), vel.getDouble(1),
                    b.getDouble("radiusEarthRadii"),
                    b.getDouble("densityGCm3"),
                    new Color((float) col.getDouble(0), (float) col.getDouble(1), (float) col.getDouble(2)),
                    system.pathLength);
            if (b.has("name")) {
                body.setName(b.getString("name"));
            }
            system.bodies.add(body);
        }
        if (!system.bodies.isEmpty()) {
            system.centeredBody = system.bodies.get(0);
        }
        return system;
    }

    /**
     * Get the state derivative vector for specified conditions
     * @param u current InputVector (null)
     * @param x new StateVector
     * @return a new StateVector containing state derivatives
     */
    public StateVector getStateDeriv(InputVector u, StateVector x) {
        this.loadStateVector(x);
        this.sumForces();
        StateVector xd = new StateVector(Body.NUM_STATES * bodies.size() + 1);
        int i = 0;
        xd.set(0, 1.0);
        for (Body body : bodies) {
            StateVector body_xd = body.getStateDeriv(null, null);
            for (int j = 0; j < Body.NUM_STATES; j++) {
                xd.set(Body.NUM_STATES * i + j + 1, body_xd.get(j));
            }
            i++;
        }
        return xd;
    }

    /**
     * Create a new state vector containing time plus each bodie's states
     * @return current system state vector
     */
    public StateVector getStateVector() {
        StateVector x = new StateVector(Body.NUM_STATES * bodies.size() + 1);
        int i = 0;
        x.set(0, time);
        for (Body body : bodies) {
            for (int j = 0; j < Body.NUM_STATES; j++) {
                x.set(Body.NUM_STATES * i + j + 1, 0.0);
            }
            StateVector littleX = body.getStateVector();
            for (int j = 0; j < Body.NUM_STATES; j++) {
                x.set(Body.NUM_STATES * i + j + 1, littleX.get(j));
            }
            i++;
        }
        return x;
    }

    /**
     * Load the provided state vector contents into our system
     * @param x new StateVector to load
     */
    public void loadStateVector(StateVector x) {
        if (Body.NUM_STATES*bodies.size() != (x.length()-1) ) {
            throw new RuntimeException("StateVector size doesn't match");
        } else { // sizes match
            time = x.get(0);
            int i = 0;
            for (Body body : bodies) {
                body.loadStateVector(x, Body.NUM_STATES * i + 1);
                i++;
            }
        }
    }


    /**
     * center about a given planet body, by index number
     * @param planetNumber Planet to center about
     */
    public void setCenter(int planetNumber) {
        centeredBody = bodies.get(planetNumber);
    }

    /**
     * Centers the view on the Nth-largest currently-existing body (by
     * radius), rank 0 being the largest. Does nothing if fewer than rank+1
     * bodies currently exist - bodies are continually gained/lost to
     * collisions and culling, so a given rank can go out of range at any
     * time.
     * @param rank 0 for the largest body, 1 for the second-largest, etc.
     */
    public void setCenterOnNthLargest(int rank) {
        List<Body> sortedAscending = getBodiesSortedBySize();
        int index = sortedAscending.size() - 1 - rank; // largest is last in ascending order
        if (index >= 0) {
            centeredBody = sortedAscending.get(index);
        }
    }

    /**
     * Record the center point-of-view of the system
     * @param X_AU set the X position of the center
     * @param Y_AU set the Y position of the center
     */
    public void setCenterXY(double X_AU, double Y_AU) {
        // replace any previous center coordinates with a new fake body
        centeredBody = new Body(X_AU, Y_AU, 0., 0., 1.0, 1.0, Color.black(),0);
    }

    /**
     * Clears and sums the forces acting on each body by every other body
     */
    private void sumForces() {

        // modified so we never do the same pair twice, by
        // starting the inner (i) loop at one past the
        // first body.

        // two passes - one to zero forces, one to add forces
        // first, zero forces

        for (Body body : bodies) {
            body.resetForces();
        }

        // now add mutual attractive force to both bodies
        for (int i = 0; i < bodies.size(); i++) {
            Body body1 = bodies.get(i);
            for (int j = i+1; j < bodies.size(); j++) {
                Body body2 = bodies.get(j);
                Body.addForces(body1, body2);
            }
        }
    }

    /**
     * How many bodies are interacting?
     * @return number of bodies in system
     */
    public int countBodies() {
        return bodies.size();
    }

    /**
     * Returns the current bodies sorted by size (radius), smallest first.
     * Does not modify this system's own body list or ordering.
     * @return a new list of bodies sorted ascending by {@link Body#getRadius()}
     */
    public List<Body> getBodiesSortedBySize() {
        List<Body> sorted = new ArrayList<>(bodies);
        sorted.sort(Comparator.comparingDouble(Body::getRadius));
        return sorted;
    }

    /**
     * What is the total mass represented?
     * @return total mass in kg
     */
    public double totalMass() {
        double totalMass = 0.0;
        for (Body body : bodies) {
            totalMass += body.getMass();
        }
        return totalMass;
    }

     /**
     * What is the total x-moment represented?
     * @return total y-moment in kg-m
     */
    public double totalXMoment() {
        double totalXMoment = 0.0;
        for (Body body : bodies) {
            totalXMoment += body.getMass()*body.getX();
        }
        return totalXMoment;
    }

    /**
     * What is the total y-moment represented?
     * @return total y-moment in kg-m
     */
    public double totalYMoment() {
        double totalYMoment = 0.0;
        for (Body body : bodies) {
            totalYMoment += body.getMass()*body.getY();
        }
        return totalYMoment;
    }

    /**
     * Where is the center-of-mass of the system in X direction?
     * @return center of mass in X direction, m
     */
    public double getCmX() {
        return totalXMoment()/totalMass();
    }

    /**
     * Where is the center-of-mass of the system in Y direction?
     * @return center of mass in Y direction, m
     */
    public double getCmY() {
        return totalYMoment()/totalMass();
    }

   /**
     * Join any bodies that have impacted, absorbing the smaller one
     * with the larger one.
     */
    private void handleImpacts() {
        // look at each body; see if needs to merge with another
        // this will be indicated if the body has a join list.
        // We go through the join list and absorb those bodies into 
        // the current body regardless of mass; the joined body will
        // carry the trailing path of the larger mass, however.
 
        // deletionList will contain bodies to be removed, but
        // we don't make changes to the bodies list while iterating
        ArrayList<Body> deletionList = new ArrayList<>();

        // now loop through list of bodies looking for bodies that have
        // collided with another.

        // BUG - A body to be absorbed should have it's joinList parsed first
        // or some collisions will go undetected.

        for (Body body : bodies) {
            // avoid double absorption (and deletion!)
            if (!deletionList.contains(body)) {
                if (body.joinList != null) {
                    // here with a body in contact with at least one other
                    // loop through list of contacting bodies
                    for (Body interloper : body.joinList) {
                        body.absorb(interloper, deletionList);
                    }
                    // now that we've absorbed all interlopers, delete the join list
                    body.deleteJoinList();
                }
            }
        }

        // now delete absorbed bodies
        bodies.removeAll(deletionList);
    }


    /**
     * Draw each body on screen; record their position
     * @param minR  minimum radius for disks
     */
    public void glRender(float minR) {
        for (Body body : bodies) {
            body.glRender(true, minR, centeredBody);
            if (time % (trailDecimation * SolarSystem.dt) < 1.0) {
                body.recordPosition(time);
            }
        }
    }

    /**
     * Finds whichever body's on-screen disk contains the given point, for
     * mouse hit-testing. Coordinates and minR must be in the same
     * centeredBody-relative world space and minimum-radius clamp that
     * glRender()/Body.glRender() draw with, or the hit test won't line up
     * with what's actually on screen. If several bodies' disks overlap the
     * point, returns whichever one's center is closest to it.
     * @param worldX X, relative to centeredBody (0 if none), in meters
     * @param worldY Y, relative to centeredBody (0 if none), in meters
     * @param minR   minimum on-screen disk radius, matching the minR passed to glRender()
     * @return the closest body whose (possibly minR-clamped) disk contains the point, or null
     */
    public Body findBodyAt(double worldX, double worldY, float minR) {
        double centerX = (centeredBody != null) ? centeredBody.getX() : 0.0;
        double centerY = (centeredBody != null) ? centeredBody.getY() : 0.0;

        Body closest = null;
        double closestDist2 = Double.MAX_VALUE;
        for (Body body : bodies) {
            double dx = worldX - (body.getX() - centerX);
            double dy = worldY - (body.getY() - centerY);
            double dist2 = dx*dx + dy*dy;

            float r = (float) body.getRadius();
            if (r < minR) r = minR;

            if (dist2 <= (double) r * r && dist2 < closestDist2) {
                closest = body;
                closestDist2 = dist2;
            }
        }
        return closest;
    }

    /**
     * Draws a histogram of the current bodies' sizes as a column of bars
     * stacked vertically from (rightX,bottomY) - smallest-size bin at the
     * bottom, largest at the top - with each bar's baseline (zero count) at
     * rightX and extending leftward, proportional to that bin's count, so
     * the whole histogram is right-justified against rightX. (This is a
     * left/count-up histogram rotated 90 degrees CCW: what was the
     * bin/horizontal axis becomes vertical, and what was the count/vertical
     * axis becomes horizontal, extending from the right margin rather than
     * up from a bottom one.) Draws in whatever color is currently set via
     * glColor*, so callers can match it to other on-screen elements (e.g.
     * the HUD text) - the median-size labels inherit the same color. Bodies
     * are binned by radius on a LOG scale rather than linearly:
     * RandomSystem's log-normal size distribution spans several orders of
     * magnitude, so linear bins would dump nearly everything into the
     * smallest bucket and the shape of the distribution wouldn't show at
     * all. Does nothing if there are no bodies.
     * @param font          font to label each non-empty bin with its median body size, in Earth radii
     * @param layout        where to draw the histogram and how its bars/gaps/bins are sized
     * @param selectedBody  the currently-selected body (or null for none) - if non-null, its bin's
     *                      bar is drawn in a lighter shade of whatever color the caller set, so the
     *                      histogram shows which size bucket the selection falls into
     */
    public void glRenderSizeHistogram(TrueTypeFont font, HistogramLayout layout, Body selectedBody) {
        float rightX = layout.rightX();
        float bottomY = layout.bottomY();
        float barThickness = layout.barThickness();
        float gap = layout.gap();
        float maxBarLength = layout.maxBarLength();
        int numBins = layout.numBins();

        if (bodies.isEmpty() || numBins < 1) {
            return;
        }

        double[] logMinAndRange = computeLogRadiusRange();
        double logMin = logMinAndRange[0];
        double logRange = logMinAndRange[1];

        List<List<Double>> binRadii = new ArrayList<>(numBins);
        for (int i = 0; i < numBins; i++) {
            binRadii.add(new ArrayList<>());
        }
        for (Body body : bodies) {
            binRadii.get(binIndexFor(body.getRadius(), logMin, logRange, numBins)).add(body.getRadius());
        }

        int selectedBin = (selectedBody != null)
                ? binIndexFor(selectedBody.getRadius(), logMin, logRange, numBins)
                : -1;

        // whatever color the caller set via glColor* before calling (e.g. matching the run-state
        // HUD text) - captured so the selected bin's bar can be drawn in a lighter version of it,
        // then restored for every label and the rest of the bars
        FloatBuffer currentColor = BufferUtils.createFloatBuffer(4);
        glGetFloatv(GL_CURRENT_COLOR, currentColor);
        float baseR = currentColor.get(0);
        float baseG = currentColor.get(1);
        float baseB = currentColor.get(2);

        int maxCount = 0;
        for (List<Double> radii : binRadii) {
            if (radii.size() > maxCount) maxCount = radii.size();
        }

        float binSlot = barThickness + gap;
        double binLogWidth = logRange / numBins;
        // labels were drawn at half the font's baked size; 4pts larger than that (two +2pt bumps),
        // expressed as a scale fraction so it stays correct if the baked font size ever changes
        float labelScale = (font.getHeight() * 0.5f + 4f) / font.getHeight();
        for (int i = 0; i < numBins; i++) {
            List<Double> radii = binRadii.get(i);
            float y0 = bottomY + i * binSlot;

            if (!radii.isEmpty()) {
                // at least 1px so a bin with just a few bodies (relative to the most populous one)
                // still shows a visible mark, rather than rounding away to nothing
                float barLength = Math.max(1f, maxBarLength * radii.size() / (float) maxCount);
                float y1 = y0 + barThickness;
                float x0 = rightX - barLength;
                float x1 = rightX;

                if (i == selectedBin) {
                    // blend halfway toward white, regardless of the base color
                    glColor3f(0.5f*(baseR + 1f), 0.5f*(baseG + 1f), 0.5f*(baseB + 1f));
                } else {
                    glColor3f(baseR, baseG, baseB);
                }
                glDisable(GL_TEXTURE_2D); // in case the previous bin's label left this enabled
                glBegin(GL_QUADS);
                glVertex2f(x0, y0);
                glVertex2f(x1, y0);
                glVertex2f(x1, y1);
                glVertex2f(x0, y1);
                glEnd();
            }

            // representative size for this row: the actual median if any bodies fall in it,
            // otherwise the bin's own (empty) size range midpoint, so every row is still labeled
            double representativeRadiusM = radii.isEmpty()
                    ? Math.exp(logMin + (i + 0.5) * binLogWidth)
                    : median(radii);
            double representativeRadiusER = representativeRadiusM / earthRadius;
            glColor3f(baseR, baseG, baseB); // labels always in the base color, never the highlight
            font.drawString(rightX + 5f, y0, formatRadiusER(representativeRadiusER) + " ER",
                    labelScale, labelScale, TrueTypeFont.ALIGN_LEFT);
        }
    }

    /**
     * Which log-scale size bin (0 to numBins-1) a given radius falls into, given the bin range's
     * log-min and log-span - shared by {@link #glRenderSizeHistogram} between binning every body and
     * locating a single body of interest (e.g. the current selection) within those same bins.
     * @param radius   the radius to bin, meters
     * @param logMin   log of the smallest radius across all bodies
     * @param logRange log(largest radius) - logMin across all bodies; <= 0 means every body is
     *                 (nearly) the same size, so everything falls into bin 0
     * @param numBins  number of bins the range is divided into
     */
    private static int binIndexFor(double radius, double logMin, double logRange, int numBins) {
        if (logRange <= 0.0) {
            return 0;
        }
        double t = (Math.log(radius) - logMin) / logRange; // 0..1
        int bin = (int) (t * numBins);
        if (bin >= numBins) bin = numBins - 1; // clamp the t==1.0 edge
        if (bin < 0) bin = 0; // defensive: a radius below the observed minimum shouldn't occur
        return bin;
    }

    /**
     * The current bodies' radius range, log-scaled, as {logMin, logRange} - the same two values
     * {@link #binIndexFor} needs, shared between {@link #glRenderSizeHistogram} and
     * {@link #getBodiesInSizeBin} so both bucket bodies identically. Callers must not call this with
     * an empty {@code bodies} list (both current callers already guard against that).
     */
    private double[] computeLogRadiusRange() {
        double minRadius = Double.MAX_VALUE;
        double maxRadius = -Double.MAX_VALUE;
        for (Body body : bodies) {
            double r = body.getRadius();
            if (r < minRadius) minRadius = r;
            if (r > maxRadius) maxRadius = r;
        }
        double logMin = Math.log(minRadius);
        return new double[] {logMin, Math.log(maxRadius) - logMin};
    }

    /**
     * Every currently-existing body whose radius falls in the given log-scale size bin, using the
     * exact same bucketing {@link #glRenderSizeHistogram} draws with - for highlighting every body in
     * a bin when the user hovers over its bar (see {@code Orbit.handleMouseMove()}).
     * @param binIndex bin index, 0 to numBins-1 (largest-size bin is numBins-1)
     * @param numBins  number of bins the size range is divided into, matching the histogram currently
     *                 on screen ({@code HistogramLayout.numBins()})
     * @return bodies in that bin, or an empty list if there are no bodies at all
     */
    public List<Body> getBodiesInSizeBin(int binIndex, int numBins) {
        if (bodies.isEmpty()) {
            return List.of();
        }
        double[] logMinAndRange = computeLogRadiusRange();
        double logMin = logMinAndRange[0];
        double logRange = logMinAndRange[1];

        List<Body> result = new ArrayList<>();
        for (Body body : bodies) {
            if (binIndexFor(body.getRadius(), logMin, logRange, numBins) == binIndex) {
                result.add(body);
            }
        }
        return result;
    }

    /** Median of a list of values; averages the two middle values for an even-sized list. */
    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int n = sorted.size();
        return (n % 2 == 1)
                ? sorted.get(n / 2)
                : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    /** Formats a size in Earth radii compactly, scaling precision/notation to its magnitude. */
    static String formatRadiusER(double er) {
        if (er < 0.01) {
            return String.format(Locale.US, "%.1e", er);
        } else if (er < 10) {
            return String.format(Locale.US, "%.2f", er);
        } else {
            return String.format(Locale.US, "%.1f", er);
        }
    }

    /**
     * Returns number of active bodies
     * @return length of bodies list
     */
    int getNumBodies() {
        return bodies.size();
    }

    /**
     * Gets elapsed simulated time in seconds
     * @return seconds of sim time
     */
    double getTime_sec() {
        return time;
    }

    /**
     * Gets elapsed simulated time in minutes
     * @return hours of sim time
     */
    double getTime_min() {
        return time / MINUTE;
    }


    /**
     * Gets elapsed simulated time in hours
     * @return hours of sim time
     */
    double getTime_hr() {
        return time / HOUR;
    }


    /**
     * Gets formatted string of sim time
     * like 15y 234d 00:00:00.0
     */
    String getFormattedTime() {
        double remainder;

        int years = (int) Math.floor(time/YEAR);
        remainder = time - YEAR*(double)years;
        int days  = (int) Math.floor(remainder/DAY);
        remainder = remainder - DAY*(double)days;
        int hrs   = (int) Math.floor(remainder/HOUR);
        remainder = remainder - HOUR*(double)hrs;
        int mins  = (int) Math.floor(remainder/MINUTE);
        remainder = remainder - MINUTE*(double)mins;
        int secs  = (int) Math.floor(remainder);

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);

        if (time > MINUTE ) { // mins > 0
            if (time > HOUR) { // hrs > 0
                if (time > DAY) { // days > 0
                    if (time > YEAR) {
                        formatter.format("%dy %3dd %2d:%02d:%02d",
                                years, days, hrs, mins, secs);
                    } else {
                        formatter.format("%dd %2d:%02d:%02d",
                                days, hrs, mins, secs);
                    }
                } else {
                    formatter.format("%d:%02d:%02d",
                                hrs, mins, secs);
                }
            } else {
                formatter.format("%d:%02d", mins, secs);
            }
        } else {
            formatter.format("%d", secs);
        }

        return sb.toString();
    }

}
