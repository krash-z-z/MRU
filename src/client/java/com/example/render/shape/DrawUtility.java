package com.example.render.shape;

import com.example.render.core.Pipelines;
import com.example.render.core.ShaderState;
import com.example.render.util.Color4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3x2f;

/**
 * Backward-compatible GPU drawing utility delegating to {@link ShapeRenderer} and {@link Pipelines}.
 */
public final class DrawUtility {

    public static final DrawUtility INSTANCE = new DrawUtility();

    private DrawUtility() {}

    public static void drawSquircle(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int color) {
        ShapeRenderer.drawSquircle(graphics, x, y, width, height, radius, smoothing, color);
    }

    public static void drawSquircle(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        ShapeRenderer.drawSquircle(graphics, x, y, width, height, radius, 0.8f, color);
    }

    public static void drawSquircle(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int color) {
        ShapeRenderer.drawSquircleExact(graphics, x, y, width, height, rTL, rTR, rBR, rBL, smoothing, color, color, color, color);
    }

    public static void drawSquircle(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, int color) {
        ShapeRenderer.drawSquircleExact(graphics, x, y, width, height, rTL, rTR, rBR, rBL, 0.8f, color, color, color, color);
    }

    public static void drawSquircleExact(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight) {
        ShapeRenderer.drawSquircleExact(graphics, x, y, width, height, radius, radius, radius, radius, smoothing, colorTopLeft, colorBottomLeft, colorBottomRight, colorTopRight);
    }

    public static void drawSquircleExact(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight) {
        ShapeRenderer.drawSquircleExact(graphics, x, y, width, height, rTL, rTR, rBR, rBL, smoothing, colorTopLeft, colorBottomLeft, colorBottomRight, colorTopRight);
    }

    public static void drawSquircleExact(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int colorLeft, int colorRight) {
        ShapeRenderer.drawSquircleExact(graphics, x, y, width, height, radius, radius, radius, radius, smoothing, colorLeft, colorLeft, colorRight, colorRight);
    }

    public static void drawSquircleExact(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int color) {
        ShapeRenderer.drawSquircleExact(graphics, x, y, width, height, radius, radius, radius, radius, smoothing, color, color, color, color);
    }

    public static void drawHueSpectrum(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int alpha) {
        ShapeRenderer.drawHueSpectrum(graphics, x, y, width, height, radius, smoothing, alpha);
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        ShapeRenderer.drawRoundedRect(graphics, x, y, width, height, radius, color);
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, int color) {
        ShapeRenderer.drawRoundedRect(graphics, x, y, width, height, rTL, rTR, rBR, rBL, color);
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int color) {
        ShapeRenderer.drawRoundedOutline(graphics, x, y, width, height, radius, smoothing, color);
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int color) {
        ShapeRenderer.drawRoundedOutline(graphics, x, y, width, height, rTL, rTR, rBR, rBL, smoothing, color);
    }

    public static void drawPill(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        ShapeRenderer.drawPill(graphics, x, y, width, height, color);
    }

    public static void drawCircle(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        ShapeRenderer.drawCircle(graphics, centerX, centerY, radius, color);
    }

    public static void drawHexagon(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        ShapeRenderer.drawHexagon(graphics, centerX, centerY, radius, color);
    }

    public static void drawHexagonOutline(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        ShapeRenderer.drawHexagonOutline(graphics, centerX, centerY, radius, color);
    }

