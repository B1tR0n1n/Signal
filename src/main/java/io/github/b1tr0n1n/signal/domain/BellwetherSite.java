package io.github.b1tr0n1n.signal.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * The milestone-one campaign rules for the Bellwether relay: the shared bus, its radio consequences,
 * and the canonical history of successful acts. Pure Java; the Minecraft layer persists and presents it.
 *
 * <p>Every operation returns the radio messages it newly delivered so the caller can present them
 * exactly once.
 */
public final class BellwetherSite {
    public static final String ACTOR_PLAYER = "elias";

    public static final String NODE_GENERATOR = "bellwether/generator";
    public static final String NODE_PANEL = "bellwether/distribution_panel";
    public static final String NODE_PUMP = "bellwether/pump";
    public static final String NODE_RELAY = "bellwether/relay";
    public static final String NODE_LOCAL_TX = "bellwether/local_transmitter";

    private final BellwetherBus bus;
    private final RadioLog radio;
    private final CampaignHistory history;

    public BellwetherSite() {
        this(new BellwetherBus(), new RadioLog(), new CampaignHistory());
    }

    public BellwetherSite(BellwetherBus bus, RadioLog radio, CampaignHistory history) {
        this.bus = bus;
        this.radio = radio;
        this.history = history;
    }

    public BellwetherBus bus() {
        return bus;
    }

    public RadioLog radio() {
        return radio;
    }

    public CampaignHistory history() {
        return history;
    }

    /** Called once when the scenario is created. */
    public List<RadioMessage> begin() {
        List<RadioMessage> out = new ArrayList<>();
        if (!history.contains("scenario_started")) {
            history.record("scenario_started", ACTOR_PLAYER, "bellwether", "");
        }
        deliver(RadioCatalog.OPENING, out);
        return out;
    }

    /** First time the player reaches the facility. Purely presentational dialogue; no state changes. */
    public List<RadioMessage> arrive() {
        List<RadioMessage> out = new ArrayList<>();
        deliver(RadioCatalog.ARRIVAL, out);
        return out;
    }

    public Outcome<RepairResult> installFuse(boolean hasFuse, int expectedRevision) {
        RepairResult r = bus.installFuse(hasFuse, expectedRevision);
        if (r == RepairResult.INSTALLED) {
            history.record("fuse_replaced", ACTOR_PLAYER, NODE_PANEL, "F1");
        }
        return new Outcome<>(r, evaluateTriggers());
    }

    public Outcome<IsolatorResult> closeIsolator() {
        IsolatorResult r = bus.closeIsolator();
        if (r == IsolatorResult.CLOSED) {
            history.record("supply_restored", ACTOR_PLAYER, NODE_GENERATOR, Integer.toString(BellwetherBus.GENERATOR_SUPPLY));
        }
        return new Outcome<>(r, evaluateTriggers());
    }

    public Outcome<IsolatorResult> openIsolator() {
        IsolatorResult r = bus.openIsolator();
        if (r == IsolatorResult.OPENED) {
            history.record("supply_isolated", ACTOR_PLAYER, NODE_GENERATOR, "");
        }
        return new Outcome<>(r, evaluateTriggers());
    }

    public Outcome<ApplyResult> applyAllocation(Set<Service> requested, int expectedRevision) {
        ApplyResult r = bus.apply(requested, expectedRevision);
        if (r == ApplyResult.APPLIED) {
            history.record("allocation_applied", ACTOR_PLAYER, NODE_PANEL, serviceList(bus.configured()));
        }
        return new Outcome<>(r, evaluateTriggers());
    }

    /** One-shot consequences of the current physical state. Safe to call any number of times. */
    public List<RadioMessage> evaluateTriggers() {
        List<RadioMessage> out = new ArrayList<>();
        if (bus.isRunning(Service.LOCAL_TRANSMITTER)) {
            if (!history.contains("local_service_restored")) {
                history.record("local_service_restored", ACTOR_PLAYER, NODE_LOCAL_TX, "");
            }
            deliver(RadioCatalog.LOCAL_RESTORED, out);
        }
        return out;
    }

    private void deliver(RadioMessage m, List<RadioMessage> out) {
        if (radio.deliver(m.id())) {
            history.record("radio_delivered", m.speaker(), m.channel(), m.id());
            out.add(m);
        }
    }

    public static String serviceList(Set<Service> services) {
        StringJoiner j = new StringJoiner(",");
        for (Service s : Service.values()) {
            if (services.contains(s)) {
                j.add(s.id());
            }
        }
        return j.toString();
    }

    /** Result of an operation plus the radio messages it newly delivered. */
    public record Outcome<R>(R result, List<RadioMessage> delivered) {
    }
}
