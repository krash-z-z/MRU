package com.example.render.setting;

public class BooleanSetting extends ModuleSetting<Boolean> {
    public BooleanSetting(String name, boolean initialValue) {
        super(name, initialValue);
    }

    public void toggle() {
        setValue(!getValue());
    }

    @Override
    public String displayValue() {
        return value ? "ON" : "OFF";
    }
}
