package com.example.client.gui;

import com.example.render.blur.ShadowBlurUtil.BlurType;
import com.example.render.gradient.GradientType;
import com.example.render.setting.BooleanSetting;
import com.example.render.setting.EnumSetting;
import com.example.render.setting.FloatSetting;
import com.example.render.setting.MultiBooleanSetting;

public final class HudConfig {
    public static final HudConfig INSTANCE = new HudConfig();

    public final FloatSetting cornerRadius = new FloatSetting("Corner Radius", 24.0f, 0.0f, 60.0f, 1.0f, "px");
    public final FloatSetting cornerSmoothing = new FloatSetting("Corner Smoothing", 0.8f, 0.0f, 1.0f, 0.05f);
    public final FloatSetting shadowBlur = new FloatSetting("Shadow Blur", 14.0f, 0.0f, 48.0f, 1.0f, "px");
    public final FloatSetting shadowOpacity = new FloatSetting("Shadow Opacity", 0.35f, 0.0f, 1.0f, 0.05f);
    public final FloatSetting boxWidth = new FloatSetting("Box Width", 210.0f, 120.0f, 320.0f, 5.0f, "px");
    public final FloatSetting boxHeight = new FloatSetting("Box Height", 110.0f, 60.0f, 200.0f, 5.0f, "px");
    
    public final FloatSetting blurOpacity = new FloatSetting("Glass Opacity", 0.45f, 0.10f, 1.0f, 0.05f);
    public final FloatSetting acrylicOpacity = blurOpacity;

    public final FloatSetting gaussianPasses = new FloatSetting("Gaussian Passes", 2.0f, 1.0f, 6.0f, 1.0f);
    public final FloatSetting gaussianRadius = new FloatSetting("Gaussian Radius", 20.0f, 1.0f, 48.0f, 1.0f, "px");
    public final FloatSetting gaussianSigma = new FloatSetting("Gaussian Sigma", 7.0f, 0.5f, 24.0f, 0.5f);
    public final FloatSetting gaussianOffset = new FloatSetting("Gaussian Offset", 1.0f, 0.5f, 3.0f, 0.1f);

    public final FloatSetting acrylicRadius = new FloatSetting("Acrylic Radius", 24.0f, 4.0f, 64.0f, 1.0f, "px");
    public final FloatSetting acrylicChromatic = new FloatSetting("Acrylic Chromatic", 0.35f, 0.0f, 1.0f, 0.05f);
    public final FloatSetting acrylicHaze = new FloatSetting("Acrylic Haze", 0.40f, 0.0f, 1.0f, 0.05f);
    public final FloatSetting acrylicGrain = new FloatSetting("Acrylic Grain", 0.30f, 0.0f, 1.0f, 0.05f);

    public final EnumSetting<BlurType> blurType = new EnumSetting<>(
        "Blur Mode",
        BlurType.values(),
        BlurType.GAUSSIAN,
        type -> switch (type) {
            case GAUSSIAN -> "Gaussian Blur";
            case ACRYLIC -> "Acrylic Blur";
            case NONE -> "None (Solid/Tint)";
        }
    );

    public final EnumSetting<GradientType> gradientType = new EnumSetting<>(
        "Gradient Type",
        GradientType.values(),
        GradientType.LINEAR,
        type -> "Linear Gradient"
    );

    public final MultiBooleanSetting features = new MultiBooleanSetting(
        "Visual Features",
        new BooleanSetting("Drop Shadows", true),
        new BooleanSetting("Background Blur", true),
        new BooleanSetting("Specular Sheen", true),
        new BooleanSetting("MSDF Typography", true),
        new BooleanSetting("Glass Outline", true)
    );

    private HudConfig() {}
}
