package com.example.render.gradient;

import com.example.render.util.Color4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gradient definition supporting Linear gradient configurations
 * with customizable start/end coordinates, angles, and arbitrary color stops.
 */
public class Gradient {

    public final GradientType type;
    public final List<GradientStop> stops;

    public float startX = 0.0f;
    public float startY = 0.0f;
    public float endX = 1.0f;
    public float endY = 1.0f;
    public float angleDeg = 0.0f;
    public boolean useAngle = false;

    public Gradient(GradientType type, List<GradientStop> stops) {
        this.type = type != null ? type : GradientType.LINEAR;
        List<GradientStop> sorted = new ArrayList<>(stops != null ? stops : List.of());
        if (sorted.isEmpty()) {
            sorted.add(new GradientStop(0f, Color4f.WHITE));
            sorted.add(new GradientStop(1f, Color4f.BLACK));
        } else if (sorted.size() == 1) {
            sorted.add(new GradientStop(1f, sorted.get(0).color));
        }
        Collections.sort(sorted);
        this.stops = Collections.unmodifiableList(sorted);
    }

    public static Gradient linear(float startX, float startY, float endX, float endY, GradientStop... stops) {
        Gradient g = new Gradient(GradientType.LINEAR, List.of(stops));
        g.startX = startX;
        g.startY = startY;
        g.endX = endX;
        g.endY = endY;
        g.useAngle = false;
        return g;
    }

    public static Gradient linearAngle(float angleDegrees, GradientStop... stops) {
        Gradient g = new Gradient(GradientType.LINEAR, List.of(stops));
        g.angleDeg = angleDegrees;
        g.useAngle = true;
        return g;
    }

    public Gradient withAngle(float angleDegrees) {
        Gradient g = new Gradient(this.type, this.stops);
        g.startX = this.startX;
        g.startY = this.startY;
        g.endX = this.endX;
        g.endY = this.endY;
        g.angleDeg = angleDegrees;
        g.useAngle = true;
        return g;
    }

    public Gradient withRotation(float speedDegPerTick) {
        return GradientRotator.rotate(this, speedDegPerTick);
    }

    public static Gradient flowing(float baseAngle, float speedDegPerTick, Color4f startColor, Color4f endColor) {
        return builder(GradientType.LINEAR)
            .angle(GradientRotator.calculateAngle(baseAngle, speedDegPerTick))
            .addStop(0.0f, startColor)
            .addStop(1.0f, endColor)
            .build();
    }

    public Color4f evaluate(float u, float v) {
        return evaluate(u, v, 1.0f, 1.0f);
    }

    public Color4f evaluate(float u, float v, float width, float height) {
        float t;
        if (useAngle) {
            float rad = (float) Math.toRadians(angleDeg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float maxProj = 0.5f * (width * Math.abs(cos) + height * Math.abs(sin));
            if (maxProj < 0.00001f) {
                t = 0.5f;
            } else {
                float proj = (u - 0.5f) * width * cos + (v - 0.5f) * height * sin;
                t = 0.5f + proj / (2.0f * maxProj);
            }
        } else {
            float dx = endX - startX;
            float dy = endY - startY;
            float lenSq = dx * dx + dy * dy;
            if (lenSq < 0.00001f) {
                t = 0.5f;
            } else {
                float pu = u - startX;
                float pv = v - startY;
                t = (pu * dx + pv * dy) / lenSq;
            }
        }

        t = Math.clamp(t, 0.0f, 1.0f);
        return sampleTimeline(t);
    }

    public Color4f evaluateAt(float px, float py, float rectX, float rectY, float rectW, float rectH) {
        float u = rectW > 0 ? (px - rectX) / rectW : 0f;
        float v = rectH > 0 ? (py - rectY) / rectH : 0f;
        return evaluate(u, v, rectW, rectH);
    }

    public Color4f sampleTimeline(float t) {
        int n = stops.size();
        if (n == 0) return Color4f.WHITE;
        if (t <= stops.get(0).position) return stops.get(0).color;
        if (t >= stops.get(n - 1).position) return stops.get(n - 1).color;

        for (int i = 0; i < n - 1; i++) {
            GradientStop s0 = stops.get(i);
            GradientStop s1 = stops.get(i + 1);
            if (t >= s0.position && t <= s1.position) {
                float range = s1.position - s0.position;
                float localT = range > 0.00001f ? (t - s0.position) / range : 0f;
                return Color4f.lerp(s0.color, s1.color, localT);
            }
        }
        return stops.get(n - 1).color;
    }

    public static Builder builder(GradientType type) {
        return new Builder(type);
    }

    public static Builder linearBuilder() {
        return new Builder(GradientType.LINEAR);
    }

    public static class Builder {
        private final GradientType type;
        private final List<GradientStop> stops = new ArrayList<>();
        private float startX = 0f, startY = 0f, endX = 1f, endY = 1f;
        private float angleDeg = 0f;
        private boolean useAngle = false;

        public Builder(GradientType type) {
            this.type = type != null ? type : GradientType.LINEAR;
        }

        public Builder start(float x, float y) {
            this.startX = x;
            this.startY = y;
            this.useAngle = false;
            return this;
        }

        public Builder end(float x, float y) {
            this.endX = x;
            this.endY = y;
            this.useAngle = false;
            return this;
        }

        public Builder angle(float degrees) {
            this.angleDeg = degrees;
            this.useAngle = true;
            return this;
        }

        public Builder addStop(float position, Color4f color) {
            this.stops.add(new GradientStop(position, color));
            return this;
        }

        public Builder addStop(float position, int argb) {
            this.stops.add(GradientStop.of(position, argb));
            return this;
        }

        public Gradient build() {
            Gradient g = new Gradient(this.type, this.stops);
            g.startX = this.startX;
            g.startY = this.startY;
            g.endX = this.endX;
            g.endY = this.endY;
            g.angleDeg = this.angleDeg;
            g.useAngle = this.useAngle;
            return g;
        }
    }
}
