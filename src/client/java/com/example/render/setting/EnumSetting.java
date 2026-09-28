package com.example.render.setting;

import java.util.function.Function;

public class EnumSetting<T extends Enum<T>> extends ModuleSetting<T> {
    private final T[] values;
    private final Function<T, String> label;

    public EnumSetting(String name, T[] values, T initialValue) {
        this(name, values, initialValue, Enum::name);
    }

    public EnumSetting(String name, T[] values, T initialValue, Function<T, String> label) {
        super(name, initialValue);
        this.values = values;
        this.label = label != null ? label : Enum::name;
    }

    public T[] getValues() {
        return values;
    }

    public int getIndex() {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) return i;
        }
        return 0;
    }

    public void selectIndex(int index) {
        if (index >= 0 && index < values.length) {
            setValue(values[index]);
        }
    }

    public void cycle() {
        selectIndex((getIndex() + 1) % values.length);
    }

    public String optionDisplay(int index) {
        if (index >= 0 && index < values.length) {
            return label.apply(values[index]);
        }
        return "";
    }

    @Override
    public String displayValue() {
        return label.apply(value);
    }
}
