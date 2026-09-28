package com.example.render.setting;

import java.util.Locale;

public class FloatSetting extends ModuleSetting<Float> {
    private final float min;
    private final float max;
    private final float step;
    private final String suffix;

    public FloatSetting(String name, float initialValue, float min, float max, float step, String suffix) {
        super(name, Math.clamp(initialValue, min, max));
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix != null ? suffix : "";
    }

    public FloatSetting(String name, float initialValue, float min, float max, float step) {
        this(name, initialValue, min, max, step, "");
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    public String getSuffix() {
        return suffix;
    }

    public float getNormalized() {
        float range = max - min;
        return range <= 0.0001f ? 0.0f : Math.clamp((value - min) / range, 0.0f, 1.0f);
    }

    public void setByPercent(float percent) {
        float pct = Math.clamp(percent, 0.0f, 1.0f);
        float raw = min + (max - min) * pct;
        if (step > 0.0f) {
            raw = Math.round((raw - min) / step) * step + min;
        }
        setValue(Math.clamp(raw, min, max));
    }

    @Override
    public String displayValue() {
        if (step >= 1.0f) {
            return String.format(Locale.ROOT, "%d%s", Math.round(value), suffix);
        }
        return String.format(Locale.ROOT, "%.1f%s", value, suffix);
    }
}
