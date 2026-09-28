package com.example.client.gui.hud;

import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.Easing;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.BooleanSetting;
import com.example.render.setting.FloatSetting;
import com.example.render.setting.ModuleSetting;
import com.example.render.shape.DrawUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.Locale;

public class TargetInfoWidget extends HudWidget {

    public final BooleanSetting showHead = new BooleanSetting("Head Texture", true);
    public final BooleanSetting headHatLayer = new BooleanSetting("Hat Layer", true);
    public final FloatSetting headRadius = new FloatSetting("Head Radius", 5.0f, 0.0f, 16.0f, 1.0f, "px");
    public final FloatSetting headSize = new FloatSetting("Head Size", 24.0f, 16.0f, 36.0f, 1.0f, "px");
    public final BooleanSetting showHealthBar = new BooleanSetting("Health Bar", true);
    public final BooleanSetting showHealthText = new BooleanSetting("Health Value", true);
    public final BooleanSetting showAbsorption = new BooleanSetting("Absorption Bar", true);

    private final Animation healthAnim = new Animation(400L, Easing.SATISFYING_SPRING);
    private final Animation goldenAnim = new Animation(400L, Easing.SATISFYING_SPRING);
    private final Animation numberAnim = new Animation(400L, Easing.SATISFYING_SPRING);

    private LivingEntity target = null;
    private String lastTargetName = "Target";
    private float lastTargetHealth = 20.0f;
    private float lastTargetMaxHealth = 20.0f;
    private float lastTargetAbsorption = 0.0f;

    public TargetInfoWidget() {
        super("target_info", "Target Info", 160.0f, 160.0f, 120.0f, 36.0f, true, true);
        this.cornerRadius.setValue(11.0f);
        this.cornerSmoothing.setValue(0.85f);
        this.backgroundOpacity.setValue(0.60f);
        this.showOutline.setValue(false);
        this.showShadow.setValue(true);
        this.healthAnim.reset(1.0f);
        this.goldenAnim.reset(0.0f);
        this.numberAnim.reset(20.0f);
    }

    @Override
    public List<ModuleSetting<?>> getSettings() {
        List<ModuleSetting<?>> list = super.getSettings();
        list.add(showHead);
        if (showHead.getValue()) {
            list.add(headHatLayer);
            list.add(headRadius);
            list.add(headSize);
        }
        list.add(showHealthBar);
        list.add(showHealthText);
        list.add(showAbsorption);
        return list;
    }

