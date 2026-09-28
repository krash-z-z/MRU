package com.example.render.post;

import com.example.render.blur.ShadowBlurUtil;
import com.example.render.shape.CornerRadii;

import java.util.ArrayList;
import java.util.List;

/**
 * Blur request queue and post-processing shape dispatch.
 */
public final class BlurUtil {
    public static final BlurUtil INSTANCE = new BlurUtil();

    public static final float DEFAULT_TINT_STRENGTH = 0.45f;
    public static final int MAX_GAUSSIAN_PASSES = 6;
    public static final int DEFAULT_GAUSSIAN_PASSES = 2;
    public static final int DEFAULT_GAUSSIAN_RADIUS = 20;
    public static final float DEFAULT_GAUSSIAN_SIGMA = 7.0f;
    public static final float DEFAULT_GAUSSIAN_OFFSET = 1.0f;

    private final List<Shape> queuedShapes = new ArrayList<>();

    private BlurUtil() {}

    public static class RequestBuilder {
        private float x, y, width, height;
        private float rTL, rTR, rBR, rBL;
        private float smoothing = 0.8f;
        private Boolean squircle = true;
        private float tintStrength = -1.0f;
        private boolean shadow = false;
        private ShadowBlurUtil.BlurType blurType = ShadowBlurUtil.BlurType.GAUSSIAN;
        private boolean liquidGlass = false;
        private float refraction = 0.0f;
        private float chromaticAberration = 0.0f;
        private float highlight = 0.0f;
        private float sampleEscape = 0.0f;
        private int shapeMode = 1;
        private float blurStrength = 1.0f;

        public RequestBuilder bounds(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            return this;
        }

        public RequestBuilder corners(float radius) {
            return corners(radius, radius, radius, radius);
        }

        public RequestBuilder corners(float rTL, float rTR, float rBR, float rBL) {
            this.rTL = rTL; this.rTR = rTR; this.rBR = rBR; this.rBL = rBL;
            return this;
        }

        public RequestBuilder corners(CornerRadii radii) {
            if (radii != null) {
                this.rTL = radii.tl(); this.rTR = radii.tr();
                this.rBR = radii.br(); this.rBL = radii.bl();
                this.smoothing = radii.smoothing();
            }
            return this;
        }

        public RequestBuilder smoothing(float smoothing) {
            this.smoothing = smoothing;
            return this;
        }

        public RequestBuilder squircle(boolean squircle) {
            this.squircle = squircle;
            this.shapeMode = squircle ? 1 : 0;
            return this;
        }

        public RequestBuilder mode(ShadowBlurUtil.BlurType blurType) {
            this.blurType = blurType;
            return this;
        }

        public RequestBuilder strength(float strength) {
            this.blurStrength = strength;
            return this;
        }

        public RequestBuilder tint(float tintStrength) {
            this.tintStrength = tintStrength;
            return this;
        }

        public RequestBuilder shadow(boolean shadow) {
            this.shadow = shadow;
            return this;
        }

        public RequestBuilder liquidGlass(float refraction, float chromaticAberration, float highlight, float sampleEscape) {
            this.liquidGlass = true;
            this.refraction = refraction;
            this.chromaticAberration = chromaticAberration;
            this.highlight = highlight;
            this.sampleEscape = sampleEscape;
            return this;
        }

        public void apply() {
            if (width <= 0.0f || height <= 0.0f) return;
            BlurUtil.INSTANCE.queuedShapes.add(new Shape(
                x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle,
                tintStrength, shadow, blurType, liquidGlass, refraction,
                chromaticAberration, highlight, sampleEscape, shapeMode, blurStrength
            ));
        }
    }

    public RequestBuilder queue() {
        return new RequestBuilder();
    }

