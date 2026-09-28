package com.example.client.gui.components.settings;

import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.ColorAnimation;
import com.example.render.animation.Easing;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.BooleanSetting;
import com.example.render.shape.DrawUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class BooleanSettingComponent {
    public final BooleanSetting setting;
    public float x = 0.0f;
    public float y = 0.0f;
    public float width = 140.0f;
    public float height = 18.0f;

    private final Animation enableAnimation = new Animation(300L, Easing.BAKEK);
    private final Animation hoverAnimation = new Animation(200L, Easing.SMOOTH_IN_OUT);
    private final Animation circleOpacityAnimation = new Animation(300L, 0.75f, Easing.SMOOTH_IN_OUT);
    private final ColorAnimation backgroundColorAnimation = new ColorAnimation(300L, 0xFF18181B, Easing.SMOOTH_IN_OUT);

    public BooleanSettingComponent(BooleanSetting setting) {
        this.setting = setting;
        boolean enabled = setting.getValue();
        int activeColor = UITheme.INSTANCE.accent();
        this.enableAnimation.reset(enabled ? 1.0f : 0.0f);
        this.circleOpacityAnimation.reset(enabled ? 1.0f : 0.75f);
        this.backgroundColorAnimation.reset(enabled ? activeColor : 0xFF18181B);
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

    public void update(float mouseX, float mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean enabled = setting.getValue();
        int activeColor = UITheme.INSTANCE.accent();
        hoverAnimation.update(hovered);
        enableAnimation.update(enabled ? 1.0f : 0.0f);
        circleOpacityAnimation.update(enabled ? 1.0f : 0.75f);
        backgroundColorAnimation.update(enabled ? activeColor : 0xFF18181B);
    }

    public void render(GuiGraphicsExtractor graphics, float mouseX, float mouseY) {
        update(mouseX, mouseY);

        float checkWidth = 18.0f;
        float checkHeight = 10.0f;
        float leftPadding = 8.0f;
        float fontSize = 8.0f;

        float enableVal = enableAnimation.getValue();
        float hoverVal = hoverAnimation.getValue();
        int textColor = UITheme.INSTANCE.highlightColor(hoverVal, enableVal);

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfRegular,
            setting.getName(),
            this.x + leftPadding,
            this.y + (this.height - fontSize) * 0.5f - 0.5f,
            fontSize,
            textColor
        );

        float switchX = this.x + this.width - checkWidth - 8.0f;
        float switchY = this.y + (this.height - checkHeight) * 0.5f;
        float trackRadius = checkHeight * 0.5f;

        DrawUtility.drawSquircle(
            graphics,
            switchX,
            switchY,
            checkWidth,
            checkHeight,
            trackRadius,
            0.85f,
            backgroundColorAnimation.getColor()
        );

        float knobRadius = 4.0f;
        float knobTravel = checkWidth - checkHeight;
        float knobCenterX = switchX + trackRadius + knobTravel * enableVal;
        float knobCenterY = switchY + trackRadius;
        int knobAlpha = Math.max(0, Math.min(255, (int) (255.0f * circleOpacityAnimation.getValue())));
        DrawUtility.drawCircle(
            graphics,
            knobCenterX,
            knobCenterY,
            knobRadius,
            (knobAlpha << 24) | 0x00FFFFFF
        );
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            setting.setValue(!setting.getValue());
            return true;
        }
        return false;
    }
}
