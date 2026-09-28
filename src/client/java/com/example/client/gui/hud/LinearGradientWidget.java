package com.example.client.gui.hud;

import com.example.render.post.BlurUtil;
import com.example.render.gradient.Gradient;
import com.example.render.gradient.GradientRotator;
import com.example.render.gradient.GradientType;
import com.example.render.gradient.GradientUtil;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.BooleanSetting;
import com.example.render.setting.ColorSetting;
import com.example.render.setting.FloatSetting;
import com.example.render.setting.ModuleSetting;
import com.example.render.shape.DrawUtility;
import com.example.render.util.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class LinearGradientWidget extends HudWidget {

    public final ColorSetting startColor = new ColorSetting("Start Color", 0xFFFA7460);
    public final ColorSetting endColor = new ColorSetting("End Color", 0xFF4073F2);
    public final FloatSetting angle = new FloatSetting("Angle", 45.0f, 0.0f, 360.0f, 5.0f, "°");
    public final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    public final FloatSetting rotationSpeed = new FloatSetting("Rotation Speed", 1.0f, -10.0f, 10.0f, 0.5f, "°/t");

    public LinearGradientWidget() {
        super("linear_gradient_box", "Linear Gradient Box", 30.0f, 40.0f, 130.0f, 130.0f, true, true);
        this.cornerRadius.setValue(18.0f);
        this.cornerSmoothing.setValue(0.85f);
        this.backgroundOpacity.setValue(0.65f);
        this.showOutline.setValue(false);
    }

    @Override
    public List<ModuleSetting<?>> getSettings() {
        List<ModuleSetting<?>> list = super.getSettings();
        list.add(startColor);
        list.add(endColor);
        list.add(angle);
        list.add(rotate);
        if (rotate.getValue()) {
            list.add(rotationSpeed);
        }
        return list;
    }

    private long lastUpdateTime = 0L;

    @Override
    public void update(Minecraft mc, boolean editorOpen) {
        if (!rotate.getValue()) {
            lastUpdateTime = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (lastUpdateTime == 0L) {
            lastUpdateTime = now;
            return;
        }

        long elapsedMs = now - lastUpdateTime;
        if (elapsedMs <= 0L) return;

        float speed = rotationSpeed.getValue();
        if (Math.abs(speed) > 0.0001f) {
            float newAngle = GradientRotator.tickAngle(angle.getValue(), speed, elapsedMs);
            angle.setValue(newAngle);
        }
        lastUpdateTime = now;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Minecraft mc, boolean editorOpen) {
        update(mc, editorOpen);

        float rTL = getRadiusTL();
        float rTR = getRadiusTR();
        float rBR = getRadiusBR();
        float rBL = getRadiusBL();
        float s = cornerSmoothing.getValue();
        float opacity = backgroundOpacity.getValue() * getAnimationAlpha();
        float x = bounds.x;
        float y = bounds.y;
        float w = bounds.width;
        float h = bounds.height;
        float glassAlpha = Math.max(0.0f, Math.min(1.0f, opacity));
        Color4f c0 = startColor.getValue().multiplyAlpha(glassAlpha);
        Color4f c1 = endColor.getValue().multiplyAlpha(glassAlpha);

        float currentAngle = angle.getValue();

        Gradient gradient = Gradient.builder(GradientType.LINEAR)
            .angle(currentAngle)
            .addStop(0.00f, c0)
            .addStop(1.00f, c1)
            .build();

        Color4f cTL = gradient.evaluate(0.0f, 0.0f, w, h);
        Color4f cBL = gradient.evaluate(0.0f, 1.0f, w, h);
        Color4f cBR = gradient.evaluate(1.0f, 1.0f, w, h);
        Color4f cTR = gradient.evaluate(1.0f, 0.0f, w, h);

        if (showShadow.getValue() && shadowOpacity.getValue() > 0.001f && shadowBlur.getValue() > 0.5f) {
            int sAlpha = Math.max(0, Math.min(255, (int) (255.0f * shadowOpacity.getValue() * getAnimationAlpha())));
            DrawUtility.drawShadow(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, (sAlpha << 24), shadowBlur.getValue(), shadowOffsetY.getValue());
        }

        DrawUtility.drawSquircleExact(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, cTL.toArgbInt(), cBL.toArgbInt(), cBR.toArgbInt(), cTR.toArgbInt());

        if (showTint.getValue() && tintStrength.getValue() > 0.005f) {
            int tAlpha = Math.max(0, Math.min(255, (int) (255.0f * tintStrength.getValue() * opacity * tintColor.getValue().a())));
            int tCol = (tAlpha << 24) | (tintColor.getValue().toArgbInt() & 0x00FFFFFF);
            DrawUtility.drawSquircleExact(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, tCol, tCol, tCol, tCol);
        }

        if (showOutline.getValue()) {
            int outlineAlpha = Math.max(0, Math.min(255, (int) (75.0f * opacity)));
            DrawUtility.drawRoundedOutline(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, (outlineAlpha << 24) | 0x00FFFFFF);
        }

        FontRenderer.INSTANCE.drawCentered(graphics, FontRenderer.Face.SfSemibold, "Flowing Gradient", x + w * 0.5f, y + h - 26.0f, 8.5f, 0xFFFFFFFF);
        String angleText = rotate.getValue()
            ? String.format("Angle: %.0f° (%.1f°/t)", currentAngle, rotationSpeed.getValue())
            : String.format("Angle: %.0f°", currentAngle);
        FontRenderer.INSTANCE.drawCentered(graphics, FontRenderer.Face.SfRegular, angleText, x + w * 0.5f, y + h - 13.0f, 7.0f, 0xCCFFFFFF);
    }
}
