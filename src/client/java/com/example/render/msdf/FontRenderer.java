package com.example.render.msdf;

import com.example.render.font.FontFace;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Backward-compatibility proxy delegating to {@link com.example.render.font.FontRenderer}.
 */
public final class FontRenderer {
    public static final FontRenderer INSTANCE = new FontRenderer();

    private FontRenderer() {}

    public enum Face {
        SfRegular(FontFace.SF_REGULAR),
        SfMedium(FontFace.SF_MEDIUM),
        SfSemibold(FontFace.SF_SEMIBOLD);

        private final FontFace delegate;

        Face(FontFace delegate) {
            this.delegate = delegate;
        }

        public FontFace getDelegate() {
            return delegate;
        }
    }

    public void draw(GuiGraphicsExtractor graphics, Face face, String text, float x, float y, float size, int color) {
        com.example.render.font.FontRenderer.INSTANCE.draw(graphics, face.delegate, text, x, y, size, color);
    }

    public void drawCentered(GuiGraphicsExtractor graphics, Face face, String text, float centerX, float y, float size, int color) {
        com.example.render.font.FontRenderer.INSTANCE.drawCentered(graphics, face.delegate, text, centerX, y, size, color);
    }

    public void drawWithShadow(GuiGraphicsExtractor graphics, Face face, String text, float x, float y, float size, int color, int shadowColor) {
        com.example.render.font.FontRenderer.INSTANCE.drawWithShadow(graphics, face.delegate, text, x, y, size, color, shadowColor);
    }

    public float width(Face face, String text, float size) {
        return com.example.render.font.FontRenderer.INSTANCE.width(face.delegate, text, size);
    }
}
