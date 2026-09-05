# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Orbit is a real-time 2D orbital mechanics simulator: it numerically integrates N-body gravitational
dynamics and renders the result with OpenGL (via LWJGL). It began as 1980s simulation-computer test
code, was ported through Mac/SGI/Java-applet incarnations, was rewritten in 2011 as a NetBeans Ant
project for a high-school calculus demonstration, and was modernized in 2026 to Maven/LWJGL 3/JUnit 5.
See `README.txt` for the full history. `archive/` holds the old C, applet, and Java-applet
predecessors — reference only, not built or maintained.

## Build system

Standard Maven layout (`src/main/java`, `src/test/java`). Java 21, LWJGL 3, JUnit 5, `org.json` (state
save/load, see Architecture below). Use the bundled wrapper so no local Maven install is required:

```
./mvnw compile                 # compile only
./mvnw test                    # run the full JUnit 5 suite (no LWJGL/display needed)
./mvnw test -Dtest=StateVectorTest         # run one test class
./mvnw package                 # build dist/Orbit.jar (self-contained, includes LWJGL natives)
./mvnw compile exec:exec       # compile and run in place, for development
./mvnw clean                   # remove target/
```

LWJGL's native libraries are resolved per build-machine OS/arch via Maven profiles keyed on `os.family`
+ `os.arch` in `pom.xml` (mac/aarch64, mac/x86_64, linux/aarch64, linux, windows) — nothing to install
by hand, unlike the old Ant setup which needed a hand-placed `liblwjgl.jnilib` and a NetBeans-global
`LWJGL-2.7.1.classpath` library that wasn't checked into the repo.

### macOS: GLFW's first-thread requirement

GLFW (and Cocoa) requires the JVM's main thread to be the process's first OS thread, which a plain
`java -jar ...` invocation can't guarantee. `Orbit.main()` handles this itself: on macOS, if it detects
it wasn't launched with `-XstartOnFirstThread`, it relaunches itself as a child process with that flag
set (using `java.class.path`) and waits on it — see `relaunchOnFirstThreadIfNeeded()`. This is why
`exec-maven-plugin` is configured to use the `exec` goal (which always forks a real `java` process with
a real classpath) rather than `java` (which runs in-process inside Maven's own JVM and leaves
`java.class.path` unset, breaking the relaunch). Keep this in mind if `mvn exec:java` is ever
reintroduced — it will fail on macOS.

### Don't reintroduce AWT — it deadlocked GLFW on the first frame

`TrueTypeFont` originally used `java.awt.Font`/`GraphicsEnvironment`/`Graphics2D` (off-screen only, to
rasterize a font atlas bitmap). On macOS, merely touching those AWT classes lazily spun up AWT's own
Cocoa/AppKit integration thread. Since GLFW also directly drives Cocoa on the `-XstartOnFirstThread`
main thread, the two competed for the same native run loop: `create()` (including `initFont()`)
completed fine, but the very first `glfwPollEvents()` call in `run()` then blocked forever inside
`-[NSApplication nextEventMatchingMask:untilDate:inMode:dequeue:]` (diagnosed with `sample <pid>`), and
the window stayed a blank, unresponsive black rectangle — no exception, no GL error, nothing in the log.
Forcing AWT headless mode (`System.setProperty("java.awt.headless", "true")`) worked around it, but that
was in turn only partially effective (macOS's `JRSAppKitAWT` bridge would log
`"Process manager already initialized: can't fully enable headless mode"` on every run, since GLFW had
already engaged Cocoa's process manager before AWT's headless flag could apply) and needed to be set as
the very first statement in `main()`, before anything else ran.