    public void apply(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass, float refraction, float chromaticAberration, float highlight, float sampleEscape, int shapeMode, float blurStrength) {
        if (width <= 0.0f || height <= 0.0f) return;
        queuedShapes.add(new Shape(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, refraction, chromaticAberration, highlight, sampleEscape, shapeMode, blurStrength));
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
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, shadow, ShadowBlurUtil.BlurType.GAUSSIAN, false);
    }

    public void apply(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, false, ShadowBlurUtil.BlurType.GAUSSIAN, false);
    }

    public void apply(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle) {
        apply(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN, false);
    }

    public void apply(float x, float y, float width, float height, float radius) {
        apply(x, y, width, height, radius, radius, radius, radius, 0.8f, true, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN, false, 0.0f, 0.0f, 0.0f, 0.0f, 1, 1.0f);
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
        return queuedShapes.isEmpty();
    }

    public List<Shape> drainShapes(float guiScale) {
        if (queuedShapes.isEmpty()) return List.of();
        List<Shape> result = new ArrayList<>(queuedShapes.size());
        for (Shape shape : queuedShapes) {
            result.add(new Shape(
                shape.x() * guiScale,
                shape.y() * guiScale,
                shape.width() * guiScale,
                shape.height() * guiScale,
                shape.rTL() * guiScale,
                shape.rTR() * guiScale,
                shape.rBR() * guiScale,
                shape.rBL() * guiScale,
                shape.smoothing(),
                shape.squircle(),
                shape.tintStrength(),
                shape.shadow(),
                shape.blurType(),
                shape.liquidGlass(),
                shape.refraction(),
                shape.chromaticAberration(),
                shape.highlight(),
                shape.sampleEscape() * guiScale,
                shape.shapeMode(),
                shape.blurStrength()
            ));
        }
        queuedShapes.clear();
        return result;
    }

    public void clear() {
        queuedShapes.clear();
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
        public Shape(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass, float refraction, float chromaticAberration, float highlight, float sampleEscape, int shapeMode) {
            this(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, refraction, chromaticAberration, highlight, sampleEscape, shapeMode, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass, float refraction, float chromaticAberration, float highlight, float sampleEscape) {
            this(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, refraction, chromaticAberration, highlight, sampleEscape, squircle != null && squircle ? 1 : 0, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType, boolean liquidGlass) {
            this(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, liquidGlass, 0.85f, 0.45f, 0.50f, 7.0f, squircle != null && squircle ? 1 : 0, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType) {
            this(x, y, width, height, rTL, rTR, rBR, rBL, smoothing, squircle, tintStrength, shadow, blurType, false, 0.0f, 0.0f, 0.0f, 0.0f, squircle != null && squircle ? 1 : 0, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength, boolean shadow, ShadowBlurUtil.BlurType blurType) {
            this(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, shadow, blurType, false, 0.0f, 0.0f, 0.0f, 0.0f, squircle != null && squircle ? 1 : 0, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength, ShadowBlurUtil.BlurType blurType) {
            this(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, false, blurType, false, 0.0f, 0.0f, 0.0f, 0.0f, squircle != null && squircle ? 1 : 0, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle, float tintStrength) {
            this(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, tintStrength, false, ShadowBlurUtil.BlurType.GAUSSIAN, false, 0.0f, 0.0f, 0.0f, 0.0f, squircle != null && squircle ? 1 : 0, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float radius, float smoothing, Boolean squircle) {
            this(x, y, width, height, radius, radius, radius, radius, smoothing, squircle, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN, false, 0.0f, 0.0f, 0.0f, 0.0f, squircle != null && squircle ? 1 : 0, 1.0f);
        }

        public Shape(float x, float y, float width, float height, float radius) {
            this(x, y, width, height, radius, radius, radius, radius, 0.8f, true, -1.0f, false, ShadowBlurUtil.BlurType.GAUSSIAN, false, 0.0f, 0.0f, 0.0f, 0.0f, 1, 1.0f);
        }

        public float radius() {
            return Math.max(Math.max(rTL, rTR), Math.max(rBR, rBL));
        }
    }
}
