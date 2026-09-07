//
// Orbit.java
//
// Main class of the orbital mechanics demonstrator program
//
// Originally written 2001 by Bruce Jackson, bruce@jaxfam.org
// Ported to Mac OSX Project Builder 020327 EBJ
// Ported to NetBeans 2011-04-21 EBJ
// Ported to LWJGL 3 / GLFW 2026

package org.jaxfam.orbit;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

public class Orbit {

    static final double initial_dt = 5000.;         /** seconds per step       */
    static final double initial_m2pix = 1e-9;       /** 1 million km per pixel */
    public int DISPLAY_HEIGHT;              /** window content-area height, in screen
                                                  coordinates - set from the current monitor's
                                                  video mode at the start of create(), so the
                                                  window fills the whole screen              */
    public int DISPLAY_WIDTH;               /** window content-area width, in screen
                                                  coordinates - set alongside DISPLAY_HEIGHT   */
    public static final Logger LOGGER = Logger.getLogger(Orbit.class.getName());
    static final double SHIFT_PIX = 100.0;  /** left/right/up/down/keys dist   */
    static final double HIT_RADIUS_PIX = 8.0; /** min clickable radius around a body,
                                                    regardless of its rendered on-screen
                                                    size - keeps tiny/zoomed-out bodies
                                                    selectable without inflating the dot
                                                    itself                              */
    static final double DRAG_THRESHOLD_PIX = 4.0; /** cursor movement beyond this, since
                                                    the left button went down, counts as a
                                                    click-and-drag pan rather than a click -
                                                    see handleMouseButton()             */
    static final File STATE_FILE = new File(".JOS.json"); /** IC/checkpoint file R
                                                    reloads and S saves to, in the
                                                    working directory              */
    static final float REFERENCE_DISPLAY_HEIGHT = 900f; /** DISPLAY_HEIGHT this UI's fixed-pixel
                                                    font size and histogram geometry were tuned
                                                    against (a typical Mac logical/point
                                                    resolution) - see uiScale                  */
    static final String HELP_TEXT = // shown down the left side of the screen while showHelp is set
              "Keyboard:\n"
            + "P: run/pause\n"
            + "+/-/scroll: zoom in/out\n"
            + "Page Up/Down: speed up/slow down\n"
            + "Left/Right/Up/Down: pan\n"
            + "Home: recenter on origin\n"
            + "0-9: center on Nth-largest body\n"
            + "Tab/Shift-Tab: next/previous body\n"
            + "V: toggle high-visibility\n"
            + "H: toggle size histogram\n"
            + "S: save state\n"
            + "R: reload state\n"
            + "Esc: exit\n"
            + "?: toggle this help\n"
            + "\n"
            + "Mouse:\n"
            + "hover: highlight body\n"
            + "click: select/deselect body\n"
            + "click + drag: pan view\n"
            + "scroll: zoom in/out";

    public static boolean run     = false;  /** keep-alive flag (P toggles)    */
    public static boolean showHistogram = false; /** draw size histogram (H toggles) */
    public static boolean showHelp = false; /** draw key/mouse help panel (? toggles)  */

    static TrueTypeFont trueTypeFont;
    HistogramLayout histogramLayout;        /** where/how to draw the size histogram; computed once
                                                 in create(), since it depends only on fixed display/
                                                 font geometry that doesn't change frame to frame     */

    long window;                            /** GLFW window handle             */
    int framebufferWidth, framebufferHeight; /** actual pixel size of the drawable
                                                  area, which can differ from
                                                  DISPLAY_WIDTH/HEIGHT on HiDPI
                                                  (Retina) displays              */

    double m2pix;                           /** meters per display pixel       */
    float uiScale;                          /** DISPLAY_HEIGHT / REFERENCE_DISPLAY_HEIGHT, set in
                                                  create() once the monitor's video mode is known;
                                                  scales the font bake size and histogram geometry
                                                  so they read the same relative size regardless of
                                                  whether DISPLAY_HEIGHT is a Mac logical/point
                                                  resolution (small numbers, a few hundred to ~1100)
                                                  or Windows' raw physical-pixel resolution (much
                                                  larger, e.g. 1080/1440/2160) - without it, fixed-
                                                  pixel-sized text and histogram bars that read fine
                                                  on Mac are tiny on a high-res Windows display   */

    OrbitalSystem system;                   /** model we are running           */
    StateVector IC_vector;                  /** initial state vector           */

    double centerX, centerY;                /** screen zoom center coords      */

    Body hoveredBody;                       /** body currently under the mouse cursor, or null */
    Body selectedBody;                      /** single body last clicked on, or null           */
    List<Body> binHoveredBodies = new ArrayList<>(); /** bodies currently highlighted because the
                                                            cursor is over their histogram bar; empty
                                                            when it isn't                             */

    boolean leftButtonDown;                 /** true while the left mouse button is held down  */
    boolean dragOccurred;                   /** true once the cursor has moved past
                                                  DRAG_THRESHOLD_PIX since the button went down -
                                                  gates click-selection on release                */
    double dragStartCursorX, dragStartCursorY; /** cursor position when the left button went
                                                     down, for the click-vs-drag threshold        */
    double lastCursorX, lastCursorY;        /** most recent cursor position, for incremental
                                                  drag-panning deltas between move events         */

    static final int SCENARIO_SOLAR_SYSTEM = 1;
    static final int SCENARIO_TRIAX        = 2;
    static final int SCENARIO_RANDOM       = 3;

