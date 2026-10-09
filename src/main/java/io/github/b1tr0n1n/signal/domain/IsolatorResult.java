package io.github.b1tr0n1n.signal.domain;

public enum IsolatorResult {
    CLOSED,
    OPENED,
    ALREADY_CLOSED,
    ALREADY_OPEN,
    /** Closing the isolator onto a damaged fuse is refused. */
    REFUSED_FUSE_DAMAGED
}
