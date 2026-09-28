package com.example.client.gui.components.settings;

import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.Easing;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.FloatSetting;
import com.example.render.shape.DrawUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SliderSettingComponent {
    public final FloatSetting setting;
    public float x = 0.0f;
    public float y = 0.0f;
    public float width = 140.0f;
    public float height = 29.0f;

    private final Animation movingAnimation = new Animation(200L, Easing.SMOOTH_IN_OUT);
    private final Animation valueAnimation = new Animation(300L, Easing.BAKEK);
    private final Animation hoverAnimation = new Animation(250L, Easing.SMOOTH_IN_OUT);
    private boolean dragging = false;

    public SliderSettingComponent(FloatSetting setting) {
        this.setting = setting;
        float range = setting.getMax() - setting.getMin();
        float norm = range <= 0.0001f ? 0.0f : (setting.getValue() - setting.getMin()) / range;
        this.valueAnimation.reset(norm);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getHeight() {
        return height;
    }

    private float getNormalizedValue() {
        float range = setting.getMax() - setting.getMin();
        return range <= 0.0001f ? 0.0f : Math.max(0.0f, Math.min(1.0f, (setting.getValue() - setting.getMin()) / range));
    }

    private void applyNormalizedValue(float pct) {
        float p = Math.max(0.0f, Math.min(1.0f, pct));
        float range = setting.getMax() - setting.getMin();
        float raw = setting.getMin() + range * p;
        float step = setting.getStep();
        if (step > 0.0f) {
            raw = Math.round((raw - setting.getMin()) / step) * step + setting.getMin();
        }
        setting.setValue(Math.max(setting.getMin(), Math.min(setting.getMax(), raw)));
    }

    public void update(float mouseX, float mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        hoverAnimation.update(hovered);
        movingAnimation.update(dragging);

        if (dragging) {
            float trackX = this.x + 8.0f;
            float trackW = this.width - 16.0f;
            float pct = trackW > 0.0f ? (mouseX - trackX) / trackW : 0.0f;
            applyNormalizedValue(pct);
            valueAnimation.setValue(getNormalizedValue());
        } else {
            valueAnimation.update(getNormalizedValue());
        }
    }

    public void render(GuiGraphicsExtractor graphics, float mouseX, float mouseY) {
        update(mouseX, mouseY);

        float leftPadding = 8.0f;
        float fontSize = 7.5f;

        float hover = hoverAnimation.getValue();
        int headerColor = UITheme.INSTANCE.highlightColor(hover);
        int accentColor = UITheme.INSTANCE.accent();
        int accentSecondary = UITheme.INSTANCE.secondaryAccent();

        String valStr = setting.displayValue();
        float valFontSize = 7.0f;
        float valWidth = FontRenderer.INSTANCE.width(FontRenderer.Face.SfMedium, valStr, valFontSize);

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfMedium,
            valStr,
            this.x + this.width - leftPadding - valWidth,
            this.y + 4.0f,
            valFontSize,
            accentColor
        );

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfRegular,
            setting.getName(),
            this.x + leftPadding,
            this.y + 4.0f,
            fontSize,
            headerColor
        );

        float trackX = this.x + leftPadding;
        float trackW = this.width - leftPadding * 2.0f;
        float trackH = 4.0f;
        float trackY = this.y + 17.0f;
        float trackRadius = trackH * 0.5f;

        float animPct = Math.max(0.0f, Math.min(1.0f, valueAnimation.getValue()));

        float dragHold = movingAnimation.getValue();
        float hoverVal = hoverAnimation.getValue();
        float growFactor = dragHold + (1.0f - dragHold) * (hoverVal * 0.10f);

        float aspect = 0.55f + (0.95f - 0.55f) * dragHold;
        float thumbSize = 8.0f + 1.5f * growFactor;
        float restingHalfWidth = thumbSize * 0.55f * 0.5f;

        float thumbCenterX = Math.max(trackX + restingHalfWidth, Math.min(trackX + trackW - restingHalfWidth, trackX + trackW * animPct));
        float thumbCenterY = trackY + trackH * 0.5f;

        float thumbW = thumbSize * aspect;
        float thumbH = thumbSize;
        float thumbX = thumbCenterX - thumbW * 0.5f;
        float thumbY = thumbCenterY - thumbH * 0.5f;

        float fillW = Math.max(trackH * 0.5f, Math.min(trackW, thumbCenterX - trackX));

        DrawUtility.drawSquircle(graphics, trackX, trackY, trackW, trackH, trackRadius, 0.85f, 0x4C141417);

        if (fillW > 0.5f) {
            DrawUtility.drawSquircleExact(graphics, trackX, trackY, fillW, trackH, trackRadius, 0.85f, accentColor, accentSecondary);
        }

        float pillRadius = Math.min(thumbW, thumbH) * 0.5f;
        DrawUtility.drawSquircle(graphics, thumbX, thumbY, thumbW, thumbH, pillRadius, 0.85f, 0xFFFFFFFF);
        DrawUtility.drawRoundedOutline(graphics, thumbX, thumbY, thumbW, thumbH, pillRadius, 0.85f, 0x22000000);
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            this.dragging = true;
            float trackX = this.x + 8.0f;
            float trackW = this.width - 16.0f;
            float pct = trackW > 0.0f ? (mouseX - trackX) / trackW : 0.0f;
            applyNormalizedValue(pct);
            valueAnimation.setValue(getNormalizedValue());
            return true;
        }
        return false;
    }

    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == 0) {
            this.dragging = false;
        }
    }
}
