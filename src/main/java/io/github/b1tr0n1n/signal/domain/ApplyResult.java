package io.github.b1tr0n1n.signal.domain;

public enum ApplyResult {
    APPLIED,
    /** Requested set equals the current configuration; nothing changed and no revision is consumed. */
    UNCHANGED,
    /** The panel was opened against an older revision; the client must refresh before applying. */
    STALE_REVISION,
    /** Requested demand exceeds the bus capacity. */
    OVER_CAPACITY
}
