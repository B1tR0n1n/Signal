package io.github.b1tr0n1n.signal.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authored radio lines for milestone one. Hard-coded on purpose: a data-driven loader is planned
 * (handoff section 25) but is not claimed until its loader and validation exist.
 */
public final class RadioCatalog {
    public static final String CHANNEL_BELLWETHER = "bellwether_maintenance";

    public static final RadioMessage OPENING = new RadioMessage(
            "bellwether/opening", CHANNEL_BELLWETHER, "nia", "radio.signal.bellwether.opening");
    public static final RadioMessage ARRIVAL = new RadioMessage(
            "bellwether/arrival", CHANNEL_BELLWETHER, "nia", "radio.signal.bellwether.arrival");
    public static final RadioMessage LOCAL_RESTORED = new RadioMessage(
            "bellwether/local_restored", CHANNEL_BELLWETHER, "nia", "radio.signal.bellwether.local_restored");

    private static final Map<String, RadioMessage> BY_ID = new LinkedHashMap<>();

    static {
        for (RadioMessage m : new RadioMessage[]{OPENING, ARRIVAL, LOCAL_RESTORED}) {
            BY_ID.put(m.id(), m);
        }
    }

    private RadioCatalog() {
    }

    public static Optional<RadioMessage> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