    public static void drawUniversalShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int shapeMode, int shadowColor, float blurRadius, float offsetY) {
        ShapeRenderer.drawUniversalShadow(graphics, x, y, width, height, rTL, rTR, rBR, rBL, smoothing, shapeMode, shadowColor, blurRadius, offsetY);
    }

    public static void drawCircleShadow(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int shadowColor, float blurRadius, float offsetY) {
        ShapeRenderer.drawCircleShadow(graphics, centerX, centerY, radius, shadowColor, blurRadius, offsetY);
    }

    public static void drawPillShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int shadowColor, float blurRadius, float offsetY) {
        ShapeRenderer.drawPillShadow(graphics, x, y, width, height, shadowColor, blurRadius, offsetY);
    }

    public static void drawHexagonShadow(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int shadowColor, float blurRadius, float offsetY) {
        ShapeRenderer.drawHexagonShadow(graphics, centerX, centerY, radius, shadowColor, blurRadius, offsetY);
    }

    public static void drawShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        ShapeRenderer.drawShadow(graphics, x, y, width, height, radius, radius, radius, radius, 0.0f, color, 8.0f, 2.0f);
    }

    public static void drawShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int shadowColor, float blurRadius, float offsetY) {
        ShapeRenderer.drawShadow(graphics, x, y, width, height, radius, radius, radius, radius, smoothing, shadowColor, blurRadius, offsetY);
    }

    public static void drawShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int shadowColor, float blurRadius, float offsetY) {
        ShapeRenderer.drawShadow(graphics, x, y, width, height, rTL, rTR, rBR, rBL, smoothing, shadowColor, blurRadius, offsetY);
    }

    public static void drawGlow(GuiGraphicsExtractor graphics, float x, float y, float size, int color) {
        if (size <= 0.0f || ((color >>> 24) & 255) == 0) return;
        graphics.guiRenderState.addGuiElement(new ShaderState.GlowState(
            Pipelines.GLOW,
            new Matrix3x2f(graphics.pose()), x, y, x + size, y + size, color, graphics.scissorStack.peek()
        ));
    }

    public static void drawGradientBox(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int mode, float angleDegrees, int colorStart, int colorEnd) {
        if ((((colorStart | colorEnd) >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f) return;
        float halfShortSide = Math.min(width, height) * 0.5f;
        float rRatio = halfShortSide > 0.0001f ? Math.clamp(radius / halfShortSide, 0.0f, 1.0f) : 0.0f;

        graphics.guiRenderState.addGuiElement(
            new ShaderState.GradientRectState(
                Pipelines.GRADIENT_RECT,
                new Matrix3x2f(graphics.pose()),
                x, y, x + width, y + height,
                rRatio,
                smoothing,
                mode,
                angleDegrees,
                colorStart,
                colorEnd,
                0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height) {
        ShapeRenderer.drawTexture(graphics, textureId, x, y, width, height, 0xFFFFFFFF);
    }

    public static void drawTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, int color) {
        ShapeRenderer.drawTexture(graphics, textureId, x, y, width, height, color);
    }

    public static void drawTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int color) {
        drawTexture(graphics, textureId, x, y, width, height, u1, v1, u2, v2, color, color);
    }

    public static void drawTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int colorLeft, int colorRight) {
        if ((((colorLeft | colorRight) >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f) return;
        var texture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
        graphics.guiRenderState.addGuiElement(
            new ShaderState.TextureState(
                Pipelines.TINTED_TEXTURE,
                TextureSetup.singleTexture(texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                new Matrix3x2f(graphics.pose()),
                x, y, x + width, y + height,
                u1, v1, u2, v2,
                colorLeft, colorRight,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawRoundedTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, float radius, int color) {
        ShapeRenderer.drawRoundedTexture(graphics, textureId, x, y, width, height, radius, color);
    }

    public static void drawRoundedTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, float radius, float uPixels, float vPixels, float regionPixels, int color) {
        ShapeRenderer.drawRoundedTexture(graphics, textureId, x, y, width, height, radius, uPixels, vPixels, regionPixels, color);
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius) {
        ShapeRenderer.drawPlayerHead(graphics, player, x, y, size, radius);
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius, boolean includeHatLayer) {
        ShapeRenderer.drawPlayerHead(graphics, player, x, y, size, radius, includeHatLayer, 1.0f);
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius, boolean includeHatLayer, float alpha) {
        ShapeRenderer.drawPlayerHead(graphics, player, x, y, size, radius, includeHatLayer, alpha);
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, Identifier skinTexture, float x, float y, float size, float radius, boolean includeHatLayer, float alpha) {
        if (skinTexture == null || alpha <= 0.01f) return;
        int a = Math.clamp((int) (255.0f * alpha), 0, 255);
        int color = (a << 24) | 0x00FFFFFF;
        if (includeHatLayer) {
            ShapeRenderer.drawRoundedTexture(graphics, skinTexture, x, y, size, size, radius, 8.0f, 8.0f, 8.0f, color);
            ShapeRenderer.drawRoundedTexture(graphics, skinTexture, x, y, size, size, radius, 40.0f, 8.0f, 8.0f, color);
        } else {
            ShapeRenderer.drawRoundedTexture(graphics, skinTexture, x, y, size, size, radius, 8.0f, 8.0f, 8.0f, color);
        }
    }

    public static void drawPlayerHeadWithHat(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius) {
        ShapeRenderer.drawPlayerHeadWithHat(graphics, player, x, y, size, radius);
    }

    public static void drawPlayerHeadWithHat(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius, float alpha) {
        ShapeRenderer.drawPlayerHead(graphics, player, x, y, size, radius, true, alpha);
    }

    public static void drawEntityHead(GuiGraphicsExtractor graphics, LivingEntity entity, float x, float y, float size, float radius) {
        ShapeRenderer.drawEntityHead(graphics, entity, x, y, size, radius);
    }
}
