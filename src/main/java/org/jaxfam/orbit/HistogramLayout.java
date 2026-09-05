//
// HistogramLayout.java
//
// Part of the orbital mechanics demonstrator program
//
// 2026 Written to bundle OrbitalSystem.glRenderSizeHistogram()'s layout parameters
//

package org.jaxfam.orbit;

/**
 * Layout for {@link OrbitalSystem#glRenderSizeHistogram(TrueTypeFont, HistogramLayout, Body)}: where to
 * draw the histogram and how its bars/gaps/bins are sized, bundled into one immutable value so the
 * render call doesn't need six loose parameters.
 * @param rightX       right edge (bar baseline, count == 0), in the current coordinate system;
 *                     labels are drawn just to the right of this
 * @param bottomY      bottom edge, where the smallest-size bin starts, in the current coordinate system
 * @param barThickness vertical thickness of each bar
 * @param gap          vertical gap left empty between one bin's bar and the next
 * @param maxBarLength leftward length of the longest bar (i.e. the most populous bin)
 * @param numBins      number of size buckets to divide the bodies into
 */
public record HistogramLayout(
        float rightX,
        float bottomY,
        float barThickness,
        float gap,
        float maxBarLength,
        int numBins) {

    /** Total vertical space this layout's bins occupy, stacked together. */
    public float totalHeight() {
        return numBins * (barThickness + gap);
    }
}
