package io.github.b1tr0n1n.signal.domain;

import java.util.Optional;

/**
 * Loads on the Bellwether shared bus. Demands are fictional capacity units, not watts.
 * The string id is the stable persistence/network identity; never persist the ordinal.
 */
public enum Service {
    PUMP("pump", 5),
    LOCAL_TRANSMITTER("local_transmitter", 1),
    REGIONAL_RELAY("regional_relay", 3);

    private final String id;
    private final int demand;

    Service(String id, int demand) {
        this.id = id;
        this.demand = demand;
    }

    public String id() {
        return id;
    }

    public int demand() {
        return demand;
    }

    public static Optional<Service> byId(String id) {
        for (Service s : values()) {
            if (s.id.equals(id)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