    private LivingEntity resolveTarget(Minecraft mc, boolean editorOpen) {
        if (mc.hitResult instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        if (mc.crosshairPickEntity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        if (mc.player != null && mc.player.getLastHurtMob() != null && mc.player.getLastHurtMob().isAlive()) {
            return mc.player.getLastHurtMob();
        }
        if (editorOpen || mc.player != null) {
            return mc.player;
        }
        return null;
    }

    @Override
    public void update(Minecraft mc, boolean editorOpen) {
        LivingEntity resolved = resolveTarget(mc, editorOpen);
        if (resolved != null) {
            this.target = resolved;
            this.lastTargetName = resolved.getName().getString();
            this.lastTargetHealth = resolved.getHealth();
            this.lastTargetMaxHealth = Math.max(1.0f, resolved.getMaxHealth());
            this.lastTargetAbsorption = resolved.getAbsorptionAmount();
        } else if (editorOpen) {
            this.target = mc.player;
            this.lastTargetName = mc.player != null ? mc.player.getName().getString() : "Target";
            this.lastTargetHealth = 16.0f;
            this.lastTargetMaxHealth = 20.0f;
            this.lastTargetAbsorption = 4.0f;
        }

        float hPct = Math.max(0.0f, Math.min(1.0f, lastTargetHealth / lastTargetMaxHealth));
        float gPct = Math.max(0.0f, Math.min(1.0f, lastTargetAbsorption / lastTargetMaxHealth));

        healthAnim.update(hPct);
        goldenAnim.update(gPct);
        numberAnim.update(lastTargetHealth);
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

        if (showShadow.getValue() && shadowOpacity.getValue() > 0.001f && shadowBlur.getValue() > 0.5f) {
            int sAlpha = Math.max(0, Math.min(255, (int) (255.0f * shadowOpacity.getValue() * getAnimationAlpha())));
            DrawUtility.drawShadow(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, (sAlpha << 24), shadowBlur.getValue(), shadowOffsetY.getValue());
        }

        int bgAlpha = Math.max(0, Math.min(255, (int) (255.0f * opacity * 0.40f)));
        DrawUtility.drawSquircleExact(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, (bgAlpha << 24) | 0x000E1015, (bgAlpha << 24) | 0x000B0D11, (bgAlpha << 24) | 0x000B0D11, (bgAlpha << 24) | 0x000E1015);

        if (showTint.getValue() && tintStrength.getValue() > 0.005f) {
            int tAlpha = Math.max(0, Math.min(255, (int) (255.0f * tintStrength.getValue() * opacity * tintColor.getValue().a())));
            int tCol = (tAlpha << 24) | (tintColor.getValue().toArgbInt() & 0x00FFFFFF);
            DrawUtility.drawSquircleExact(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, tCol, tCol, tCol, tCol);
        }

        if (showOutline.getValue()) {
            int outlineAlpha = Math.max(0, Math.min(255, (int) (55.0f * opacity)));
            DrawUtility.drawRoundedOutline(graphics, x, y, w, h, rTL, rTR, rBR, rBL, s, (outlineAlpha << 24) | 0x00FFFFFF);
        }

        boolean hasHead = showHead.getValue();
        float curHeadSize = Math.min(h - 8.0f, headSize.getValue());
        float leftPad = hasHead ? (curHeadSize + 11.0f) : 8.0f;

        if (hasHead) {
            float headX = x + 5.0f;
            float headY = y + (h - curHeadSize) * 0.5f;
            float hRadius = Math.min(curHeadSize * 0.5f, headRadius.getValue());

            AbstractClientPlayer playerToDraw = (target instanceof AbstractClientPlayer p) ? p : mc.player;
            if (playerToDraw != null) {
                DrawUtility.drawPlayerHead(graphics, playerToDraw, headX, headY, curHeadSize, hRadius, headHatLayer.getValue(), opacity);
            }
            DrawUtility.drawRoundedOutline(graphics, headX, headY, curHeadSize, curHeadSize, hRadius, 0.85f, ((int) (40.0f * opacity) << 24) | 0x00FFFFFF);
        }

        int textColor = ((int) (240.0f * opacity) << 24) | 0x00FFFFFF;
        int accentColor = UITheme.INSTANCE.accentAlpha(opacity);

        float numX = x + w - 6.0f;
        float numY = y + 7.0f;

        if (showHealthText.getValue()) {
            String healthStr = String.format(Locale.ROOT, "%.1f", Math.max(0.0f, numberAnim.getValue()));
            float numW = FontRenderer.INSTANCE.width(FontRenderer.Face.SfSemibold, healthStr, 8.0f);
            numX = x + w - 6.0f - numW;
            FontRenderer.INSTANCE.draw(graphics, FontRenderer.Face.SfSemibold, healthStr, numX, numY, 8.0f, accentColor);
        }

        float nameX = x + leftPad;
        float nameY = y + 7.0f;
        FontRenderer.INSTANCE.draw(graphics, FontRenderer.Face.SfMedium, lastTargetName, nameX, nameY, 8.5f, textColor);

        if (showHealthBar.getValue()) {
            float barX = x + leftPad;
            float barY = y + h - 11.0f;
            float barW = w - leftPad - 6.0f;
            float barH = 3.5f;
            float barR = 1.0f;

            int barBgColor = ((int) (70.0f * opacity) << 24);
            DrawUtility.drawSquircle(graphics, barX, barY, barW, barH, barR, 0.0f, barBgColor);

            float hProgress = Mth.clamp(healthAnim.getValue(), 0.0f, 1.0f);
            float fillW = barW * hProgress;
            if (fillW > 0.5f) {
                int fillC1 = UITheme.INSTANCE.accentAlpha(opacity);
                int fillC2 = UITheme.INSTANCE.secondaryAccentAlpha(opacity);
                DrawUtility.drawSquircleExact(graphics, barX, barY, fillW, barH, barR, 0.0f, fillC1, fillC1, fillC2, fillC2);
            }

            if (showAbsorption.getValue()) {
                float gProgress = Mth.clamp(goldenAnim.getValue(), 0.0f, 1.0f);
                float goldW = barW * gProgress;
                if (goldW > 0.5f) {
                    int goldColor = ((int) (240.0f * opacity) << 24) | 0x00FFDC51;
                    float goldX = barX + barW - goldW;
                    DrawUtility.drawSquircle(graphics, goldX, barY, goldW, barH, barR, 0.0f, goldColor);
                }
            }
        }
    }
}
