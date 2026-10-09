package io.github.b1tr0n1n.signal.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The authored shared bus at the Bellwether relay: one generator feeding the pump, the local
 * transmitter and the regional relay through one main fuse and one isolator.
 *
 * <p>This is deliberately <em>not</em> a generic graph solver. Every load hangs off a single bus.
 * Richer topology arrives with a second site.
 *
 * <p>Power, configuration and communication reachability are kept separate:
 * <ul>
 *   <li>{@link #configured()} is what the breakers are set to (the approved allocation);</li>
 *   <li>{@link #isRunning(Service)} is whether a load actually has power;</li>
 *   <li>{@link #relayReachable()} additionally requires antenna alignment.</li>
 * </ul>
 *
 * <p>Every successful mutation increments {@link #revision()} so stale UI submissions can be rejected.
 */
public final class BellwetherBus {
    /** Generator rating in capacity units. */
    public static final int GENERATOR_SUPPLY = 6;

    private FuseState fuse;
    private boolean isolated;
    private final EnumSet<Service> configured;
    private boolean relayAligned;
    private int revision;

    /** Fresh scenario state: fuse damaged, supply isolated by Nia, all breakers open, antenna misaligned. */
    public BellwetherBus() {
        this(FuseState.DAMAGED, true, EnumSet.noneOf(Service.class), false, 0);
    }

    /** Restore persisted state. */
    public BellwetherBus(FuseState fuse, boolean isolated, Set<Service> configured, boolean relayAligned, int revision) {
        this.fuse = fuse;
        this.isolated = isolated;
        this.configured = configured.isEmpty() ? EnumSet.noneOf(Service.class) : EnumSet.copyOf(configured);
        this.relayAligned = relayAligned;
        this.revision = revision;
    }

    public FuseState fuse() {
        return fuse;
    }

    public boolean isolated() {
        return isolated;
    }

    public boolean relayAligned() {
        return relayAligned;
    }

    public int revision() {
        return revision;
    }

    public Set<Service> configured() {
        return Collections.unmodifiableSet(configured);
    }

    /** Power reaches the bus only through a good fuse and a closed isolator. */
    public boolean busLive() {
        return fuse == FuseState.INSTALLED && !isolated;
    }

    public int availableSupply() {
        return busLive() ? GENERATOR_SUPPLY : 0;
    }

    public int configuredDemand() {
        return demandOf(configured);
    }

    public boolean isRunning(Service service) {
        return busLive() && configured.contains(service);
    }

    /** A powered relay still reaches nobody without antenna alignment. Route policy arrives in M2. */
    public boolean relayReachable() {
        return isRunning(Service.REGIONAL_RELAY) && relayAligned;
    }

    public static int demandOf(Set<Service> services) {
        int total = 0;
        for (Service s : services) {
            total += s.demand();
        }
        return total;
    }

    public AllocationPreview preview(Set<Service> requested) {
        int required = demandOf(requested);
        boolean valid = required <= GENERATOR_SUPPLY;
        boolean relayWithoutRoute = requested.contains(Service.REGIONAL_RELAY) && !relayAligned;
        return new AllocationPreview(requested, required, GENERATOR_SUPPLY, availableSupply(), valid, busLive(), relayWithoutRoute);
    }

    /**
     * Apply a breaker configuration. Invalid allocations are prevented rather than tripped.
     * Configuration is allowed while isolated: breakers can be set before the isolator is closed.
     */
    public ApplyResult apply(Set<Service> requested, int expectedRevision) {
        if (expectedRevision != revision) {
            return ApplyResult.STALE_REVISION;
        }
        if (!preview(requested).valid()) {
            return ApplyResult.OVER_CAPACITY;
        }
        if (configured.equals(requested.isEmpty() ? EnumSet.noneOf(Service.class) : EnumSet.copyOf(requested))) {
            return ApplyResult.UNCHANGED;
        }
        configured.clear();
        configured.addAll(requested);
        revision++;
        return ApplyResult.APPLIED;
    }

    /**
     * Fit the replacement fuse. Only {@link RepairResult#INSTALLED} means a fuse should be consumed.
     *
     * @param hasFuse whether the acting player holds a replacement fuse (checked by the caller against the real inventory)
     */
    public RepairResult installFuse(boolean hasFuse, int expectedRevision) {
        if (fuse == FuseState.INSTALLED) {
            return RepairResult.ALREADY_INSTALLED;
        }
        if (expectedRevision != revision) {
            return RepairResult.STALE_REVISION;
        }
        if (!isolated) {
            return RepairResult.SUPPLY_LIVE;
        }
        if (!hasFuse) {
            return RepairResult.NO_FUSE;
        }
        fuse = FuseState.INSTALLED;
        revision++;
        return RepairResult.INSTALLED;
    }

    public IsolatorResult closeIsolator() {
        if (!isolated) {
            return IsolatorResult.ALREADY_CLOSED;
        }
        if (fuse != FuseState.INSTALLED) {
            return IsolatorResult.REFUSED_FUSE_DAMAGED;
        }
        isolated = false;
        revision++;
        return IsolatorResult.CLOSED;
    }

    public IsolatorResult openIsolator() {
        if (isolated) {
            return IsolatorResult.ALREADY_OPEN;
        }
        isolated = true;
        revision++;
        return IsolatorResult.OPENED;
    }

    /** Alignment control is M2 work; exposed so tests and later content can drive it. */
    public void setRelayAligned(boolean aligned) {
        if (aligned != relayAligned) {
            relayAligned = aligned;
            revision++;
        }
    }
}