`TrueTypeFont` was rewritten to use LWJGL's `org.lwjgl.stb.STBTruetype` bindings instead (baking a
printable-ASCII bitmap atlas from a bundled font resource, `src/main/resources/fonts/`, rather than
enumerating/rendering via AWT), which removes the AWT dependency — and the deadlock risk — entirely.
There is no `System.setProperty("java.awt.headless", ...)` call anymore because there's nothing left in
this codebase that touches `java.awt.*` at runtime (`OrbitMenu` imports AWT/Swing types but is never
instantiated). If AWT-based text rendering is ever reintroduced, this whole deadlock needs to be
re-solved, not just worked around with the headless flag again.

Also note `resizeGL()` sizes the GL viewport from `framebufferWidth`/`framebufferHeight` (queried via
`glfwGetFramebufferSize()`), not `DISPLAY_WIDTH`/`DISPLAY_HEIGHT`. On HiDPI/Retina displays the real
framebuffer is larger (e.g. 2x) than the window's logical size; viewporting to the logical size instead
confines all rendering to one corner of the window.

## Architecture

The simulation is built around a small state-space framework, independent of rendering:

- **`DynamicSys`** — interface for anything that can report a state vector, compute state derivatives
  given inputs, and be reset/loaded to a state. Implemented by both `Body` (single point mass) and
  `OrbitalSystem` (the whole N-body system), so a system-of-bodies composes the same interface as one
  body.
- **`SystemVector`** → **`StateVector`** / **`InputVector`** — thin wrappers around `double[]` with
  static, allocation-per-call vector math (`add`, `mult`, `scale`). Integrators are written purely in
  terms of these, treating state as immutable values rather than mutating in place.
- **`Integrator`** (abstract) with **`Euler1Integrator`**, **`RungeKutta2Integrator`**,
  **`RungeKutta4Integrator`** — each implements `integrate(InputVector, deltaT)` by calling the
  `DynamicSys`'s `getStateDeriv()` one or more times per step and combining the results. `OrbitalSystem`
  currently always selects `RungeKutta4Integrator`.
