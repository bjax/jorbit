// This file can be viewed as HTML using asciidoc

README for Orbit
================
:version:0.1
:author:Bruce Jackson

This file directory contains source for an orbital mechanics simulator, 'Orbit'.

Features
--------

Dependencies
------------

Requires Java 21+ and Maven (or use the bundled +./mvnw+ wrapper, which
needs no local Maven install). LWJGL 3 and its native libraries are
pulled automatically by Maven for the build machine's OS/architecture;
there's nothing to install by hand.

History
-------

Original version of this program was written by Bruce Jackson in the
mid 1980s as a test program for the ADI-100, a special-purpose
simulation computer, installed at the Naval Air Test Center's Manned
Flight Simulator facility in Patuxent River, Maryland.

A Macintosh application was written by Bruce Jackson in 1987, orbit,
and distributed via AOL libraries, which drew black objects against a
white background.

The code was rewritten in the mid 1990s for use on Silicon Graphics
computers at Langley Research Center.

It was then ported to Java around 2001 as a web app but was not
distributed. 

Bruce rewrote it again in 2011 as a demonstration for a high school
class on the application of calculus in simulation.

In 2026 the project was modernized: build moved from Ant/NetBeans to
Maven, rendering ported from LWJGL 2 to LWJGL 3 (GLFW), and tests
moved from JUnit 4 to JUnit 5.

Installation
------------

Build a self-contained, runnable jar with:

    ./mvnw package

This produces dist/Orbit.jar, which bundles LWJGL and its native
libraries for the build machine's OS/architecture and can be moved
wherever. Run it with:

    java -jar dist/Orbit.jar

On macOS, Orbit.jar relaunches itself with -XstartOnFirstThread as
needed (GLFW/Cocoa requires the JVM's main thread to be the process's
first thread); no extra flag is required from the caller.

For development, `./mvnw compile exec:exec` compiles and runs in place
without building the jar.


Execution
---------

On startup, Orbit prompts on the console for which scenario to run:

  1) Solar System - the real solar system's planets and major moons
  2) Triax - three equal bodies in a rotating equilateral triangle
  3) Random planetoid cluster (default)

then for each of that scenario's initial-condition parameters (number of
bodies, size range, step size, etc.), showing the current default in
brackets - press Enter to accept it, or type a new value.

A reminder - "'p' to run/pause, '?' for help, 'c'/'C' to circularize,
'Esc' to exit" - is always shown centered at the bottom of the screen.

Keyboard controls, once the window has focus:

P:: Pause/resume the simulation
S:: Save the system's current state - not necessarily its original initial
conditions, but whatever it presently is - to .JOS.json in the working
directory
R:: Reload the system from .JOS.json and pause it, replacing whatever is
currently on screen. A fresh .JOS.json is written automatically at
startup, so R always has something valid to load even before S has ever
been pressed. Since it's a plain JSON file (see
src/main/resources/schema/orbital-system.schema.json), it can be
hand-edited between S and R to try out a specific scenario.
Tab / Shift-Tab:: Select the next/previous body (in list order, wrapping
at either end)
V:: Toggle high-visibility rendering (larger disk, brighter trail) for
every body at once. A selected body always stays highlighted regardless
of this toggle.
H:: Toggle a log-scale histogram of body sizes (upper right, just below
the body-count/time text; largest-size bin at top, colored to match, all
12 bins labeled in Earth radii - median size for bins with bodies, the
bin's own size-range midpoint for empty ones). The selected body's own
size bin, if any, is drawn in a lighter shade so it stands out.
c:: Circularize every body's velocity into an orbit at 25% of full
circular speed about the system's current center of mass - not enough
speed to hold that radius, so bodies will actually spiral inward rather
than stay put. A one-shot reset, not a toggle - press again anytime to
re-circularize from wherever the bodies currently are.
Shift-C:: Same as c, but at 100% (true circular) speed.
+:: Zoom in
-:: Zoom out
Page Up:: Double the simulation time step (speed up)
Page Down:: Halve the simulation time step (slow down)
Left/Right/Up/Down:: Pan the view
Home:: Recenter the view on the origin
0-9:: Center the view on the Nth-largest body (0 = largest)
?:: Toggle a help panel (keyboard/mouse controls) down the left side of
the screen. Hidden by default. If the current scenario recolors bodies
by orbit shape (currently only the Random planetoid cluster), the panel
also explains it: red shows hyperbolic/indeterminate (from curve fit),
blue indicates elliptical.
Esc:: Exit

Mouse
~~~~~

Hovering over a body highlights it (same larger-disk/brighter-trail
effect as V) until the cursor moves off it. Clicking a body selects it,
making the highlight persist even after the cursor moves away; only one
body can be selected at a time. Clicking on empty space, or clicking
another body, deselects/reselects accordingly.

If the size histogram (H) is shown, hovering over one of its bars
instead highlights every body in that size bin at once - the cursor
only needs to be over the bar's row, not its exact (possibly very
short) length.

