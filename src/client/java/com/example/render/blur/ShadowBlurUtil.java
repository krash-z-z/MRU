package com.example.render.blur;

import com.example.render.shape.DrawUtility;
import com.example.render.util.Color4f;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ShadowBlurUtil {

    public enum BlurType {
        NONE,
        GAUSSIAN,
        ACRYLIC
    }

    public static void drawShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                  float radius, float cornerSmoothing, float blurRadius, float spread, Color4f shadowColor) {
        if (shadowColor == null || shadowColor.a <= 0.001f || blurRadius <= 0.1f) return;
        DrawUtility.drawShadow(graphics, x - spread, y - spread, width + spread * 2f, height + spread * 2f,
                radius + spread, cornerSmoothing, shadowColor.toArgbInt(), blurRadius, 3.0f);
    }

    public static void drawBoxWithShadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                         float radius, float cornerSmoothing,
                                         float blurRadius, float spread, Color4f shadowColor, Color4f boxColor) {
        if (shadowColor != null && shadowColor.a > 0.01f) {
            drawShadow(graphics, x, y, width, height, radius, cornerSmoothing, blurRadius, spread, shadowColor);
        }
        if (boxColor != null && boxColor.a > 0.01f) {
            DrawUtility.drawSquircle(graphics, x, y, width, height, radius, cornerSmoothing, boxColor.toArgbInt());
        }
    }

    public static void drawBlurredBox(GuiGraphicsExtractor graphics, BlurType type,
                                      float x, float y, float width, float height,
                                      float radius, float cornerSmoothing,
                                      float blurRadius, Color4f shadowColor, Color4f tintColor) {
        if (shadowColor != null && shadowColor.a > 0.01f) {
            drawShadow(graphics, x, y, width, height, radius, cornerSmoothing, blurRadius, 2.0f, shadowColor);
        }

        if (type != BlurType.NONE) {
            BlurUtil.INSTANCE.apply(x, y, width, height, radius, cornerSmoothing, true, -1.0f, type);
        }

        if (tintColor != null && tintColor.a > 0.01f) {
            DrawUtility.drawSquircle(graphics, x, y, width, height, radius, cornerSmoothing, tintColor.toArgbInt());
        }
    }
}
