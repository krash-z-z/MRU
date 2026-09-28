package com.example.render.setting;

import com.example.render.util.Color4f;

public class ColorSetting extends ModuleSetting<Color4f> {

    public ColorSetting(String name, Color4f initialValue) {
        super(name, initialValue != null ? initialValue : new Color4f(1.0f, 1.0f, 1.0f, 1.0f));
    }

    public ColorSetting(String name, int argbInt) {
        this(name, Color4f.fromArgbInt(argbInt));
    }

    public ColorSetting(String name, float r, float g, float b, float a) {
        this(name, new Color4f(r, g, b, a));
    }

    public int getArgbInt() {
        return getValue().toArgbInt();
    }

    public void setArgbInt(int argb) {
        setValue(Color4f.fromArgbInt(argb));
    }

    public String getHex() {
        Color4f c = getValue();
        int r = Math.round(Math.clamp(c.r, 0.0f, 1.0f) * 255.0f);
        int g = Math.round(Math.clamp(c.g, 0.0f, 1.0f) * 255.0f);
        int b = Math.round(Math.clamp(c.b, 0.0f, 1.0f) * 255.0f);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    @Override
    public String displayValue() {
        return getHex();
    }
}
