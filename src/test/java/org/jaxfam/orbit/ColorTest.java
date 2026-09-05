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
public class ColorTest {

    Color instance;
    double eps;

    public ColorTest() {
        eps = 1e-7;
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
    }

    @BeforeEach
    public void setUp() {
        instance = new Color(0.1f, 0.2f, 0.4f);
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of r method, of class Color.
     */
    @Test
    public void testR() {
        float expResult = 0.1F;
        float result = instance.r();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of g method, of class Color.
     */
    @Test
    public void testG() {
        float expResult = 0.2F;
        float result = instance.g();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of b method, of class Color.
     */
    @Test
    public void testB() {
        float expResult = 0.4F;
        float result = instance.b();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of black method, of class Color.
     */
    @Test
    public void testBlack() {
        Color result = Color.black();
        assertEquals(0.0f, result.r(), 0.0);
        assertEquals(0.0f, result.g(), 0.0);
        assertEquals(0.0f, result.b(), 0.0);
    }

    /**
     * Test of white method, of class Color.
     */
    @Test
    public void testWhite() {
        Color result = Color.white();
        assertEquals(1.0f, result.r(), 0.0);
        assertEquals(1.0f, result.g(), 0.0);
        assertEquals(1.0f, result.b(), 0.0);
    }

    /**
     * Test of gray method, of class Color.
     */
    @Test
    public void testGray() {
        Color result = Color.gray();
        assertEquals(0.5f, result.r(), 0.0);
        assertEquals(0.5f, result.g(), 0.0);
        assertEquals(0.5f, result.b(), 0.0);
    }

    /**
     * Test of red method, of class Color.
     */
    @Test
    public void testRed() {
        Color result = Color.red();
        assertEquals(1.0f, result.r(), 0.0);
        assertEquals(0.0f, result.g(), 0.0);
        assertEquals(0.0f, result.b(), 0.0);
    }

    /**
     * Test of green method, of class Color.
     */
    @Test
    public void testGreen() {
        Color result = Color.green();
        assertEquals(0.0f, result.r(), 0.0);
        assertEquals(1.0f, result.g(), 0.0);
        assertEquals(0.0f, result.b(), 0.0);
    }

    /**
     * Test of blue method, of class Color.
     */
    @Test
    public void testBlue() {
        Color result = Color.blue();
        assertEquals(0.0f, result.r(), 0.0);
        assertEquals(0.0f, result.g(), 0.0);
        assertEquals(1.0f, result.b(), 0.0);
    }

    /**
     * Test of pink method, of class Color.
     */
    @Test
    public void testPink() {
        Color result = Color.pink();
        assertEquals(0.737255f, result.r(), eps);
        assertEquals(0.560784f, result.g(), eps);
        assertEquals(0.560784f, result.b(), eps);
    }

    /**
     * Test of yellow method, of class Color.
     */
    @Test
    public void testYellow() {
        Color result = Color.yellow();
        assertEquals(1.0f, result.r(), 0.0);
        assertEquals(1.0f, result.g(), 0.0);
        assertEquals(0.0f, result.b(), 0.0);
    }

    /**
     * Test of cyan method, of class Color.
     */
    @Test
    public void testCyan() {
        Color result = Color.cyan();
        assertEquals(0.0f, result.r(), 0.0);
        assertEquals(1.0f, result.g(), 0.0);
        assertEquals(1.0f, result.b(), 0.0);
    }

    /**
     * Test of magenta method, of class Color.
     */
    @Test
    public void testMagenta() {
        Color result = Color.magenta();
        assertEquals(1.0f, result.r(), 0.0);
        assertEquals(0.0f, result.g(), 0.0);
        assertEquals(1.0f, result.b(), 0.0);
    }

    /**
     * Test of orange method, of class Color.
     */
    @Test
    public void testOrange() {
        Color result = Color.orange();
        assertEquals(1.0f, result.r(), 0.0);
        assertEquals(0.5f, result.g(), 0.0);
        assertEquals(0.0f, result.b(), 0.0);
    }

    /**
     * Test of darkGray method, of class Color.
     */
    @Test
    public void testDarkGray() {
        Color result = Color.darkGray();
        assertEquals(0.25f, result.r(), 0.0);
        assertEquals(0.25f, result.g(), 0.0);
        assertEquals(0.25f, result.b(), 0.0);
    }

}