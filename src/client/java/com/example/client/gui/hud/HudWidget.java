package com.example.client.gui.hud;

import com.example.render.post.BlurUtil;
import com.example.render.blur.ShadowBlurUtil;
import com.example.render.setting.BooleanSetting;
import com.example.render.setting.ColorSetting;
import com.example.render.setting.EnumSetting;
import com.example.render.setting.FloatSetting;
import com.example.render.setting.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public abstract class HudWidget {
    public final String id;
    public final String title;
    public final HudBounds bounds;
    public final boolean movable;
    private boolean enabled;

    public final float defaultX;
    public final float defaultY;
    public final boolean defaultEnabled;

    private float animationAlpha = 1.0f;

    public enum BlurMode {
        GAUSSIAN("Gaussian"),
        ACRYLIC("Acrylic"),
        NONE("None");

        private final String label;
        BlurMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public final FloatSetting cornerRadius = new FloatSetting("Corner Radius", 16.0f, 0.0f, 32.0f, 1.0f, "px");
    public final BooleanSetting separateCorners = new BooleanSetting("Separate Corners", false);
    public final FloatSetting cornerRadiusTL = new FloatSetting("Radius Top-Left", 16.0f, 0.0f, 32.0f, 1.0f, "px");
    public final FloatSetting cornerRadiusTR = new FloatSetting("Radius Top-Right", 16.0f, 0.0f, 32.0f, 1.0f, "px");
    public final FloatSetting cornerRadiusBR = new FloatSetting("Radius Bottom-Right", 16.0f, 0.0f, 32.0f, 1.0f, "px");
    public final FloatSetting cornerRadiusBL = new FloatSetting("Radius Bottom-Left", 16.0f, 0.0f, 32.0f, 1.0f, "px");
    public final FloatSetting cornerSmoothing = new FloatSetting("Corner Smoothing", 0.8f, 0.0f, 1.0f, 0.05f);
    public final FloatSetting backgroundOpacity = new FloatSetting("Opacity", 0.65f, 0.0f, 1.0f, 0.05f);
    public final EnumSetting<BlurMode> blurType = new EnumSetting<>("Blur Mode", BlurMode.values(), BlurMode.GAUSSIAN, BlurMode::getLabel);
    public final FloatSetting blurStrength = new FloatSetting("Blur Strength", 1.0f, 0.0f, 3.0f, 0.1f);
    public final BooleanSetting showOutline = new BooleanSetting("Outline", false);
    public final BooleanSetting showShadow = new BooleanSetting("Shadow", true);
    public final FloatSetting shadowBlur = new FloatSetting("Shadow Blur", 14.0f, 0.0f, 48.0f, 1.0f, "px");
    public final FloatSetting shadowOpacity = new FloatSetting("Shadow Opacity", 0.35f, 0.0f, 1.0f, 0.05f);
    public final FloatSetting shadowOffsetY = new FloatSetting("Shadow Offset", 2.0f, -10.0f, 20.0f, 0.5f, "px");
    public final BooleanSetting showTint = new BooleanSetting("Tint Overlay", true);
    public final FloatSetting tintStrength = new FloatSetting("Tint Strength", 0.45f, 0.0f, 1.0f, 0.05f);
    public final ColorSetting tintColor = new ColorSetting("Tint Color", 0x3012141C);
    public final FloatSetting scale = new FloatSetting("Scale", 1.0f, 0.5f, 1.5f, 0.05f);

    public float getRadiusTL() {
        return separateCorners.getValue() ? cornerRadiusTL.getValue() : cornerRadius.getValue();
    }

    public float getRadiusTR() {
        return separateCorners.getValue() ? cornerRadiusTR.getValue() : cornerRadius.getValue();
    }

    public float getRadiusBR() {
        return separateCorners.getValue() ? cornerRadiusBR.getValue() : cornerRadius.getValue();
    }

    public float getRadiusBL() {
        return separateCorners.getValue() ? cornerRadiusBL.getValue() : cornerRadius.getValue();
    }

    protected HudWidget(String id, String title, float x, float y, float width, float height, boolean enabledByDefault, boolean movable) {
        this.id = id;
        this.title = title;
        this.bounds = new HudBounds(x, y, width, height);
        this.enabled = enabledByDefault;
        this.movable = movable;
        this.defaultX = x;
        this.defaultY = y;
        this.defaultEnabled = enabledByDefault;
    }

    protected HudWidget(String id, String title, float x, float y, float width, float height) {
        this(id, title, x, y, width, height, true, true);
    }

    public boolean supportsCornerRadius() {
        return true;
    }

    public boolean supportsSmoothing() {
        return true;
    }

    public List<ModuleSetting<?>> getSettings() {
        List<ModuleSetting<?>> list = new ArrayList<>();
        if (supportsCornerRadius()) {
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
            if (supportsSmoothing()) {
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
        return list;
    }

    public void reset() {
        this.bounds.x = defaultX;
        this.bounds.y = defaultY;
        this.enabled = defaultEnabled;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public HudBounds getBounds() {
        return bounds;
    }

    public boolean getMovable() {
        return movable;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getAnimationAlpha() {
        return animationAlpha;
    }

    public void setAnimationAlpha(float alpha) {
        this.animationAlpha = alpha;
    }

    public float getScale() {
        return scale.getValue();
    }

    public void update(Minecraft mc, boolean editorOpen) {
    }

    public boolean visible(Minecraft mc, boolean editorOpen) {
        return enabled || editorOpen;
    }

    public abstract void render(GuiGraphicsExtractor graphics, Minecraft mc, boolean editorOpen);

    public List<BlurUtil.Shape> blurBoxes(float guiScale, float tintStrength) {
        if (blurType.getValue() == BlurMode.NONE) return List.of();
        float strength = blurStrength.getValue();
        if (strength <= 0.001f) return List.of();
        float rTL = getRadiusTL();
        float rTR = getRadiusTR();
        float rBR = getRadiusBR();
        float rBL = getRadiusBL();
        return List.of(new BlurUtil.Shape(
            bounds.x * guiScale,
            bounds.y * guiScale,
            bounds.width * guiScale,
            bounds.height * guiScale,
            rTL * guiScale,
            rTR * guiScale,
            rBR * guiScale,
            rBL * guiScale,
            cornerSmoothing.getValue(),
            supportsSmoothing(),
            showTint.getValue() ? this.tintStrength.getValue() : 0.0f,
            showShadow.getValue(),
            blurType.getValue() == BlurMode.ACRYLIC ? ShadowBlurUtil.BlurType.ACRYLIC : ShadowBlurUtil.BlurType.GAUSSIAN,
            false,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            supportsSmoothing() ? 1 : 0,
            strength
        ));
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        return false;
    }

    public void mouseReleased(int button) {
    }

    public void onDrag(float mouseX, float mouseY) {
    }

    public boolean onDragStart(float mouseX, float mouseY) {
        return false;
    }

    public long dragHoldMillis() {
        return 0L;
    }
}
