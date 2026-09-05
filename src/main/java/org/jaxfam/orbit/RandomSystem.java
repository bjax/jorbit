//
// SolarSystem.java
//
// Part of the orbital mechanics demonstrator program
//
// Originally written 2001 by Bruce Jackson, bruce@jaxfam.org
// Ported to Mac OSX Project Builder 020327 EBJ
// Ported to NetBeans 2011-04-21 EBJ

package org.jaxfam.orbit;

import java.util.ArrayList;
import java.util.Random;

class RandomSystem extends OrbitalSystem {

    /**
     * Default constructor
     * @param IC_dt      initial time frame step size, sec
     * @param numBodies  number of bodies to create
     * @param pathLength how many points to record
     * @param distShape  size-distribution shape: -1 concentrates bodies at the small
     *                   end, 0 is uniform across the whole range, +1 concentrates them
     *                   at the large end, and values between smoothly blend toward
     *                   uniform (see getShapedSizeDist())
     * @param minRadius  smallest body radius allowed
     * @param maxRadius  largest body radius allowed
     * @param maxV_IC    upper limit on initial V, m/s
     * @param density    planetary density, g/cm^3; H20 = 1.0
     * @param maxSystemR maximum distance in AU from [0,0]
     */
    public RandomSystem(double IC_dt,
            int numBodies,
            int thePathLength,
            double distShape,
            double minRadius,
            double maxRadius,
            double maxV_IC,
            double density,
            double maxSystemR) {

//        /* Circularize moons about planets, and planets about the Sun */
//
//        Moon.circularizeAbout(Earth);
//
        super();

        pathLength = thePathLength;

        maxR = maxSystemR;
        bodies = new ArrayList<>(numBodies);
        double orbitAngle;
        double velocityAngle;
        double initialVelocity_m_s;
        double distFromCenter_AU;
        double diskRadius_em;

        enforce_R_limit = true;

        Random generator = new Random();

        Body body;

        for (int i = 0; i < numBodies; i++) {
            orbitAngle          = 2*Math.PI*generator.nextDouble();
            velocityAngle       = 2*Math.PI*generator.nextDouble();
            initialVelocity_m_s = maxV_IC*generator.nextDouble();
            // sqrt, not a bare uniform draw: a ring at radius r has area proportional to r, so
            // sampling r uniformly puts the same number of bodies in every ring regardless of its
            // area, making density scale as 1/r (much denser near the center); sqrt(uniform)
            // compensates so the ring gets bodies proportional to its own area instead, giving a
            // uniform density across the whole disk
            distFromCenter_AU   = maxR*Math.sqrt(generator.nextDouble());
            diskRadius_em       = getShapedSizeDist(distShape,
                                    minRadius, maxRadius,
                                    generator );

            body = new Body(
                    distFromCenter_AU*Math.sin(orbitAngle), // X_ic_AU
                    distFromCenter_AU*Math.cos(orbitAngle), // Y_ic_AU
                    initialVelocity_m_s*Math.sin(velocityAngle), // U_ic
                    initialVelocity_m_s*Math.cos(velocityAngle), // V_ic
                    diskRadius_em,                          // disk radius, earth radii
                    density,                             // density, g/cm^3
                    Color.white(),                       // disk color
                    pathLength);                         // trailing path points
            body.setName(generateName(generator));

            bodies.add(body);
        }
        
        dt = IC_dt;
        trailDecimation = 2; /* how many points to skip when remembering path */
        centeredBody = bodies.get(0);

        // unlike SolarSystem's traditional per-planet colors, every body here starts the same
        // plain white, so recoloring by orbit shape adds a meaningful signal instead of
        // overriding an intentional one
        colorByOrbitShape = true;

        // the size distribution is the whole point of this scenario, so show it immediately
        defaultShowHistogram = true;
    }

    /**
     * Samples a body size (Earth radii) between minRadius and maxRadius, using a
     * distribution whose shape smoothly varies with distShape: -1 concentrates bodies
     * at the small end (a half-normal distribution peaked at minRadius), 0 is uniform
     * across the whole range, and +1 concentrates them at the large end (the
     * mirror-image half-normal, peaked at maxRadius). Values in between are a
     * probabilistic mixture of the uniform distribution and whichever half-normal
     * applies, weighted by |distShape| (so e.g. -0.5 draws from the small-end
     * half-normal half the time and uniform the other half) - the population's shape
     * morphs continuously as distShape sweeps from -1 through 0 to +1. Values outside
     * [-1, 1] behave the same as -1/+1 (weight clamped to a full half-normal).
     * <p>
     * The shaped [0, 1] fraction is mapped onto [minRadius, maxRadius] logarithmically,
     * not linearly, matching how {@link OrbitalSystem#glRenderSizeHistogram} bins bodies
     * by log(radius) (sizes here can span orders of magnitude - e.g. the default 0.001 to
     * 50 Earth radii - so linear bins would be visually useless, and a linear fraction
     * mapping would put nearly all bodies in the last, huge-in-absolute-terms decade of
     * that range regardless of distShape, masking any change to the shape entirely).
     * @param distShape  shape parameter: -1 (peak at small end) to +1 (peak at large end)
     * @param minRadius  smallest body radius allowed, Earth radii
     * @param maxRadius  largest body radius allowed, Earth radii
     * @param generator  random source
     * @return a body radius in [minRadius, maxRadius], Earth radii
     */
    private static double getShapedSizeDist(double distShape,
            double minRadius,
            double maxRadius,
            Random generator) {
        double fraction = sizeShapeFraction(distShape, generator);
        double logMin = Math.log(Math.max(minRadius, Double.MIN_NORMAL));
        double logMax = Math.log(Math.max(maxRadius, Double.MIN_NORMAL));
        return Math.exp(logMin + fraction * (logMax - logMin));
    }

    /** The [0, 1] fraction sampled by getShapedSizeDist(), before scaling to [minRadius, maxRadius]. */
    private static double sizeShapeFraction(double distShape, Random generator) {
        double uniform = generator.nextDouble();
        if (distShape == 0.0) {
            return uniform;
        }
        double weight = Math.min(Math.abs(distShape), 1.0); // probability of drawing from the half-normal
        if (generator.nextDouble() >= weight) {
            return uniform;
        }
        double halfNormal = halfNormalFraction(generator);
        return (distShape < 0) ? halfNormal : 1.0 - halfNormal;
    }

    /**
     * A half-normal-shaped sample in [0, 1], peaked at 0 and tapering off like the
     * positive half of a bell curve. Built from |Z| for a standard normal Z, scaled by
     * a sigma chosen so its ~3-sigma tail lands near 1; the rare sample beyond that is
     * clamped to 1 rather than distorting the rest of the distribution with a wider,
     * flatter curve.
     */
    private static double halfNormalFraction(Random generator) {
        final double sigma = 1.0 / 3.0;
        double sample = Math.abs(generator.nextGaussian()) * sigma;
        return Math.min(sample, 1.0);
    }

    private static final char[] NAME_PREFIXES = {'J', 'X', 'R'};

    /**
     * Generates a designation for a randomly-generated planetoid: one of J/X/R,
     * a random three-digit number, and a random lowercase letter (e.g. "J042q").
     */
    private static String generateName(Random generator) {
        char prefix = NAME_PREFIXES[generator.nextInt(NAME_PREFIXES.length)];
        int number = generator.nextInt(1000);
        char letter = (char) ('a' + generator.nextInt(26));
        return String.format("%c%03d%c", prefix, number, letter);
    }
}
