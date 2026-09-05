/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jaxfam.orbit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author ebjackso
 */
public class TrailingPathTest {

    TrailingPath tp;
    final int cap = 5;
    final double eps = 1e-14;

    public TrailingPathTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUp() {
        tp = new TrailingPath(cap); // small cap to check wrap
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of addPt and getT, getX, getY methods of class TrailingPath.
     */
    @Test
    public void testAddGetPt() {
        double t = 0.0;
        double x =-1.0;
        double y = 345.6;
        tp.addPt(t, x, y);
        assertEquals(1, tp.length());
        assertEquals(t, tp.getT(0), eps);
        assertEquals(x, tp.getX(0), eps);
        assertEquals(y, tp.getY(0), eps);
    }


    /**
     * Test of length method of class TrailingPath wtth no data stored
     */
    @Test
    public void testZeroLength() {
        int result = tp.length();
        assertEquals(0, result);
        assertThrows(IndexOutOfBoundsException.class, () -> tp.getX(0));
    }


    /**
     * Test of length method, of class TrailingPath.
     */
    @Test
    public void testLength() {
        double t = 0.0;
        double x = 0.0;
        double y = 0.0;
        int expResult = 3;
        for (int i = 0; i < expResult; i++)
            tp.addPt(t, x, y);
        int result = tp.length();
        assertEquals(expResult, result);
    }

    /**
     * Test of length method, of class TrailingPath.
     */
    @Test
    public void testOutOfBoundsAccess() {
        double t = 0.0;
        double x = 0.0;
        double y = 0.0;
        int expResult = 3;
        for (int i = 0; i < expResult; i++)
            tp.addPt(t, x, y);
        int result = tp.length();
        assertThrows(IndexOutOfBoundsException.class, () -> tp.getY(expResult));
    }

    /**
     * Test wrapping logic of class TrailingPath
     *
     * This should store time points at 2.0 .. 6.0
     */

    @Test
    public void testSingleWrap() {
        double t, x, y;
        int delta = 2;
        for (int i=0; i < cap+delta; i++) {
            t =        (double) i;
            x =  2.0 * t;
            y = -1.0 * t;
            tp.addPt(t, x, y);
        }
        assertEquals(cap, tp.length());
        for (int i = 0; i < cap; i++) {
            t =  (double) (i + delta);
            x =  2.0 * t;
            y = -1.0 * t;
            assertEquals( t, tp.getT(i), eps);
            assertEquals( x, tp.getX(i), eps);
            assertEquals( y, tp.getY(i), eps);
        }
    }

    /**
     * Test extended wrapping logic of class TrailingPath
     *
     * This should store time points at 12.0 .. 16.0
     */

    @Test
    public void testTripleWrap() {
        double t, x, y;
        int bias = 2*cap + 2;
        for (int i=0; i < cap+bias; i++) {
            t =  (double) i;
            x =  2.0 * t;
            y = -1.0 * t;
            tp.addPt(t, x, y);
        }
        assertEquals(cap, tp.length());
        for (int i = 0; i < cap; i++) {
            t =  (double) (i + bias);
            x =  2.0 * t;
            y = -1.0 * t;
            assertEquals( t, tp.getT(i), eps);
            assertEquals( x, tp.getX(i), eps);
            assertEquals( y, tp.getY(i), eps);
        }
    }

    /**
     * Test what happens over extended period of time
     */

    @Test
    public void testOnFlyWrap() {
        double t, x, y;
        int firstRec, lenM1;
        int length = 10*cap + 17;
        assertEquals(0, tp.length());
        for (int i=0; i < length; i++) {
            t =  (double) i;
            x =  2.0 * t;
            y = -1.0 * t;
            tp.addPt(t, x, y);
            lenM1 = tp.length()-1;
            if (i < cap) {
                assertEquals( i, lenM1);
                firstRec = 0;
            } else {
                assertEquals( cap-1, lenM1);
                firstRec = i - cap + 1;
            }
            System.out.print(i);
            System.out.print(": ");
            // test every component in path
            for(int j = 0; j < tp.length(); j++) {
                double expectedT = (double) (firstRec+j);
                double expectedX =  2.0 * expectedT;
                double expectedY = -1.0 * expectedT;
                System.out.print(expectedT);
                if (j < (tp.length()-1)) System.out.print(", ");
                assertEquals( expectedT, tp.getT(j), eps);
                assertEquals( expectedX, tp.getX(j), eps);
                assertEquals( expectedY, tp.getY(j), eps);
            }
            System.out.println();
        }
    }


}