    /*
     * Default constructor; instantiates model. Prompts on stdin for which
     * scenario to run and then for each of its construction parameters
     * (Enter accepts the default shown in brackets), so the initial
     * conditions can be tuned without editing code.
     */
    public Orbit() {
        Scanner scanner = new Scanner(System.in);
        switch (promptScenario(scanner)) {
            case SCENARIO_SOLAR_SYSTEM: {
                System.out.println("Configure solar system (press Enter to accept default):");
                double dt = promptDouble(scanner, "Step size, sec", 5000.);
                int trailLength = promptInt(scanner, "Trailing path length", 10000);
                system = new SolarSystem(dt, trailLength);
                break;
            }
            case SCENARIO_TRIAX: {
                System.out.println("Triax: three equal bodies in a rotating equilateral triangle "
                        + "(fixed, no parameters to configure).");
                system = new TriaxSystem();
                break;
            }
            case SCENARIO_RANDOM:
            default: {
                System.out.println("Configure random planetoid cluster (press Enter to accept default):");
                double dt        = promptDouble(scanner, "Step size, sec", 1200.);
                int numBodies     = promptInt(scanner, "Number of bodies", 200);
                int pathLength    = promptInt(scanner, "Trailing path length", 200);
                double distShape  = promptDouble(scanner, "Size-distribution shape (-1 = small-end peak, 0 = uniform, +1 = large-end peak)", -1.0);
                double minRadius  = promptDouble(scanner, "Min body size, Earth radii", 0.001);
                double maxRadius  = promptDouble(scanner, "Max body size, Earth radii", 50.0);
                double maxV_IC    = promptDouble(scanner, "Max initial velocity, m/s", 1000.);
                double density    = promptDouble(scanner, "Body density, g/cm^3 (H2O = 1)", 2.0);
                double maxSystemR = promptDouble(scanner, "Initial system radius, AU", 3.0);
                system = new RandomSystem(dt, numBodies, pathLength, distShape,
                        minRadius, maxRadius, maxV_IC, density, maxSystemR);
                break;
            }
        }
        IC_vector = system.getStateVector();
        centerX = 0.0;
        centerY = 0.0;
        applySystemDefaults();

        // save initial conditions to STATE_FILE so R always has something valid to
        // reload, even before the user has pressed S themselves
        saveState();
    }

    /**
     * Applies the just-chosen system's own default presentation: its default zoom
     * (OrbitalSystem.defaultM2pix - each scenario may need a very different zoom to look
     * reasonable, e.g. TriaxSystem's fraction-of-an-AU scale vs. SolarSystem's several AU),
     * whether every body starts highlighted as if V had been pressed
     * (OrbitalSystem.defaultHighViz), and whether the size histogram overlay starts shown
     * as if H had been pressed (OrbitalSystem.defaultShowHistogram).
     */
    private void applySystemDefaults() {
        m2pix = system.defaultM2pix;
        if (system.defaultHighViz) {
            for (Body body : system.bodies) {
                body.isHighlighted = true;
            }
        }
        showHistogram = system.defaultShowHistogram;
    }

    /**
     * Prompts on stdin for which startup scenario to run.
     * @return one of SCENARIO_SOLAR_SYSTEM, SCENARIO_TRIAX, SCENARIO_RANDOM
     */
    private static int promptScenario(Scanner scanner) {
        System.out.println("Choose a scenario:");
        System.out.println("  " + SCENARIO_SOLAR_SYSTEM + ") Solar System");
        System.out.println("  " + SCENARIO_TRIAX + ") Triax (three-body equilateral-triangle configuration)");
        System.out.println("  " + SCENARIO_RANDOM + ") Random planetoid cluster");
        while (true) {
            int choice = promptInt(scanner, "Selection", SCENARIO_RANDOM);
            if (choice == SCENARIO_SOLAR_SYSTEM || choice == SCENARIO_TRIAX || choice == SCENARIO_RANDOM) {
                return choice;
            }
            System.out.println("Enter " + SCENARIO_SOLAR_SYSTEM + ", " + SCENARIO_TRIAX
                    + ", or " + SCENARIO_RANDOM + ".");
        }
    }

