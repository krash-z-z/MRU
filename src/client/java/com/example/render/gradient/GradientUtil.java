package com.example.render.gradient;

import com.example.render.blur.ShadowBlurUtil;
import com.example.render.shape.DrawUtility;
import com.example.render.util.Color4f;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * GradientUtil: High-performance GPU rendering for Linear gradients
 * mapped over squircles, rounded rects, pills, and circles.
 */
public class GradientUtil {

    public static void drawGradientRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Gradient gradient) {
        drawGradientRoundedRect(graphics, x, y, width, height, 0.0f, 0.0f, gradient);
    }

    public static void drawGradientRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                               float radius, float cornerSmoothing, Gradient gradient) {
        if (width <= 0 || height <= 0 || gradient == null) return;

        Color4f cTL = gradient.evaluate(0.0f, 0.0f, width, height);
        Color4f cBL = gradient.evaluate(0.0f, 1.0f, width, height);
        Color4f cBR = gradient.evaluate(1.0f, 1.0f, width, height);
        Color4f cTR = gradient.evaluate(1.0f, 0.0f, width, height);

        DrawUtility.drawSquircleExact(graphics, x, y, width, height, radius, cornerSmoothing,
                cTL.toArgbInt(), cBL.toArgbInt(), cBR.toArgbInt(), cTR.toArgbInt());
    }

    public static void drawGradientPill(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Gradient gradient) {
        float radius = Math.min(width, height) * 0.5f;
        drawGradientRoundedRect(graphics, x, y, width, height, radius, 0.5f, gradient);
    }

    public static void drawGradientCircle(GuiGraphicsExtractor graphics, float cx, float cy, float radius, Gradient gradient) {
        if (radius <= 0 || gradient == null) return;
        drawGradientRoundedRect(graphics, cx - radius, cy - radius, radius * 2f, radius * 2f, radius, 0.0f, gradient);
    }

    public static void drawGradientBoxWithShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                                 float radius, float cornerSmoothing,
                                                 float shadowBlur, Color4f shadowColor, Gradient gradient) {
        if (shadowColor != null && shadowColor.a > 0.01f) {
            ShadowBlurUtil.drawShadow(graphics, x, y, width, height, radius, cornerSmoothing, shadowBlur, 2f, shadowColor);
        }
        drawGradientRoundedRect(graphics, x, y, width, height, radius, cornerSmoothing, gradient);
    }

    public static float getRotatingAngle(float baseAngle, float speedDegPerTick) {
        return GradientRotator.calculateAngle(baseAngle, speedDegPerTick);
    }

    public static float getRotatingAngle(float speedDegPerTick) {
        return GradientRotator.calculateAngle(0.0f, speedDegPerTick);
    }

    public static void drawRotatingGradientRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Gradient gradient, float speedDegPerTick) {
        drawGradientRoundedRect(graphics, x, y, width, height, 0.0f, 0.0f, gradient != null ? gradient.withRotation(speedDegPerTick) : null);
    }

    public static void drawRotatingGradientRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                                       float radius, float cornerSmoothing, Gradient gradient, float speedDegPerTick) {
        drawGradientRoundedRect(graphics, x, y, width, height, radius, cornerSmoothing, gradient != null ? gradient.withRotation(speedDegPerTick) : null);
    }

    public static void drawRotatingGradientPill(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Gradient gradient, float speedDegPerTick) {
        drawGradientPill(graphics, x, y, width, height, gradient != null ? gradient.withRotation(speedDegPerTick) : null);
    }

    public static void drawRotatingGradientCircle(GuiGraphicsExtractor graphics, float cx, float cy, float radius, Gradient gradient, float speedDegPerTick) {
        drawGradientCircle(graphics, cx, cy, radius, gradient != null ? gradient.withRotation(speedDegPerTick) : null);
    }

    public static void drawRotatingGradientBoxWithShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                                         float radius, float cornerSmoothing,
                                                         float shadowBlur, Color4f shadowColor, Gradient gradient, float speedDegPerTick) {
        drawGradientBoxWithShadow(graphics, x, y, width, height, radius, cornerSmoothing, shadowBlur, shadowColor,
                gradient != null ? gradient.withRotation(speedDegPerTick) : null);
    }
}
