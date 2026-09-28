package com.example.client.gui.hud;

import com.example.render.post.BlurUtil;
import com.example.render.blur.ShadowBlurUtil;
import com.example.render.setting.EnumSetting;
import com.example.render.setting.FloatSetting;
import com.example.render.setting.ModuleSetting;
import com.example.render.shape.DrawUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class LiquidGlassWidget extends HudWidget {

    public enum GlassShape {
        ROUNDED_RECT("Rounded Rect", 0),
        SQUIRCLE("Squircle", 1),
        PILL("Pill", 2),
        CIRCLE("Circle", 3),
        HEXAGON("Hexagon", 4);

        private final String label;
        private final int id;

        GlassShape(String label, int id) {
            this.label = label;
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public int getId() {
            return id;
        }
    }

    public final EnumSetting<GlassShape> shape = new EnumSetting<>("Shape", GlassShape.values(), GlassShape.SQUIRCLE, GlassShape::getLabel);
    public final FloatSetting refractionStrength = new FloatSetting("Refraction", 1.0f, 0.0f, 2.5f, 0.05f);
    public final FloatSetting chromaticAberration = new FloatSetting("Dispersion", 0.65f, 0.0f, 1.5f, 0.05f);
    public final FloatSetting specularHighlight = new FloatSetting("Highlight", 0.60f, 0.0f, 1.0f, 0.05f);
    public final FloatSetting sampleEscape = new FloatSetting("Edge Escape", 24.0f, 0.0f, 120.0f, 1.0f, "px");

    public LiquidGlassWidget() {
        super("liquid_glass_box", "Liquid Glass Box", 30.0f, 70.0f, 200.0f, 120.0f, true, true);
        this.cornerRadius.setValue(20.0f);
        this.cornerSmoothing.setValue(0.85f);
        this.backgroundOpacity.setValue(1.0f);
        this.showOutline.setValue(true);
        this.showShadow.setValue(true);
        this.shadowBlur.setValue(16.0f);
        this.shadowOpacity.setValue(0.35f);
        this.shadowOffsetY.setValue(2.0f);
        this.blurType.setValue(BlurMode.ACRYLIC);
        this.blurStrength.setValue(1.0f);
    }

    @Override
    public boolean supportsCornerRadius() {
        GlassShape s = shape.getValue();
        return s == GlassShape.ROUNDED_RECT || s == GlassShape.SQUIRCLE;
    }

    @Override
    public boolean supportsSmoothing() {
        return shape.getValue() == GlassShape.SQUIRCLE;
    }

    @Override
    public List<ModuleSetting<?>> getSettings() {
        List<ModuleSetting<?>> list = new ArrayList<>();
        list.add(shape);

        GlassShape s = shape.getValue();
        if (s == GlassShape.ROUNDED_RECT || s == GlassShape.SQUIRCLE) {
            if (!separateCorners.getValue()) {
                list.add(cornerRadius);
            }
            list.add(separateCorners);
            if (separateCorners.getValue()) {
                list.add(cornerRadiusTL);
                list.add(cornerRadiusTR);
                list.add(cornerRadiusBR);
                list.add(cornerRadiusBL);
            }
            if (s == GlassShape.SQUIRCLE) {
                list.add(cornerSmoothing);
            }
        }

        list.add(backgroundOpacity);
        list.add(blurType);
        if (blurType.getValue() != BlurMode.NONE) {
            list.add(blurStrength);
        }
        list.add(showOutline);
        list.add(showShadow);
        if (showShadow.getValue()) {
            list.add(shadowBlur);
            list.add(shadowOpacity);
            list.add(shadowOffsetY);
        }
        list.add(showTint);
        if (showTint.getValue()) {
            list.add(tintStrength);
            list.add(tintColor);
        }
        list.add(scale);

        // Liquid glass optical parameters
        list.add(refractionStrength);
        list.add(chromaticAberration);
        list.add(specularHighlight);
        list.add(sampleEscape);

        return list;
    }

    @Override
    public List<BlurUtil.Shape> blurBoxes(float guiScale, float tintStrength) {
        float rTL = getRadiusTL();
        float rTR = getRadiusTR();
        float rBR = getRadiusBR();
        float rBL = getRadiusBL();

        float effTint = showTint.getValue() ? this.tintStrength.getValue() : 0.0f;
        GlassShape curShape = shape.getValue();

        float boxX = bounds.x;
        float boxY = bounds.y;
        float boxW = bounds.width;
        float boxH = bounds.height;
        float boxRTL = rTL;
        float boxRTR = rTR;
        float boxRBR = rBR;
        float boxRBL = rBL;

        if (curShape == GlassShape.CIRCLE || curShape == GlassShape.HEXAGON) {
            float r = Math.min(boxW, boxH) * 0.5f;
            float cx = boxX + boxW * 0.5f;
            float cy = boxY + boxH * 0.5f;
            boxX = cx - r;
            boxY = cy - r;
            boxW = r * 2.0f;
            boxH = r * 2.0f;
            boxRTL = r;
            boxRTR = r;
            boxRBR = r;
            boxRBL = r;
        } else if (curShape == GlassShape.PILL) {
            float r = Math.min(boxW, boxH) * 0.5f;
            boxRTL = r;
            boxRTR = r;
            boxRBR = r;
            boxRBL = r;
        }

        ShadowBlurUtil.BlurType bType = null;
        if (blurType.getValue() != BlurMode.NONE && blurStrength.getValue() > 0.001f) {
            bType = (blurType.getValue() == BlurMode.ACRYLIC)
                ? ShadowBlurUtil.BlurType.ACRYLIC
                : ShadowBlurUtil.BlurType.GAUSSIAN;
        }

        return List.of(new BlurUtil.Shape(
            boxX * guiScale,
            boxY * guiScale,
            boxW * guiScale,
            boxH * guiScale,
            boxRTL * guiScale,
            boxRTR * guiScale,
            boxRBR * guiScale,
            boxRBL * guiScale,
            cornerSmoothing.getValue(),
            curShape == GlassShape.SQUIRCLE,
            effTint,
            showShadow.getValue(),
            bType,
            true, // liquidGlass = true
            refractionStrength.getValue(),
            chromaticAberration.getValue(),
            specularHighlight.getValue(),
            sampleEscape.getValue() * guiScale,
            curShape.getId(),
            blurStrength.getValue()
        ));
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Minecraft mc, boolean editorOpen) {
        float x = bounds.x;
        float y = bounds.y;
        float w = bounds.width;
        float h = bounds.height;
        float animAlpha = getAnimationAlpha();
        if (w <= 0.0f || h <= 0.0f || animAlpha <= 0.001f) return;

        float rTL = getRadiusTL();
        float rTR = getRadiusTR();
        float rBR = getRadiusBR();
        float rBL = getRadiusBL();
        float s = cornerSmoothing.getValue();
        float opacity = backgroundOpacity.getValue() * animAlpha;
        GlassShape curShape = shape.getValue();

        float drawX = x;
        float drawY = y;
        float drawW = w;
        float drawH = h;
        float drawRTL = rTL;
        float drawRTR = rTR;
        float drawRBR = rBR;
        float drawRBL = rBL;

        float r = Math.min(w, h) * 0.5f;
        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;

        if (curShape == GlassShape.CIRCLE || curShape == GlassShape.HEXAGON) {
            drawX = cx - r;
            drawY = cy - r;
            drawW = r * 2.0f;
            drawH = r * 2.0f;
            drawRTL = r;
            drawRTR = r;
            drawRBR = r;
            drawRBL = r;
        } else if (curShape == GlassShape.PILL) {
            drawRTL = r;
            drawRTR = r;
            drawRBR = r;
            drawRBL = r;
        }

        // 1. Soft Realistic Diffuse Shadow
        if (showShadow.getValue() && shadowOpacity.getValue() > 0.001f && shadowBlur.getValue() > 0.5f) {
            int sAlpha = Math.max(0, Math.min(255, (int) (255.0f * shadowOpacity.getValue() * animAlpha)));
            int shadowColor = (sAlpha << 24);
            float sBlur = shadowBlur.getValue();
            float sOffY = shadowOffsetY.getValue();

            switch (curShape) {
                case ROUNDED_RECT -> DrawUtility.drawShadow(graphics, drawX, drawY, drawW, drawH, drawRTL, drawRTR, drawRBR, drawRBL, 0.0f, shadowColor, sBlur, sOffY);
                case SQUIRCLE -> DrawUtility.drawShadow(graphics, drawX, drawY, drawW, drawH, drawRTL, drawRTR, drawRBR, drawRBL, s, shadowColor, sBlur, sOffY);
                case CIRCLE -> DrawUtility.drawCircleShadow(graphics, cx, cy, r, shadowColor, sBlur, sOffY);
                case PILL -> DrawUtility.drawPillShadow(graphics, drawX, drawY, drawW, drawH, shadowColor, sBlur, sOffY);
                case HEXAGON -> DrawUtility.drawHexagonShadow(graphics, cx, cy, r, shadowColor, sBlur, sOffY);
            }
        }

        // 2. Optical Tint Overlay (if enabled)
        if (showTint.getValue() && tintStrength.getValue() > 0.005f) {
            int tAlpha = Math.max(0, Math.min(255, (int) (255.0f * tintStrength.getValue() * opacity * tintColor.getValue().a())));
            int tCol = (tAlpha << 24) | (tintColor.getValue().toArgbInt() & 0x00FFFFFF);

            switch (curShape) {
                case ROUNDED_RECT -> DrawUtility.drawRoundedRect(graphics, drawX, drawY, drawW, drawH, drawRTL, drawRTR, drawRBR, drawRBL, tCol);
                case SQUIRCLE -> DrawUtility.drawSquircleExact(graphics, drawX, drawY, drawW, drawH, drawRTL, drawRTR, drawRBR, drawRBL, s, tCol, tCol, tCol, tCol);
                case CIRCLE -> DrawUtility.drawCircle(graphics, cx, cy, r, tCol);
                case PILL -> DrawUtility.drawPill(graphics, drawX, drawY, drawW, drawH, tCol);
                case HEXAGON -> DrawUtility.drawHexagon(graphics, cx, cy, r, tCol);
            }
        }

        // 3. Specular Rim / Bevel Outline
        if (showOutline.getValue()) {
            int outlineAlpha = Math.max(0, Math.min(255, (int) (80.0f * opacity)));
            int outCol = (outlineAlpha << 24) | 0x00FFFFFF;

            switch (curShape) {
                case ROUNDED_RECT -> DrawUtility.drawRoundedOutline(graphics, drawX, drawY, drawW, drawH, drawRTL, drawRTR, drawRBR, drawRBL, 0.0f, outCol);
                case SQUIRCLE -> DrawUtility.drawRoundedOutline(graphics, drawX, drawY, drawW, drawH, drawRTL, drawRTR, drawRBR, drawRBL, s, outCol);
                case CIRCLE, PILL -> DrawUtility.drawRoundedOutline(graphics, drawX, drawY, drawW, drawH, drawRTL, drawRTR, drawRBR, drawRBL, 0.0f, outCol);
                case HEXAGON -> DrawUtility.drawHexagonOutline(graphics, cx, cy, r, outCol);
            }
        }
    }
}
