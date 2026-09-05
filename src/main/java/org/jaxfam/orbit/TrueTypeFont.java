//
// TrueTypeFont.java
//
// Part of the orbital mechanics demonstrator program
//
// 2026 Written for the LWJGL 3 port, replacing an AWT-based renderer
//

package org.jaxfam.orbit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * Minimal TrueType-via-OpenGL text renderer built on LWJGL's stb_truetype
 * bindings. Bakes a fixed printable-ASCII bitmap font atlas once at
 * construction, then draws strings as textured quads in immediate mode,
 * matching the legacy/fixed-function GL context the rest of this program
 * uses. Deliberately AWT-free: touching java.awt.Font/GraphicsEnvironment on
 * macOS spins up AWT's own Cocoa/AppKit thread, which can deadlock against
 * GLFW's direct use of the -XstartOnFirstThread main thread (see CLAUDE.md).
 *
 * @author Bruce Jackson, bruce@jaxfam.org
 */
public class TrueTypeFont {

    public static final int ALIGN_LEFT   = 0;
    public static final int ALIGN_RIGHT  = 1;
    public static final int ALIGN_CENTER = 2;

    private static final int FIRST_CHAR = 32;   // ' '
    private static final int NUM_CHARS  = 96;   // through '~' (126)
    private static final int ATLAS_SIZE = 512;  // pixels, square

    private final STBTTBakedChar.Buffer charData;
    private final STBTTAlignedQuad quad;
    private final int textureId;
    private final float fontHeight;

    /**
     * @param resourcePath classpath resource path to a .ttf file, e.g.
     *                     "/fonts/Inconsolata-Regular.ttf"
     * @param pixelHeight  glyph height to bake the atlas at, in pixels
     */
    public TrueTypeFont(String resourcePath, float pixelHeight) {
        this.fontHeight = pixelHeight;

        ByteBuffer fontData = readResourceToByteBuffer(resourcePath);
        ByteBuffer bitmap = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE);
        charData = STBTTBakedChar.malloc(NUM_CHARS);

        int result = STBTruetype.stbtt_BakeFontBitmap(
                fontData, pixelHeight, bitmap, ATLAS_SIZE, ATLAS_SIZE, FIRST_CHAR, charData);
        if (result < 0) {
            throw new IllegalStateException(
                    "Font atlas too small to fit all glyphs of " + resourcePath);
        }

        quad = STBTTAlignedQuad.malloc();
        textureId = uploadAtlas(bitmap, ATLAS_SIZE, ATLAS_SIZE);
    }

    private static ByteBuffer readResourceToByteBuffer(String resourcePath) {
        try (InputStream in = TrueTypeFont.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Font resource not found: " + resourcePath);
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load font resource: " + resourcePath, ex);
        }
    }

    private static int uploadAtlas(ByteBuffer bitmap, int width, int height) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        // single-channel (coverage/alpha only) atlas; current glColor tints it
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, width, height, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
        return id;
    }

    /** @return the pixel height this font's atlas was baked at */
    public float getHeight() {
        return fontHeight;
    }

    /** @return the width, in pixels at scale 1, of a single line of text */
    public float getWidth(String line) {
        float[] penX = {0f};
        float[] penY = {0f};
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
            STBTruetype.stbtt_GetBakedQuad(
                    charData, ATLAS_SIZE, ATLAS_SIZE, c - FIRST_CHAR, penX, penY, quad, true);
        }
        return penX[0];
    }

    /**
     * Draws (possibly multi-line, '\n'-separated) text with the given
     * origin, scale, and horizontal alignment. Successive lines step
     * downward on screen (decreasing world Y), matching this program's
     * Y-up coordinate convention.
     * @param x      origin X, in the current coordinate system
     * @param y      origin Y of the first line's baseline
     * @param text   text to draw; '\n' starts a new line
     * @param scaleX horizontal scale factor
     * @param scaleY vertical scale factor
     * @param align  one of ALIGN_LEFT, ALIGN_RIGHT, ALIGN_CENTER
     */
    public void drawString(float x, float y, String text, float scaleX, float scaleY, int align) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);
        // this class's glyph quads are wound for a Y-up world (see drawLine);
        // culling is irrelevant to text but disabled defensively so a
        // reasoning mistake here can't silently blank out later frames' body
        // disks, which rely on GL_CULL_FACE staying enabled
        glDisable(GL_CULL_FACE);

        float lineY = y;
        for (String line : text.split("\n", -1)) {
            float startX;
            switch (align) {
                case ALIGN_RIGHT:  startX = x - getWidth(line) * scaleX;        break;
                case ALIGN_CENTER: startX = x - getWidth(line) * scaleX / 2f;   break;
                case ALIGN_LEFT:
                default:           startX = x;                                 break;
            }
            drawLine(startX, lineY, line, scaleX, scaleY);
            lineY -= fontHeight * scaleY;
        }

        glEnable(GL_CULL_FACE);
    }

    private void drawLine(float x, float y, String line, float scaleX, float scaleY) {
        float[] penX = {0f};
        float[] penY = {0f};

        glBegin(GL_QUADS);
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
            STBTruetype.stbtt_GetBakedQuad(
                    charData, ATLAS_SIZE, ATLAS_SIZE, c - FIRST_CHAR, penX, penY, quad, true);

            // stb_truetype's quad.y0/y1 are baseline-relative offsets for a
            // Y-down image space (y0 above baseline is negative); negate to
            // place them correctly in this program's Y-up world
            float x0 = x + quad.x0() * scaleX;
            float x1 = x + quad.x1() * scaleX;
            float y0 = y - quad.y0() * scaleY;
            float y1 = y - quad.y1() * scaleY;

            glTexCoord2f(quad.s0(), quad.t0()); glVertex2f(x0, y0);
            glTexCoord2f(quad.s1(), quad.t0()); glVertex2f(x1, y0);
            glTexCoord2f(quad.s1(), quad.t1()); glVertex2f(x1, y1);
            glTexCoord2f(quad.s0(), quad.t1()); glVertex2f(x0, y1);
        }
        glEnd();
    }

    /** Releases the native resources (GL texture and off-heap buffers) used by this font. */
    public void destroy() {
        glDeleteTextures(textureId);
        charData.free();
        quad.free();
    }
}
