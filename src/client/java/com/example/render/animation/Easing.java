package com.example.render.animation;

@FunctionalInterface
public interface Easing {
    float ease(float t, float b, float c, float d);

    default float apply(float value) {
        return ease(value, 0.0f, 1.0f, 1.0f);
    }

    static Easing generate(double x1, double y1, double x2, double y2) {
        return generate((float) x1, (float) y1, (float) x2, (float) y2);
    }

    static Easing generate(float x1, float y1, float x2, float y2) {
        return (t, b, c, d) -> {
            if (d <= 0.0001f) return b + c;
            float progress = Math.max(0.0f, Math.min(1.0f, t / d));
            float solvedT = solveCubic(progress, x1, x2);
            float eased = sampleCurve(solvedT, y1, y2);
            return b + c * eased;
        };
    }

    private static float sampleCurve(float t, float p1, float p2) {
        float oneMinusT = 1.0f - t;
        return 3.0f * oneMinusT * oneMinusT * t * p1 + 3.0f * oneMinusT * t * t * p2 + t * t * t;
    }

    private static float sampleCurveDerivative(float t, float p1, float p2) {
        float oneMinusT = 1.0f - t;
        return 3.0f * (oneMinusT * oneMinusT * p1 + 2.0f * oneMinusT * t * (p2 - p1) + t * t * (1.0f - p2));
    }

    private static float solveCubic(float x, float p1, float p2) {
        float t = x;
        for (int i = 0; i < 8; i++) {
            float currentX = sampleCurve(t, p1, p2) - x;
            if (Math.abs(currentX) < 1e-5f) return t;
            float d = sampleCurveDerivative(t, p1, p2);
            if (Math.abs(d) < 1e-6f) break;
            t -= currentX / d;
            t = Math.max(0.0f, Math.min(1.0f, t));
        }
        return t;
    }

    Easing SATISFYING_SPRING = generate(0.20f, 1.26f, 0.15f, 1.0f);
    Easing HUD_APPEAR = generate(0.34f, 1.22f, 0.45f, 1.0f);
    Easing HUD_DISAPPEAR = generate(0.38f, 0.0f, 0.25f, 1.0f);
    Easing BAKEK = generate(0.45f, 1.45f, 0.49f, 1.15f);
    Easing BAKEK_SMALLER = generate(0.45f, 1.45f, 0.43f, 0.91f);
    Easing BAKEK_PAGES = generate(0.1f, 1.07f, 0.34f, 1.04f);
    Easing BAKEK_SIZE = generate(0.27f, 1.09f, 0.49f, 1.06f);
    Easing BAKEK_BACK = generate(0.62f, -0.16f, 0.8f, 0.37f);
    Easing BAKEK_MANY = generate(0.25f, 1.07f, 0.11f, 1.1f);
    Easing SMOOTH_IN_OUT = generate(0.42f, 0.0f, 0.58f, 1.0f);

    Easing LINEAR = (t, b, c, d) -> d <= 0.0001f ? b + c : c * (t / d) + b;
    Easing SINE_IN_OUT = (t, b, c, d) -> -c / 2.0f * ((float) Math.cos(Math.PI * t / d) - 1.0f) + b;
    Easing SINE_IN = (t, b, c, d) -> -c * (float) Math.cos(t / d * (Math.PI / 2.0)) + c + b;
    Easing SINE_OUT = (t, b, c, d) -> c * (float) Math.sin(t / d * (Math.PI / 2.0)) + b;
    Easing CUBIC_IN_OUT = (t, b, c, d) -> {
        float v = t / (d / 2.0f);
        if (v < 1.0f) return c / 2.0f * v * v * v + b;
        v -= 2.0f;
        return c / 2.0f * (v * v * v + 2.0f) + b;
    };
    Easing QUAD_OUT = (t, b, c, d) -> {
        float v = t / d;
        return -c * v * (v - 2.0f) + b;
    };
    Easing QUAD_IN = (t, b, c, d) -> {
        float v = t / d;
        return c * v * v + b;
    };
    Easing QUAD_IN_OUT = (t, b, c, d) -> {
        float v = t / (d / 2.0f);
        if (v < 1.0f) return c / 2.0f * v * v + b;
        return -c / 2.0f * ((--v) * (v - 2.0f) - 1.0f) + b;
    };
    Easing EASE_OUT_BACK = (t, b, c, d) -> {
        float s = 1.70158f;
        float v = t / d - 1.0f;
        return c * (v * v * ((s + 1.0f) * v + s) + 1.0f) + b;
    };
    Easing EASE_IN_BACK = (t, b, c, d) -> {
        float s = 1.70158f;
        float v = t / d;
        return c * v * v * ((s + 1.0f) * v - s) + b;
    };
    Easing EASE_IN_OUT_BACK = (t, b, c, d) -> {
        float s = 1.70158f * 1.525f;
        float v = t / (d / 2.0f);
        if (v < 1.0f) return c / 2.0f * (v * v * ((s + 1.0f) * v - s)) + b;
        v -= 2.0f;
        return c / 2.0f * (v * v * ((s + 1.0f) * v + s) + 2.0f) + b;
    };
    Easing EASE_OUT_BOUNCE = (t, b, c, d) -> {
        float v = t / d;
        if (v < (1.0f / 2.75f)) {
            return c * (7.5625f * v * v) + b;
        } else if (v < (2.0f / 2.75f)) {
            v -= (1.5f / 2.75f);
            return c * (7.5625f * v * v + 0.75f) + b;
        } else if (v < (2.5f / 2.75f)) {
            v -= (2.25f / 2.75f);
            return c * (7.5625f * v * v + 0.9375f) + b;
        } else {
            v -= (2.625f / 2.75f);
            return c * (7.5625f * v * v + 0.984375f) + b;
        }
    };
    Easing EASE_OUT_ELASTIC = (t, b, c, d) -> {
        if (t <= 0.0f) return b;
        float v = t / d;
        if (v >= 1.0f) return b + c;
        float p = d * 0.3f;
        float a = c;
        float s = p / 4.0f;
        return a * (float) Math.pow(2.0, -10.0 * v) * (float) Math.sin((v * d - s) * (2.0 * Math.PI) / p) + c + b;
    };
    Easing EASE_OUT_EXPO = (t, b, c, d) -> (t >= d) ? b + c : c * (-(float) Math.pow(2.0, -10.0 * t / d) + 1.0f) + b;
    Easing EASE_OUT_QUINT = (t, b, c, d) -> {
        float v = t / d - 1.0f;
        return c * (v * v * v * v * v + 1.0f) + b;
    };
}
