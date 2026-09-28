package com.example.render.gradient;

import com.example.render.util.Color4f;

/**
 * Represents a color stop along a gradient timeline.
 */
public class GradientStop implements Comparable<GradientStop> {
    public final float position;
    public final Color4f color;

    public GradientStop(float position, Color4f color) {
        this.position = Math.clamp(position, 0.0f, 1.0f);
        this.color = color != null ? color : Color4f.WHITE;
    }

    public static GradientStop of(float position, Color4f color) {
        return new GradientStop(position, color);
    }

    public static GradientStop of(float position, int argb) {
        return new GradientStop(position, Color4f.fromArgb(argb));
    }

    @Override
    public int compareTo(GradientStop other) {
        return Float.compare(this.position, other.position);
    }
}
