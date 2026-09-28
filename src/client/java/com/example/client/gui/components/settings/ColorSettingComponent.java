package com.example.client.gui.components.settings;

import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.Easing;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.ColorSetting;
import com.example.render.shape.DrawUtility;
import com.example.render.util.Color4f;
import com.example.render.util.ScissorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

public class ColorSettingComponent {
    public final ColorSetting setting;
    public float x = 0.0f;
    public float y = 0.0f;
    public float width = 140.0f;
    public float height = 18.0f;

    private static final float ROW_HEIGHT = 18.0f;
    private static final float PICKER_BODY_HEIGHT = 76.0f;

    private boolean expanded = false;
    private final Animation expandAnimation = new Animation(280L, Easing.HUD_APPEAR);
    private final Animation hoverAnimation = new Animation(200L, Easing.SMOOTH_IN_OUT);

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;
    private float alpha = 1.0f;

    private boolean draggingColor = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    public ColorSettingComponent(ColorSetting setting) {
        this.setting = setting;
        syncFromSetting();
    }

    private void syncFromSetting() {
        Color4f c = setting.getValue();
        float[] hsb = c.toHsb();
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = c.a;
    }

    private void applyToSetting() {
        Color4f c = Color4f.fromHsb(hue, saturation, brightness, alpha);
        setting.setValue(c);
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getHeight() {
        float anim = expandAnimation.getValue();
        return ROW_HEIGHT + PICKER_BODY_HEIGHT * anim;
    }

    public void update(float mouseX, float mouseY) {
        boolean rowHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        hoverAnimation.update(rowHovered);
        expandAnimation.update(expanded ? 1.0f : 0.0f);

        if (draggingColor) updateColorDrag(mouseX, mouseY);
        if (draggingHue) updateHueDrag(mouseX);
        if (draggingAlpha) updateAlphaDrag(mouseX);
    }

    public void render(GuiGraphicsExtractor graphics, float mouseX, float mouseY) {
        update(mouseX, mouseY);

        float anim = expandAnimation.getValue();
        this.height = ROW_HEIGHT + PICKER_BODY_HEIGHT * anim;

        float hoverVal = hoverAnimation.getValue();
        int textColor = UITheme.INSTANCE.highlightColor(hoverVal, expanded ? 1.0f : 0.0f);
        float fontSize = 7.5f;

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfRegular,
            setting.getName(),
            this.x + 8.0f,
            this.y + (ROW_HEIGHT - fontSize) * 0.5f - 0.5f,
            fontSize,
            textColor
        );

        float swatchSize = 11.0f;
        float swatchRadius = 3.0f;
        float swatchX = this.x + this.width - swatchSize - 8.0f;
        float swatchY = this.y + (ROW_HEIGHT - swatchSize) * 0.5f;

        String hex = setting.getHex();
        float hexWidth = FontRenderer.INSTANCE.width(FontRenderer.Face.SfRegular, hex, 6.5f) + 8.0f;
        float hexX = swatchX - hexWidth - 5.0f;
        float hexY = this.y + (ROW_HEIGHT - 12.0f) * 0.5f;

        DrawUtility.drawSquircle(graphics, hexX, hexY, hexWidth, 12.0f, 3.0f, 0.8f, 0x3318181B);
        DrawUtility.drawRoundedOutline(graphics, hexX, hexY, hexWidth, 12.0f, 3.0f, 0.8f, 0x22FFFFFF);
        FontRenderer.INSTANCE.draw(graphics, FontRenderer.Face.SfRegular, hex, hexX + 4.0f, hexY + 2.5f, 6.5f, 0xCCA1A1AA);

        DrawUtility.drawSquircle(graphics, swatchX, swatchY, swatchSize, swatchSize, swatchRadius, 0.8f, setting.getArgbInt());
        DrawUtility.drawRoundedOutline(graphics, swatchX, swatchY, swatchSize, swatchSize, swatchRadius, 0.8f, 0x66FFFFFF);

        if (anim > 0.01f) {
            float bodyY = this.y + ROW_HEIGHT;
            float bodyH = PICKER_BODY_HEIGHT * anim;

            ScissorUtil.run(graphics, this.x + 4.0f, bodyY, this.width - 8.0f, bodyH, () -> {
                drawPickerBody(graphics, this.x + 6.0f, bodyY + 2.0f, this.width - 12.0f, anim);
            });
        }
    }

