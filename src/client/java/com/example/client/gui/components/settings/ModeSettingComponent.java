package com.example.client.gui.components.settings;

import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.Easing;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.EnumSetting;
import com.example.render.shape.DrawUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class ModeSettingComponent {
    public final EnumSetting<?> setting;
    public float x = 0.0f;
    public float y = 0.0f;
    public float width = 140.0f;
    public float height = 48.0f;

    private static class ModeEntry {
        final Animation hoverAnimation = new Animation(300L, Easing.SMOOTH_IN_OUT);
        final Animation activeAnimation = new Animation(350L, Easing.SMOOTH_IN_OUT);
    }

    private final List<ModeEntry> entries = new ArrayList<>();
    private final Animation headerHoverAnim = new Animation(250L, Easing.SMOOTH_IN_OUT);

    public ModeSettingComponent(EnumSetting<?> setting) {
        this.setting = setting;
        for (int i = 0; i < setting.getValues().length; i++) {
            entries.add(new ModeEntry());
        }
        recalcHeight();
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void recalcHeight() {
        this.height = 19.0f + setting.getValues().length * 13.0f + 6.0f;
    }

    public float getHeight() {
        recalcHeight();
        return height;
    }

    public void update(float mouseX, float mouseY) {
        recalcHeight();
        boolean headerHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 18.0f;
        headerHoverAnim.update(headerHovered);

        float boxY = y + 17.0f;
        for (int i = 0; i < setting.getValues().length; i++) {
            if (i >= entries.size()) entries.add(new ModeEntry());
            ModeEntry entry = entries.get(i);
            boolean isSelected = i == setting.getIndex();
            float itemY = boxY + 3.0f + i * 13.0f;
            boolean itemHovered = mouseX >= x + 2.0f && mouseX <= x + width - 2.0f && mouseY >= itemY && mouseY <= itemY + 13.0f;
            entry.hoverAnimation.update(itemHovered);
            entry.activeAnimation.update(isSelected);
        }
    }

    public void render(GuiGraphicsExtractor graphics, float mouseX, float mouseY) {
        update(mouseX, mouseY);

        float leftPadding = 8.0f;
        float fontSize = 7.5f;

        float hover = headerHoverAnim.getValue();
        int headerColor = UITheme.INSTANCE.highlightColor(hover);

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfRegular,
            setting.getName(),
            this.x + leftPadding,
            this.y + 4.0f,
            fontSize,
            headerColor
        );

        float boxX = this.x + 4.0f;
        float boxY = this.y + 17.0f;
        float boxW = this.width - 8.0f;
        float boxH = 6.0f + setting.getValues().length * 13.0f;

        DrawUtility.drawSquircle(graphics, boxX, boxY, boxW, boxH, 6.0f, 0.6f, 0x4C141417);
        DrawUtility.drawRoundedOutline(graphics, boxX, boxY, boxW, boxH, 6.0f, 0.6f, 0x1AFFFFFF);

        for (int i = 0; i < setting.getValues().length; i++) {
            ModeEntry entry = entries.get(i);
            float itemY = boxY + 3.0f + i * 13.0f;
            float activeVal = entry.activeAnimation.getValue();
            float hoverVal = entry.hoverAnimation.getValue();
            int itemColor = UITheme.INSTANCE.highlightColor(hoverVal, activeVal);

            String label = setting.optionDisplay(i);
            FontRenderer.INSTANCE.draw(
                graphics,
                FontRenderer.Face.SfRegular,
                label,
                boxX + 6.0f,
                itemY + 2.5f,
                7.0f,
                itemColor
            );

            if (activeVal > 0.01f) {
                int xAlpha = Math.max(0, Math.min(255, (int) (255.0f * activeVal)));
                FontRenderer.INSTANCE.drawCentered(
                    graphics,
                    FontRenderer.Face.SfMedium,
                    "x",
                    boxX + boxW - 8.0f,
                    itemY + 2.5f,
                    7.0f,
                    (xAlpha << 24) | (UITheme.INSTANCE.accent() & 0x00FFFFFF)
                );
            }
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button == 0) {
            float boxY = y + 17.0f;
            for (int i = 0; i < setting.getValues().length; i++) {
                float itemY = boxY + 3.0f + i * 13.0f;
                if (mouseX >= x + 2.0f && mouseX <= x + width - 2.0f && mouseY >= itemY && mouseY <= itemY + 13.0f) {
                    setting.selectIndex(i);
                    return true;
                }
            }
        }
        return false;
    }
}
