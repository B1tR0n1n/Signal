package io.github.b1tr0n1n.signal.campaign;

import io.github.b1tr0n1n.signal.command.SignalCommands;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.domain.Service;
import io.github.b1tr0n1n.signal.domain.RadioMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;

/**
 * Forge-bus handlers. The tick does bounded work only: a handful of bound positions, and only when a
 * campaign exists. Worlds without a SIGNAL campaign pay one map lookup per tick and nothing else.
 */
public final class CampaignEvents {
    private static final int PRESSURE_STEP_TICKS = 30;
    private static final int PROJECT_TICKS = 20;
    private static final double ARRIVAL_DISTANCE_SQR = 9.0 * 9.0;

    /**
     * Natural hostile spawns are refused inside the site (with an 8-block margin) while a campaign is
     * active. Wildlife and everything outside the site spawn as usual; no game rule is changed.
     */
    @SubscribeEvent
    public void onSpawnPosition(MobSpawnEvent.PositionCheck event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION
                || event.getEntity().getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        SignalCampaignData data = SignalCampaignData.get(level.getServer());
        if (data.isActive() && level.dimension().equals(data.dimension())
                && data.bounds().inflatedBy(8).isInside(BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SignalCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SignalCampaignData data = SignalCampaignData.get(player.server);
            if (data.isActive() && data.isParticipant(player.getUUID())) {
                RadioPresenter.presentPending(player.server, data);
            }
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SiteInteractions.forgetPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        SignalCampaignData data = SignalCampaignData.get(server);
        if (!data.isActive()) {
            return;
        }
        ServerLevel level = data.level(server);
        if (level == null) {
            return;
        }
        long t = level.getGameTime();
        BellwetherSite site = data.site();

        if (t % PRESSURE_STEP_TICKS == 0) {
            int target = site.bus().isRunning(Service.PUMP) ? 3 : 0;
            int p = data.pumpPressure();
            if (p != target) {
                data.setPumpPressure(p + Integer.signum(target - p));
                SiteProjector.project(level, data);
            }
        }
        if (t % PROJECT_TICKS == 0) {
            // Catches chunks that reloaded since the last change; cheap and bounded.
            SiteProjector.project(level, data);
            arrival(server, level, data);
        }
        ambience(level, data, t);
    }

    private static void arrival(MinecraftServer server, ServerLevel level, SignalCampaignData data) {
        if (data.participant() == null || data.site().radio().isDelivered("bellwether/arrival")) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(data.participant());
        if (player == null || player.level() != level) {
            return;
        }
        BlockPos e = data.facilityEntrance();
        if (player.distanceToSqr(e.getX() + 0.5, e.getY(), e.getZ() + 0.5) <= ARRIVAL_DISTANCE_SQR) {
            List<RadioMessage> delivered = data.site().arrive();
            RadioPresenter.presentDelivered(server, data, delivered);
        }
    }

    /** State-carrying machine sounds: irregular generator rhythm, pump cycling, relay clicks. */
    private static void ambience(ServerLevel level, SignalCampaignData data, long t) {
        var bus = data.site().bus();
        for (Map.Entry<BlockPos, String> e : data.bindings().entrySet()) {
            BlockPos pos = e.getKey();
            if (!level.isLoaded(pos)) {
                continue;
            }
            switch (e.getValue()) {
                case BellwetherSite.NODE_GENERATOR -> {
                    if (bus.busLive() && level.getBlockState(pos).getBlock() instanceof io.github.b1tr0n1n.signal.block.GeneratorBlock
                            && t % 23 == 0 && level.random.nextInt(3) != 0) {
                        level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.7F, 0.55F + level.random.nextFloat() * 0.1F);
                    }
                }
                case BellwetherSite.NODE_PUMP -> {
                    if (bus.isRunning(Service.PUMP) && level.getBlockState(pos).getBlock() instanceof io.github.b1tr0n1n.signal.block.PumpControllerBlock
                            && t % 60 == 0) {
                        level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.25F, 0.5F);
                    }
                }
                case BellwetherSite.NODE_RELAY -> {
                    if (bus.isRunning(Service.REGIONAL_RELAY) && level.getBlockState(pos).getBlock() instanceof io.github.b1tr0n1n.signal.block.RelayControllerBlock
                            && t % 40 == 0) {
                        level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.4F, 1.6F);
                    }
                }
                default -> {
                }
            }
        }
    }
}
