package org.dave.CompactMachines.integration;

import net.minecraft.util.StatCollector;

public enum HoppingMode {
    Disabled('d', "disabled"),
    Import('I', "importing"),
    Export('E', "exporting"),
    Auto('A', "auto"),
    Null('N', "null");

    public final char initial;
    public final String key;
    
    HoppingMode(char initial, String key) {
        this.initial = initial;
        this.key = key;
    }

    public final String getLocalizedName() {
        return StatCollector.translateToLocal("container.cm:hoppingMode." + key);
    }
    
    public static HoppingMode fromInteger(int i) {
        return values()[i];
    }

    public HoppingMode next() {
        if(this == Null) return this;
        else return fromInteger(ordinal() + 1);
    }
};
