package com.example.client.gui.hud;

import com.example.render.post.BlurUtil;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.BooleanSetting;
import com.example.render.setting.FloatSetting;
import com.example.render.setting.ModuleSetting;
import com.example.render.shape.DrawUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class SunsetPeachWidget extends HudWidget {

    public final FloatSetting peachIntensity = new FloatSetting("Peach Intensity", 0.85f, 0.20f, 1.0f, 0.05f);
    public final BooleanSetting showHead = new BooleanSetting("Head Texture", false);
    public final BooleanSetting headHatLayer = new BooleanSetting("Hat Layer", true);
    public final FloatSetting headRadius = new FloatSetting("Head Radius", 8.0f, 0.0f, 24.0f, 1.0f, "px");
    public final FloatSetting headSize = new FloatSetting("Head Size", 36.0f, 16.0f, 64.0f, 1.0f, "px");

    public SunsetPeachWidget() {
        super("sunset_peach_card", "Sunset Peach Card", 30.0f, 190.0f, 210.0f, 108.0f, true, true);
        this.cornerRadius.setValue(20.0f);
        this.cornerSmoothing.setValue(0.85f);
        this.backgroundOpacity.setValue(0.60f);
        this.showOutline.setValue(false);
    }

    @Override
    public List<ModuleSetting<?>> getSettings() {
        List<ModuleSetting<?>> list = super.getSettings();
        list.add(peachIntensity);
        list.add(showHead);
        list.add(headHatLayer);
        list.add(headRadius);
        list.add(headSize);
        return list;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Minecraft mc, boolean editorOpen) {
        float rTL = getRadiusTL();
        float rTR = getRadiusTR();
        float rBR = getRadiusBR();
        float rBL = getRadiusBL();
        float s = cornerSmoothing.getValue();
        float opacity = backgroundOpacity.getValue() * getAnimationAlpha();
        float intensity = peachIntensity.getValue();

        float x = bounds.x;
        float y = bounds.y;
        float w = bounds.width;
        float h = bounds.height;

        float glassAlpha = Math.max(0.0f, Math.min(1.0f, opacity));
        int cTL = ((int) (glassAlpha * 255.0f) << 24) | ((int) (0xFA * intensity) << 16) | ((int) (0x70 * intensity) << 8) | 0x60;
        int cBL = ((int) (glassAlpha * 255.0f) << 24) | ((int) (0xEE * intensity) << 16) | ((int) (0x52 * intensity) << 8) | 0x44;
        int cBR = ((int) (glassAlpha * 255.0f) << 24) | ((int) (0xFC * intensity) << 16) | ((int) (0x8A * intensity) << 8) | 0x6D;
        int cTR = ((int) (glassAlpha * 255.0f) << 24) | ((int) (0xFF * intensity) << 16) | ((int) (0xA0 * intensity) << 8) | 0x7A;

        if (showShadow.getValue() && shadowOpacity.getValue() > 0.001f && shadowBlur.getValue() > 0.5f) {
            int sAlpha = Math.max(0, Math.min(255, (int) (255.0f * shadowOpacity.getValue() * getAnimationAlpha())));
            DrawUtility.drawShadow(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, (sAlpha << 24), shadowBlur.getValue(), shadowOffsetY.getValue());
        }

        DrawUtility.drawSquircleExact(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, cTL, cBL, cBR, cTR);

        int sheenTop = ((int) (0.22f * 255.0f * opacity) << 24) | 0xFFFFFF;
        int sheenBot = 0x00FFFFFF;
        DrawUtility.drawSquircleExact(graphics, x, y, w, h * 0.45f, rTL, rTR, 0.0f, 0.0f, s, sheenTop, sheenBot, sheenBot, sheenTop);

        if (showTint.getValue() && tintStrength.getValue() > 0.005f) {
            int tAlpha = Math.max(0, Math.min(255, (int) (255.0f * tintStrength.getValue() * opacity * tintColor.getValue().a())));
            int tCol = (tAlpha << 24) | (tintColor.getValue().toArgbInt() & 0x00FFFFFF);
            DrawUtility.drawSquircleExact(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, tCol, tCol, tCol, tCol);
        }

        if (showOutline.getValue()) {
            int outlineAlpha = Math.max(0, Math.min(255, (int) (70.0f * opacity)));
            DrawUtility.drawRoundedOutline(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, (outlineAlpha << 24) | 0x00FFFFFF);
        }

        float fontSize = 11.0f;
        float textY = y + h * 0.5f - 4.5f;
        float startTextX = x + 16.0f;

        if (showHead.getValue() && mc.player != null) {
            float curHeadSize = Math.min(h - 16.0f, headSize.getValue());
            float headX = x + 14.0f;
            float headY = y + (h - curHeadSize) * 0.5f;
            float hRadius = Math.min(curHeadSize * 0.5f, headRadius.getValue());
            DrawUtility.drawPlayerHead(graphics, mc.player, headX, headY, curHeadSize, hRadius, headHatLayer.getValue(), opacity);
            DrawUtility.drawRoundedOutline(graphics, headX, headY, curHeadSize, curHeadSize, hRadius, 0.85f, ((int) (50.0f * opacity) << 24) | 0x00FFFFFF);
            startTextX = headX + curHeadSize + 12.0f;
        }

        int textWhite = ((int) (240.0f * getAnimationAlpha()) << 24) | 0x00FFFFFF;
        int textShadowCol = ((int) (80.0f * getAnimationAlpha()) << 24);

        FontRenderer.INSTANCE.drawWithShadow(graphics, FontRenderer.Face.SfRegular, "sf regular", startTextX, textY, fontSize, textWhite, textShadowCol);
        float w1 = FontRenderer.INSTANCE.width(FontRenderer.Face.SfRegular, "sf regular", fontSize);

        float boldX = startTextX + w1 + 12.0f;
        FontRenderer.INSTANCE.drawWithShadow(graphics, FontRenderer.Face.SfSemibold, "sf bold", boldX, textY, fontSize, (Math.min(255, (int) (255.0f * getAnimationAlpha())) << 24) | 0x00FFFFFF, textShadowCol);
        float w2 = FontRenderer.INSTANCE.width(FontRenderer.Face.SfSemibold, "sf bold", fontSize);

        float mediumX = boldX + w2 + 12.0f;
        FontRenderer.INSTANCE.drawWithShadow(graphics, FontRenderer.Face.SfMedium, "sf medium", mediumX, textY, fontSize, textWhite, textShadowCol);
    }
}
