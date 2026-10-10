package io.github.b1tr0n1n.signal.scenario;

import io.github.b1tr0n1n.signal.campaign.RadioPresenter;
import io.github.b1tr0n1n.signal.campaign.SignalCampaignData;
import io.github.b1tr0n1n.signal.campaign.SiteProjector;
import io.github.b1tr0n1n.signal.domain.RadioMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creating and resetting the Bellwether campaign. Shared by the operator command and the GameTests so
 * the tests exercise the real path. Player-facing preparation (teleport, game mode, game rules) stays in
 * the command.
 */
public final class ScenarioService {
    private ScenarioService() {
    }

    public enum Status { CREATED, ALREADY_EXISTS, UNREADABLE_DATA, BLOCKED }

    /**
     * @param guard      occupancy report when the guard ran
     * @param bounds     planned site bounds (null if planning did not happen)
     * @param problemKey translation key for a planning problem (e.g. build height), else null
     */
    public record CreateResult(Status status, @Nullable ScenarioGuard.Report guard, @Nullable BoundingBox bounds,
                               @Nullable String problemKey) {
    }

    public static CreateResult create(ServerLevel level, BlockPos anchor, @Nullable UUID participant) {
        SignalCampaignData data = SignalCampaignData.get(level.getServer());
        if (data.loadError() != null) {
            return new CreateResult(Status.UNREADABLE_DATA, null, null, null);
        }
        if (data.isInitialized()) {
            return new CreateResult(Status.ALREADY_EXISTS, null, null, null);
        }
        BellwetherScenario.Plan plan = BellwetherScenario.plan(level, anchor);
        if (!plan.ok()) {
            return new CreateResult(Status.BLOCKED, null, plan.bounds(), plan.problemKey());
        }
        ScenarioGuard.Report report = ScenarioGuard.check(level, plan.bounds(), plan.terrain());
        if (!report.ok()) {
            return new CreateResult(Status.BLOCKED, report, plan.bounds(), report.reasonKey());
        }
        BellwetherScenario.Result built = BellwetherScenario.build(level, plan);
        data.initialize(BellwetherScenario.ID, level.dimension(), anchor, plan.bounds(), built.start(), built.startYaw(),
                built.entrance(), built.frame(), participant, built.bindings());
        removeHostiles(level, plan.bounds());
        SiteProjector.project(level, data);
        return new CreateResult(Status.CREATED, report, plan.bounds(), null);
    }

    /**
     * Rebuild the service facility (equipment, cabinet, log) inside its own footprint and begin a fresh
     * timeline. The sculpted terrain, road and tower are left as they are. Items spilled by rebuilding the
     * cabinet and lectern are removed from the facility footprint only.
     */
    public static void reset(ServerLevel level, SignalCampaignData data, @Nullable UUID participant) {
        BoundingBox facility = ServiceFacility.box(data.frame());
        SiteCanvas c = new SiteCanvas(level, data.bounds(), level.getGameTime());
        ServiceFacility.build(c, data.frame());
        c.finishShapes();
        Map<BlockPos, String> bindings = new LinkedHashMap<>();
        data.bindings().forEach((pos, node) -> {
            if (!facility.isInside(pos)) {
                bindings.put(pos, node);
            }
        });
        bindings.putAll(c.bindings);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, aabb(facility))) {
            item.discard();
        }
        data.initialize(BellwetherScenario.ID, level.dimension(), data.anchor(), data.bounds(), data.start(), data.startYaw(),
                data.facilityEntrance(), data.frame(), participant != null ? participant : data.participant(), bindings);
        removeHostiles(level, data.bounds());
        SiteProjector.project(level, data);
    }

    /** Deliver the opening transmission. Called after the participant has been prepared. */
    public static void begin(ServerLevel level, SignalCampaignData data) {
        List<RadioMessage> opening = data.site().begin();
        data.setDirty();
        RadioPresenter.presentDelivered(level.getServer(), data, opening);
    }

    private static void removeHostiles(ServerLevel level, BoundingBox box) {
        for (Entity e : level.getEntities((Entity) null, aabb(box), e -> e instanceof Enemy)) {
            e.discard();
        }
    }

    public static AABB aabb(BoundingBox box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
    }
}
