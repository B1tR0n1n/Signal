package io.github.b1tr0n1n.signal.block;

import net.minecraft.util.StringRepresentable;

public enum RelayState implements StringRepresentable {
    /** No power. */
    OFF("off"),
    /** Powered carrier, but no route (antenna misaligned). */
    POWERED("powered"),
    /** Powered and aligned: regional reachability. */
    LINKED("linked");

    private final String name;

    RelayState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
