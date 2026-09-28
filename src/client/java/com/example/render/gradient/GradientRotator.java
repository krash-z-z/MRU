package com.example.render.gradient;

/**
 * GradientRotator: Manages dynamic rotation for linear gradients.
 * Operates at 1.0 degree per tick (20 ticks/sec = 50ms/tick) by default,
 * with customizable speed and smooth frame-rate independent rotation.
 */
public class GradientRotator {

    public static final float DEFAULT_SPEED = 1.0f; // 1 degree per tick
    public static final float MS_PER_TICK = 50.0f;

    private boolean enabled = true;
    private float speed = DEFAULT_SPEED;
    private float baseAngle = 0.0f;

    public GradientRotator() {
        this(true, DEFAULT_SPEED, 0.0f);
    }

    public GradientRotator(float speed) {
        this(true, speed, 0.0f);
    }

    public GradientRotator(boolean enabled, float speed, float baseAngle) {
        this.enabled = enabled;
        this.speed = speed;
        this.baseAngle = baseAngle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getBaseAngle() {
        return baseAngle;
    }

    public void setBaseAngle(float baseAngle) {
        this.baseAngle = baseAngle;
    }

    /**
     * Calculates the current angle based on elapsed system time.
     */
    public float getCurrentAngle() {
        return calculateAngle(baseAngle, enabled ? speed : 0.0f);
    }

    /**
     * Applies this rotator to a base gradient, returning a rotated gradient copy.
     */
    public Gradient apply(Gradient baseGradient) {
        if (baseGradient == null) return null;
        if (!enabled || Math.abs(speed) < 0.0001f) {
            return baseGradient.withAngle(baseAngle);
        }
        return baseGradient.withAngle(getCurrentAngle());
    }

    /**
     * Static helper: Calculates rotating angle given a base angle and rotation speed (degrees per tick).
     */
    public static float calculateAngle(float baseAngle, float speedDegPerTick) {
        if (Math.abs(speedDegPerTick) < 0.00001f) {
            return normalizeAngle(baseAngle);
        }
        double timeTicks = (double) System.currentTimeMillis() / MS_PER_TICK;
        double currentAngle = (double) baseAngle + timeTicks * (double) speedDegPerTick;
        return normalizeAngle((float) currentAngle);
    }

    /**
     * Static helper: Rotates any Gradient dynamically using the specified speed in degrees per tick.
     */
    public static Gradient rotate(Gradient baseGradient, float baseAngle, float speedDegPerTick) {
        if (baseGradient == null) return null;
        float angle = calculateAngle(baseAngle, speedDegPerTick);
        return baseGradient.withAngle(angle);
    }

    /**
     * Static helper: Rotates any Gradient dynamically using its internal angle as base.
     */
    public static Gradient rotate(Gradient baseGradient, float speedDegPerTick) {
        if (baseGradient == null) return null;
        return rotate(baseGradient, baseGradient.angleDeg, speedDegPerTick);
    }

    /**
     * Advances an angle value by the specified speed per tick based on elapsed milliseconds.
     */
    public static float tickAngle(float currentAngle, float speedDegPerTick, long elapsedMs) {
        if (elapsedMs <= 0L || Math.abs(speedDegPerTick) < 0.00001f) {
            return normalizeAngle(currentAngle);
        }
        float deltaTicks = (float) elapsedMs / MS_PER_TICK;
        return normalizeAngle(currentAngle + deltaTicks * speedDegPerTick);
    }

    /**
     * Advances an angle value by the specified speed for a single tick.
     */
    public static float tickAngle(float currentAngle, float speedDegPerTick) {
        return normalizeAngle(currentAngle + speedDegPerTick);
    }

    /**
     * Normalizes an angle into the [0, 360) range.
     */
    public static float normalizeAngle(float angle) {
        float mod = angle % 360.0f;
        if (mod < 0.0f) {
            mod += 360.0f;
        }
        return mod;
    }
}
