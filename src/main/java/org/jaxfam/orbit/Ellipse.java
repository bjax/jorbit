//
// Ellipse.java
//
// Part of the orbital mechanics demonstrator program
//
// 2026 Written to bundle Body.fitEllipse()'s result
//

package org.jaxfam.orbit;

/**
 * An ellipse in standard form: center, semi-major/minor axis lengths, and rotation
 * angle. Returned by {@link Body#fitEllipse()}; all lengths are in the same units
 * as {@link Body#getX()}/{@link Body#getY()} (meters, world/inertial coordinates,
 * not relative to any centered body).
 * @param centerX   center X, m
 * @param centerY   center Y, m
 * @param semiMajor semi-major axis length, m (always &gt;= semiMinor)
 * @param semiMinor semi-minor axis length, m
 * @param angle     rotation of the semi-major axis from the X axis, radians
 */
public record Ellipse(
        double centerX,
        double centerY,
        double semiMajor,
        double semiMinor,
        double angle) {
}
