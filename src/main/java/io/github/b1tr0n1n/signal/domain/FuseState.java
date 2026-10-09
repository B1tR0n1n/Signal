package io.github.b1tr0n1n.signal.domain;

public enum FuseState {
    /** Main fuse F1 overheated; Nia isolated the supply before it failed outright. */
    DAMAGED("damaged"),
    /** Replacement fuse fitted. */
    INSTALLED("installed");

    private final String id;

    FuseState(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static FuseState byId(String id) {
        for (FuseState s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown fuse state: " + id);
    }
}