    /**
     * Prompts on stdin for a double, showing the default and accepting it on
     * a blank line; re-prompts on unparseable input. Falls back to the
     * default immediately if no interactive input is available at all (e.g.
     * stdin isn't a terminal), rather than blocking or throwing.
     */
    private static double promptDouble(Scanner scanner, String label, double defaultValue) {
        while (true) {
            String line = promptLine(scanner, label, String.valueOf(defaultValue));
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException ex) {
                System.out.println("Not a number, try again.");
            }
        }
    }

    /** Same as {@link #promptDouble}, but for an int. */
    private static int promptInt(Scanner scanner, String label, int defaultValue) {
        while (true) {
            String line = promptLine(scanner, label, String.valueOf(defaultValue));
            if (line.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("Not a whole number, try again.");
            }
        }
    }

    private static String promptLine(Scanner scanner, String label, String defaultDisplay) {
        System.out.print(label + " [" + defaultDisplay + "]: ");
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException ex) {
            return ""; // no interactive input available (e.g. stdin isn't a terminal)
        }
    }

    /** increase the screen scaling */
    void zoomIn() {
        m2pix = 2.0 * m2pix;
        resizeGL();
    }

    /** decrease the screen scaling */
    void zoomOut() {
        m2pix = 0.5 * m2pix;
        resizeGL();
    }

    /** how many scroll units (accumulated yoffset) it takes to double/halve m2pix - trackpads
        report many small-magnitude scroll events per gesture rather than one per discrete
        "notch" like a mouse wheel, so scaling directly by yoffset (as if one full unit meant
        one full +/- zoom step, per SCROLL_UNITS_PER_OCTAVE == 1) made a small gesture zoom
        drastically too far; 5 spreads that same one-octave change across five scroll units
        instead, matching a reported 5x-too-sensitive feel                                  */
    static final double SCROLL_UNITS_PER_OCTAVE = 5.0;

    /**
     * Mouse scroll wheel handler: zooms in/out on the screen center, same direction sense as
     * +/-, but scaled continuously with the scrolled amount (unlike +/-'s fixed per-press
     * step) so a fast/long scroll zooms further than a short one.
     * @param yoffset vertical scroll amount from GLFW's scroll callback; positive is
     *                "away from the user" (scroll up / forward), which conventionally
     *                zooms in, matching map and image-viewer scroll behavior
     */
    void handleScroll(double yoffset) {
        m2pix *= Math.pow(2.0, yoffset / SCROLL_UNITS_PER_OCTAVE);
        resizeGL();
    }

    /** double the simulation time step, speeding up how fast the system evolves */
    void speedUp() {
        OrbitalSystem.dt = 2.0 * OrbitalSystem.dt;
    }

    /** halve the simulation time step, slowing down how fast the system evolves */
    void slowDown() {
        OrbitalSystem.dt = 0.5 * OrbitalSystem.dt;
    }

    /**
     * move screen center to the left by a fixed number of pixels
     * must convert delta pixels into meters
     */
    void shiftLeft() {
        centerX -= SHIFT_PIX/m2pix;
        updateSystemCenter();
    }

    /**
     * move screen center to the right by a fixed number of pixels
     * must convert delta pixels into meters
     */
    void shiftRight() {
        centerX += SHIFT_PIX/m2pix;
        updateSystemCenter();
    }

    /**
     * move screen center to the left by a fixed number of pixels
     * must convert delta pixels into meters
     */
    void shiftUp() {
        centerY += SHIFT_PIX/m2pix;
        updateSystemCenter();
    }

    /**
     * move screen center to the right by a fixed number of pixels
     * must convert delta pixels into meters
     */
    void shiftDown() {
        centerY -= SHIFT_PIX/m2pix;
        updateSystemCenter();
    }

    /**
     * move screen center to system center (0,0)
     */
    private void setSystemCenterZeroZero() {
        centerY = centerX = 0;
        updateSystemCenter();
    }

    /**
     * move screen center point
     * centerX, centerY are in current screen pixels
     */
    void updateSystemCenter() {
        system.setCenterXY(
                centerX/OrbitalSystem.AU2m,
                centerY/OrbitalSystem.AU2m);
        resizeGL();
    }

    /**
     * S's key handler: writes the system's CURRENT state (whatever it presently is,
     * not necessarily its original initial conditions) to STATE_FILE, in the
     * schema/orbital-system.schema.json format. This becomes the new checkpoint R
     * reloads. Also called once from the constructor, so a valid file always exists
     * even before the user presses S themselves.
     */
    void saveState() {
        try {
            system.saveToFile(STATE_FILE);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not save state to " + STATE_FILE, ex);
        }
    }

    /**
     * R's key handler: reloads the system from STATE_FILE (as last written by
     * saveState()) and pauses it, replacing the old in-memory
     * OrbitalSystem.resetToIC() behavior. Pausing (rather than immediately running)
     * gives a chance to inspect/select bodies in the reloaded state before it starts
     * evolving. Since this discards the old OrbitalSystem object wholesale, any
     * hover/selection referencing its bodies is cleared first to avoid dangling
     * references into a system that's no longer displayed.
     */
    void reloadFromFile() {
        OrbitalSystem loaded;
        try {
            loaded = OrbitalSystem.loadFromFile(STATE_FILE);
        } catch (IOException | RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Could not reload state from " + STATE_FILE, ex);
            return;
        }
        system = loaded;
        hoveredBody = null;
        selectedBody = null;
        binHoveredBodies = List.of();
        setSystemCenterZeroZero();
        run = false;
    }

    /**
     * V's key handler: toggles Body.isHighlighted (brighter trail, larger minimum
     * disk size) on every body in the system at once, same as the old global highViz
     * flag - unconditionally, regardless of whether a body is currently selected.
     * A selected body always stays highlighted either way, since Body.glRender()
     * computes {@code highlighted = isHighlighted || selected || hovered}; this just
     * means V keeps working as a plain toggle for every other body too, instead of
     * being blocked by an active selection.
     */
    void toggleHighlight() {
        for (Body body : system.bodies) {
            body.isHighlighted = !body.isHighlighted;
        }
    }

    /**
     * Updates which body (if any) is under the mouse cursor and, while the left button is
     * held down, pans the view by the cursor's movement since the last call (click-and-drag
     * panning) - see handleMouseButton() for how this coexists with click-to-select.
     * @param cursorX cursor X, in window content-area coordinates (same units as DISPLAY_WIDTH)
     * @param cursorY cursor Y, in window content-area coordinates (same units as DISPLAY_HEIGHT)
     */
    void handleMouseMove(double cursorX, double cursorY) {
        if (leftButtonDown) {
            // pan by the cursor's movement since the last event, so the world point that was
            // under the cursor at the start of the drag stays under it: X is un-flipped
            // (screen and world X agree) but Y is flipped (cursor is top-down, world is
            // bottom-up) - same flip as the worldX/worldY conversion below
            centerX -= (cursorX - lastCursorX) / m2pix;
            centerY += (cursorY - lastCursorY) / m2pix;
            updateSystemCenter();

            double totalDX = cursorX - dragStartCursorX;
            double totalDY = cursorY - dragStartCursorY;
            if (Math.hypot(totalDX, totalDY) > DRAG_THRESHOLD_PIX) {
                dragOccurred = true;
            }
        }
        lastCursorX = cursorX;
        lastCursorY = cursorY;

        // histogram bars live in the same 1:1-pixel, screen-centered space render()'s HUD text
        // uses (post resizeGL(1f)), not the centeredBody-relative, m2pix-scaled world space bodies
        // render in - so hit-testing against them needs its own (unscaled) conversion
        int hitBin = -1;
        if (showHistogram) {
            float hudX = (float) (cursorX - DISPLAY_WIDTH / 2.0);
            float hudY = (float) (DISPLAY_HEIGHT / 2.0 - cursorY);
            hitBin = findHistogramBinAt(hudX, hudY);
        }
        updateBinHover(hitBin);

        if (hitBin >= 0) {
            // cursor is over the histogram; a body-hover highlight underneath would be confusing
            // alongside the bin highlight, so clear it rather than hit-testing bodies at all
            if (hoveredBody != null) {
                hoveredBody.hovered = false;
                hoveredBody = null;
            }
            return;
        }

        // invert resizeGL()'s ortho projection: screen center is world origin (relative to
        // centeredBody), Y flips since the cursor is top-down but the world is bottom-up
        double worldX = (cursorX - DISPLAY_WIDTH  / 2.0) / m2pix;
        double worldY = (DISPLAY_HEIGHT / 2.0 - cursorY) / m2pix;

        // HIT_RADIUS_PIX (not the render-only 1px minR glRender() clamps to) so tiny/zoomed-out
        // bodies stay hoverable/clickable without making their rendered dot any bigger
        Body hit = system.findBodyAt(worldX, worldY, (float) (HIT_RADIUS_PIX / m2pix));
        if (hit != hoveredBody) {
            if (hoveredBody != null) {
                hoveredBody.hovered = false;
            }
            hoveredBody = hit;
            if (hoveredBody != null) {
                hoveredBody.hovered = true;
            }
        }
    }

    /**
     * Which histogram bin (if any) the given HUD-space point falls on, hit-testing against the bar
     * "track" each bin occupies (from the right-edge baseline leftward by the layout's
     * maxBarLength, regardless of that particular bin's actual bar length, and only within the
     * bar's own thickness, not the gap below it) rather than requiring the cursor land on a
     * possibly very short or absent bar - matching how a scrollbar track is easier to grab than
     * the thumb itself.
     * @param hudX cursor X in the same 1:1-pixel, screen-centered space render()'s HUD text uses
     * @param hudY cursor Y in that same space
     * @return the bin index (0 = smallest, matching glRenderSizeHistogram()'s bottom-to-top
     *         order), or -1 if the point isn't over any bin's track
     */
    private int findHistogramBinAt(float hudX, float hudY) {
        float rightX = histogramLayout.rightX();
        float bottomY = histogramLayout.bottomY();
        float barThickness = histogramLayout.barThickness();
        float gap = histogramLayout.gap();
        float maxBarLength = histogramLayout.maxBarLength();
        int numBins = histogramLayout.numBins();
        float binSlot = barThickness + gap;

        if (hudX < rightX - maxBarLength || hudX > rightX) {
            return -1;
        }
        if (hudY < bottomY || hudY >= bottomY + numBins * binSlot) {
            return -1;
        }
        int bin = (int) ((hudY - bottomY) / binSlot);
        float offsetInSlot = hudY - (bottomY + bin * binSlot);
        if (offsetInSlot > barThickness) {
            return -1; // in the gap between bars, not on one
        }
        if (bin < 0) bin = 0;
        if (bin >= numBins) bin = numBins - 1;
        return bin;
    }

    /**
     * Updates which bodies are highlighted because the cursor is hovering their histogram bar
     * (see findHistogramBinAt()): clears hover on the previous bin's bodies and sets it on the new
     * bin's, mirroring how the single-body hoveredBody hover works, but for every body in a size
     * bin at once rather than just one.
     * @param binIndex the bin now under the cursor, or -1 if none
     */
    private void updateBinHover(int binIndex) {
        for (Body body : binHoveredBodies) {
            body.hovered = false;
        }
        binHoveredBodies = (binIndex >= 0)
                ? system.getBodiesInSizeBin(binIndex, histogramLayout.numBins())
                : List.of();
        for (Body body : binHoveredBodies) {
            body.hovered = true;
        }
    }

    /**
     * Left mouse button press/release handler. A press just starts tracking a candidate
     * drag (records the cursor position, doesn't select anything yet); a release either
     * selects/deselects (handleMouseClick()) if the cursor never moved past
     * DRAG_THRESHOLD_PIX since the press, or - if it did - is simply the end of a
     * click-and-drag pan (handleMouseMove() already did the panning as it happened), so no
     * selection change happens. This lets click-to-select and click-and-drag-to-pan share
     * the same button without fighting each other.
     * @param action GLFW_PRESS or GLFW_RELEASE
     */
    void handleMouseButton(int action) {
        if (action == GLFW_PRESS) {
            leftButtonDown = true;
            dragOccurred = false;
            dragStartCursorX = lastCursorX;
            dragStartCursorY = lastCursorY;
        } else if (action == GLFW_RELEASE) {
            leftButtonDown = false;
            if (!dragOccurred) {
                handleMouseClick();
            }
        }
    }

    /**
     * Selects whichever body is currently hovered (clearing any previously
     * selected body first), or deselects if the click was on empty space.
     */
    void handleMouseClick() {
        if (selectedBody != null) {
            selectedBody.selected = false;
        }
        selectedBody = hoveredBody;
        if (selectedBody != null) {
            selectedBody.selected = true;
        }
    }

    /**
     * Tab/Shift-Tab key handler: shifts selection to the next (forward) or previous
     * body in OrbitalSystem.bodies, cycling around at either end. If no body is
     * currently selected (or the previously-selected one is no longer in the list,
     * e.g. absorbed in a collision), forward selects the first body and backward the
     * last. Does nothing if there are no bodies at all.
     * @param forward true for Tab (next body), false for Shift-Tab (previous body)
     */
    void shiftSelection(boolean forward) {
        List<Body> bodies = system.bodies;
        if (bodies.isEmpty()) {
            return;
        }
        int currentIndex = (selectedBody != null) ? bodies.indexOf(selectedBody) : -1;
        int nextIndex;
        if (currentIndex < 0) {
            nextIndex = forward ? 0 : bodies.size() - 1;
        } else {
            nextIndex = forward
                    ? (currentIndex + 1) % bodies.size()
                    : (currentIndex - 1 + bodies.size()) % bodies.size();
        }
        if (selectedBody != null) {
            selectedBody.selected = false;
        }
        selectedBody = bodies.get(nextIndex);
        selectedBody.selected = true;
    }

    /**
     * Handles a single key press (repeats and releases are ignored, matching
     * the original Keyboard.enableRepeatEvents(false) behavior).
     * @param mods bitmask of GLFW_MOD_* modifiers held during the press (only
     *             GLFW_MOD_SHIFT is currently consulted: to distinguish Tab from
     *             Shift-Tab, and Slash from Shift-Slash i.e. '?')
     */
    void handleKeyPress(int key, int mods) {
        switch (key) {
            case GLFW_KEY_EQUAL:  zoomIn();               break;
            case GLFW_KEY_MINUS:  zoomOut();              break;
            case GLFW_KEY_PAGE_UP:   speedUp();           break;
            case GLFW_KEY_PAGE_DOWN: slowDown();          break;
            case GLFW_KEY_LEFT:   shiftLeft();            break;
            case GLFW_KEY_RIGHT:  shiftRight();           break;
            case GLFW_KEY_UP:     shiftUp();              break;
            case GLFW_KEY_DOWN:   shiftDown();            break;
            case GLFW_KEY_HOME:   setSystemCenterZeroZero(); break;
            case GLFW_KEY_P:      run = !run;             break;
            case GLFW_KEY_R:      reloadFromFile();       break;
            case GLFW_KEY_S:      saveState();            break;
            case GLFW_KEY_V:      toggleHighlight();     break;
            case GLFW_KEY_H:      showHistogram = !showHistogram; break;
            case GLFW_KEY_SLASH:  if ((mods & GLFW_MOD_SHIFT) != 0) { showHelp = !showHelp; } break;
            case GLFW_KEY_TAB:    shiftSelection((mods & GLFW_MOD_SHIFT) == 0); break;
            case GLFW_KEY_0:      system.setCenterOnNthLargest(0);    break;
            case GLFW_KEY_1:      system.setCenterOnNthLargest(1);    break;
            case GLFW_KEY_2:      system.setCenterOnNthLargest(2);    break;
            case GLFW_KEY_3:      system.setCenterOnNthLargest(3);    break;
            case GLFW_KEY_4:      system.setCenterOnNthLargest(4);    break;
            case GLFW_KEY_5:      system.setCenterOnNthLargest(5);    break;
            case GLFW_KEY_6:      system.setCenterOnNthLargest(6);    break;
            case GLFW_KEY_7:      system.setCenterOnNthLargest(7);    break;
            case GLFW_KEY_8:      system.setCenterOnNthLargest(8);    break;
            case GLFW_KEY_9:      system.setCenterOnNthLargest(9);    break;
        }
    }

    /** run loop until window is closed or escape key is down */
    void run() {
        while (!glfwWindowShouldClose(window)
                && glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS) {
            glfwPollEvents();
            if (run) update();
            render();
            glfwSwapBuffers(window);
        }
    }

    /**
     * Open the display window, set the title, create the keyboard and mouse,
     * initialize the graphics
     */
    void create() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new RuntimeException("Unable to initialize GLFW");
        }

        // fill the whole screen: size the window to the primary monitor's current video
        // mode and pass that monitor to glfwCreateWindow, which makes it a true fullscreen
        // window on that display rather than a fixed-size windowed one
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidMode = glfwGetVideoMode(monitor);
        if (vidMode == null) {
            throw new RuntimeException("Unable to query primary monitor's video mode");
        }
        DISPLAY_WIDTH = vidMode.width();
        DISPLAY_HEIGHT = vidMode.height();
        uiScale = DISPLAY_HEIGHT / REFERENCE_DISPLAY_HEIGHT;

        // no version/profile hints: on macOS this yields a legacy 2.1 context
        // (required for the immediate-mode/fixed-function rendering below);
        // elsewhere it yields the default compatibility context.
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(DISPLAY_WIDTH, DISPLAY_HEIGHT, "Orbital Mechanics", monitor, 0);
        if (window == 0) {
            glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                handleKeyPress(key, mods);
            }
        });
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> handleMouseMove(xpos, ypos));
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                handleMouseButton(action);
            }
        });
        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> handleScroll(yoffset));

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // vsync, replaces LWJGL2's Display.sync(60)
        GL.createCapabilities();

        // On HiDPI (Retina) displays the drawable framebuffer is larger, in
        // actual pixels, than the window's logical DISPLAY_WIDTH/HEIGHT; the
        // viewport must cover the real framebuffer or rendering is confined
        // to a corner of the window.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            framebufferWidth = w.get(0);
            framebufferHeight = h.get(0);
        }

        glfwShowWindow(window);
        // On macOS, an unbundled command-line java process doesn't get
        // automatically activated: glfwShowWindow() alone leaves the window
        // ordered-front only within its own (background) app, so it renders
        // behind whatever app already has focus (e.g. the launching Terminal).
        // Explicitly focus it so the window is actually visible to the user.
        glfwFocusWindow(window);
        // On Windows, that glfwFocusWindow() call can itself be silently denied:
        // Win32's SetForegroundWindow refuses to hand over the foreground once the
        // launching console's grace period has lapsed, which JVM startup plus LWJGL's
        // native-library extraction routinely outlasts. The window is visible but
        // never actually receives keyboard focus, so keystrokes keep going to the
        // launching console (which swallows them) until the user manually clicks the
        // window; Ctrl-C still "works" because it's a console signal, not a GLFW key
        // event. Iconifying and immediately restoring goes through a different Win32
        // code path that isn't subject to that same foreground-lock restriction, and
        // reliably grabs real focus.
        glfwIconifyWindow(window);
        glfwRestoreWindow(window);
        glfwFocusWindow(window);

        //OpenGL
        initGL();
        resizeGL();
        initFont();
        histogramLayout = computeHistogramLayout();
        setSystemCenterZeroZero();
    }

    /**
     * Lays out the size histogram just below the "N bodies"/time text's last
     * line (computed from its actual geometry so this stays correct if the
     * text position or font size ever changes), right-justified with room
     * to its right for the size labels.
     */
    private HistogramLayout computeHistogramLayout() {
        // bar thickness/gap derivation history: original design used a fixed-fraction layout
        // (300px total height / 12 bins, 0.8275 fill fraction); bars were later shrunk to 1/4 that
        // thickness, then doubled back to 1/2 - kept as named constants rather than a fraction
        // recomputed every frame, since none of this depends on anything that changes at runtime
        final int numBins = 12;
        final float oldBinSlot = 300f / numBins;      // previous (pre-shrink) per-bin slot size
        final float oldBarThickness = oldBinSlot * 0.8275f; // previous bar fill fraction
        final float gap = (oldBinSlot - oldBarThickness) * uiScale;     // gap to preserve
        final float barThickness = (oldBarThickness / 2f) * uiScale;    // 1/4 as thick, then doubled: net 1/2
        final float maxBarLength = 150f * uiScale;

        float textBottomY = (DISPLAY_HEIGHT/2f - 50f * uiScale) - trueTypeFont.getHeight();
        float bottomY = textBottomY - 20f * uiScale - numBins * (barThickness + gap);
        float rightX = DISPLAY_WIDTH/2f - 100 * uiScale; // leaves room for the size labels

        return new HistogramLayout(rightX, bottomY, barThickness, gap, maxBarLength, numBins);
    }

    /**
     * Close everything
     */
    void destroy() {
        if (trueTypeFont != null) {
            trueTypeFont.destroy();
        }
        if (window != 0) {
            glfwDestroyWindow(window);
        }
        glfwTerminate();
        GLFWErrorCallback previous = glfwSetErrorCallback(null);
        if (previous != null) {
            previous.free();
        }
    }

    /**
     * Initialized the graphics engine
     */
    void initGL() {
        //2D Initialization
        glEnable(GL_COLOR_MATERIAL);
        glDisable(GL_DITHER);
        glDepthFunc(GL_LESS); // Depth function less or equal
        glEnable(GL_NORMALIZE); // calculated normals when scaling
        glEnable(GL_CULL_FACE); // prevent render of back surface
        glEnable(GL_BLEND); // Enabled blending
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // selects blending method
        glEnable(GL_ALPHA_TEST); // allows alpha channels or transperancy
        glAlphaFunc(GL_GREATER, 0.1f); // sets aplha function
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // High quality visuals
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST); //  Really Nice Perspective Calculations
        glShadeModel(GL_SMOOTH); // Enable Smooth Shading

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // black background
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);
    }

    /**
     * Update the graphics scale, viewport, modelview matrix
     * This provides a default scaling of m2pix
     */
    void resizeGL() {
        resizeGL( (float)m2pix );
    }
    /**
     * Update the graphics scale, viewport, modelview matrix
     * 2D display with variable screen scaling
     */
    void resizeGL( float scale) {
        //2D Scene
        glViewport(0, 0, framebufferWidth, framebufferHeight);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(-0.5f*DISPLAY_WIDTH  / scale,
                 0.5f*DISPLAY_WIDTH  / scale,
                -0.5f*DISPLAY_HEIGHT / scale,
                 0.5f*DISPLAY_HEIGHT / scale,
                -1.0, 1.0);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        int error = glGetError();
        if (error != 0) {
            throw new RuntimeException();
        }
    }

    /**
     * Set up the font for drawing strings
     */
    void initFont() {
        trueTypeFont = new TrueTypeFont("/fonts/Inconsolata-Regular.ttf", 18f * uiScale);
    }

    /**
     * Move the model one step ahead in time
     */
    public void update() {
        system.Propagate();
    }

    /**
     * Draw the solar system at a given scale
     */
    public void render() {

        int error;

        // fit once per frame (rather than separately in drawSelectedBodyEllipse() and
        // buildSelectionInfoText(), which both want it) - null if there's no selection,
        // not enough trail history yet, or the trail doesn't currently fit an ellipse
        Ellipse selectedBodyEllipse = null;
        if (selectedBody != null) {
            try {
                selectedBodyEllipse = selectedBody.fitEllipse();
            } catch (IllegalStateException ex) {
                // handled below by each user of selectedBodyEllipse treating null as
                // "nothing to draw/describe yet"
            }
        }

        resizeGL();
        glDisable(GL_TEXTURE_2D); // Disable Texture Mapping
        glClear(GL_COLOR_BUFFER_BIT);
        glLoadIdentity();
        glColor3f(1.0f, 1.0f, 1.0f); // white
        system.glRender( (float) (1.0/m2pix) );
        error = glGetError();
        if (error != 0) {
            throw new RuntimeException();
        }

        if (selectedBodyEllipse != null) {
            drawSelectedBodyEllipse(selectedBodyEllipse);
            error = glGetError();
            if (error != 0) {
                throw new RuntimeException();
            }
        }

        resizeGL(1f);
        glEnable(GL_TEXTURE_2D); // Enable Texture Mapping
        if (run) {
            glColor3f(0f,1f,0f);
        } else {
            glColor3f(1f,0f,0f);
        }
        trueTypeFont.drawString(DISPLAY_WIDTH/2 - 10, DISPLAY_HEIGHT/2 - 50,
                    "" + system.getNumBodies() + " bodies\n"
                  + system.getFormattedTime(), 1f, 1f, TrueTypeFont.ALIGN_RIGHT);
        error = glGetError();
        if (error != 0) {
            throw new RuntimeException();
        }

        // always plain white, unlike the run-state green/red HUD text above, since this is
        // a constant hint rather than status that changes with P
        glColor3f(1f, 1f, 1f);
        trueTypeFont.drawString(0, -DISPLAY_HEIGHT/2f + 30,
                "'p' to run/pause, '?' for help, 'Esc' to exit", 1f, 1f, TrueTypeFont.ALIGN_CENTER);
        error = glGetError();
        if (error != 0) {
            throw new RuntimeException();
        }

        if (showHistogram) {
            // color (and the median-size labels) match the run-state HUD text above - re-asserted
            // here since the bottom hint text drawn just above this switched the current color to
            // plain white; layout computed once in create(), see computeHistogramLayout()
            if (run) {
                glColor3f(0f,1f,0f);
            } else {
                glColor3f(1f,0f,0f);
            }
            system.glRenderSizeHistogram(trueTypeFont, histogramLayout, selectedBody);
            error = glGetError();
            if (error != 0) {
                throw new RuntimeException();
            }
        }

        if (showHelp) {
            float helpX = -DISPLAY_WIDTH/2f + 20;
            float helpY = DISPLAY_HEIGHT/2f - 20;
            glColor3f(1f, 1f, 1f);
            trueTypeFont.drawString(helpX, helpY, HELP_TEXT, 1f, 1f, TrueTypeFont.ALIGN_LEFT);
            error = glGetError();
            if (error != 0) {
                throw new RuntimeException();
            }

            // only meaningful when the current scenario recolors bodies by orbit shape
            // (Body.updateOrbitColor(), gated on OrbitalSystem.colorByOrbitShape) - appended
            // below the rest of the help text rather than baked into the constant HELP_TEXT,
            // since it doesn't apply to every scenario
            if (system.colorByOrbitShape) {
                int helpLineCount = HELP_TEXT.split("\n", -1).length;
                float lineY = helpY - (helpLineCount + 1) * trueTypeFont.getHeight();

                glColor3f(1f, 1f, 1f);
                trueTypeFont.drawString(helpX, lineY, "Orbital shape (from curve fit):", 1f, 1f, TrueTypeFont.ALIGN_LEFT);
                lineY -= trueTypeFont.getHeight();

                // colors match Body.updateOrbitColor()'s Color.lightRed()/Color.lightBlue()
                glColor3f(1.0f, 0.4f, 0.4f);
                trueTypeFont.drawString(helpX, lineY, "Red", 1f, 1f, TrueTypeFont.ALIGN_LEFT);
                glColor3f(1f, 1f, 1f);
                trueTypeFont.drawString(helpX + trueTypeFont.getWidth("Red"), lineY,
                        " shows hyperbolic/indeterminate", 1f, 1f, TrueTypeFont.ALIGN_LEFT);
                lineY -= trueTypeFont.getHeight();

                glColor3f(0.4f, 0.7f, 1.0f);
                trueTypeFont.drawString(helpX, lineY, "Blue", 1f, 1f, TrueTypeFont.ALIGN_LEFT);
                glColor3f(1f, 1f, 1f);
                trueTypeFont.drawString(helpX + trueTypeFont.getWidth("Blue"), lineY,
                        " indicates elliptical", 1f, 1f, TrueTypeFont.ALIGN_LEFT);

                error = glGetError();
                if (error != 0) {
                    throw new RuntimeException();
                }
            }
        }

        if (selectedBody != null) {
            // always plain white, regardless of the selected body's own color or the run-state
            // green/red used above, so the panel stays legible no matter which body it's about
            glColor3f(1f, 1f, 1f);
            float[] anchor = selectionInfoAnchor(selectedBody);
            trueTypeFont.drawString(anchor[0], anchor[1],
                    buildSelectionInfoText(selectedBody, selectedBodyEllipse),
                    SELECTION_INFO_SCALE, SELECTION_INFO_SCALE, TrueTypeFont.ALIGN_LEFT);
            error = glGetError();
            if (error != 0) {
                throw new RuntimeException();
            }
        }

    }

    /**
     * Draws a dashed ellipse fitted to the selected body's current trail
     * (Body.fitEllipse()), in the same centeredBody-relative world space
     * system.glRender() just drew bodies/trails in - must be called before
     * resizeGL(1f) switches to the 1:1-pixel HUD space. Refit fresh every frame
     * from whatever the trail currently holds, so the ellipse continually
     * reshapes as the trail grows and the orbit is perturbed by other bodies,
     * rather than being a one-time fit locked to some past moment. Drawn as a
     * dashed line (alternating drawn/skipped runs of sampled segments, since
     * this legacy/fixed-function GL context has no shader-based line style) in
     * the body's own color at full intensity, so it reads as distinct from the
     * selected body's own (dimmer, solid) trail line. Does nothing if the body
     * doesn't yet have enough trail history to fit an ellipse, or its trail
     * doesn't currently fit one (e.g. hyperbolic/parabolic) - see render()'s
     * selectedBodyEllipse, which is null in either of those cases.
     * @param fit the selected body's already-fitted ellipse (render() fits this once per
     *            frame and shares it with buildSelectionInfoText() too)
     */
    private void drawSelectedBodyEllipse(Ellipse fit) {
        double centerX = (system.centeredBody != null) ? system.centeredBody.getX() : 0.0;
        double centerY = (system.centeredBody != null) ? system.centeredBody.getY() : 0.0;

        double cosA = Math.cos(fit.angle());
        double sinA = Math.sin(fit.angle());
        float[] xs = new float[ELLIPSE_SEGMENTS + 1];
        float[] ys = new float[ELLIPSE_SEGMENTS + 1];
        for (int i = 0; i <= ELLIPSE_SEGMENTS; i++) {
            double t = 2.0 * Math.PI * i / ELLIPSE_SEGMENTS;
            double localX = fit.semiMajor() * Math.cos(t);
            double localY = fit.semiMinor() * Math.sin(t);
            xs[i] = (float) (fit.centerX() + localX*cosA - localY*sinA - centerX);
            ys[i] = (float) (fit.centerY() + localX*sinA + localY*cosA - centerY);
        }

        Color c = selectedBody.getColor();
        glColor3f(c.r(), c.g(), c.b());
        glBegin(GL_LINES);
        for (int i = 0; i < ELLIPSE_SEGMENTS; i++) {
            if (i % (ELLIPSE_DASH_ON_SEGMENTS + ELLIPSE_DASH_OFF_SEGMENTS) < ELLIPSE_DASH_ON_SEGMENTS) {
                glVertex2f(xs[i], ys[i]);
                glVertex2f(xs[i + 1], ys[i + 1]);
            }
        }
        glEnd();
    }

    static final float SELECTION_INFO_SCALE = 0.6f; /** data block text size, as a fraction
                                                          of the HUD's baked font size        */

    static final int ELLIPSE_SEGMENTS = 96;     /** points sampled around the fitted ellipse   */
    static final int ELLIPSE_DASH_ON_SEGMENTS  = 2; /** drawn segments per dash-pattern cycle   */
    static final int ELLIPSE_DASH_OFF_SEGMENTS = 2; /** skipped segments per dash-pattern cycle */

    /**
     * Position, in the same screen-pixel space {@code render()}'s HUD text is drawn in
     * (origin at screen center, Y-up, 1 unit = 1 pixel), for the given body's data block:
     * offset diagonally down-and-right into its lower-right quadrant, clear of its own
     * on-screen disk, like an air-traffic control radar data block tracks its target - it
     * moves every frame along with whatever the body is currently doing (its own motion,
     * panning, zooming), rather than sitting in a fixed screen corner.
     */
    private float[] selectionInfoAnchor(Body body) {
        double cx = (system.centeredBody != null) ? system.centeredBody.getX() : 0.0;
        double cy = (system.centeredBody != null) ? system.centeredBody.getY() : 0.0;
        double bodyScreenX = (body.getX() - cx) * m2pix;
        double bodyScreenY = (body.getY() - cy) * m2pix;

        // same minR-clamp glRender()/findBodyAt() use, boosted 4x since a selected body is
        // always rendered highlighted, so the block clears the disk at any zoom/body size
        double minRMeters = 1.0 / m2pix;
        double onScreenRadiusMeters = Math.max(body.getRadius(), 4.0 * minRMeters);
        double onScreenRadiusPx = onScreenRadiusMeters * m2pix;

        double margin = 8.0;
        return new float[] {
                (float) (bodyScreenX + onScreenRadiusPx + margin),
                (float) (bodyScreenY - onScreenRadiusPx - margin)
        };
    }

    /**
     * Builds the multi-line info panel text for the currently selected body: its
     * optional name, disk size in Earth radii, inertial speed, and orbit-shape
     * classification (with an estimated period alongside it, for an elliptical orbit -
     * see describeOrbitShape()).
     * @param fit the body's already-fitted ellipse (null if none - see render()), shared
     *            with drawSelectedBodyEllipse() rather than re-fitting here
     */
    private static String buildSelectionInfoText(Body body, Ellipse fit) {
        StringBuilder sb = new StringBuilder();
        if (body.getName() != null) {
            sb.append(body.getName()).append('\n');
        }
        sb.append("Size: ")
          .append(OrbitalSystem.formatRadiusER(body.getRadius() / OrbitalSystem.earthRadius))
          .append(" ER\n");
        sb.append("Velocity: ")
          .append(String.format(Locale.US, "%.2f km/s", inertialSpeedKmS(body)))
          .append('\n');
        sb.append("Orbit: ").append(describeOrbitShape(body, fit));
        return sb.toString();
    }

    /**
     * The body's inertial speed - the magnitude of its (U, V) velocity components - in km/s.
     */
    private static double inertialSpeedKmS(Body body) {
        double u = body.getU();
        double v = body.getV();
        return Math.sqrt(u*u + v*v) / 1000.0;
    }

    /**
     * Classifies the body's current orbit shape and, for an elliptical orbit, its
     * estimated period - kept to one line rather than adding a second, so an elliptical
     * orbit's classification is abbreviated ("Ell") to make room for the period
     * alongside it (e.g. "Ell (3 d 04 hr)"), via Body.estimatePeriod(). A
     * hyperbolic/parabolic orbit is unbound and has no period to show, so those stay
     * their full, unabbreviated words with nothing appended. A non-null fit means
     * fitEllipse() already succeeded (see render()), so the orbit is currently
     * elliptical; a null fit falls back to Body.findDiscriminantLeastSquares() (which
     * needs at least 5 recorded trail points) to tell hyperbolic from parabolic, or
     * reports there isn't enough trail history yet otherwise (e.g. right after selecting
     * a body, or one with a short/zero trail length).
     * @param fit the body's already-fitted ellipse, or null if its trail doesn't
     *            currently fit one
     */
    private static String describeOrbitShape(Body body, Ellipse fit) {
        if (fit != null) {
            try {
                return "Ell (" + formatDuration(body.estimatePeriod(fit)) + ")";
            } catch (IllegalStateException ex) {
                return "Ell";
            }
        }
        double discriminant;
        try {
            discriminant = body.findDiscriminantLeastSquares();
        } catch (IllegalStateException ex) {
            return "(need more trail history)";
        }
        return (discriminant > 0.0) ? "Hyperbolic" : "Parabolic";
    }

    /**
     * Formats a duration (an estimated orbital period, seconds) at whichever of three
     * tiers suits its magnitude - years+days, days+hours, or hours+minutes - rather than
     * a single fixed unit that would read as either absurdly large (minutes for a
     * multi-year period) or as "0" (years for a sub-day one). Only the smallest shown
     * unit (minutes) gets a leading zero; the larger units are never zero-padded since
     * they're the leading part of the string.
     * @param seconds duration to format, seconds
     */
    private static String formatDuration(double seconds) {
        if (seconds >= OrbitalSystem.YEAR) {
            int years = (int) (seconds / OrbitalSystem.YEAR);
            double remainder = seconds - years * OrbitalSystem.YEAR;
            int days = (int) (remainder / OrbitalSystem.DAY);
            return years + " yr " + days + " d";
        } else if (seconds >= OrbitalSystem.DAY) {
            int days = (int) (seconds / OrbitalSystem.DAY);
            double remainder = seconds - days * OrbitalSystem.DAY;
            int hours = (int) (remainder / OrbitalSystem.HOUR);
            return days + " d " + hours + " hr";
        } else {
            int hours = (int) (seconds / OrbitalSystem.HOUR);
            double remainder = seconds - hours * OrbitalSystem.HOUR;
            int minutes = (int) (remainder / OrbitalSystem.MINUTE);
            return hours + " hr " + String.format(Locale.US, "%02d", minutes) + " min";
        }
    }

    static final String RELAUNCHED_PROPERTY = "orbit.relaunchedOnFirstThread";

    /**
     * On macOS, GLFW/Cocoa requires the JVM's main thread to be the process's
     * first thread, which a plain "java -jar Orbit.jar" cannot guarantee.
     * If we're not already flagged as relaunched, re-exec ourselves with
     * -XstartOnFirstThread and wait for the child to finish.
     * @return true if we relaunched (caller should not continue running)
     */
    private static boolean relaunchOnFirstThreadIfNeeded() {
        boolean isMac = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("mac");
        if (!isMac || System.getProperty(RELAUNCHED_PROPERTY) != null) {
            return false;
        }
        try {
            String javaBin = System.getProperty("java.home")
                    + java.io.File.separator + "bin"
                    + java.io.File.separator + "java";
            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-XstartOnFirstThread");
            command.add("-D" + RELAUNCHED_PROPERTY + "=true");
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(Orbit.class.getName());
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            System.exit(process.waitFor());
        } catch (java.io.IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Failed to relaunch on first thread", ex);
        }
        return true;
    }

    /**
     * Main program entry point; instantiates us and enters the run loop
     * @param args These are unused
     */
    public static void main(String[] args) {
    if (relaunchOnFirstThreadIfNeeded()) {
        return;
    }
    Orbit main = null;
    try {
      System.out.println("esc   - Exit");
      main = new Orbit();
      main.create();
      main.run();
    }
    catch(Exception ex) {
      LOGGER.log(Level.SEVERE,ex.toString(),ex);
    }
    finally {
      if(main != null) {
        main.destroy();
      }
    }
  }
}
