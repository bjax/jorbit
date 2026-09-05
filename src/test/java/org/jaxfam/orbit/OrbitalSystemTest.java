/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jaxfam.orbit;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author ebjackso
 */
public class OrbitalSystemTest {
    OrbitalSystem twoBodyBalancedX;
    OrbitalSystem twoBodyBalancedY;
    OrbitalSystem twoBodyPlusXHeavier;
    OrbitalSystem twoBodyPlusXLighter;
    OrbitalSystem twoBodyPlusYHeavier;
    OrbitalSystem twoBodyPlusYLighter;
    OrbitalSystem fourBodyBalanced;
    OrbitalSystem fourBodyHeavyUpperRight;
    static final double pct = 1e-14;
    static final double bodyR1Mass = 1000.0*(4.0/3.0)*Math.PI*Math.pow(1.0*OrbitalSystem.earthRadius, 3.0);
    static final double bodyR2Mass = 1000.0*(4.0/3.0)*Math.PI*Math.pow(2.0*OrbitalSystem.earthRadius, 3.0);
    static final double bodyR1Moment = bodyR1Mass*OrbitalSystem.AU2m;
    static final double bodyR2Moment = bodyR2Mass*OrbitalSystem.AU2m;

    double expected;

    public OrbitalSystemTest() {
    }

    public void setUpTwoBodyBalancedX() {
        twoBodyBalancedX = new OrbitalSystem();
        twoBodyBalancedX.maxR = 10.0;
        twoBodyBalancedX.bodies = new ArrayList<Body>(2);
        twoBodyBalancedX.bodies.add(new Body(-1.0,0.0,0.0,0.0,1.0,1.0,Color.white(),10));
        twoBodyBalancedX.bodies.add(new Body(+1.0,0.0,0.0,0.0,1.0,1.0,Color.white(),10));
    }

