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

A reminder - "'p' to run/pause, '?' for help, 'Esc' to exit" - is always
shown centered at the bottom of the screen.

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

Bugs
----