- **`OrbitalSystem`** (implements `DynamicSys`) — owns the `ArrayList<Body> bodies` and drives one
  simulation step in `Propagate()`: sum pairwise gravitational forces (`Body.addForces` is static and
  applies equal/opposite force to both bodies in one call, halving the N² work), resolve collisions,
  integrate the combined state vector, write it back into each `Body`, then cull bodies that have
  escaped past `3*maxR`. Also owns view-centering (`setCenter`, `setCenterXY`) and rendering dispatch
  (`glRender`, plus `glRenderSizeHistogram` — a log-scale-binned bar chart of body sizes, since the
  size distribution spans orders of magnitude and linear bins would be visually useless. Drawn as bins
  stacked vertically, largest at top, bars right-justified against a right-edge baseline and extending
  leftward — a conventional bottom-left/count-up histogram rotated 90° CCW. Every bin is labeled (via a
  passed-in `TrueTypeFont`), in Earth radii, just right of where its bar would be: the median radius of
  its bodies if any fell in it, otherwise the bin's own (empty) log-space size-range midpoint, so the
  size axis reads the same whether or not a bin is currently populated. Position and bar/gap/bin sizing
  are bundled into a `HistogramLayout` record (rather than six loose parameters) — `Orbit` computes one
  once in `create()` (`computeHistogramLayout()`), since it depends only on fixed display/font geometry,
  and reuses it every frame rather than rebuilding it 60 times a second. Uses whatever GL color is
  currently set rather than choosing its own, so `Orbit` can match it (bars and labels both) to the
  HUD text — `Orbit.render()` re-asserts the run-state color right before calling it, since the bottom
  hint text drawn just above switches the current color to white first. Also takes the currently
  `selectedBody` (or `null`): if non-null, `binIndexFor()` (shared with the main per-body binning loop,
  so both use the exact same log-scale bucketing) locates its bin, and that bin's bar alone is drawn in
  a lighter shade — blended halfway toward white via `glGetFloatv(GL_CURRENT_COLOR, ...)` rather than a
  hardcoded highlight color, so it stays a lighter version of whatever base color the caller set
  (green/red matching run state) instead of clashing with it; labels always stay the base color,
  never the highlight, restored explicitly before each one since the bar draw may have changed it).
  The reverse direction also works: `getBodiesInSizeBin(binIndex, numBins)` returns every body
  currently in a given bin (sharing `binIndexFor`/a factored-out `computeLogRadiusRange()` with
  `glRenderSizeHistogram`, so both bucket identically), and `Orbit.handleMouseMove()` hit-tests the
  cursor against each bin's bar *track* (`findHistogramBinAt()` — the full `rightX - maxBarLength` to
  `rightX` span at that bin's row, not just its own possibly short/absent bar, the way a scrollbar
  track is easier to grab than the thumb) and sets `Body.hovered` on every body in whichever bin it
  finds, via `updateBinHover()` — the same boolean single-body hover already drives, just applied to a
  whole bin's bodies at once (tracked in `Orbit.binHoveredBodies`, cleared and replaced each move event).
  Hitting the histogram takes over the cursor for this purpose: normal single-body world-space hover
  hit-testing is skipped whenever `findHistogramBinAt()` finds a bin, so the two mechanisms don't fight
  over `hovered`. `saveToFile()`/`loadFromFile()` serialize the whole system — integration parameters plus
  every body's position, velocity, size, density, color, and optional name — as JSON matching
  `src/main/resources/schema/orbital-system.schema.json` (hand-written to/from `org.json`'s
  `JSONObject`/`JSONArray`, not validated against the schema file at runtime — no schema-validator
  dependency is pulled in; the schema is the documented contract, and the Java code is kept in sync with
  it by hand). Bodies' X/Y/U/V are whatever they currently are, not necessarily their original initial
  conditions, so a save can capture either. See `Orbit`'s `S`/`R` key handlers below for how the app
  uses this. Each system also carries its own default presentation: `defaultM2pix` (zoom, initialized to
  `Orbit.initial_m2pix` but overridable — a system spanning a fraction of an AU, like `TriaxSystem`,
  needs a far tighter default than one spanning several AU, like `SolarSystem`) and `defaultHighViz`
  (starts every body pre-highlighted, as if `V` had been pressed, if set). `Orbit`'s constructor applies
  both via `applySystemDefaults()` right after building whichever scenario was chosen — see below.
