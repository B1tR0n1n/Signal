package io.github.b1tr0n1n.signal.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Plain-language explanation of a proposed allocation, computed from the same rules the server applies.
 * The client may compute this for display; the server always recomputes it before applying.
 *
 * @param requested       services in the proposal
 * @param required        sum of their demands
 * @param capacity        nominal capacity of the bus (generator rating)
 * @param available       capacity actually reaching the bus right now (0 while isolated or faulted)
 * @param valid           whether the proposal may be applied
 * @param busLive         whether the services would run immediately after apply
 * @param relayWithoutRoute the proposal powers the relay but its antenna is not aligned, so it reaches nobody
 */
public record AllocationPreview(Set<Service> requested, int required, int capacity, int available,
                                boolean valid, boolean busLive, boolean relayWithoutRoute) {

    public AllocationPreview {
        requested = requested.isEmpty() ? EnumSet.noneOf(Service.class) : EnumSet.copyOf(requested);
    }

    public int shortfall() {
        return Math.max(0, required - capacity);
    }
}
