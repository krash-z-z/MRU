package com.example.render.blur;

import java.util.List;

/**
 * Backward-compatibility wrapper delegating to {@link com.example.render.post.BlurUtil}.
 */
public final class BlurUtil {
    public static final BlurUtil INSTANCE = new BlurUtil();

    public static final float DEFAULT_TINT_STRENGTH = com.example.render.post.BlurUtil.DEFAULT_TINT_STRENGTH;
    public static final int MAX_GAUSSIAN_PASSES = com.example.render.post.BlurUtil.MAX_GAUSSIAN_PASSES;
    public static final int DEFAULT_GAUSSIAN_PASSES = com.example.render.post.BlurUtil.DEFAULT_GAUSSIAN_PASSES;
    public static final int DEFAULT_GAUSSIAN_RADIUS = com.example.render.post.BlurUtil.DEFAULT_GAUSSIAN_RADIUS;
    public static final float DEFAULT_GAUSSIAN_SIGMA = com.example.render.post.BlurUtil.DEFAULT_GAUSSIAN_SIGMA;
    public static final float DEFAULT_GAUSSIAN_OFFSET = com.example.render.post.BlurUtil.DEFAULT_GAUSSIAN_OFFSET;

    private BlurUtil() {}

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass, float refraction, float chromaticAberration, float highlight, float sampleEscape, int shapeMode, float blurStrength) {
        com.example.render.post.BlurUtil.INSTANCE.apply(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, refraction, chromaticAberration, highlight, sampleEscape, shapeMode, blurStrength);
    }

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass, float refraction, float chromaticAberration, float highlight, float sampleEscape, int shapeMode) {
        apply(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, refraction, chromaticAberration, highlight, sampleEscape, shapeMode, 1.0f);
    }

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass, float refraction, float chromaticAberration, float highlight, float sampleEscape) {
        apply(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, refraction, chromaticAberration, highlight, sampleEscape, squircle != null && squircle ? 1 : 0, 1.0f);
    }

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass) {
        apply(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, 0.85f, 0.45f, 0.50f, 7.0f, squircle != null && squircle ? 1 : 0, 1.0f);
    }

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType) {
        apply(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, false);
    }

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow) {
        apply(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, ShadowBlurUtil.BlurType.GAUSSIAN, false);
    }

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength) {
        apply(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, false, ShadowBlurUtil.BlurType.GAUSSIAN, false);
    }

    public void apply(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, shadow, blurType, false);
    }

    public void apply(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength, ShadowBlurUtil.BlurType blurType) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, false, blurType, false);
    }

    public void apply(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength, boolean shadow) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, shadow, blurType(shadow), false);
    }

    public void apply(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, false, ShadowBlurUtil.BlurType.GAUSSIAN, false);
    }

    public void apply(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN, false);
    }

    public void apply(float x, float y, float width, float height, float radius) {
        com.example.render.post.BlurUtil.INSTANCE.apply(x, y, width, height, radius);
    }

    public void applyCircle(float centerX, float centerY, float radius) {
        apply(centerX - radius, centerY - radius, radius * 2.0f, radius * 2.0f, radius, -1.0f, false, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN);
    }

    public void applyPill(float x, float y, float width, float height) {
        apply(x, y, width, height, Math.min(width, height) * 0.5f, 0.5f, true, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN);
    }

    public void applyLiquidGlass(float x, float y, float width, float height, float radius, float smoothing, float tintStrength, float refraction, float chromaticAberration, float highlight, float sampleEscape) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, true, tintStrength, false, ShadowBlurUtil.BlurType.ACRYLIC, true, refraction, chromaticAberration, highlight, sampleEscape, 1, 1.0f);
    }

    public void applyLiquidGlass(float x, float y, float width, float height, float radius, float smoothing, float tintStrength, float refraction, float chromaticAberration, float highlight, float sampleEscape, float blurStrength, ShadowBlurUtil.BlurType blurType, int shapeMode) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, shapeMode == 1, tintStrength, false, blurType != null ? blurType : ShadowBlurUtil.BlurType.ACRYLIC, true, refraction, chromaticAberration, highlight, sampleEscape, shapeMode, blurStrength);
    }

    public boolean isEmpty() {
        return com.example.render.post.BlurUtil.INSTANCE.isEmpty();
    }

    public List<Shape> drainShapes(float guiScale) {
        var postShapes = com.example.render.post.BlurUtil.INSTANCE.drainShapes(guiScale);
        return postShapes.stream().map(s -> new Shape(
            s.x(), s.y(), s.width(), s.height(),
            s.rTL(), s.rTR(), s.rBR(), s.rBL(),
            s.smoothing(), s.squircle(), s.tintStrength(), s.shadow(),
            s.blurType(), s.liquidGlass(), s.refraction(), s.chromaticAberration(),
            s.highlight(), s.sampleEscape(), s.shapeMode(), s.blurStrength()
        )).toList();
    }

    public void clear() {
        com.example.render.post.BlurUtil.INSTANCE.clear();
    }

    private static ShadowBlurUtil.BlurType blurType(boolean shadow) {
        return ShadowBlurUtil.BlurType.GAUSSIAN;
    }

    public record Shape(
        float x, float y, float width, float height,
        float rTL, float rTR, float rBR, float rBL,
        float smoothing, Boolean squircle,
        float tintStrength, boolean shadow,
        ShadowBlurUtil.BlurType blurType,
        boolean liquidGlass,
        float refraction,
        float chromaticAberration,
        float highlight,
        float sampleEscape,
        int shapeMode,
        float blurStrength
    ) {
        public Shape(float x, float y, float width, float height, float radius) {
            this(x, y, width, height, radius, radius, radius, radius, 0.8f, true, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN, false, 0.0f, 0.0f, 0.0f, 0.0f, 1, 1.0f);
        }

        public float radius() {
            return Math.max(Math.max(rTL, rTR), Math.max(rBR, rBL));
        }
    }
}
