package com.example.render.animation;

public class Animation {
    private long duration;
    private float value;
    private Easing easing;
    private long startTime;
    private float startValue;
    private float targetValue;
    private boolean done;

    public Animation(long duration, float initialValue, Easing easing) {
        this.duration = Math.max(1L, duration);
        this.easing = easing != null ? easing : Easing.SMOOTH_IN_OUT;
        this.value = initialValue;
        this.startValue = initialValue;
        this.targetValue = initialValue;
        this.done = true;
    }

    public Animation(long duration, Easing easing) {
        this(duration, 0.0f, easing);
    }

    public void update(boolean bool) {
        update(bool ? 1.0f : 0.0f);
    }

    public float update(float newValue) {
        long currentTime = System.currentTimeMillis();
        if (Float.compare(newValue, this.targetValue) != 0) {
            this.startValue = this.value;
            this.targetValue = newValue;
            this.startTime = currentTime;
            this.done = false;
        }

        long elapsed = currentTime - this.startTime;
        if (elapsed >= this.duration) {
            this.value = this.targetValue;
            this.done = true;
            return this.value;
        } else {
            float progress = (float) elapsed / (float) this.duration;
            float easedProgress = this.easing.ease(progress, 0.0f, 1.0f, 1.0f);
            this.value = this.startValue + (this.targetValue - this.startValue) * easedProgress;
            return this.value;
        }
    }

    public void setValue(float newValue) {
        this.value = newValue;
        this.startValue = newValue;
        this.targetValue = newValue;
        this.done = true;
    }

    public void reset(float initialValue) {
        this.value = initialValue;
        this.startValue = initialValue;
        this.targetValue = initialValue;
        this.done = true;
    }

    public void reset() {
        reset(0.0f);
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = Math.max(1L, duration);
    }

    public float getValue() {
        return value;
    }

    public Easing getEasing() {
        return easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing != null ? easing : Easing.SMOOTH_IN_OUT;
    }

    public boolean isDone() {
        return done;
    }

    public float getTargetValue() {
        return targetValue;
    }

    public float getLinearProgress() {
        if (this.done) return 1.0f;
        long elapsed = System.currentTimeMillis() - this.startTime;
        if (elapsed >= this.duration) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) elapsed / (float) this.duration));
    }
}