- **`SolarSystem`**, **`RandomSystem`**, **`TempSystem`**, **`TriaxSystem`** — all extend `OrbitalSystem`
  and differ only in how they populate `bodies` in their constructor: fixed real solar-system bodies, a
  randomly generated planetoid cluster (configurable count/velocity/density/radius; size distribution
  shape is a tunable `distShape` from -1 to +1 — see `getShapedSizeDist()`'s javadoc: -1 is a
  half-normal distribution peaked at the small end, 0 is uniform, +1 is the mirror-image half-normal
  peaked at the large end, and values between are a probabilistic mixture of uniform and whichever
  half-normal applies, weighted by `|distShape|`, so the population's shape morphs continuously across
  the range. Each body also gets a randomly generated designation via `generateName()` — one of
  J/X/R, a random three-digit number, and a random lowercase letter, e.g. `"J042q"`), a minimal fixed
  test scenario (unwired, debugging only), and the "Triax" scenario —
  three equal-mass bodies at the vertices of an equilateral triangle, each moving perpendicular to its
  radius vector at a matching speed, so the triangle rotates rigidly about the origin (a Lagrange
  three-body relative-equilibrium configuration) rather than the bodies flying apart or collapsing
  together — respectively. `TriaxSystem` sets `defaultM2pix` from its vertices' known 0.01 AU distance
  from the origin, since the base default zoom (tuned for `SolarSystem`'s several-AU scale) would render
  it as a barely-visible few-pixel cluster, and also sets `defaultHighViz = true`. `RandomSystem`,
  `SolarSystem`, and `TempSystem` leave `defaultHighViz` at its default (`false`).
- **`Body`** (implements `DynamicSys`) — one point mass: position/velocity/mass/radius/density, force
  accumulators, its own initial-condition `StateVector`, an optional display `name` (null unless set;
  `SolarSystem` sets one on each of its planets/moons, `RandomSystem`'s generated bodies stay unnamed),
  and an owned `TrailingPath` (a ring buffer of past positions used to draw the orbit trail) — but only
  when constructed with a trail length greater than 1; `glRender()`/`recordPosition()`/`resetToIC()` all
  check for a null `trail` and simply skip trail-related work rather than throwing, since
  `OrbitalSystem.loadFromFile()` can legitimately construct a body with trail length 0 (e.g. from a
  hand-edited JSON file that says `"trailLength": 0`). `Color` is a small RGB(A) helper used for
  body/trail colors. Its disk rendering is hand-rolled (`drawDisk`, a `GL_TRIANGLE_FAN`) since LWJGL 3
  dropped the old `org.lwjgl.util.glu.Disk` helper. `findDiscriminant()`/`findDiscriminantLeastSquares()`
  classify
  the body's current orbit as elliptical/parabolic/hyperbolic purely from its recorded `TrailingPath`
  (no knowledge of the central body's mass needed) by fitting a general conic and checking the sign of
  its discriminant - `fitEllipse()` (see `Orbit`'s selection-rendering description below) goes further
  and reduces that same least-squares conic fit to a drawable ellipse; `updateOrbitColor()` uses the
  least-squares fit to recolor the body light
  blue/light red while clearly elliptical/hyperbolic, or its original color otherwise. `Propagate()`
  calls it every step for every body only when `OrbitalSystem.colorByOrbitShape` is set — per-scenario,
  since it would override a system's own intentional body colors (e.g. `SolarSystem`'s traditional
  per-planet colors) if it ran unconditionally. `RandomSystem` sets it (its planetoids all start plain
  white, so recoloring by shape adds a signal instead of overriding one); `SolarSystem`, `TempSystem`,
  and `TriaxSystem` leave it at its default (`false`). `RandomSystem` also sets
  `defaultShowHistogram = true` (the size histogram is applied via `applySystemDefaults()` alongside
  `defaultM2pix`/`defaultHighViz`, as if `H` had been pressed) since the size distribution is the whole
  point of that scenario; the others leave it at its default (`false`).
- **`Orbit`** — the entry point and main loop. Owns the `OrbitalSystem system` instance, chosen at
  startup by an stdin prompt (`promptScenario()`) between `SolarSystem`/`TriaxSystem`/`RandomSystem` —
  `TempSystem` stays code-only/unwired, per its own javadoc, debugging-only — then prompts on stdin for
  each of that scenario's construction parameters (`Scanner`, default shown in brackets, blank accepts
  it, invalid input re-prompts) before building it. Falls back to all defaults immediately, without
  blocking, if no interactive stdin is available (see `promptLine()`'s `NoSuchElementException`
  handling). Then calls `applySystemDefaults()` to apply that system's own `defaultM2pix`/
  `defaultHighViz` (see `OrbitalSystem` above) before the first frame renders. Sets up a GLFW
  window/context and OpenGL state, and runs a loop of
  `glfwPollEvents()` → `update()` (advances the model via `system.Propagate()`) → `render()`, throttled
  by vsync (`glfwSwapInterval(1)`). Keyboard is handled via a `GLFWKeyCallback` (registered once, fires
  only on `GLFW_PRESS` — no key-repeat, same as the old `Keyboard.enableRepeatEvents(false)`) that
  dispatches to `handleKeyPress()`: `P` pause/resume, `+`/`-` zoom, arrow keys pan, `Home` recenter on
  origin, `0`-`9` center view on the Nth-largest body by radius (`OrbitalSystem.setCenterOnNthLargest`,
  0 = largest; re-sorts `bodies` by size on every press since collisions/culling can reorder or shrink
  the list), `S`/`R` save/reload state (see below), `Tab`/`Shift-Tab` shift selection to the next/previous
  body (see `shiftSelection()` below), `V` toggle high-viz rendering (`Body.isHighlighted` — larger disk,
  brighter trail), `H` toggle the size histogram overlay, `?` (`GLFW_KEY_SLASH` with `GLFW_MOD_SHIFT` —
  the mods bitmask distinguishes it from plain `/`, the same way it distinguishes Tab from Shift-Tab)
  toggle a help panel listing every key/mouse control (`HELP_TEXT`, a constant multi-line string), drawn
  down the left side of the screen (`ALIGN_LEFT`, plain white) when `showHelp` is set — hidden by
  default. When the active scenario has `OrbitalSystem.colorByOrbitShape` set (currently only
  `RandomSystem`), `render()` appends three more lines below `HELP_TEXT` explaining that recoloring:
  "Orbital shape (from curve fit):", then "Red shows hyperbolic/indeterminate" and "Blue indicates elliptical"
  with just the color word itself drawn in that color (`Color.lightRed()`/`Color.lightBlue()`'s exact
  RGB, matching `Body.updateOrbitColor()`) and the rest of the line plain white - built from three
  separate `drawString()` calls per colored line (`TrueTypeFont.getWidth()` positions the white
  remainder right after the colored word) rather than one call, since `drawString()` only takes a single
  color for the whole string. Omitted entirely for scenarios that don't recolor by orbit shape, since
  the legend would be meaningless there. `Escape` is polled directly in the
  run loop, matching the old behavior. The window is requested with no explicit GL version/profile hints,
  which is what keeps immediate-mode calls (`glBegin`/`glVertex2f`/matrix-stack calls) working: on macOS
  this yields a legacy 2.1 context, since requesting 3.2+ there requires an explicit core/forward-compat
  profile that would break fixed-function rendering. `DISPLAY_WIDTH`/`DISPLAY_HEIGHT` are no longer fixed
  constants (1440x900) - `create()` queries the primary monitor's current video mode
  (`glfwGetPrimaryMonitor()`/`glfwGetVideoMode()`) and sets them from it before creating the window,
  passing that same monitor handle as `glfwCreateWindow()`'s fourth argument, which makes it a true
  fullscreen window on that display (filling the whole screen, no window chrome) rather than a
  fixed-size windowed one; every other use of `DISPLAY_WIDTH`/`DISPLAY_HEIGHT` throughout the file (ortho
  projection, HUD/selection-panel layout, mouse-to-world conversion) picks up the monitor's actual
  resolution automatically since they're ordinary instance fields now, not compile-time constants.
  On-screen text is drawn using `TrueTypeFont`, which
  bakes a bitmap font atlas from a bundled TTF resource via LWJGL's `stb_truetype` bindings and draws it
  as textured quads — see the AWT note above for why it isn't AWT-based. `render()` also draws a
  constant reminder, `'p' to run/pause, '?' for help, 'Esc' to exit`, centered at the bottom of the screen
  (`TrueTypeFont.ALIGN_CENTER`, always plain white) - unlike the "N bodies"/time HUD text above it, this
  doesn't change with run state, so it's drawn separately rather than folded into that green/red block.

  Body highlighting is driven by three independent per-`Body` booleans, combined in `Body.glRender()` as
  `highlighted = isHighlighted || selected || hovered`: `isHighlighted` is the global one `V` flips,
  `hovered` tracks whichever body (if any) the mouse cursor is currently over, and `selected` is sticky
  until explicitly cleared. Mouse input is wired up in `Orbit.create()` via `glfwSetCursorPosCallback`
  (→ `handleMouseMove()`) and `glfwSetMouseButtonCallback` (→ `handleMouseButton()` on either left-button
  transition). `handleMouseMove()` inverts `resizeGL()`'s ortho projection to convert the cursor's window
  content-area coordinates into the same centeredBody-relative world space bodies are rendered in, then
  hit-tests via `OrbitalSystem.findBodyAt()` (nearest body whose on-screen, minR-clamped radius contains
  the point) to set/clear `hovered` on at most one body. The minR passed for hit-testing is deliberately
  larger than the one `glRender()` clamps the *rendered* dot to: `Orbit.HIT_RADIUS_PIX` (8px) vs.
  `glRender()`'s 1px floor, so a body that's zoomed out to a barely-visible dot still has a comfortably
  clickable area around it — the dot itself doesn't get any bigger, only the invisible hit-test radius
  does. `handleMouseClick()` selects whichever body is currently hovered
  (clearing any previously-selected body first) or deselects if the click hit empty space — only one body
  can be `selected` at a time.

  Selection has to coexist with click-and-drag panning on the same button, so `handleMouseClick()` isn't
  called directly from the button callback — `handleMouseButton(action)` is. A `GLFW_PRESS` just starts
  tracking a candidate drag (`leftButtonDown = true`, records the cursor position in
  `dragStartCursorX`/`Y`, clears `dragOccurred`); it doesn't select anything yet, since a press might turn
  into a drag. While `leftButtonDown`, every `handleMouseMove()` call also pans the view by the cursor's
  movement since the *previous* move event (`lastCursorX`/`Y`, so the pan tracks incrementally rather than
  needing to recompute from the drag's start each time) — `centerX -= dx/m2pix; centerY += dy/m2pix`, fed
  into `updateSystemCenter()` exactly like the arrow-key handlers. The asymmetric signs mirror
  `handleMouseMove()`'s own worldX/worldY conversion just below: screen X and world X agree, but screen Y
  is top-down while world Y is bottom-up, so only Y needs the sign flip. This makes a drag feel like
  grabbing the canvas — whatever world point was under the cursor when the drag started stays under it as
  you drag — which is the *opposite* sign convention from the arrow keys' camera-pans-in-that-direction
  semantics (dragging right pans the camera **left** so content follows the cursor right). Each
  `handleMouseMove()` call while dragging also checks the cursor's total displacement since the press
  against `DRAG_THRESHOLD_PIX` (4px) and latches `dragOccurred = true` once it's exceeded. On
  `GLFW_RELEASE`, `handleMouseButton()` only calls `handleMouseClick()` if `dragOccurred` is still false —
  so a plain click (no meaningful movement) selects/deselects as before, while a click-and-drag ends the
  pan without disturbing whatever was already selected. Verified live: a press-release with no movement
  still selects the hovered body; a press, 100px drag, and release pans the view by exactly the expected
  world-space delta and leaves the selection untouched.

  `glfwSetScrollCallback` (→ `handleScroll(yoffset)`) zooms in/out on the screen center, same direction
  sense as `zoomIn()`/`zoomOut()` (`+`/`-`, positive `yoffset` zooming in - scroll up/forward, the
  conventional direction in maps and image viewers). Unlike `+`/`-`'s fixed per-press step, it scales
  continuously with `yoffset`: `m2pix *= 2^(yoffset / SCROLL_UNITS_PER_OCTAVE)`. An initial version
  treated every scroll callback as a full `zoomIn()`/`zoomOut()` regardless of magnitude, which was
  fine for a discrete mouse-wheel notch but wildly (reported 5x) too sensitive on a trackpad, which
  reports many small-magnitude scroll events per gesture rather than one per notch;
  `SCROLL_UNITS_PER_OCTAVE = 5.0` spreads one full octave of zoom across 5 accumulated scroll units
  instead of 1, confirmed live (`yoffset=1.0` now moves `m2pix` by `2^(1/5)` ≈ 1.15x, not 2x; five
  such events still compound to exactly 2x, preserving the same total zoom per unit of scroll input).

  `V`'s handler (`Orbit.toggleHighlight()`) unconditionally flips
  `isHighlighted` on every body at once, regardless of whether a body is currently selected (an
  earlier version instead cleared it whenever something was selected, so `V` could never turn
  highlighting back on until you deselected — a usability trap, since selecting a body to inspect it
  is exactly when you'd want to compare it against other highlighted bodies). A selected body always
  stays highlighted either way, since `Body.glRender()` computes
  `highlighted = isHighlighted || selected || hovered`.
  `Orbit.shiftSelection()` (bound to `Tab`/`Shift-Tab`, distinguished via the key callback's `mods`
  bitmask and `GLFW_MOD_SHIFT`) moves the selection to the next/previous entry in
  `OrbitalSystem.bodies` by index, cycling at either end; if nothing is selected, or the previously
  selected body is no longer in the list (e.g. absorbed in a collision), it starts from the first
  (`Tab`) or last (`Shift-Tab`) body.

  `render()` fits the selected body's ellipse once per frame (`selectedBody.fitEllipse()`, `null` if it
  throws `IllegalStateException` - not enough trail history yet, or the trail doesn't currently fit an
  ellipse) and shares that single `Ellipse` with both `drawSelectedBodyEllipse(Ellipse)` and
  `buildSelectionInfoText(Body, Ellipse)`, rather than each independently re-running `fitEllipse()`'s
  O(n) least-squares solve.

  While a body is selected, `render()` also draws a data block for it (`selectionInfoAnchor()` +
  `buildSelectionInfoText()`): its optional `name`, its size in Earth radii, its inertial speed
  (`inertialSpeedKmS()` - magnitude of `(U, V)`, in km/s), and its orbit-shape classification
  (`describeOrbitShape(Body, Ellipse)`) — kept to one line rather than adding a second: a non-null fit
  means the trail currently fits an ellipse, so the classification is abbreviated to "Ell" to make room
  for an estimated period alongside it, e.g. `Orbit: Ell (0 hr 29 min)` (`Body.estimatePeriod(Ellipse)`;
  just "Ell" with nothing appended if that itself throws). A hyperbolic or parabolic orbit is unbound and
  has no period, so those fall back to `Body.findDiscriminantLeastSquares()` and stay their full,
  unabbreviated words ("Hyperbolic"/"Parabolic") with nothing appended — or a placeholder until at least
  5 trail points have been recorded. `formatDuration(double)` renders the estimated period at whichever
  of three tiers suits its magnitude — `{years} yr {days} d`, `{days} d {hours} hr`, or
  `{hours} hr {minutes} min` — using `OrbitalSystem`'s `YEAR`/`DAY`/`HOUR`/`MINUTE` constants; only the
  smallest shown unit (minutes) is zero-padded, since the larger units lead the string and are never
  zero. Drawn at `SELECTION_INFO_SCALE` (0.6x the HUD's baked font size, so it reads as a secondary
  annotation rather than competing with the HUD), always plain white regardless of the selected body's
  own color so it stays legible for any body, and anchored into its
  lower-right quadrant, clear of its on-screen disk (`selectionInfoAnchor()` converts the body's world
  position to the same screen-pixel space `render()`'s HUD text uses, offsetting past the disk's own
  highlighted radius so the block never overlaps it) — recomputed every frame, so it tracks the body the
  way an air-traffic-control radar data block tracks its target, rather than sitting fixed in a screen
  corner.

  `Body.estimatePeriod(Ellipse)` estimates the orbital period from the trail via Kepler's second law
  (equal areas swept, relative to the focus, in equal times) rather than a swept-angle average rate,
  which would only be exact for a circular orbit. The ellipse's center isn't its focus - of the two
  candidate foci (offset from center by `c = sqrt(semiMajor^2 - semiMinor^2)` along the major axis), only
  one is physically real - so `sweptAreaSegments()` computes the swept-area/time rate for both, and
  `sweptAreaRateVariance()` picks whichever is more nearly constant across the trail's segments: that's
  exactly what Kepler's second law predicts for the true focus and not the empty one, so - like
  `findDiscriminantLeastSquares()`/`fitEllipse()` - it's determined from the same purely observational
  trail data, with no need to know where any body's mass actually is. Total swept area divided by total
  elapsed time gives the areal velocity; dividing that into the whole ellipse's area
  (`pi * semiMajor * semiMinor`) gives the period. Verified against synthetic points placed exactly on a
  true Kepler orbit - solving Kepler's equation `M = E - e sin E` via Newton-Raphson for each sample,
  including a notably eccentric case - before ever rendering anything (`PeriodEstimateTest`); the
  straight-chord swept-area approximation between discrete sampled points systematically undercounts the
  true curved-arc area by `O(dTheta^2)` per segment (biasing the estimated period high), so those tests
  use dense sampling to shrink that bias below tolerance rather than loosening the tolerance to hide it.

  While a body is selected, `render()` also calls `drawSelectedBodyEllipse(Ellipse)` (skipped when the
  shared fit above is `null`), which draws the fitted `Ellipse` as a dashed line (alternating
  drawn/skipped runs of `ELLIPSE_SEGMENTS`-sampled points, since this legacy/fixed-function GL context
  has no shader-based line style) in the body's own color at full
  intensity, visually distinct from that body's own dimmer, solid trail line. Refit fresh every frame
  against whatever the trail currently holds, so the ellipse continually reshapes as the trail grows and
  the orbit is perturbed by other bodies, rather than being a one-time fit locked to some past moment -
  confirmed live watching a lightly-perturbed circular orbit visibly grow and precess over several loops,
  with the dashed ellipse growing right along with it. `Body.fitEllipse()` reduces the same general
  conic `findDiscriminantLeastSquares()` already fits (both now share `fitConicCoefficients()`, which
  returns the raw `{A,B,C,D,E}` instead of just the discriminant) to standard form - center, semi-axis
  lengths, rotation angle - via the standard analytic-geometry reduction (center solves the conic's
  gradient to zero; the rotation angle `atan2(B,A-C)/2` eliminates the xy cross-term; the two resulting
  axis-aligned coefficients give the semi-axis lengths once divided into the conic's value at the
  center). Returns an `Ellipse` record (`Ellipse.java`, world-space meters, `semiMajor` normalized to
  always be `>= semiMinor` by swapping axes and adding 90° to the angle if needed). Verified against
  synthetic points placed exactly on known ellipses (`EllipseFitTest`) before ever rendering anything.

  `S`/`R` persist and reload state through `Orbit.STATE_FILE` (`.JOS.json` in the working directory,
  gitignored): `saveState()` writes the system's current state there via `OrbitalSystem.saveToFile()`
  (called once from the constructor too, so a valid file always exists even before the user presses `S`);
  `reloadFromFile()` reads it back via `OrbitalSystem.loadFromFile()`, replaces `system` outright with the
  freshly-built one, clears `hoveredBody`/`selectedBody` (they'd otherwise dangle, pointing at `Body`
  objects from the discarded system), recenters the view, and sets `run = false` — reloading pauses
  rather than immediately continuing, so the reloaded state can be inspected/selected before it starts
  evolving. Because the file is plain, schema-documented JSON, it can be hand-edited between an `S` and
  an `R` to try out a specific scenario without touching code — `OrbitalSystem.loadFromFile()` doesn't
  care whether the file it's reading was written by `saveToFile()` or by a human.

`OrbitMenu` (a Swing `JMenuBar`) and `solarsystem.xml` (an XML initial-conditions file) exist but are
not wired up to anything yet (menu actions all throw `UnsupportedOperationException`; nothing in
`src/` reads the XML file).

All simulation code lives under package `org.jaxfam.orbit`, source in `src/main/java`, tests in
`src/test/java` mirroring the same package structure (standard Maven layout).
