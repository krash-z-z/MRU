package com.example.render.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class ModuleSetting<T> {
    protected final String name;
    protected final T defaultValue;
    protected T value;
    protected final List<Consumer<T>> listeners = new ArrayList<>();

    public ModuleSetting(String name, T initialValue) {
        this.name = name;
        this.defaultValue = initialValue;
        this.value = initialValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
    }

    public void addListener(Consumer<T> listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void reset() {
        setValue(defaultValue);
    }

    public abstract String displayValue();
}
