package com.example.render.shape;

import com.example.render.context.Color;
import com.example.render.core.Pipelines;
import com.example.render.core.ShaderState;
import com.example.render.util.Color4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3x2f;

/**
 * Modern GPU-accelerated 2D Shape Renderer.
 */
public final class ShapeRenderer {

    private ShapeRenderer() {}

    // --- RECTANGLES & SQUIRCLES ---

    public static void drawRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0 || ((color >>> 24) & 0xFF) <= 0) return;
        drawSquircleExact(graphics, x, y, width, height, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, color, color, color, color);
    }

    public static void drawRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Color color) {
        if (color == null) return;
        drawRect(graphics, x, y, width, height, color.toArgb());
    }

    public static void drawRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Color4f color) {
        if (color == null) return;
        drawRect(graphics, x, y, width, height, color.toArgbInt());
    }

    public static void drawSquircle(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int color) {
        drawSquircleExact(graphics, x, y, width, height, radius, radius, radius, radius, smoothing, color, color, color, color);
    }

    public static void drawSquircle(GuiGraphicsExtractor graphics, float x, float y, float width, float height, CornerRadii radii, int color) {
        if (radii == null) {
            drawRect(graphics, x, y, width, height, color);
            return;
        }
        drawSquircleExact(graphics, x, y, width, height, radii.tl(), radii.tr(), radii.br(), radii.bl(), radii.smoothing(), color, color, color, color);
    }

    public static void drawSquircle(GuiGraphicsExtractor graphics, float x, float y, float width, float height, CornerRadii radii, Color color) {
        if (color == null) return;
        drawSquircle(graphics, x, y, width, height, radii, color.toArgb());
    }

    public static void drawSquircleExact(
        GuiGraphicsExtractor graphics,
        float x, float y, float width, float height,
        float rTL, float rTR, float rBR, float rBL, float smoothing,
        int cTL, int cBL, int cBR, int cTR
    ) {
        if ((((cTL | cBL | cBR | cTR) >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f) return;
        float halfShortSide = Math.min(width, height) * 0.5f;
        float ratioTL = halfShortSide > 0.0001f ? Math.clamp(rTL / halfShortSide, 0.0f, 4.0f) : 0.0f;
        float ratioTR = halfShortSide > 0.0001f ? Math.clamp(rTR / halfShortSide, 0.0f, 4.0f) : 0.0f;
        float ratioBR = halfShortSide > 0.0001f ? Math.clamp(rBR / halfShortSide, 0.0f, 4.0f) : 0.0f;
        float ratioBL = halfShortSide > 0.0001f ? Math.clamp(rBL / halfShortSide, 0.0f, 4.0f) : 0.0f;

        graphics.guiRenderState.addGuiElement(
            new ShaderState.SquircleState(
                Pipelines.SQUIRCLE,
                new Matrix3x2f(graphics.pose()),
                x, y, x + width, y + height,
                ratioTL, ratioTR, ratioBR, ratioBL,
                Math.clamp(smoothing, 0.0f, 1.0f),
                cTL, cBL, cBR, cTR,
                0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawHueSpectrum(
        GuiGraphicsExtractor graphics,
        float x, float y, float width, float height,
        float radius, float smoothing, int alpha
    ) {
        if (width <= 0.05f || height <= 0.05f || (alpha & 0xFF) <= 0) return;
        int a = alpha & 0xFF;
        int[] rainbow = {
            0x00FF0000,
            0x00FFFF00,
            0x0000FF00,
            0x0000FFFF,
            0x000000FF,
            0x00FF00FF,
            0x00FF0000
        };

        int numSegs = rainbow.length - 1;
        float segW = width / numSegs;

        for (int i = 0; i < numSegs; i++) {
            float sx = x + i * segW;
            float sw = (i == numSegs - 1) ? ((x + width) - sx) : (segW + 2.0f);
            int cA = (a << 24) | rainbow[i];
            int cB = (a << 24) | rainbow[i + 1];

            float rTL = (i == 0) ? radius : 0.0f;
            float rBL = (i == 0) ? radius : 0.0f;
            float rTR = (i == numSegs - 1) ? radius : 0.0f;
            float rBR = (i == numSegs - 1) ? radius : 0.0f;
            float s = (i == 0 || i == numSegs - 1) ? smoothing : 0.0f;

            drawSquircleExact(graphics, sx, y, sw, height, rTL, rTR, rBR, rBL, s, cA, cA, cB, cB);
        }
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        drawRoundedRect(graphics, x, y, width, height, radius, radius, radius, radius, color);
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, Color color) {
        if (color == null) return;
        drawRoundedRect(graphics, x, y, width, height, radius, color.toArgb());
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, Color4f color) {
        if (color == null) return;
        drawRoundedRect(graphics, x, y, width, height, radius, color.toArgbInt());
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f) return;
        float halfShortSide = Math.min(width, height) * 0.5f;
        float ratioTL = halfShortSide > 0.0001f ? Math.clamp(rTL / halfShortSide, 0.0f, 1.0f) : 0.0f;
        float ratioTR = halfShortSide > 0.0001f ? Math.clamp(rTR / halfShortSide, 0.0f, 1.0f) : 0.0f;
        float ratioBR = halfShortSide > 0.0001f ? Math.clamp(rBR / halfShortSide, 0.0f, 1.0f) : 0.0f;
        float ratioBL = halfShortSide > 0.0001f ? Math.clamp(rBL / halfShortSide, 0.0f, 1.0f) : 0.0f;

        graphics.guiRenderState.addGuiElement(
            new ShaderState.ShaderRoundedRectState(
                Pipelines.ROUNDED_RECT,
                new Matrix3x2f(graphics.pose()),
                x, y, x + width, y + height,
                ratioTL, ratioTR, ratioBR, ratioBL,
                0.0f,
                color,
                0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    // --- OUTLINES ---

    public static void drawRoundedOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, float smoothing, int color) {
        drawRoundedOutline(graphics, x, y, width, height, radius, radius, radius, radius, smoothing, color);
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, CornerRadii radii, int color) {
        if (radii == null) return;
        drawRoundedOutline(graphics, x, y, width, height, radii.tl(), radii.tr(), radii.br(), radii.bl(), radii.smoothing(), color);
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, CornerRadii radii, Color color) {
        if (color == null) return;
        drawRoundedOutline(graphics, x, y, width, height, radii, color.toArgb());
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f) return;
        float halfShortSide = Math.min(width, height) * 0.5f;
        float ratioTL = halfShortSide > 0.0001f ? Math.clamp(rTL / halfShortSide, 0.0f, 1.0f) : 0.0f;
        float ratioTR = halfShortSide > 0.0001f ? Math.clamp(rTR / halfShortSide, 0.0f, 1.0f) : 0.0f;
        float ratioBR = halfShortSide > 0.0001f ? Math.clamp(rBR / halfShortSide, 0.0f, 1.0f) : 0.0f;
        float ratioBL = halfShortSide > 0.0001f ? Math.clamp(rBL / halfShortSide, 0.0f, 1.0f) : 0.0f;

        graphics.guiRenderState.addGuiElement(
            new ShaderState.ShaderRoundedRectState(
                Pipelines.ROUNDED_OUTLINE,
                new Matrix3x2f(graphics.pose()),
                x, y, x + width, y + height,
                ratioTL, ratioTR, ratioBR, ratioBL,
                Math.clamp(smoothing, 0.0f, 1.0f),
                color,
                0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    // --- CIRCLES, PILLS, HEXAGONS ---

    public static void drawCircle(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || radius <= 0.05f) return;
        graphics.guiRenderState.addGuiElement(
            new ShaderState.ShaderCircleState(
                Pipelines.CIRCLE,
                new Matrix3x2f(graphics.pose()),
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                color,
                0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawCircle(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, Color color) {
        if (color == null) return;
        drawCircle(graphics, centerX, centerY, radius, color.toArgb());
    }

    public static void drawPill(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int color) {
        if (width <= 0.05f || height <= 0.05f) return;
        drawSquircle(graphics, x, y, width, height, Math.min(width, height) * 0.5f, 0.5f, color);
    }

    public static void drawPill(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Color color) {
        if (color == null) return;
        drawPill(graphics, x, y, width, height, color.toArgb());
    }

    public static void drawHexagon(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || radius <= 0.05f) return;
        graphics.guiRenderState.addGuiElement(
            new ShaderState.ShaderCircleState(
                Pipelines.HEXAGON,
                new Matrix3x2f(graphics.pose()),
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                color,
                0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawHexagonOutline(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || radius <= 0.05f) return;
        graphics.guiRenderState.addGuiElement(
            new ShaderState.ShaderCircleState(
                Pipelines.HEXAGON_OUTLINE,
                new Matrix3x2f(graphics.pose()),
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                color,
                0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawUniversalShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int shapeMode, int color, float blurRadius, float offsetY) {
        if (((color >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f || blurRadius <= 0.1f) return;
        float pad = blurRadius * 2.5f;
        float left = x - pad;
        float top = y + offsetY - pad;
        float right = x + width + pad;
        float bottom = y + offsetY + height + pad;
        graphics.guiRenderState.addGuiElement(
            new ShaderState.UniversalShadowState(
                Pipelines.UNIVERSAL_SHADOW,
                new Matrix3x2f(graphics.pose()),
                left, top, right, bottom,
                rTL, rTR, rBR, rBL,
                smoothing,
                shapeMode,
                blurRadius,
                color,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawCircleShadow(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int shadowColor, float blurRadius, float offsetY) {
        if (((shadowColor >>> 24) & 0xFF) <= 0 || radius <= 0.05f || blurRadius <= 0.1f) return;
        drawUniversalShadow(graphics, centerX - radius, centerY - radius, radius * 2.0f, radius * 2.0f, radius, radius, radius, radius, 0.0f, 3, shadowColor, blurRadius, offsetY);
    }

    public static void drawPillShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, int shadowColor, float blurRadius, float offsetY) {
        if (((shadowColor >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f || blurRadius <= 0.1f) return;
        float r = Math.min(width, height) * 0.5f;
        drawUniversalShadow(graphics, x, y, width, height, r, r, r, r, 0.0f, 2, shadowColor, blurRadius, offsetY);
    }

    public static void drawHexagonShadow(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int shadowColor, float blurRadius, float offsetY) {
        if (((shadowColor >>> 24) & 0xFF) <= 0 || radius <= 0.05f || blurRadius <= 0.1f) return;
        drawUniversalShadow(graphics, centerX - radius, centerY - radius, radius * 2.0f, radius * 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 4, shadowColor, blurRadius, offsetY);
    }

    // --- SHADOW & GLOW ---

    public static void drawShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, int color, float blurRadius, float offsetY) {
        drawUniversalShadow(graphics, x, y, width, height, rTL, rTR, rBR, rBL, smoothing, smoothing > 0.001f ? 1 : 0, color, blurRadius, offsetY);
    }

    public static void drawShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height, CornerRadii radii, Color color, float blurRadius, float offsetY) {
        if (color == null || radii == null) return;
        drawShadow(graphics, x, y, width, height, radii.tl(), radii.tr(), radii.br(), radii.bl(), radii.smoothing(), color.toArgb(), blurRadius, offsetY);
    }

    // --- TEXTURES & HEADS ---

    public static void drawTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f) return;
        var texture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
        graphics.guiRenderState.addGuiElement(
            new ShaderState.TextureState(
                Pipelines.TINTED_TEXTURE,
                TextureSetup.singleTexture(texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                new Matrix3x2f(graphics.pose()),
                x, y, x + width, y + height,
                0.0f, 0.0f, 1.0f, 1.0f,
                color,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawRoundedTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, float radius, int color) {
        drawRoundedTexture(graphics, textureId, x, y, width, height, radius, 0.0f, 0.0f, 0.0f, color);
    }

    public static void drawRoundedTexture(GuiGraphicsExtractor graphics, Identifier textureId, float x, float y, float width, float height, float radius, float uPixels, float vPixels, float regionPixels, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || width <= 0.05f || height <= 0.05f) return;
        var texture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
        float halfShortSide = Math.min(width, height) * 0.5f;
        float rRatio = halfShortSide > 0.0001f ? Math.clamp(radius / halfShortSide, 0.0f, 1.0f) : 0.0f;
        FilterMode filter = regionPixels > 0.01f ? FilterMode.NEAREST : FilterMode.LINEAR;

        graphics.guiRenderState.addGuiElement(
            new ShaderState.RoundedTextureState(
                Pipelines.ROUNDED_TEXTURE,
                TextureSetup.singleTexture(texture.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(filter)),
                new Matrix3x2f(graphics.pose()),
                x, y, x + width, y + height,
                rRatio, 0.8f,
                uPixels, vPixels, regionPixels,
                color, 0.75f,
                graphics.scissorStack.peek()
            )
        );
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius, boolean includeHatLayer, float alpha) {
        if (player == null || alpha <= 0.01f) return;
        Identifier skin = null;
        try {
            skin = player.getSkin().body().texturePath();
        } catch (Throwable ignored) {}
        if (skin == null) {
            try {
                skin = DefaultPlayerSkin.getDefaultTexture();
            } catch (Throwable ignored) {}
        }
        if (skin == null) return;
        int a = Math.clamp((int) (255.0f * alpha), 0, 255);
        int color = (a << 24) | 0x00FFFFFF;
        if (includeHatLayer) {
            drawRoundedTexture(graphics, skin, x, y, size, size, radius, 8.0f, 8.0f, 8.0f, color);
            drawRoundedTexture(graphics, skin, x, y, size, size, radius, 40.0f, 8.0f, 8.0f, color);
        } else {
            drawRoundedTexture(graphics, skin, x, y, size, size, radius, 8.0f, 8.0f, 8.0f, color);
        }
    }

    public static void drawPlayerHead(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius) {
        drawPlayerHead(graphics, player, x, y, size, radius, false, 1.0f);
    }

    public static void drawPlayerHeadWithHat(GuiGraphicsExtractor graphics, AbstractClientPlayer player, float x, float y, float size, float radius) {
        drawPlayerHead(graphics, player, x, y, size, radius, true, 1.0f);
    }

    public static void drawEntityHead(GuiGraphicsExtractor graphics, LivingEntity entity, float x, float y, float size, float radius) {
        if (entity instanceof AbstractClientPlayer player) {
            drawPlayerHead(graphics, player, x, y, size, radius, true, 1.0f);
        }
    }
}
