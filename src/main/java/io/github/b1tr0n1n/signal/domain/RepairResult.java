package io.github.b1tr0n1n.signal.domain;

public enum RepairResult {
    /** Fuse fitted. The caller must consume exactly one replacement fuse. */
    INSTALLED,
    /** Already repaired; nothing consumed. Covers duplicate clicks and duplicate packets. */
    ALREADY_INSTALLED,
    /** The supply is live; working on it is refused. Nothing consumed. */
    SUPPLY_LIVE,
    /** The player has no replacement fuse. */
    NO_FUSE,
    STALE_REVISION
}
