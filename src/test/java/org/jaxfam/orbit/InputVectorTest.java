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
public class InputVectorTest {
    InputVector instance;
    double eps = 1e-14;

    public InputVectorTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUp() {
        instance = new InputVector(3);
        instance.set(0, 0.0);
        instance.set(1, 1.0);
        instance.set(2, 2.0);
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of length method, of class InputVector.
     */
    @Test
    public void testLength() {
        int expResult = 3;
        int result = instance.length();
        assertEquals(expResult, result);
    }

    /**
     * Test of set and get methods, of class InputVector.
     */
    @Test
    public void testSetGet() {
        assertEquals(0.0, instance.get(0), eps);
        assertEquals(1.0, instance.get(1), eps);
        assertEquals(2.0, instance.get(2), eps);
        instance.set(1, 4.0);
        assertEquals(4.0, instance.get(1), eps);
    }

    /**
     * Test of scale method, of class InputVector.
     */
    @Test
    public void testScale() {
        instance.scale(2.0);
        assertEquals(0.0, instance.get(0), eps);
        assertEquals(2.0, instance.get(1), eps);
        assertEquals(4.0, instance.get(2), eps);
    }

    /**
     * Test of copy method
     */
    @Test
    public void testCopy() {
        InputVector copy = new InputVector(instance);
        assertEquals(3, copy.length());
        assertEquals(0.0, copy.get(0), eps);
        assertEquals(1.0, copy.get(1), eps);
        assertEquals(2.0, copy.get(2), eps);
        // check to make sure it is a deep copy
        instance.set(0, -3.0);
        assertEquals(0.0, copy.get(0), eps);
        copy.set(1, -4.0);
        assertEquals(1.0, instance.get(1), eps);
    }

    /**
     * Test of plus method, of class InputVector.
     */
    @Test
    public void testPlus() {
        InputVector dup = new InputVector(instance);
        dup.scale(2.0);
        instance.plus(dup);  // instance becomes 3x original value
        assertEquals(0.0, instance.get(0), eps);
        assertEquals(3.0, instance.get(1), eps);
        assertEquals(6.0, instance.get(2), eps);
    }

    /**
     * Test of get with negative index
     */
    @Test
    public void testGetBoundsLow() {
        assertThrows(IndexOutOfBoundsException.class, () -> instance.get(-1));
    }

    /**
     * Test of get with positive index
     */
    @Test
    public void testGetBoundsHigh() {
        assertThrows(IndexOutOfBoundsException.class, () -> instance.get(3));
    }

    /**
     * Test of set with negative index
     */
    @Test
    public void testSetBoundsLow() {
        assertThrows(IndexOutOfBoundsException.class, () -> instance.set(-1, 0.0));
    }

    /**
     * Test of set with positive index
     */
    @Test
    public void testSetBoundsHigh() {
        assertThrows(IndexOutOfBoundsException.class, () -> instance.set(4, 0.0));
    }
    /**
     * Test of mult method, of class InputVector.
     */
    @Test
    public void testMult() {
        Double factor = 2.0;
        InputVector expResult = null;
        InputVector result = InputVector.mult(instance, factor);
        assertEquals(0.0, result.get(0), eps);
        assertEquals(2.0, result.get(1), eps);
        assertEquals(4.0, result.get(2), eps);
    }

    /**
     * Test of add method, of class InputVector.
     */
    @Test
    public void testAdd() {
        InputVector dup = new InputVector(instance);
        InputVector result = InputVector.add(instance, dup);
        assertEquals(0.0, result.get(0), eps);
        assertEquals(2.0, result.get(1), eps);
        assertEquals(4.0, result.get(2), eps);
    }

}