    private void drawPickerBody(GuiGraphicsExtractor graphics, float px, float py, float pw, float animAlpha) {
        int alphaMod = Math.max(0, Math.min(255, (int) (255.0f * animAlpha)));

        float satValH = 42.0f;
        Color4f pureHue = Color4f.fromHsb(hue, 1.0f, 1.0f);
        int topL = (alphaMod << 24) | 0x00FFFFFF;
        int topR = (alphaMod << 24) | (pureHue.toArgbInt() & 0x00FFFFFF);
        int bot = (alphaMod << 24) | 0x00000000;

        DrawUtility.drawSquircleExact(graphics, px, py, pw, satValH, 4.0f, 0.7f, topL, bot, bot, topR);
        DrawUtility.drawRoundedOutline(graphics, px, py, pw, satValH, 4.0f, 0.7f, (Math.min(255, (int) (50.0f * animAlpha)) << 24) | 0x00FFFFFF);

        float curX = px + Math.clamp(saturation, 0.0f, 1.0f) * pw;
        float curY = py + (1.0f - Math.clamp(brightness, 0.0f, 1.0f)) * satValH;
        DrawUtility.drawCircle(graphics, curX, curY, 3.5f, 0xFFFFFFFF);
        DrawUtility.drawCircle(graphics, curX, curY, 2.5f, setting.getArgbInt());

        float hueY = py + satValH + 6.0f;
        float hueH = 6.0f;
        drawHueSpectrum(graphics, px, hueY, pw, hueH, animAlpha);

        float hueCurX = px + Math.clamp(hue, 0.0f, 1.0f) * pw;
        DrawUtility.drawCircle(graphics, hueCurX, hueY + hueH * 0.5f, 3.8f, 0xFFFFFFFF);
        DrawUtility.drawCircle(graphics, hueCurX, hueY + hueH * 0.5f, 2.6f, (alphaMod << 24) | (pureHue.toArgbInt() & 0x00FFFFFF));

        float alphaY = hueY + hueH + 6.0f;
        float alphaH = 6.0f;
        int c0 = (0x00 << 24) | (pureHue.toArgbInt() & 0x00FFFFFF);
        int c1 = (alphaMod << 24) | (pureHue.toArgbInt() & 0x00FFFFFF);
        DrawUtility.drawSquircleExact(graphics, px, alphaY, pw, alphaH, 3.0f, 0.5f, c0, c0, c1, c1);
        DrawUtility.drawRoundedOutline(graphics, px, alphaY, pw, alphaH, 3.0f, 0.5f, (Math.min(255, (int) (40.0f * animAlpha)) << 24) | 0x00FFFFFF);

        float alphaCurX = px + Math.clamp(alpha, 0.0f, 1.0f) * pw;
        DrawUtility.drawCircle(graphics, alphaCurX, alphaY + alphaH * 0.5f, 3.8f, 0xFFFFFFFF);
        DrawUtility.drawCircle(graphics, alphaCurX, alphaY + alphaH * 0.5f, 2.6f, setting.getArgbInt());
    }

    private void drawHueSpectrum(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float alphaMod) {
        int alpha = Math.max(0, Math.min(255, (int) (255.0f * alphaMod)));
        DrawUtility.drawHueSpectrum(graphics, x, y, w, h, 3.0f, 0.5f, alpha);
        DrawUtility.drawRoundedOutline(graphics, x, y, w, h, 3.0f, 0.5f, (Math.min(255, (int) (40.0f * alphaMod)) << 24) | 0x00FFFFFF);
    }

    private void updateColorDrag(float mouseX, float mouseY) {
        float px = this.x + 6.0f;
        float py = this.y + ROW_HEIGHT + 2.0f;
        float pw = this.width - 12.0f;
        float satValH = 42.0f;

        this.saturation = Math.clamp((mouseX - px) / pw, 0.0f, 1.0f);
        this.brightness = Math.clamp(1.0f - (mouseY - py) / satValH, 0.0f, 1.0f);
        applyToSetting();
    }

    private void updateHueDrag(float mouseX) {
        float px = this.x + 6.0f;
        float pw = this.width - 12.0f;
        this.hue = Math.clamp((mouseX - px) / pw, 0.0f, 1.0f);
        applyToSetting();
    }

    private void updateAlphaDrag(float mouseX) {
        float px = this.x + 6.0f;
        float pw = this.width - 12.0f;
        this.alpha = Math.clamp((mouseX - px) / pw, 0.0f, 1.0f);
        applyToSetting();
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT) {
            this.expanded = !this.expanded;
            syncFromSetting();
            return true;
        }

        if (expanded && expandAnimation.getValue() > 0.5f) {
            float px = this.x + 6.0f;
            float py = this.y + ROW_HEIGHT + 2.0f;
            float pw = this.width - 12.0f;
            float satValH = 42.0f;
            float hueY = py + satValH + 6.0f;
            float hueH = 6.0f;
            float alphaY = hueY + hueH + 6.0f;
            float alphaH = 6.0f;

            if (mouseX >= px - 4.0f && mouseX <= px + pw + 4.0f && mouseY >= py && mouseY <= py + satValH) {
                draggingColor = true;
                updateColorDrag(mouseX, mouseY);
                return true;
            } else if (mouseX >= px - 4.0f && mouseX <= px + pw + 4.0f && mouseY >= hueY - 2.0f && mouseY <= hueY + hueH + 2.0f) {
                draggingHue = true;
                updateHueDrag(mouseX);
                return true;
            } else if (mouseX >= px - 4.0f && mouseX <= px + pw + 4.0f && mouseY >= alphaY - 2.0f && mouseY <= alphaY + alphaH + 2.0f) {
                draggingAlpha = true;
                updateAlphaDrag(mouseX);
                return true;
            }
        }
        return false;
    }

    public void mouseReleased(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingColor = false;
            draggingHue = false;
            draggingAlpha = false;
        }
    }
}
