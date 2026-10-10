package io.github.b1tr0n1n.signal.campaign;

import com.mojang.logging.LogUtils;
import io.github.b1tr0n1n.signal.domain.BellwetherBus;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.domain.CampaignHistory;
import io.github.b1tr0n1n.signal.domain.FuseState;
import io.github.b1tr0n1n.signal.domain.HistoryEvent;
import io.github.b1tr0n1n.signal.domain.RadioLog;
import io.github.b1tr0n1n.signal.domain.Service;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-level SIGNAL campaign state, stored once in the Overworld's data storage (the documented root
 * location) regardless of which dimension the site was built in. This is the authoritative store;
 * block entities only carry a node id.
 *
 * <p>A world in which no scenario was ever created has {@link #isInitialized()} false and nothing in the
 * mod changes that world.
 */
public final class SignalCampaignData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String FILE_ID = "signal_campaign";
    public static final int SCHEMA_VERSION = 1;

    private boolean initialized;
    /** Non-null when the stored data could not be understood; every action is refused with this text. */
    @Nullable
    private String loadError;

    private UUID campaignId = new UUID(0, 0);
    private String scenarioId = "";
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private BlockPos anchor = BlockPos.ZERO;
    private BoundingBox bounds = new BoundingBox(BlockPos.ZERO);
    private BlockPos start = BlockPos.ZERO;
    private float startYaw;
    private BlockPos facilityEntrance = BlockPos.ZERO;
    @Nullable
    private UUID participant;
    private final Map<BlockPos, String> bindings = new LinkedHashMap<>();
    /** Presentation only: current gauge reading 0-3. Derived from running state over time. */
    private int pumpPressure;
    private BellwetherSite site = new BellwetherSite();

    private static final SavedData.Factory<SignalCampaignData> FACTORY =
            new SavedData.Factory<>(SignalCampaignData::new, (tag, registries) -> load(tag), null);

    public static SignalCampaignData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public SignalCampaignData() {
    }

    // ----- lifecycle -----

    /** Start a new campaign timeline. Any previous timeline's history is discarded, never merged. */
    public void initialize(String scenarioId, ResourceKey<Level> dimension, BlockPos anchor, BoundingBox bounds,
                           BlockPos start, float startYaw, BlockPos facilityEntrance, @Nullable UUID participant,
                           Map<BlockPos, String> bindings) {
        this.initialized = true;
        this.loadError = null;
        this.campaignId = UUID.randomUUID();
        this.scenarioId = scenarioId;
        this.dimension = dimension;
        this.anchor = anchor.immutable();
        this.bounds = bounds;
        this.start = start.immutable();
        this.startYaw = startYaw;
        this.facilityEntrance = facilityEntrance.immutable();
        this.participant = participant;
        this.bindings.clear();
        bindings.forEach((p, n) -> this.bindings.put(p.immutable(), n));
        this.pumpPressure = 0;
        this.site = new BellwetherSite();
        setDirty();
    }

    /**
     * GameTest support only: forget the current timeline so a test world can host a fresh scenario.
     * Never called from gameplay or commands.
     */
    public void abandonForGameTest() {
        initialized = false;
        loadError = null;
        retainedUnreadable = null;
        bindings.clear();
        site = new BellwetherSite();
        setDirty();
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Nullable
    public String loadError() {
        return loadError;
    }

    public boolean isActive() {
        return initialized && loadError == null;
    }

    public UUID campaignId() {
        return campaignId;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Nullable
    public ServerLevel level(MinecraftServer server) {
        return server.getLevel(dimension);
    }

    public BlockPos anchor() {
        return anchor;
    }

    public BoundingBox bounds() {
        return bounds;
    }

    public BlockPos start() {
        return start;
    }

    public float startYaw() {
        return startYaw;
    }

    public BlockPos facilityEntrance() {
        return facilityEntrance;
    }

    @Nullable
    public UUID participant() {
        return participant;
    }

    public boolean isParticipant(UUID player) {
        return participant == null || participant.equals(player);
    }

    public Map<BlockPos, String> bindings() {
        return Collections.unmodifiableMap(bindings);
    }

    @Nullable
    public String nodeAt(BlockPos pos) {
        return bindings.get(pos);
    }

    public int pumpPressure() {
        return pumpPressure;
    }

    public void setPumpPressure(int pumpPressure) {
        if (this.pumpPressure != pumpPressure) {
            this.pumpPressure = pumpPressure;
            setDirty();
        }
    }

    public BellwetherSite site() {
        return site;
    }

    // ----- persistence -----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return save(tag);
    }

    /** Registry-independent encoding (the campaign stores only ids, numbers and strings). */
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Schema", SCHEMA_VERSION);
        tag.putBoolean("Initialized", initialized);
        if (!initialized) {
            return tag;
        }
        tag.putUUID("CampaignId", campaignId);
        tag.putString("Scenario", scenarioId);
        tag.putString("Dimension", dimension.location().toString());
        tag.putLong("Anchor", anchor.asLong());
        tag.putIntArray("Bounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        tag.putLong("Start", start.asLong());
        tag.putFloat("StartYaw", startYaw);
        tag.putLong("Entrance", facilityEntrance.asLong());
        if (participant != null) {
            tag.putUUID("Participant", participant);
        }
        ListTag b = new ListTag();
        bindings.forEach((pos, node) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("Pos", pos.asLong());
            e.putString("Node", node);
            b.add(e);
        });
        tag.put("Bindings", b);
        tag.putInt("PumpPressure", pumpPressure);

        BellwetherBus bus = site.bus();
        CompoundTag busTag = new CompoundTag();
        busTag.putString("Fuse", bus.fuse().id());
        busTag.putBoolean("Isolated", bus.isolated());
        busTag.put("Configured", strings(serviceIds(bus.configured())));
        busTag.putBoolean("RelayAligned", bus.relayAligned());
        busTag.putInt("Revision", bus.revision());
        tag.put("Bus", busTag);

        CompoundTag radio = new CompoundTag();
        radio.put("Delivered", strings(site.radio().transcript()));
        radio.put("Presented", strings(new ArrayList<>(site.radio().presentedIds())));
        tag.put("Radio", radio);

        ListTag history = new ListTag();
        for (HistoryEvent e : site.history().events()) {
            CompoundTag h = new CompoundTag();
            h.putLong("Seq", e.sequence());
            h.putString("Type", e.type());
            h.putString("Actor", e.actor());
            h.putString("Target", e.target());
            h.putString("Payload", e.payload());
            history.add(h);
        }
        tag.put("History", history);
        return tag;
    }

    public static SignalCampaignData load(CompoundTag tag) {
        SignalCampaignData d = new SignalCampaignData();
        int schema = tag.getInt("Schema");
        d.initialized = tag.getBoolean("Initialized");
        if (!d.initialized) {
            return d;
        }
        if (schema != SCHEMA_VERSION) {
            d.loadError = "SIGNAL campaign data uses schema " + schema + " but this build understands schema "
                    + SCHEMA_VERSION + ". The campaign is left untouched and inactive.";
            LOGGER.error(d.loadError);
            d.retainedUnreadable = tag.copy();
            return d;
        }
        try {
            d.campaignId = tag.getUUID("CampaignId");
            d.scenarioId = tag.getString("Scenario");
            d.dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")));
            d.anchor = BlockPos.of(tag.getLong("Anchor"));
            int[] bb = tag.getIntArray("Bounds");
            d.bounds = new BoundingBox(bb[0], bb[1], bb[2], bb[3], bb[4], bb[5]);
            d.start = BlockPos.of(tag.getLong("Start"));
            d.startYaw = tag.getFloat("StartYaw");
            d.facilityEntrance = BlockPos.of(tag.getLong("Entrance"));
            d.participant = tag.hasUUID("Participant") ? tag.getUUID("Participant") : null;
            for (Tag t : tag.getList("Bindings", Tag.TAG_COMPOUND)) {
                CompoundTag e = (CompoundTag) t;
                d.bindings.put(BlockPos.of(e.getLong("Pos")), e.getString("Node"));
            }
            d.pumpPressure = tag.getInt("PumpPressure");

            CompoundTag busTag = tag.getCompound("Bus");
            EnumSet<Service> configured = EnumSet.noneOf(Service.class);
            for (String id : readStrings(busTag.getList("Configured", Tag.TAG_STRING))) {
                configured.add(Service.byId(id).orElseThrow(() -> new IllegalStateException("Unknown service id '" + id + "'")));
            }
            BellwetherBus bus = new BellwetherBus(FuseState.byId(busTag.getString("Fuse")), busTag.getBoolean("Isolated"),
                    configured, busTag.getBoolean("RelayAligned"), busTag.getInt("Revision"));

            CompoundTag radioTag = tag.getCompound("Radio");
            RadioLog radio = new RadioLog(readStrings(radioTag.getList("Delivered", Tag.TAG_STRING)),
                    readStrings(radioTag.getList("Presented", Tag.TAG_STRING)));

            List<HistoryEvent> events = new ArrayList<>();
            for (Tag t : tag.getList("History", Tag.TAG_COMPOUND)) {
                CompoundTag h = (CompoundTag) t;
                events.add(new HistoryEvent(h.getLong("Seq"), h.getString("Type"), h.getString("Actor"),
                        h.getString("Target"), h.getString("Payload")));
            }
            d.site = new BellwetherSite(bus, radio, new CampaignHistory(events));
        } catch (RuntimeException ex) {
            d.loadError = "SIGNAL campaign data could not be read (" + ex.getMessage() + "). The campaign is left untouched and inactive.";
            LOGGER.error(d.loadError, ex);
            d.retainedUnreadable = tag.copy();
        }
        return d;
    }

    /** Raw tag kept when unreadable so saving never overwrites data this build does not understand. */
    @Nullable
    private CompoundTag retainedUnreadable;

    @Override
    public boolean isDirty() {
        return retainedUnreadable == null && super.isDirty();
    }

    private static List<String> serviceIds(java.util.Set<Service> services) {
        List<String> out = new ArrayList<>();
        for (Service s : Service.values()) {
            if (services.contains(s)) {
                out.add(s.id());
            }
        }
        return out;
    }

    private static ListTag strings(List<String> values) {
        ListTag l = new ListTag();
        for (String v : values) {
            l.add(StringTag.valueOf(v));
        }
        return l;
    }

    private static List<String> readStrings(ListTag list) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }
}
