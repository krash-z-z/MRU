package com.example.render.glass;

import com.example.render.shape.DrawUtility;
import com.example.render.util.Color4f;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * LiquidGlassBox: High-level UI container / card renderer with real-time SDF refraction,
 * chromatic dispersion, frosted blur diffusion, and specular Fresnel reflections.
 */
public final class LiquidGlassBox {

    private float x;
    private float y;
    private float width;
    private float height;
    private float rTL = 12.0f;
    private float rTR = 12.0f;
    private float rBR = 12.0f;
    private float rBL = 12.0f;
    private float rotation = 0.0f;

    private float thickness = 14.0f;
    private float baseHeight = 112.0f;
    private float indexOfRefraction = 1.52f;
    private float chromaticDispersion = 0.025f;
    private float blurRoughness = 0.65f;
    private float reflectionIntensity = 1.0f;
    private float liquidWobble = 0.0f;
    private float time = 0.0f;

    private Color4f tintColor = new Color4f(1.0f, 1.0f, 1.0f, 0.25f);
    private Color4f specularColor = Color4f.WHITE;

    private boolean hasBorder = false;
    private float borderWidth = 1.0f;
    private Color4f borderColor = new Color4f(1.0f, 1.0f, 1.0f, 0.35f);

    private boolean hasShadow = false;
    private float shadowRadius = 16.0f;
    private float shadowOpacity = 0.4f;

    private LiquidGlassRenderer.ShapeType shapeType = LiquidGlassRenderer.ShapeType.ROUNDED_RECT;

    public LiquidGlassBox() {}

    public static LiquidGlassBox create() {
        return new LiquidGlassBox();
    }

    // --- Fluent Builder Methods ---

    public LiquidGlassBox bounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    public LiquidGlassBox radius(float radius) {
        this.rTL = radius;
        this.rTR = radius;
        this.rBR = radius;
        this.rBL = radius;
        return this;
    }

    public LiquidGlassBox radii(float rTL, float rTR, float rBR, float rBL) {
        this.rTL = rTL;
        this.rTR = rTR;
        this.rBR = rBR;
        this.rBL = rBL;
        return this;
    }

    public LiquidGlassBox shape(LiquidGlassRenderer.ShapeType shapeType) {
        this.shapeType = shapeType;
        return this;
    }

    public LiquidGlassBox rotation(float rotation) {
        this.rotation = rotation;
        return this;
    }

    public LiquidGlassBox optics(float thickness, float ior, float dispersion, float blurRoughness) {
        this.thickness = thickness;
        this.baseHeight = thickness * 8.0f;
        this.indexOfRefraction = ior;
        this.chromaticDispersion = dispersion;
        this.blurRoughness = blurRoughness;
        return this;
    }

    public LiquidGlassBox thickness(float thickness) {
        this.thickness = thickness;
        this.baseHeight = thickness * 8.0f;
        return this;
    }

    public LiquidGlassBox ior(float ior) {
        this.indexOfRefraction = ior;
        return this;
    }

    public LiquidGlassBox dispersion(float dispersion) {
        this.chromaticDispersion = dispersion;
        return this;
    }

    public LiquidGlassBox blur(float roughness) {
        this.blurRoughness = roughness;
        return this;
    }

    public LiquidGlassBox reflection(float intensity) {
        this.reflectionIntensity = intensity;
        return this;
    }

    public LiquidGlassBox liquid(float wobble, float time) {
        this.liquidWobble = wobble;
        this.time = time;
        return this;
    }

    public LiquidGlassBox tint(Color4f tintColor) {
        this.tintColor = tintColor;
        return this;
    }

    public LiquidGlassBox tint(int argb) {
        this.tintColor = Color4f.fromArgb(argb);
        return this;
    }

    public LiquidGlassBox specular(Color4f specularColor) {
        this.specularColor = specularColor;
        return this;
    }

    public LiquidGlassBox border(float width, Color4f color) {
        this.hasBorder = true;
        this.borderWidth = width;
        this.borderColor = color;
        return this;
    }

    public LiquidGlassBox shadow(float radius, float opacity) {
        this.hasShadow = true;
        this.shadowRadius = radius;
        this.shadowOpacity = opacity;
        return this;
    }

    // --- Style Presets ---

    public LiquidGlassBox asCrystal() {
        this.thickness = 10.0f;
        this.baseHeight = 80.0f;
        this.indexOfRefraction = 1.54f;
        this.chromaticDispersion = 0.035f;
        this.blurRoughness = 0.15f;
        this.reflectionIntensity = 1.4f;
        this.tintColor = new Color4f(0.95f, 0.98f, 1.0f, 0.15f);
        this.specularColor = Color4f.WHITE;
        return this;
    }

    public LiquidGlassBox asFrosted() {
        this.thickness = 16.0f;
        this.baseHeight = 128.0f;
        this.indexOfRefraction = 1.48f;
        this.chromaticDispersion = 0.015f;
        this.blurRoughness = 0.85f;
        this.reflectionIntensity = 0.9f;
        this.tintColor = new Color4f(1.0f, 1.0f, 1.0f, 0.30f);
        this.specularColor = new Color4f(1.0f, 1.0f, 1.0f, 0.9f);
        return this;
    }