Clicking and dragging pans the view instead of selecting - the point
under the cursor when the drag started stays under the cursor as you
drag, like grabbing the canvas. A press only counts as a drag once the
cursor has moved a few pixels; anything short of that on release is
treated as a plain click/select instead.

The scroll wheel zooms in/out on the screen center, same as +/-.

While a body is selected, a data block next to it shows its name (if
any), size in Earth radii, and orbit shape (Elliptical/Parabolic/
Hyperbolic, once enough of its trail has been recorded to tell). The
block tracks the body every frame, like an air-traffic-control radar
tag follows its target.

Benchmarking
------------

`IntegratorBenchmark` (+src/main/java/org/jaxfam/orbit/IntegratorBenchmark.java+) is a standard,
reproducible benchmark for comparing the four `Integrator` implementations - `Euler1Integrator`,
`RungeKutta2Integrator`, `RungeKutta4Integrator` (the default `OrbitalSystem` always uses) and
`RungeKuttaKahan4Integrator` (a Kahan/compensated-summation variant of RK4) - against each other.
It builds an identical Random planetoid cluster (200 bodies, fixed seed 42, otherwise the same
defaults Orbit itself offers interactively - see `RandomSystem`'s seeded-`Random` constructor
overload, added for this), propagates it 2000 steps under each integrator in turn, and reports:

- total mechanical energy (kinetic + gravitational potential) before and after, and its relative
  drift - the true continuous N-body system conserves this exactly, so any drift is purely a
  numerical-integration artifact, not physics
- final body count (should equal the initial 200 for every integrator at this step count - see
  the caveat below)
- wall-clock time for the whole run

Run it with:

    ./mvnw compile exec:exec -Dexec.mainClass=org.jaxfam.orbit.IntegratorBenchmark

Reference output (Apple M-series, 2026; wall-clock time will vary by machine, but the energy
figures are bit-for-bit reproducible given the fixed seed):

    name         N0 Nfinal           final energy       relDrift         ms
    Euler1      200    200      -7.9323112986e+32   6.111428e-07      372.1
    RK2         200    200      -7.9323161464e+32  -6.104485e-14      504.7
    RK4         200    200      -7.9323161464e+32   6.685864e-14      824.5
    RKK4        200    200      -7.9323161464e+32   3.488277e-14      825.3

Reading these results: Euler1's first-order error is clearly visible (~1000x worse energy drift
than the three higher-order methods, though still small in absolute terms over only 2000 steps).
RK2, RK4, and RKK4 all conserve energy to within a few times machine epsilon (~1e-14 relative) -
indistinguishable from each other at this scenario's scale. RKK4's Kahan-compensated summation
gives no measurable accuracy improvement over plain RK4 here: compensation only recovers
precision when a step's position increment is smaller than one ULP of the running position
total, and at this scenario's typical position/velocity/dt magnitudes the per-step increment is
many orders of magnitude larger than that threshold, so there's essentially nothing for it to
compensate for. Timing-wise, RKK4 costs about the same as RK4 (the extra compensation
bookkeeping is a small fraction of the four `getStateDeriv()` calls RK4/RKK4 both make per step);
RK2's two calls and Euler1's one scale down accordingly.

Caveat: STEP_COUNT (2000) is deliberately kept below this seed's first body-to-body collision -
empirically found at step 3000 for RK2/RK4/RKK4 (Euler1's larger error keeps it away from the
same close encounter, at least within 5000 steps). Past that point, each integrator's own
(however slightly) different trajectory reaches the collision at a different exact moment, so
their body counts - and therefore their total-energy figures - stop describing the same physical
system, and a direct numerical comparison between integrators is no longer meaningful past that
point. `main()` still reports body count for every run and flags any mismatch, in case this
margin is ever eroded by a future change.

Bugs
----
