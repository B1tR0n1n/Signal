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

import java.util.List;
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

    public record CreateResult(Status status, @Nullable ScenarioGuard.Report guard) {
    }

    public static CreateResult create(ServerLevel level, BlockPos anchor, @Nullable UUID participant) {
        SignalCampaignData data = SignalCampaignData.get(level.getServer());
        if (data.loadError() != null) {
            return new CreateResult(Status.UNREADABLE_DATA, null);
        }
        if (data.isInitialized()) {
            return new CreateResult(Status.ALREADY_EXISTS, null);
        }
        BoundingBox box = BellwetherScenario.bounds(anchor);
        ScenarioGuard.Report report = ScenarioGuard.check(level, box);
        if (!report.ok()) {
            return new CreateResult(Status.BLOCKED, report);
        }
        buildAndStart(level, data, anchor, participant);
        return new CreateResult(Status.CREATED, report);
    }

    /**
     * Rebuild strictly inside the stored bounds and begin a fresh timeline. Caller has confirmed.
     * Items spilled by rebuilding the cabinet and lectern are removed from inside the bounds only.
     */
    public static void reset(ServerLevel level, SignalCampaignData data, @Nullable UUID participant) {
        BoundingBox box = data.bounds();
        buildAndStart(level, data, data.anchor(), participant != null ? participant : data.participant());
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, aabb(box))) {
            item.discard();
        }
    }

    private static void buildAndStart(ServerLevel level, SignalCampaignData data, BlockPos anchor, @Nullable UUID participant) {
        BoundingBox box = BellwetherScenario.bounds(anchor);
        BellwetherScenario.Result built = BellwetherScenario.build(level, anchor);
        data.initialize(BellwetherScenario.ID, level.dimension(), anchor, box, built.start(), built.startYaw(),
                built.entrance(), participant, built.bindings());
        for (Entity e : level.getEntities((Entity) null, aabb(box), e -> e instanceof Enemy)) {
            e.discard();
        }
        SiteProjector.project(level, data);
    }

    /** Deliver the opening transmission. Called after the participant has been prepared. */
    public static void begin(ServerLevel level, SignalCampaignData data) {
        List<RadioMessage> opening = data.site().begin();
        data.setDirty();
        RadioPresenter.presentDelivered(level.getServer(), data, opening);
    }

    public static AABB aabb(BoundingBox box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
    }
}
