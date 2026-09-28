package com.example.render.util;

import com.example.render.context.Color;

/**
 * High performance float-based RGBA color representation extending unified {@link Color}.
 */
public class Color4f extends Color {
    public static final Color4f WHITE = new Color4f(1f, 1f, 1f, 1f);
    public static final Color4f BLACK = new Color4f(0f, 0f, 0f, 1f);
    public static final Color4f TRANSPARENT = new Color4f(0f, 0f, 0f, 0f);

    public Color4f(float r, float g, float b, float a) {
        super(r, g, b, a);
    }

    public Color4f(float r, float g, float b) {
        super(r, g, b, 1.0f);
    }

    public Color4f(Color other) {
        super(other != null ? other.r : 1f, other != null ? other.g : 1f, other != null ? other.b : 1f, other != null ? other.a : 1f);
    }

    public static Color4f fromArgbInt(int argb) {
        return fromArgb(argb);
    }

    public static Color4f fromArgb(int argb) {
        Color c = Color.fromArgb(argb);
        return new Color4f(c.r, c.g, c.b, c.a);
    }

    public static Color4f fromRgba(int rgba) {
        Color c = Color.fromRgba(rgba);
        return new Color4f(c.r, c.g, c.b, c.a);
    }

    public static Color4f fromHsb(float hue, float saturation, float brightness, float alpha) {
        Color c = Color.fromHsb(hue, saturation, brightness, alpha);
        return new Color4f(c.r, c.g, c.b, c.a);
    }

    public static Color4f fromHsb(float hue, float saturation, float brightness) {
        return fromHsb(hue, saturation, brightness, 1.0f);
    }

    @Override
    public Color4f withAlpha(float alpha) {
        return new Color4f(this.r, this.g, this.b, alpha);
    }

    @Override
    public Color4f multiplyAlpha(float factor) {
        return new Color4f(this.r, this.g, this.b, this.a * factor);
    }

    public static Color4f lerp(Color4f start, Color4f end, float t) {
        Color c = Color.lerp(start, end, t);
        return new Color4f(c.r, c.g, c.b, c.a);
    }
}
