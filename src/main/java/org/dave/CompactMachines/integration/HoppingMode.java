package org.dave.CompactMachines.integration;

public enum HoppingMode {
    Disabled,
    Import,
    Export,
    Auto,
    Null;

    public static HoppingMode fromInteger(int i) {
        return values()[i];
    }

    public HoppingMode next() {
        if(this == Null) return this;
        else return fromInteger(ordinal() + 1);
    }
};
