package com.example.render.setting;

import java.util.ArrayList;
import java.util.List;

public class MultiBooleanSetting extends ModuleSetting<List<BooleanSetting>> {

    public MultiBooleanSetting(String name, BooleanSetting... options) {
        super(name, new ArrayList<>(List.of(options)));
    }

    public MultiBooleanSetting(String name, List<BooleanSetting> options) {
        super(name, new ArrayList<>(options));
    }

    public List<BooleanSetting> getOptions() {
        return value;
    }

    public boolean isEnabled(String optionName) {
        for (BooleanSetting b : value) {
            if (b.getName().equalsIgnoreCase(optionName)) {
                return b.getValue();
            }
        }
        return false;
    }

    public void toggle(String optionName) {
        for (BooleanSetting b : value) {
            if (b.getName().equalsIgnoreCase(optionName)) {
                b.toggle();
                setValue(value);
                return;
            }
        }
    }

    public int getEnabledCount() {
        int count = 0;
        for (BooleanSetting b : value) {
            if (b.getValue()) count++;
        }
        return count;
    }

    @Override
    public String displayValue() {
        return getEnabledCount() + " of " + value.size();
    }
}