    public void setUpTwoBodyBalancedY() {
        twoBodyBalancedY = new OrbitalSystem();
        twoBodyBalancedY.maxR = 10.0;
        twoBodyBalancedY.bodies = new ArrayList<Body>(2);
        twoBodyBalancedY.bodies.add(new Body(0.0,-1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        twoBodyBalancedY.bodies.add(new Body(0.0,+1.0,0.0,0.0,1.0,1.0,Color.white(),10));
    }

    public void setUpTwoBodyPlusXHeavier() {
        twoBodyPlusXHeavier = new OrbitalSystem();
        twoBodyPlusXHeavier.maxR = 10.0;
        twoBodyPlusXHeavier.bodies = new ArrayList<Body>(2);
        twoBodyPlusXHeavier.bodies.add(new Body(-1.0,0.0,0.0,0.0,1.0,1.0,Color.white(),10));
        twoBodyPlusXHeavier.bodies.add(new Body(+1.0,0.0,0.0,0.0,2.0,1.0,Color.white(),10));
    }

    public void setUpTwoBodyPlusXLighter() {
        twoBodyPlusXLighter = new OrbitalSystem();
        twoBodyPlusXLighter.maxR = 10.0;
        twoBodyPlusXLighter.bodies = new ArrayList<Body>(2);
        twoBodyPlusXLighter.bodies.add(new Body(-1.0,0.0,0.0,0.0,2.0,1.0,Color.white(),10));
        twoBodyPlusXLighter.bodies.add(new Body(+1.0,0.0,0.0,0.0,1.0,1.0,Color.white(),10));
    }

    public void setUpTwoBodyPlusYHeavier() {
        twoBodyPlusYHeavier = new OrbitalSystem();
        twoBodyPlusYHeavier.maxR = 10.0;
        twoBodyPlusYHeavier.bodies = new ArrayList<Body>(2);
        twoBodyPlusYHeavier.bodies.add(new Body(0.0,-1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        twoBodyPlusYHeavier.bodies.add(new Body(0.0,+1.0,0.0,0.0,2.0,1.0,Color.white(),10));
    }

    public void setUpTwoBodyPlusYLighter() {
        twoBodyPlusYLighter = new OrbitalSystem();
        twoBodyPlusYLighter.maxR = 10.0;
        twoBodyPlusYLighter.bodies = new ArrayList<Body>(2);
        twoBodyPlusYLighter.bodies.add(new Body(0.0,-1.0,0.0,0.0,2.0,1.0,Color.white(),10));
        twoBodyPlusYLighter.bodies.add(new Body(0.0,+1.0,0.0,0.0,1.0,1.0,Color.white(),10));
    }

    public void setUpFourBodyBalanced() {
        fourBodyBalanced = new OrbitalSystem();
        fourBodyBalanced.maxR = 10.0;
        fourBodyBalanced.bodies = new ArrayList<Body>(4);
        fourBodyBalanced.bodies.add(new Body(-1.0,-1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        fourBodyBalanced.bodies.add(new Body(+1.0,-1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        fourBodyBalanced.bodies.add(new Body(-1.0,+1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        fourBodyBalanced.bodies.add(new Body(+1.0,+1.0,0.0,0.0,1.0,1.0,Color.white(),10));
    }

    public void setUpFourBodyHeavyUpperRight() {
        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */

        fourBodyHeavyUpperRight = new OrbitalSystem();
        fourBodyHeavyUpperRight.maxR = 10.0;
        fourBodyHeavyUpperRight.bodies = new ArrayList<Body>(4);
        fourBodyHeavyUpperRight.bodies.add(new Body(-1.0,-1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        fourBodyHeavyUpperRight.bodies.add(new Body(+1.0,-1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        fourBodyHeavyUpperRight.bodies.add(new Body(-1.0,+1.0,0.0,0.0,1.0,1.0,Color.white(),10));
        fourBodyHeavyUpperRight.bodies.add(new Body(+1.0,+1.0,0.0,0.0,2.0,1.0,Color.white(),10));
    }

    @BeforeEach
    public void setUp() {
        setUpTwoBodyBalancedX();
        setUpTwoBodyBalancedY();
        setUpTwoBodyPlusXHeavier();
        setUpTwoBodyPlusXLighter();
        setUpTwoBodyPlusYHeavier();
        setUpTwoBodyPlusYLighter();
        setUpFourBodyBalanced();
        setUpFourBodyHeavyUpperRight();
    }

    /**
     * Test of Propagate method, of class OrbitalSystem.
     */
    @Test
    public void testPropagate() {
        // tests sumForces() and
        // the state vector integration methods indirectly
        
        OrbitalSystem sys;

        twoBodyBalancedX.Propagate();
        twoBodyBalancedY.Propagate();
        twoBodyPlusXHeavier.Propagate();
        twoBodyPlusXLighter.Propagate();
        twoBodyPlusYHeavier.Propagate();
        twoBodyPlusYLighter.Propagate();
        fourBodyBalanced.Propagate();
        fourBodyHeavyUpperRight.Propagate();

        //
        // CHECK TIME PROPAGATION
        //

        expected = OrbitalSystem.dt;
        assertEquals( expected, twoBodyBalancedX.time,        pct*expected);
        assertEquals( expected, twoBodyBalancedY.time,        pct*expected);
        assertEquals( expected, twoBodyPlusXHeavier.time,     pct*expected);
        assertEquals( expected, twoBodyPlusXLighter.time,     pct*expected);
        assertEquals( expected, twoBodyPlusYHeavier.time,     pct*expected);
        assertEquals( expected, twoBodyPlusYLighter.time,     pct*expected);
        assertEquals( expected, fourBodyBalanced.time,        pct*expected);
        assertEquals( expected, fourBodyHeavyUpperRight.time, pct*expected);

        //
        // CHECK NUMBER BODIES - SHOULD BE UNCHANGED
        //

        assertEquals( 2, twoBodyBalancedX.countBodies() );
        assertEquals( 2, twoBodyBalancedY.countBodies() );
        assertEquals( 2, twoBodyPlusXHeavier.countBodies() );
        assertEquals( 2, twoBodyPlusYHeavier.countBodies() );
        assertEquals( 2, twoBodyPlusXLighter.countBodies() );
        assertEquals( 2, twoBodyPlusYLighter.countBodies() );

        //
        // CHECK ALL FORCES
        //

        // attractive forces constrained to one axis
        expected = 0.0;
        assertEquals( expected, twoBodyBalancedX.bodies.get(0).getYForce(), pct);
        assertEquals( expected, twoBodyBalancedX.bodies.get(1).getYForce(), pct);
        assertEquals( expected, twoBodyBalancedY.bodies.get(0).getXForce(), pct);
        assertEquals( expected, twoBodyBalancedY.bodies.get(1).getXForce(), pct);

        /* balanced two-body systems */
        expected = OrbitalSystem.Kgravity*bodyR1Mass*bodyR1Mass/
                Math.pow(2.0*OrbitalSystem.AU2m,2.0);
        assertEquals(  expected, twoBodyBalancedX.bodies.get(0).getXForce(), pct*expected);
        assertEquals( -expected, twoBodyBalancedX.bodies.get(1).getXForce(), pct*expected);
        assertEquals(  expected, twoBodyBalancedY.bodies.get(0).getYForce(), pct*expected);
        assertEquals( -expected, twoBodyBalancedY.bodies.get(1).getYForce(), pct*expected);

        /* asymmetric two-body systems */
        expected = OrbitalSystem.Kgravity*bodyR1Mass*bodyR2Mass/
                Math.pow(2.0*OrbitalSystem.AU2m,2.0);
        assertEquals(  expected, twoBodyPlusXHeavier.bodies.get(0).getXForce(), pct*expected);
        assertEquals( -expected, twoBodyPlusXHeavier.bodies.get(1).getXForce(), pct*expected);
        assertEquals(  expected, twoBodyPlusXLighter.bodies.get(0).getXForce(), pct*expected);
        assertEquals( -expected, twoBodyPlusXLighter.bodies.get(1).getXForce(), pct*expected);

        /* balanced four-body system */
        sys = fourBodyBalanced;

        expected = 
          OrbitalSystem.Kgravity*bodyR1Mass*bodyR1Mass/Math.pow(2.0*OrbitalSystem.AU2m,2.0) +
          Math.cos(Math.PI/4)*OrbitalSystem.Kgravity*bodyR1Mass*bodyR1Mass/
            Math.pow(Math.sqrt(8.0)*OrbitalSystem.AU2m,2.0);
        assertEquals(  expected, sys.bodies.get(0).getXForce(), pct*expected);
        assertEquals( -expected, sys.bodies.get(1).getXForce(), pct*expected);
        assertEquals(  expected, sys.bodies.get(2).getXForce(), pct*expected);
        assertEquals( -expected, sys.bodies.get(3).getXForce(), pct*expected);
        assertEquals(  expected, sys.bodies.get(0).getYForce(), pct*expected);
        assertEquals(  expected, sys.bodies.get(1).getYForce(), pct*expected);
        assertEquals( -expected, sys.bodies.get(2).getYForce(), pct*expected);
        assertEquals( -expected, sys.bodies.get(3).getYForce(), pct*expected);

        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */

        sys = fourBodyHeavyUpperRight;

        // force for 2-Y and 1-X are same as previous balanced four-body problem
        assertEquals( -expected, sys.bodies.get(1).getXForce(), pct*expected);
        assertEquals( -expected, sys.bodies.get(2).getYForce(), pct*expected);
        
        // force for 0-X,Y is slightly higher due to heavy mass located at 45 deg angle at sqrt(8) AU
        expected =
          OrbitalSystem.Kgravity*bodyR1Mass*bodyR1Mass/Math.pow(2.0*OrbitalSystem.AU2m,2.0) +
          Math.cos(Math.PI/4)*OrbitalSystem.Kgravity*bodyR1Mass*bodyR2Mass/
            Math.pow(Math.sqrt(8.0)*OrbitalSystem.AU2m,2.0);
        assertEquals( +expected, sys.bodies.get(0).getXForce(), pct*expected);
        assertEquals( +expected, sys.bodies.get(0).getYForce(), pct*expected);

        // force for 1-Y and 2-X is higher still due to closer proximity of heavy mass located 2 AU
        expected =
          OrbitalSystem.Kgravity*bodyR1Mass*bodyR2Mass/Math.pow(2.0*OrbitalSystem.AU2m,2.0) +
          Math.cos(Math.PI/4)*OrbitalSystem.Kgravity*bodyR1Mass*bodyR1Mass/
            Math.pow(Math.sqrt(8.0)*OrbitalSystem.AU2m,2.0);
        assertEquals( +expected, sys.bodies.get(1).getYForce(), pct*expected);
        assertEquals( +expected, sys.bodies.get(2).getXForce(), pct*expected);

        // force for 3-X,Y is highest of all
        expected =
          OrbitalSystem.Kgravity*bodyR1Mass*bodyR2Mass/Math.pow(2.0*OrbitalSystem.AU2m,2.0) +
          Math.cos(Math.PI/4)*OrbitalSystem.Kgravity*bodyR1Mass*bodyR2Mass/
            Math.pow(Math.sqrt(8.0)*OrbitalSystem.AU2m,2.0);
        assertEquals( -expected, sys.bodies.get(3).getXForce(), pct*expected);
        assertEquals( -expected, sys.bodies.get(3).getYForce(), pct*expected);

        //
        // CHECK MASSES
        //

        expected = 2.0*bodyR1Mass;
        assertEquals( expected, twoBodyBalancedX.totalMass(), pct*expected);
        assertEquals( expected, twoBodyBalancedY.totalMass(), pct*expected);

        expected = bodyR1Mass + bodyR2Mass;
        assertEquals( expected, twoBodyPlusXHeavier.totalMass(), pct*expected);
        assertEquals( expected, twoBodyPlusXLighter.totalMass(), pct*expected);
        assertEquals( expected, twoBodyPlusYHeavier.totalMass(), pct*expected);
        assertEquals( expected, twoBodyPlusYLighter.totalMass(), pct*expected);

        expected = 4.0*bodyR1Mass;
        assertEquals( expected, fourBodyBalanced.totalMass(), pct*expected);

        expected = 3.0*bodyR1Mass + bodyR2Mass;
        assertEquals( expected, fourBodyHeavyUpperRight.totalMass(), pct*expected);

        //
        // CHECK CENTER OF MASS
        //
        
        // one cycle propagation shouldn't move much

        // symmetric axes
        expected = 0.0;
        assertEquals( expected, twoBodyBalancedX.getCmX(), pct);
        assertEquals( expected, twoBodyBalancedX.getCmY(), pct);

        assertEquals( expected, twoBodyBalancedY.getCmX(), pct);
        assertEquals( expected, twoBodyBalancedY.getCmY(), pct);

        assertEquals( expected, fourBodyBalanced.getCmX(), pct);
        assertEquals( expected, fourBodyBalanced.getCmY(), pct);

        assertEquals( expected, twoBodyPlusXHeavier.getCmY(), pct);
        assertEquals( expected, twoBodyPlusXLighter.getCmY(), pct);

        assertEquals( expected, twoBodyPlusYHeavier.getCmX(), pct);
        assertEquals( expected, twoBodyPlusYLighter.getCmX(), pct);

        // two body asymmetric
        expected = (7.0/9.0)*OrbitalSystem.AU2m;
        assertEquals( expected, twoBodyPlusXHeavier.getCmX(), pct*expected);
        assertEquals(-expected, twoBodyPlusXLighter.getCmX(), pct*expected);
        assertEquals( expected, twoBodyPlusYHeavier.getCmY(), pct*expected);
        assertEquals(-expected, twoBodyPlusYLighter.getCmY(), pct*expected);

        // four body asymmetric
        expected = (7.0/11.0)*OrbitalSystem.AU2m;
        assertEquals( expected, fourBodyHeavyUpperRight.getCmX(), pct*expected);
        assertEquals( expected, fourBodyHeavyUpperRight.getCmY(), pct*expected);

    }

    /**
     * Test of handleImpacts() private method, of class OrbitalSystem.
     */
    @Test
    public void testBalancedSystemHandleImpacts() {
        // tests sumForces(), handleImpacts() and
        // the state vector integration methods indirectly

        OrbitalSystem sys;

        sys = twoBodyBalancedX;

        // modify two body balanced problem so bodies overlap
        sys.bodies.get(0).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(1).setX(+OrbitalSystem.earthRadius*0.5);

        sys.Propagate();

        // bodies should have merged
        assertEquals(1, sys.countBodies());

        // mass should not change
        expected = 2.0*bodyR1Mass;
        assertEquals(expected, sys.totalMass(), pct*expected);

        // radius should be (cube root of 2.0 - 26%) larger
        expected = Math.pow(2,1.0/3.0)*OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(0).radius_m, pct*expected);

        // centers of mass should be at 0
        expected = 0.0;
        assertEquals( expected, sys.getCmX(), pct);
        assertEquals( expected, sys.getCmY(), pct);


        sys = fourBodyBalanced;

        // modify four-body balanced problem so bodies overlap
        sys.bodies.get(0).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(0).setY(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(1).setX(+OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(1).setY(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(2).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(2).setY(+OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(3).setX(+OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(3).setY(+OrbitalSystem.earthRadius*0.5);

        sys.Propagate();

        // bodies should have merged
        assertEquals(1, sys.countBodies());

        // mass should not change
        expected = 4.0*bodyR1Mass;
        assertEquals(expected, sys.totalMass(), pct*expected);

        // radius should be (cube root of 4.0 - 59%) larger
        expected = Math.pow(4,1.0/3.0)*OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(0).radius_m, pct*expected);

        // centers of mass should be 0
        expected = 0.0;
        assertEquals( expected, sys.getCmX(), 100000.0*pct);
        assertEquals( expected, sys.getCmY(), 100000.0*pct);

    }

    /**
     * Additional test of handleImpacts() private method, of class OrbitalSystem.
     */
    @Test
    public void testUnbalancedSystemHandleImpacts01() {
        // tests sumForces(), handleImpacts() and
        // the state vector integration methods indirectly

        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */


        // MERGE BODIES 0 AND 1

        OrbitalSystem sys = fourBodyHeavyUpperRight;

        // modify four-body unbalanced problem so 0 and 1 overlap
        sys.bodies.get(0).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(1).setX(+OrbitalSystem.earthRadius*0.5);

        sys.Propagate();

        // two bodies should have merged
        assertEquals(3, sys.countBodies());

        // mass should not change
        expected = 3.0*bodyR1Mass + bodyR2Mass;
        assertEquals(expected, sys.totalMass(), pct*expected);

        // radius of merged body should be (cube root of 2.0 - 26%) larger
        expected = Math.pow(2,1.0/3.0)*OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(0).radius_m, pct*expected);

        // other radii should be unchanged
        // (note change in indices since 0 and 1 are now merged)
        expected = OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(1).radius_m, pct*expected);
        expected = 2.0*OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(2).radius_m, pct*expected);

        // check centers of mass
        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     1   |   *2* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *         0  (2x mass)
         */

        expected = (7.0/11.0)*OrbitalSystem.AU2m;
        assertEquals( expected, sys.getCmX(), pct*expected);
        expected = (7.0/11.0)*OrbitalSystem.AU2m;
        assertEquals( expected, sys.getCmY(), pct*expected);

    }

    /**
     * Additional test of handleImpacts() private method, of class OrbitalSystem.
     */
    @Test
    public void testUnbalancedSystemHandleImpacts23() {
        // tests sumForces(), handleImpacts() and
        // the state vector integration methods indirectly

        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */


        // MERGE BODIES 2 AND 3

        OrbitalSystem sys = fourBodyHeavyUpperRight;

        // modify four-body unbalanced problem so 2 and 3 overlap
        sys.bodies.get(2).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(3).setX(+OrbitalSystem.earthRadius);

        sys.Propagate();

        // two bodies should have merged
        assertEquals(3, sys.countBodies());

        // mass should not change
        expected = 3.0*bodyR1Mass + bodyR2Mass;
        assertEquals(expected, sys.totalMass(), pct*expected);

        // mass of merged body should be equal to sum
        expected = bodyR1Mass + bodyR2Mass;
        assertEquals(expected, sys.bodies.get(2).mass_kg, pct*expected);

        // radius of merged body should be (cube root of 9.0 - 2.08%) larger
        expected = Math.pow(9,1.0/3.0)*OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(2).radius_m, pct*expected);

        // other radii should be unchanged
        expected = OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(0).radius_m, pct*expected);
        assertEquals(expected, sys.bodies.get(1).radius_m, pct*expected);
        // check centers of mass
        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *        ***
         *        *2* (9x mass, located slightly +X)
         *        ***
         *         +----------> +X
         *
         *     0        1
         */

        expected = (9.0/11.0)*(7.5/9.0)*OrbitalSystem.earthRadius;
        assertEquals( expected, sys.getCmX(), pct*expected);
        expected = (7.0/11.0)*OrbitalSystem.AU2m;
        assertEquals( expected, sys.getCmY(), pct*expected);
    }

    /**
     * Additional test of handleImpacts() private method, of class OrbitalSystem.
     */
    @Test
    public void testUnbalancedSystemHandleImpacts123() {
        // tests sumForces(), handleImpacts() and
        // the state vector integration methods indirectly

        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */


        // MERGE BODIES 1, 2 AND 3

        OrbitalSystem sys = fourBodyHeavyUpperRight;

        // modify four-body unbalanced problem so 1, 2 and 3 overlap
        sys.bodies.get(1).setX(+OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(1).setY(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(2).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(2).setY(+OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(3).setX(+OrbitalSystem.earthRadius);
        sys.bodies.get(3).setY(+OrbitalSystem.earthRadius);

        sys.Propagate();

        // three bodies should have merged
        assertEquals(2, sys.countBodies());

        // mass should not change
        expected = 3.0*bodyR1Mass + bodyR2Mass;
        assertEquals(expected, sys.totalMass(), pct*expected);

        // mass of merged body should be equal to sum
        expected = 2.0*bodyR1Mass + bodyR2Mass;
        assertEquals(expected, sys.bodies.get(1).mass_kg, pct*expected);

        // radius of merged body should be (cube root of 10.0 - 215%) larger
        expected = Math.pow(10,1.0/3.0)*OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(1).radius_m, pct*expected);

        // other radius should be unchanged
        expected = OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(0).radius_m, pct*expected);

        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |
         *         |
         *        ***  (10x mass)
         *        *1*---------> +X
         *        ***
         *     0
         */

        expected = (10.0*(8.5/10.0)*OrbitalSystem.earthRadius
                - OrbitalSystem.AU2m)/11.0;
        // here the 1 cycle propagation is making small differences in position
        assertEquals( expected, sys.getCmX(), -0.0001*expected);
        assertEquals( expected, sys.getCmY(), -0.0001*expected);

    }

    /**
     * Additional test of handleImpacts() private method, of class OrbitalSystem.
     */
    @Test
    public void testUnbalancedSystemHandleImpacts0123() {
        // tests sumForces(), handleImpacts() and
        // the state vector integration methods indirectly

        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (2x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */


        // MERGE ALL BODIES 0, 1, 2 AND 3

        OrbitalSystem sys = fourBodyHeavyUpperRight;

        // modify four-body unbalanced problem so 1, 2 and 3 overlap
        sys.bodies.get(0).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(0).setY(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(1).setX(+OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(1).setY(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(2).setX(-OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(2).setY(+OrbitalSystem.earthRadius*0.5);
        sys.bodies.get(3).setX(+OrbitalSystem.earthRadius);
        sys.bodies.get(3).setY(+OrbitalSystem.earthRadius);

        sys.Propagate();

        // four bodies should have merged
        assertEquals(1, sys.countBodies());

        // mass should not change
        expected = 3.0*bodyR1Mass + bodyR2Mass;
        assertEquals(expected, sys.totalMass(), pct*expected);

        // mass of merged body should be equal to sum
        expected = 3.0*bodyR1Mass + bodyR2Mass;
        assertEquals(expected, sys.bodies.get(0).mass_kg, pct*expected);

        // radius of merged body should be (cube root of 10.0 - 222%) larger
        expected = Math.pow(11,1.0/3.0)*OrbitalSystem.earthRadius;
        assertEquals(expected, sys.bodies.get(0).radius_m, pct*expected);

        // check center of mass of combined body
        expected = (7.5/11.0)*OrbitalSystem.earthRadius;
        // here the 1 cycle propagation is making small differences in position
        assertEquals( expected, sys.getCmX(), pct*expected);
        assertEquals( expected, sys.getCmY(), pct*expected);

    }

    /**
     * Test of getIC method, of class OrbitalSystem.
     */
    @Test
    public void testGetIC() {
        OrbitalSystem sys = fourBodyHeavyUpperRight;
        StateVector ic = sys.getIC();

        checkInitialStateVectorFourBodyHeavyUpperRight(sys,ic);
    }



    /**
     * Test of resetToIC method of class OrbitalSystem
     */
    @Test
    public void testResetToIC() {
        OrbitalSystem sys = fourBodyHeavyUpperRight;
        for (int i = 0; i < 10; i++) {
            sys.Propagate();
        }
        sys.resetToIC();
        StateVector x = sys.getStateVector();
        checkInitialStateVectorFourBodyHeavyUpperRight(sys,x);
    }
    
    /**
     * Test of getStateDeriv method, of class OrbitalSystem.
     */
    @Test
    public void testGetStateDeriv() {

        OrbitalSystem sys = fourBodyHeavyUpperRight;

        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */

        // reuse force equations from earlier test
        double sideR2 = Math.pow(          2.0 * OrbitalSystem.AU2m,2.0);
        double hypoR2 = Math.pow(Math.sqrt(8.0)* OrbitalSystem.AU2m,2.0);

        // components of gravity due to two bodies alone
        // f01 is force on body 0 by body 1
        double f01x =  OrbitalSystem.Kgravity*bodyR1Mass*bodyR1Mass/sideR2;
        double f01y =  0.0;
        double f02x =  0.0;
        double f02y =  f01x;
        double f03x =  0.5*Math.sqrt(2.0)*OrbitalSystem.Kgravity*bodyR1Mass*bodyR2Mass/hypoR2;
        double f03y =  f03x;

        double f10x = -f01x;
        double f10y = -f01y;
        double f12x = -0.5*Math.sqrt(2.0)*OrbitalSystem.Kgravity*bodyR1Mass*bodyR1Mass/hypoR2;
        double f12y = -f12x;
        double f13x =  0.0;
        double f13y =  OrbitalSystem.Kgravity*bodyR1Mass*bodyR2Mass/sideR2;

        double f20x = -f02x;
        double f20y = -f02y;
        double f21x = -f12x;
        double f21y = -f12y;
        double f23x =  f13y;
        double f23y =  0.0;

        double f30x = -f03x;
        double f30y = -f03y;
        double f31x = -f13x;
        double f31y = -f13y;
        double f32x = -f23x;
        double f32y = -f23y;
        
        double f0x =  0.0 + f01x + f02x + f03x; double f0y =  0.0 + f01y + f02y + f03y;
        double f1x = f10x +  0.0 + f12x + f13x; double f1y = f10y +  0.0 + f12y + f13y;
        double f2x = f20x + f21x +  0.0 + f23x; double f2y = f20y + f21y +  0.0 + f23y;
        double f3x = f30x + f31x + f32x +  0.0; double f3y = f30y + f31y + f32y +  0.0;

        // calculate accels
        Double a0x = f0x/bodyR1Mass;  Double a0y = f0y/bodyR1Mass;
        Double a1x = f1x/bodyR1Mass;  Double a1y = f1y/bodyR1Mass;
        Double a2x = f2x/bodyR1Mass;  Double a2y = f2y/bodyR1Mass;
        Double a3x = f3x/bodyR2Mass;  Double a3y = f3y/bodyR2Mass;


        StateVector x  = sys.getStateVector();
        StateVector xd = sys.getStateDeriv(null, x);

        assertEquals(4*Body.NUM_STATES + 1,   xd.length());
        assertEquals( 1.0, xd.get( 0), pct);     // dt
        assertEquals( 0.0, xd.get( 1), pct);     // body 0 Xdot
        assertEquals( 0.0, xd.get( 2), pct);     // body 0 Ydot
        assertEquals( a0x, xd.get( 3), pct*Math.abs(a0x)); // body 0 Xdotdot
        assertEquals( a0y, xd.get( 4), pct*Math.abs(a0y)); // body 0 Ydotdot
        assertEquals( 0.0, xd.get( 5), pct);     // body 0 mdot
        assertEquals( 0.0, xd.get( 6), pct);     // body 1 Xdot
        assertEquals( 0.0, xd.get( 7), pct);     // body 1 Ydot
        assertEquals( a1x, xd.get( 8), pct*Math.abs(a1x)); // body 1 Xdotdot
        assertEquals( a1y, xd.get( 9), pct*Math.abs(a1y)); // body 1 Ydotdot
        assertEquals( 0.0, xd.get(10), pct);     // body 1 mdot
        assertEquals( 0.0, xd.get(11), pct);     // body 2 Xdot
        assertEquals( 0.0, xd.get(12), pct);     // body 2 Ydot
        assertEquals( a2x, xd.get(13), pct*Math.abs(a2x)); // body 2 Xdotdot
        assertEquals( a2y, xd.get(14), pct*Math.abs(a2y)); // body 2 Ydotdot
        assertEquals( 0.0, xd.get(15), pct);     // body 2 mdot
        assertEquals( 0.0, xd.get(16), pct);     // body 3 Xdot
        assertEquals( 0.0, xd.get(17), pct);     // body 3 Ydot
        assertEquals( a3x, xd.get(18), pct*Math.abs(a3x)); // body 3 Xdotdot
        assertEquals( a3y, xd.get(19), pct*Math.abs(a3y)); // body 3 Ydotdot
        assertEquals( 0.0, xd.get(20), pct);     // body 3 mdot
    }

    /**
     * Test of getStateVector method, of class OrbitalSystem.
     */
    @Test
    public void testGetStateVector() {
        double r = OrbitalSystem.AU2m;
        StateVector x = fourBodyHeavyUpperRight.getStateVector();
        assertEquals(4*Body.NUM_STATES + 1, x.length());
        assertEquals( 0.0, x.get( 0), pct);   // time
        assertEquals(  -r, x.get( 1), pct*r); // body 0 X
        assertEquals(  -r, x.get( 2), pct*r); // body 0 Y
        assertEquals( 0.0, x.get( 3), pct);   // body 0 U
        assertEquals( 0.0, x.get( 4), pct);   // body 0 V
        assertEquals( bodyR1Mass,
                           x.get( 5), pct*bodyR1Mass); // body 0 mass
        assertEquals(  +r, x.get( 6), pct*r); // body 1 X
        assertEquals(  -r, x.get( 7), pct*r); // body 1 Y
        assertEquals( 0.0, x.get( 8), pct);   // body 1 U
        assertEquals( 0.0, x.get( 9), pct);   // body 1 V
        assertEquals( bodyR1Mass,
                           x.get(10), pct*bodyR1Mass); // body 1 mass
        assertEquals(  -r, x.get(11), pct*r); // body 2 X
        assertEquals(  +r, x.get(12), pct*r); // body 2 Y
        assertEquals( 0.0, x.get(13), pct);   // body 2 U
        assertEquals( 0.0, x.get(14), pct);   // body 2 V
        assertEquals( bodyR1Mass,
                           x.get(15), pct*bodyR1Mass); // body 2 mass
        assertEquals(  +r, x.get(16), pct*r); // body 3 X
        assertEquals(  +r, x.get(17), pct*r); // body 3 Y
        assertEquals( 0.0, x.get(18), pct);   // body 3 U
        assertEquals( 0.0, x.get(19), pct);   // body 3 V
        assertEquals( bodyR2Mass,
                           x.get(20), pct*bodyR2Mass); // body 3 mass
    }

    /**
     * Test of loadStateVector method, of class OrbitalSystem.
     */
    @Test
    public void testLoadStateVector() {
        OrbitalSystem sys = fourBodyHeavyUpperRight;
        for (int i = 0; i < 10; i++) {
            sys.Propagate();
        }
        sys.loadStateVector( sys.getIC() );
        StateVector x = sys.getStateVector();
        checkInitialStateVectorFourBodyHeavyUpperRight(sys,x);
    }

    /**
     * Test of setCenter method, of class OrbitalSystem.
     */
    @Test
    public void testSetCenter() {
        int planetNumber = 1;
        OrbitalSystem instance = twoBodyBalancedX;
        instance.setCenter(planetNumber);
        Body expectedBody = instance.bodies.get(planetNumber);
        assertEquals(expectedBody, instance.centeredBody);
    }

    /**
     * Test of countBodies method, of class OrbitalSystem.
     */
    @Test
    public void testCountBodies() {
        assertEquals(2, twoBodyBalancedX.countBodies());
        assertEquals(2, twoBodyBalancedY.countBodies());
        assertEquals(2, twoBodyPlusXHeavier.countBodies());
        assertEquals(2, twoBodyPlusXLighter.countBodies());
        assertEquals(2, twoBodyPlusYHeavier.countBodies());
        assertEquals(2, twoBodyPlusYLighter.countBodies());
        assertEquals(4, fourBodyHeavyUpperRight.countBodies());
    }

    /**
     * Test of totalMass method, of class OrbitalSystem.
     */
    @Test
    public void testTotalMass() {
        
        expected = 2*bodyR1Mass;
        assertEquals( expected, twoBodyBalancedX.totalMass(),pct*expected);
        assertEquals( expected, twoBodyBalancedY.totalMass(),pct*expected);

        expected = bodyR1Mass + bodyR2Mass;
        assertEquals( expected, twoBodyPlusXHeavier.totalMass(),pct*expected);
        assertEquals( expected, twoBodyPlusXLighter.totalMass(),pct*expected);
        assertEquals( expected, twoBodyPlusYHeavier.totalMass(),pct*expected);
        assertEquals( expected, twoBodyPlusYLighter.totalMass(),pct*expected);

        expected = 4*bodyR1Mass;
        assertEquals( expected, fourBodyBalanced.totalMass(), pct*expected);

        expected = 3*bodyR1Mass + bodyR2Mass;
        assertEquals( expected, fourBodyHeavyUpperRight.totalMass(), pct*expected);
    }

    /**
     * Test of totalXMoment method, of class OrbitalSystem.
     */
    @Test
    public void testTotalXMoment() {
        expected = 0.0;
        assertEquals( expected, twoBodyBalancedX.totalXMoment(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyBalancedY.totalXMoment(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusYHeavier.totalXMoment(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusYLighter.totalXMoment(), pct*bodyR1Moment );
        assertEquals( expected, fourBodyBalanced.totalXMoment(), pct*bodyR1Moment );

        expected = bodyR2Moment - bodyR1Moment;
        assertEquals( expected, twoBodyPlusXHeavier.totalXMoment(), pct*expected );
        assertEquals( expected, fourBodyHeavyUpperRight.totalXMoment(), pct*expected );

        expected = -expected;
        assertEquals( expected, twoBodyPlusXLighter.totalXMoment(), pct*Math.abs(expected) );
    }

    /**
     * Test of totalYMoment method, of class OrbitalSystem.
     */
    @Test
    public void testTotalYMoment() {
        expected = 0.0;
        assertEquals( expected, twoBodyBalancedX.totalYMoment(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyBalancedY.totalYMoment(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusXHeavier.totalYMoment(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusXLighter.totalYMoment(), pct*bodyR1Moment );
        assertEquals( expected, fourBodyBalanced.totalYMoment(), pct*bodyR1Moment );

        expected = bodyR2Moment - bodyR1Moment;
        assertEquals( expected, twoBodyPlusYHeavier.totalYMoment(), pct*expected );
        assertEquals( expected, fourBodyHeavyUpperRight.totalYMoment(), pct*expected );

        expected = -expected;
        assertEquals( expected, twoBodyPlusYLighter.totalYMoment(), pct*Math.abs(expected) );
    }

    /**
     * Test of getCmX method, of class OrbitalSystem.
     */
    @Test
    public void testGetCmX() {
        expected = 0.0;
        assertEquals( expected, twoBodyBalancedX.getCmX(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyBalancedY.getCmX(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusYHeavier.getCmX(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusYLighter.getCmX(), pct*bodyR1Moment );
        assertEquals( expected, fourBodyBalanced.getCmX(), pct*bodyR1Moment );

        expected = (bodyR2Moment - bodyR1Moment)/(bodyR1Mass + bodyR2Mass);
        assertEquals( expected, twoBodyPlusXHeavier.getCmX(), pct*expected );
        
        expected = -expected;
        assertEquals( expected, twoBodyPlusXLighter.getCmX(), pct*Math.abs(expected) );

        expected = (bodyR2Moment - bodyR1Moment)/(3.0*bodyR1Mass + bodyR2Mass);
        assertEquals( expected, fourBodyHeavyUpperRight.getCmX(), pct*expected );
    }

    /**
     * Test of getCmY method, of class OrbitalSystem.
     */
    @Test
    public void testGetCmY() {
        expected = 0.0;
        assertEquals( expected, twoBodyBalancedX.getCmY(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyBalancedY.getCmY(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusXHeavier.getCmY(), pct*bodyR1Moment );
        assertEquals( expected, twoBodyPlusXLighter.getCmY(), pct*bodyR1Moment );
        assertEquals( expected, fourBodyBalanced.getCmY(), pct*bodyR1Moment );

        expected = (bodyR2Moment - bodyR1Moment)/(bodyR1Mass + bodyR2Mass);
        assertEquals( expected, twoBodyPlusYHeavier.getCmY(), pct*expected );

        expected = -expected;
        assertEquals( expected, twoBodyPlusYLighter.getCmY(), pct*Math.abs(expected) );

        expected = (bodyR2Moment - bodyR1Moment)/(3.0*bodyR1Mass + bodyR2Mass);
        assertEquals( expected, fourBodyHeavyUpperRight.getCmY(), pct*expected );
    }

//    /**
//     * Test of glRender method, of class OrbitalSystem.
//     */
//    @Test
//    public void testGlRender() {
//        System.out.println("glRender");
//        float minR = 0.0F;
//        boolean hiViz = false;
//        OrbitalSystem instance = new OrbitalSystem();
//        instance.glRender(minR, hiViz);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    private void checkInitialStateVectorFourBodyHeavyUpperRight(OrbitalSystem sys,
            StateVector x) {
        /*
         *  unbalanced four-body system
         *
         *         ^ +Y
         *         |   ***
         *     2   |   *3* (8x mass)
         *         |   ***
         *         +----------> +X
         *
         *     0        1
         */

        // confirm proper settings of state vector
        assertEquals( 4*Body.NUM_STATES + 1, x.length());

        assertEquals( 0.0,                x.get( 0), pct); // time = 0

        assertEquals(-OrbitalSystem.AU2m, x.get( 1), pct*OrbitalSystem.AU2m); // body 0 X
        assertEquals(-OrbitalSystem.AU2m, x.get( 2), pct*OrbitalSystem.AU2m); // body 0 Y
        assertEquals( 0.0,                x.get( 3), pct);                    // body 0 U
        assertEquals( 0.0,                x.get( 4), pct);                    // body 0 V
        assertEquals( bodyR1Mass,         x.get( 5), pct);                    // body 0 mass

        assertEquals(+OrbitalSystem.AU2m, x.get( 6), pct*OrbitalSystem.AU2m); // body 1 X
        assertEquals(-OrbitalSystem.AU2m, x.get( 7), pct*OrbitalSystem.AU2m); // body 1 Y
        assertEquals( 0.0,                x.get( 8), pct);                    // body 1 U
        assertEquals( 0.0,                x.get( 9), pct);                    // body 1 V
        assertEquals( bodyR1Mass,         x.get(10), pct);                    // body 1 mass

        assertEquals(-OrbitalSystem.AU2m, x.get(11), pct*OrbitalSystem.AU2m); // body 2 X
        assertEquals( OrbitalSystem.AU2m, x.get(12), pct*OrbitalSystem.AU2m); // body 2 Y
        assertEquals( 0.0,                x.get(13), pct);                    // body 2 U
        assertEquals( 0.0,                x.get(14), pct);                    // body 2 V
        assertEquals( bodyR1Mass,         x.get(15), pct);                    // body 2 mass

        assertEquals(+OrbitalSystem.AU2m, x.get(16), pct*OrbitalSystem.AU2m); // body 3 X
        assertEquals(+OrbitalSystem.AU2m, x.get(17), pct*OrbitalSystem.AU2m); // body 3 Y
        assertEquals( 0.0,                x.get(18), pct);                    // body 3 U
        assertEquals( 0.0,                x.get(19), pct);                    // body 3 V
        assertEquals( bodyR2Mass,         x.get(20), pct);                    // body 3 mass

    }

}