package io.github.b1tr0n1n.signal.domain;

/**
 * A canonical, successful narrative act. Failed actions and debug operations are never recorded.
 *
 * @param sequence monotonically increasing per campaign
 * @param type     stable event type, e.g. {@code fuse_replaced}
 * @param actor    stable character id ({@code elias} for the player)
 * @param target   stable node or character id
 * @param payload  short machine-readable detail (e.g. a service list); never translated text
 */
public record HistoryEvent(long sequence, String type, String actor, String target, String payload) {
}
