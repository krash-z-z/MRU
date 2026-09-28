package com.example.render.context;

/**
 * High-performance unified RGBA color structure with both packed integer and float channels.
 */
public class Color {

    public static final Color TRANSPARENT = ofRgba(0.0f, 0.0f, 0.0f, 0.0f);
    public static final Color WHITE = ofRgba(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Color BLACK = ofRgba(0.0f, 0.0f, 0.0f, 1.0f);
    public static final Color RED = ofRgba(1.0f, 0.0f, 0.0f, 1.0f);
    public static final Color GREEN = ofRgba(0.0f, 1.0f, 0.0f, 1.0f);
    public static final Color BLUE = ofRgba(0.0f, 0.0f, 1.0f, 1.0f);

    public final float r;
    public final float g;
    public final float b;
    public final float a;
    public final int argb;

    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }

    public Color(float r, float g, float b, float a) {
        this.r = Math.clamp(r, 0.0f, 1.0f);
        this.g = Math.clamp(g, 0.0f, 1.0f);
        this.b = Math.clamp(b, 0.0f, 1.0f);
        this.a = Math.clamp(a, 0.0f, 1.0f);
        int ia = (int) (this.a * 255.0f + 0.5f);
        int ir = (int) (this.r * 255.0f + 0.5f);
        int ig = (int) (this.g * 255.0f + 0.5f);
        int ib = (int) (this.b * 255.0f + 0.5f);
        this.argb = (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    public Color(float r, float g, float b) {
        this(r, g, b, 1.0f);
    }

    public Color(int argb) {
        this.argb = argb;
        this.a = ((argb >>> 24) & 0xFF) / 255.0f;
        this.r = ((argb >>> 16) & 0xFF) / 255.0f;
        this.g = ((argb >>> 8) & 0xFF) / 255.0f;
        this.b = (argb & 0xFF) / 255.0f;
    }

    public static Color of(int argb) {
        return new Color(argb);
    }

    public static Color ofRgb(int rgb) {
        return new Color(0xFF000000 | (rgb & 0x00FFFFFF));
    }

    public static Color ofHex(int rgbHex, float alpha) {
        int a = Math.clamp((int) (alpha * 255.0f + 0.5f), 0, 255);
        return new Color((a << 24) | (rgbHex & 0x00FFFFFF));
    }

    public static Color ofRgba(float r, float g, float b, float a) {
        return new Color(r, g, b, a);
    }

    public static Color fromArgb(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        if (a == 0.0f && (argb & 0xFF000000) == 0 && argb != 0) {
            a = 1.0f;
        }
        return new Color(r, g, b, a);
    }

    public static Color fromArgbInt(int argb) {
        return fromArgb(argb);
    }

    public static Color fromRgba(int rgba) {
        float r = ((rgba >>> 24) & 0xFF) / 255.0f;
        float g = ((rgba >>> 16) & 0xFF) / 255.0f;
        float b = ((rgba >>> 8) & 0xFF) / 255.0f;
        float a = (rgba & 0xFF) / 255.0f;
        return new Color(r, g, b, a);
    }

    public static Color fromHsb(float hue, float saturation, float brightness, float alpha) {
        hue = hue - (float) Math.floor(hue);
        saturation = Math.clamp(saturation, 0.0f, 1.0f);
        brightness = Math.clamp(brightness, 0.0f, 1.0f);
        if (saturation <= 0.0001f) {
            return new Color(brightness, brightness, brightness, alpha);
        }
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * f);
        float t = brightness * (1.0f - (saturation * (1.0f - f)));
        return switch ((int) h) {
            case 0 -> new Color(brightness, t, p, alpha);
            case 1 -> new Color(q, brightness, p, alpha);
            case 2 -> new Color(p, brightness, t, alpha);
            case 3 -> new Color(p, q, brightness, alpha);
            case 4 -> new Color(t, p, brightness, alpha);
            default -> new Color(brightness, p, q, alpha);
        };
    }

    public static Color fromHsb(float hue, float saturation, float brightness) {
        return fromHsb(hue, saturation, brightness, 1.0f);
    }

    public static Color fromColor4f(com.example.render.util.Color4f c) {
        if (c == null) return WHITE;
        return c;
    }

    public int aInt() { return (argb >>> 24) & 0xFF; }
    public int rInt() { return (argb >>> 16) & 0xFF; }
    public int gInt() { return (argb >>> 8) & 0xFF; }
    public int bInt() { return argb & 0xFF; }

    public int argb() { return argb; }
    public int toArgb() { return argb; }
    public int toArgbInt() { return argb; }

    public Color withAlpha(float alpha) {
        return new Color(this.r, this.g, this.b, alpha);
    }

    public Color withAlphaMultiplied(float factor) {
        return new Color(this.r, this.g, this.b, this.a * factor);
    }

    public Color multiplyAlpha(float factor) {
        return withAlphaMultiplied(factor);
    }

    public Color lerp(Color other, float t) {
        return lerp(this, other, t);
    }

    public static Color lerp(Color start, Color end, float t) {
        float clampedT = Math.clamp(t, 0.0f, 1.0f);
        return new Color(
            start.r + (end.r - start.r) * clampedT,
            start.g + (end.g - start.g) * clampedT,
            start.b + (end.b - start.b) * clampedT,
            start.a + (end.a - start.a) * clampedT
        );
    }

    public float[] toHsb() {
        float min = Math.min(r, Math.min(g, b));
        float max = Math.max(r, Math.max(g, b));
        float delta = max - min;
        float h = 0.0f;
        float s = (max > 0.0001f) ? delta / max : 0.0f;
        float v = max;

        if (delta > 0.0001f) {
            if (max == r) {
                h = (g - b) / delta + (g < b ? 6.0f : 0.0f);
            } else if (max == g) {
                h = (b - r) / delta + 2.0f;
            } else {
                h = (r - g) / delta + 4.0f;
            }
            h /= 6.0f;
        }
        return new float[]{h, s, v};
    }

    public com.example.render.util.Color4f toColor4f() {
        if (this instanceof com.example.render.util.Color4f c) {
            return c;
        }
        return new com.example.render.util.Color4f(this.r, this.g, this.b, this.a);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Color other)) return false;
        return this.argb == other.argb;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(argb);
    }

    @Override
    public String toString() {
        return String.format("Color[r=%.2f, g=%.2f, b=%.2f, a=%.2f, #%08X]", r, g, b, a, argb);
    }
}
