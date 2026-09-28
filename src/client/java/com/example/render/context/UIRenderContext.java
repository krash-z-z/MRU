package com.example.render.context;

import com.example.render.font.FontFace;
import com.example.render.font.FontRenderer;
import com.example.render.shape.CornerRadii;
import com.example.render.shape.ShapeRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

/**
 * High-level ergonomic UI rendering context providing fluent drawing,
 * stateful alpha multipliers, and consolidated component rendering.
 */
public class UIRenderContext {

    private final GuiGraphicsExtractor graphics;
    private float alpha = 1.0f;

    public UIRenderContext(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public static UIRenderContext of(GuiGraphicsExtractor graphics) {
        return new UIRenderContext(graphics);
    }

    public GuiGraphicsExtractor graphics() {
        return graphics;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
    }

    public Color applyAlpha(Color color) {
        if (color == null) return Color.WHITE;
        return color.withAlphaMultiplied(alpha);
    }

    // --- CARDS & BACKGROUNDS ---

    public void drawCard(float x, float y, float width, float height, CornerRadii radii, Color background) {
        ShapeRenderer.drawSquircle(graphics, x, y, width, height, radii, applyAlpha(background));
    }

    public void drawCardWithShadow(
        float x, float y, float width, float height,
        CornerRadii radii,
        Color background,
        Color shadowColor, float shadowBlur, float shadowOffsetY
    ) {
        if (shadowColor != null && shadowBlur > 0.1f) {
            ShapeRenderer.drawShadow(graphics, x, y, width, height, radii, applyAlpha(shadowColor), shadowBlur, shadowOffsetY);
        }
        ShapeRenderer.drawSquircle(graphics, x, y, width, height, radii, applyAlpha(background));
    }

    public void drawOutline(float x, float y, float width, float height, CornerRadii radii, Color outlineColor) {
        ShapeRenderer.drawRoundedOutline(graphics, x, y, width, height, radii, applyAlpha(outlineColor));
    }

    // --- TEXT ---

    public void drawText(FontFace face, String text, float x, float y, float size, Color color) {
        FontRenderer.INSTANCE.draw(graphics, face, text, x, y, size, applyAlpha(color));
    }

    public void drawTextCentered(FontFace face, String text, float centerX, float y, float size, Color color) {
        FontRenderer.INSTANCE.drawCentered(graphics, face, text, centerX, y, size, applyAlpha(color));
    }

    public void drawTextRight(FontFace face, String text, float rightX, float y, float size, Color color) {
        FontRenderer.INSTANCE.drawRight(graphics, face, text, rightX, y, size, applyAlpha(color));
    }

    public void drawTextWithShadow(FontFace face, String text, float x, float y, float size, Color color, Color shadowColor) {
        FontRenderer.INSTANCE.drawWithShadow(graphics, face, text, x, y, size, applyAlpha(color), applyAlpha(shadowColor));
    }

    public float textWidth(FontFace face, String text, float size) {
        return FontRenderer.INSTANCE.width(face, text, size);
    }

    // --- BADGES & BARS ---

    public void drawPill(float x, float y, float width, float height, Color color) {
        ShapeRenderer.drawPill(graphics, x, y, width, height, applyAlpha(color));
    }

    public void drawProgressBar(float x, float y, float width, float height, float progress, CornerRadii radii, Color background, Color fill) {
        ShapeRenderer.drawSquircle(graphics, x, y, width, height, radii, applyAlpha(background));
        float clamped = Math.clamp(progress, 0.0f, 1.0f);
        if (clamped > 0.001f) {
            ShapeRenderer.drawSquircle(graphics, x, y, width * clamped, height, radii, applyAlpha(fill));
        }
    }

    // --- IMAGES & HEADS ---

    public void drawTexture(Identifier textureId, float x, float y, float width, float height, Color tint) {
        ShapeRenderer.drawTexture(graphics, textureId, x, y, width, height, applyAlpha(tint).toArgb());
    }

    public void drawRoundedTexture(Identifier textureId, float x, float y, float width, float height, float radius, Color tint) {
        ShapeRenderer.drawRoundedTexture(graphics, textureId, x, y, width, height, radius, applyAlpha(tint).toArgb());
    }

    public void drawPlayerHead(AbstractClientPlayer player, float x, float y, float size, float radius, boolean hatLayer) {
        ShapeRenderer.drawPlayerHead(graphics, player, x, y, size, radius, hatLayer, alpha);
    }
}
