package com.example.render.shape;

/**
 * Immutable corner radii definition with optional Apple-style squircle smoothing.
 */
public record CornerRadii(float tl, float tr, float br, float bl, float smoothing) {

    public static final CornerRadii ZERO = new CornerRadii(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

    public static CornerRadii uniform(float radius) {
        return new CornerRadii(radius, radius, radius, radius, 0.8f);
    }

    public static CornerRadii uniform(float radius, float smoothing) {
        return new CornerRadii(radius, radius, radius, radius, smoothing);
    }

    public static CornerRadii of(float tl, float tr, float br, float bl) {
        return new CornerRadii(tl, tr, br, bl, 0.8f);
    }

    public static CornerRadii of(float tl, float tr, float br, float bl, float smoothing) {
        return new CornerRadii(tl, tr, br, bl, smoothing);
    }

    public boolean isUniform() {
        return tl == tr && tr == br && br == bl;
    }

    public CornerRadii scale(float factor) {
        return new CornerRadii(tl * factor, tr * factor, br * factor, bl * factor, smoothing);
    }
}
