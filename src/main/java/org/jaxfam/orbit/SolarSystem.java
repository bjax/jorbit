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

class SolarSystem extends OrbitalSystem {

    Body Sol;       /* individual planets for convenience in IC */
    Body Mercury;
    Body Venus;
    Body Earth;
    Body Moon;
    Body Mars;
    Body Jupiter;
    Body Saturn;
    Body Uranus;
    Body Neptune;
    Body Pluto;
    Body Callisto;
    Body Ganymede;
    Body Europa;
    Body Io;
               /** array of bodies for looping */

    /**
     * Default constructor; creates an OrbitalSystem containing the
     * eight planets of the solar system plus Pluto for nostalgia's sake
     * Also contains Earth's Moon and the Jovian moons.
     * @param IC_dt initial time frame step size, sec
     * @param trailLength How long to make trailing paths behind bodies
     */
    public SolarSystem(double IC_dt, int trailLength) {

        super();
        maxR = 20.0;
        pathLength = trailLength;
        int tl = pathLength;

        enforce_R_limit = false;

        /* set up the planetary bodies */
        //                  R, AU;  Mass (em's);  r er's; color
        Sol      = new Body(0.,      332800., 109.044, Color.yellow(),  tl);
        Mercury  = new Body(.390,    0.05,     0.3825, Color.pink(),    tl);
        Venus    = new Body(.720,    0.89,     0.9488, Color.green(),   tl);
        Earth    = new Body(1.000,   1.00,     1.0000, Color.blue(),    tl);
        Moon     = new Body(1.00257, 1 / 80.,  0.2724, Color.gray(),    tl);
        Mars     = new Body(1.500,   0.11,     0.5326, Color.red(),     tl);
        Jupiter  = new Body(5.200,   318.0,    11.000, Color.cyan(),    tl);
        Callisto = new Body(5.21259, 1.802e-2, 0.3768, Color.gray(),    tl);
        Ganymede = new Body(5.20715, .02482,   0.4130, Color.gray(),    tl);
        Europa   = new Body(5.20449, 8.04e-3,  0.2454, Color.gray(),    tl);
        Io       = new Body(5.20282, 1.49e-2,  0.2847, Color.gray(),    tl);
        Saturn   = new Body(9.500,   95.0,     9.0000, Color.magenta(), tl);
        Uranus   = new Body(19.20,   17.0,     4.0000, Color.orange(),  tl);
        Neptune  = new Body(30.10,   17.0,     4.0000, Color.white(),   tl);
        Pluto    = new Body(39.50,   0.002,    0.18,   Color.darkGray(),tl);

        Sol.setName("Sol");
        Mercury.setName("Mercury");
        Venus.setName("Venus");
        Earth.setName("Earth");
        Moon.setName("Moon");
        Mars.setName("Mars");
        Jupiter.setName("Jupiter");
        Callisto.setName("Callisto");
        Ganymede.setName("Ganymede");
        Europa.setName("Europa");
        Io.setName("Io");
        Saturn.setName("Saturn");
        Uranus.setName("Uranus");
        Neptune.setName("Neptune");
        Pluto.setName("Pluto");

        /* Circularize moons about planets, and planets about the Sun */
        Mercury.circularizeAbout(Sol);
        Venus.circularizeAbout(Sol);
        Earth.circularizeAbout(Sol);
        Mars.circularizeAbout(Sol);
        Jupiter.circularizeAbout(Sol);
        Saturn.circularizeAbout(Sol);
        Uranus.circularizeAbout(Sol);
        Neptune.circularizeAbout(Sol);
        Pluto.circularizeAbout(Sol);

        Moon.circularizeAbout(Earth);

        Callisto.circularizeAbout(Jupiter);
        Ganymede.circularizeAbout(Jupiter);
        Europa.circularizeAbout(Jupiter);
        Io.circularizeAbout(Jupiter);

        bodies = new ArrayList<>(15);
        bodies.add(0,Sol);
        bodies.add(1,Moon);
        bodies.add(2,Mercury);
        bodies.add(3,Venus);
        bodies.add(4,Earth);
        bodies.add(5,Mars);
        bodies.add(6,Jupiter);
        bodies.add(7,Saturn);
        bodies.add(8,Uranus);
        bodies.add(9,Neptune);
        bodies.add(10,Pluto);
        bodies.add(11,Callisto);
        bodies.add(12,Ganymede);
        bodies.add(13,Europa);
        bodies.add(14,Io);

        dt = IC_dt;
        trailDecimation = 2; /* how many points to skip when remembering path */
        centeredBody = Sol;
    }

    /** center about sun */
    public void home() {
        centeredBody = Sol;
        dt = Orbit.initial_dt;
    }

}
