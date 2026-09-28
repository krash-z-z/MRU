package com.example.render.animation;

public final class AnimationUtil {
    public static final AnimationUtil INSTANCE = new AnimationUtil();

    private AnimationUtil() {}

    @FunctionalInterface
    public interface SimpleEasing {
        float apply(float value);
    }

    public enum Mode {
        LINEAR,
        FADE,
        BOUNCE,
        BACK_IN,
        EXPONENTIAL,
        ELASTIC
    }

    public float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp01(progress);
    }

    public float apply(Mode mode, float value) {
        float t = clamp01(value);
        return switch (mode) {
            case LINEAR -> t;
            case FADE -> t * t * (3.0f - 2.0f * t);
            case BOUNCE -> easeOutBack(t);
            case BACK_IN -> easeInBack(t);
            case EXPONENTIAL -> (t >= 1.0f) ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * t);
            case ELASTIC -> easeOutElastic(t);
        };
    }

    public float easeOutBack(float value) {
        float t = clamp01(value);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1.0f, 3) + c1 * (float) Math.pow(t - 1.0f, 2);
    }

    public float easeOutSoftBack(float value) {
        float t = clamp01(value);
        float c1 = 0.72f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1.0f, 3) + c1 * (float) Math.pow(t - 1.0f, 2);
    }

    public float easeOutQuad(float value) {
        float t = clamp01(value);
        return t * (2.0f - t);
    }

    public float easeInBack(float value) {
        float t = clamp01(value);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return c3 * t * t * t - c1 * t * t;
    }

    public float easeInSine(float value) {
        float t = clamp01(value);
        return 1.0f - (float) Math.cos(t * (float) Math.PI * 0.5f);
    }

    public float easeOutSine(float value) {
        float t = clamp01(value);
        return (float) Math.sin(t * (float) Math.PI * 0.5f);
    }

    public float easeOutElastic(float value) {
        float t = clamp01(value);
        if (t <= 0.0f) return 0.0f;
        if (t >= 1.0f) return 1.0f;
        float p = 0.3f;
        return (float) Math.pow(2.0, -10.0 * t) * (float) Math.sin((t - p / 4.0f) * (2.0 * Math.PI) / p) + 1.0f;
    }

    public static class TimedAnimation {
        private float value;
        private float from;
        private float target;
        private long startMillis = 0L;
        private long durationMillis = 1L;
        private SimpleEasing easing = v -> v;

        public TimedAnimation() {
            this(0.0f);
        }

        public TimedAnimation(float initialValue) {
            this.value = initialValue;
            this.from = initialValue;
            this.target = initialValue;
        }

        public float getValue() {
            return value;
        }

        public void snap(float next) {
            value = next;
            from = next;
            target = next;
            startMillis = 0L;
            durationMillis = 1L;
            easing = v -> v;
        }

        public void run(float next, long duration, SimpleEasing easing) {
            run(next, duration, easing, false);
        }

        public void run(float next, long duration, SimpleEasing easing, boolean safe) {
            if (safe && isAlive() && next == target) {
                return;
            }
            from = value;
            target = next;
            startMillis = System.currentTimeMillis();
            durationMillis = Math.max(1L, duration);
            this.easing = easing;
        }

        public boolean update() {
            boolean alive = isAlive();
            if (alive) {
                float part = AnimationUtil.INSTANCE.clamp01((float) (System.currentTimeMillis() - startMillis) / (float) durationMillis);
                value = from + (target - from) * easing.apply(part);
            } else {
                startMillis = 0L;
                value = target;
            }
            return alive;
        }

        public boolean isAlive() {
            return startMillis > 0L && System.currentTimeMillis() - startMillis < durationMillis;
        }
    }
}