    public LiquidGlassBox asAquaFluid(float animTime) {
        this.thickness = 14.0f;
        this.baseHeight = 112.0f;
        this.indexOfRefraction = 1.34f;
        this.chromaticDispersion = 0.02f;
        this.blurRoughness = 0.45f;
        this.reflectionIntensity = 1.2f;
        this.liquidWobble = 0.8f;
        this.time = animTime;
        this.tintColor = new Color4f(0.15f, 0.65f, 1.0f, 0.35f);
        this.specularColor = new Color4f(0.8f, 0.95f, 1.0f, 1.0f);
        return this;
    }

    public LiquidGlassBox asDarkObsidian() {
        this.thickness = 14.0f;
        this.baseHeight = 112.0f;
        this.indexOfRefraction = 1.62f;
        this.chromaticDispersion = 0.01f;
        this.blurRoughness = 0.55f;
        this.reflectionIntensity = 1.3f;
        this.tintColor = new Color4f(0.08f, 0.09f, 0.12f, 0.65f);
        this.specularColor = new Color4f(0.7f, 0.75f, 0.85f, 1.0f);
        return this;
    }

    // --- Render Execution ---

    public void render(GuiGraphicsExtractor graphics) {
        if (width <= 0.0f || height <= 0.0f) return;

        if (graphics != null) {
            float s = 0.85f;
            float opacity = tintColor.a;

            // 1. Drop shadow
            if (hasShadow && shadowOpacity > 0.001f) {
                int sAlpha = Math.max(0, Math.min(255, (int) (255.0f * shadowOpacity)));
                DrawUtility.drawShadow(graphics, x, y, width, height, rTL, rTR, rBR, rBL, s, (sAlpha << 24), shadowRadius, 2.0f);
            }

            // 2. Liquid Glass Base Fill
            int baseArgb = tintColor.toArgbInt();
            DrawUtility.drawSquircleExact(graphics, x, y, width, height, rTL, rTR, rBR, rBL, s, baseArgb, baseArgb, baseArgb, baseArgb);

            // 3. Top Curved Dome Specular Sheen
            float waveWobble = liquidWobble > 0.0f ? (float) Math.sin(time * 2.5f) * 0.05f : 0.0f;
            float sheenHeight = Math.max(8.0f, height * (0.42f + waveWobble));
            int sheenTopAlpha = Math.max(0, Math.min(255, (int) (255.0f * 0.35f * reflectionIntensity)));
            int sheenTop = (sheenTopAlpha << 24) | 0x00FFFFFF;
            int sheenBot = 0x00FFFFFF;

            DrawUtility.drawSquircleExact(
                graphics,
                x + 1.0f, y + 1.0f, width - 2.0f, sheenHeight,
                Math.max(0.0f, rTL - 1.0f), Math.max(0.0f, rTR - 1.0f), 0.0f, 0.0f,
                s,
                sheenTop, sheenBot, sheenBot, sheenTop
            );

            // 4. Bottom Internal Refraction Reflection Highlight
            int bottomSheenAlpha = Math.max(0, Math.min(255, (int) (255.0f * 0.18f * reflectionIntensity)));
            float botSheenHeight = Math.max(6.0f, height * 0.28f);
            int botSheenBot = (bottomSheenAlpha << 24) | 0x00FFFFFF;
            int botSheenTop = 0x00FFFFFF;

            DrawUtility.drawSquircleExact(
                graphics,
                x + 1.0f, y + height - botSheenHeight - 1.0f, width - 2.0f, botSheenHeight,
                0.0f, 0.0f, Math.max(0.0f, rBR - 1.0f), Math.max(0.0f, rBL - 1.0f),
                s,
                botSheenTop, botSheenBot, botSheenBot, botSheenTop
            );

            // 5. Specular Border Outline
            if (hasBorder && borderWidth > 0.0f && borderColor.a > 0.001f) {
                DrawUtility.drawRoundedOutline(
                    graphics,
                    x, y, width, height,
                    rTL, rTR, rBR, rBL,
                    s,
                    borderColor.toArgbInt()
                );
            }
        } else {
            LiquidGlassRenderer.INSTANCE.renderLiquidGlass(
                shapeType,
                x, y, width, height,
                rTL, rTR, rBR, rBL,
                rotation,
                thickness,
                baseHeight,
                indexOfRefraction,
                chromaticDispersion,
                blurRoughness,
                reflectionIntensity,
                liquidWobble,
                time,
                tintColor,
                specularColor
            );
        }
    }

    public void render() {
        render(null);
    }

    // --- Static Convenience Methods ---

    public static void draw(float x, float y, float width, float height, float radius, Color4f tint) {
        create().bounds(x, y, width, height).radius(radius).tint(tint).render();
    }

    public static void draw(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, Color4f tint) {
        create().bounds(x, y, width, height).radii(rTL, rTR, rBR, rBL).tint(tint).render();
    }

    public static void drawFrosted(float x, float y, float width, float height, float radius) {
        create().bounds(x, y, width, height).radius(radius).asFrosted().render();
    }

    public static void drawCrystal(float x, float y, float width, float height, float radius) {
        create().bounds(x, y, width, height).radius(radius).asCrystal().render();
    }

    public static void drawAqua(float x, float y, float width, float height, float radius, float time) {
        create().bounds(x, y, width, height).radius(radius).asAquaFluid(time).render();
    }

    public static void drawWithBorder(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, Color4f tint, Color4f borderColor, float borderWidth) {
        create().bounds(x, y, width, height).radius(radius).tint(tint).border(borderWidth, borderColor).render(graphics);
    }
}
