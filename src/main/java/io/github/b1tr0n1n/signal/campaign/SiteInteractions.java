package io.github.b1tr0n1n.signal.campaign;

import io.github.b1tr0n1n.signal.block.DistributionPanelBlock;
import io.github.b1tr0n1n.signal.block.GeneratorBlock;
import io.github.b1tr0n1n.signal.block.PumpControllerBlock;
import io.github.b1tr0n1n.signal.block.RelayControllerBlock;
import io.github.b1tr0n1n.signal.block.SiteNodeBlockEntity;
import io.github.b1tr0n1n.signal.domain.ApplyResult;
import io.github.b1tr0n1n.signal.domain.BellwetherBus;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.domain.IsolatorResult;
import io.github.b1tr0n1n.signal.domain.RepairResult;
import io.github.b1tr0n1n.signal.domain.Service;
import io.github.b1tr0n1n.signal.network.PanelSnapshot;
import io.github.b1tr0n1n.signal.network.SignalNetwork;
import io.github.b1tr0n1n.signal.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side handling of every player interaction with site equipment. All validation happens here:
 * campaign state, participant, dimension, loaded chunk, block identity, binding, reach, revision and
 * inventory. The client only ever submits intentions.
 */
public final class SiteInteractions {
    /** Panel actions arrive via packets; same reach the vanilla interaction allows, squared. */
    private static final double MAX_REACH_SQR = 8.0 * 8.0;
    /** Minimum ticks between panel actions per player, to absorb accidental double submits. */
    private static final int ACTION_COOLDOWN_TICKS = 4;
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();

    private SiteInteractions() {
    }

    // ----- right-click in world -----

    public static void onUse(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        SignalCampaignData data = SignalCampaignData.get(level.getServer());
        String node = boundNode(level, data, pos);
        if (node == null || !data.isParticipant(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.signal.not_bound").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (state.getBlock() instanceof GeneratorBlock) {
            useGenerator(level, player, data, pos);
        } else if (state.getBlock() instanceof DistributionPanelBlock) {
            SignalNetwork.sendSnapshot(player, snapshot(data, player, pos, ""), true);
        } else if (state.getBlock() instanceof PumpControllerBlock) {
            inspectPump(player, data);
        } else if (state.getBlock() instanceof RelayControllerBlock) {
            inspectRelay(player, data);
        }
    }

    private static void useGenerator(ServerLevel level, ServerPlayer player, SignalCampaignData data, BlockPos pos) {
        BellwetherSite site = data.site();
        BellwetherBus bus = site.bus();
        if (player.isShiftKeyDown()) {
            if (bus.isolated()) {
                say(player, "message.signal.generator.already_isolated");
                return;
            }
            BellwetherSite.Outcome<IsolatorResult> o = site.openIsolator();
            changed(level, data, o.delivered());
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.9F, 0.6F);
            say(player, "message.signal.generator.isolated");
            return;
        }
        if (!bus.isolated()) {
            inspectGenerator(player, data);
            return;
        }
        BellwetherSite.Outcome<IsolatorResult> o = site.closeIsolator();
        switch (o.result()) {
            case REFUSED_FUSE_DAMAGED -> {
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.4F);
                warn(player, "message.signal.generator.refused_fuse");
            }
            case CLOSED -> {
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
                level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.25F, 0.5F);
                player.sendSystemMessage(Component.translatable("message.signal.generator.closed",
                        BellwetherBus.GENERATOR_SUPPLY, bus.configuredDemand()).withStyle(ChatFormatting.GREEN));
                changed(level, data, o.delivered());
            }
            default -> inspectGenerator(player, data);
        }
    }

    private static void inspectGenerator(ServerPlayer player, SignalCampaignData data) {
        BellwetherBus bus = data.site().bus();
        player.sendSystemMessage(Component.translatable("message.signal.inspect.generator",
                BellwetherBus.GENERATOR_SUPPLY,
                Component.translatable(bus.isolated() ? "state.signal.isolated" : "state.signal.closed"),
                bus.availableSupply(), bus.configuredDemand()).withStyle(ChatFormatting.GRAY));
    }

    private static void inspectPump(ServerPlayer player, SignalCampaignData data) {
        BellwetherBus bus = data.site().bus();
        player.sendSystemMessage(Component.translatable("message.signal.inspect.pump",
                Service.PUMP.demand(),
                Component.translatable(bus.isRunning(Service.PUMP) ? "state.signal.running" : "state.signal.off"),
                data.pumpPressure()).withStyle(ChatFormatting.GRAY));
    }

    private static void inspectRelay(ServerPlayer player, SignalCampaignData data) {
        BellwetherBus bus = data.site().bus();
        player.sendSystemMessage(Component.translatable("message.signal.inspect.relay",
                Service.REGIONAL_RELAY.demand(),
                Component.translatable(bus.isRunning(Service.REGIONAL_RELAY) ? "state.signal.powered" : "state.signal.off"),
                Component.translatable(bus.relayAligned() ? "state.signal.aligned" : "state.signal.misaligned"),
                Component.translatable(bus.relayReachable() ? "state.signal.reachable" : "state.signal.unreachable"))
                .withStyle(ChatFormatting.GRAY));
    }

    // ----- panel packets -----

    /** Result of a validated panel action, exposed for GameTests. */
    public enum PanelOutcome { REJECTED, DONE }

    public static PanelOutcome applyAllocation(ServerPlayer player, BlockPos pos, Set<Service> requested, int expectedRevision) {
        ServerLevel level = player.serverLevel();
        SignalCampaignData data = SignalCampaignData.get(level.getServer());
        if (!validPanelAction(player, level, data, pos)) {
            return PanelOutcome.REJECTED;
        }
        BellwetherSite.Outcome<ApplyResult> o = data.site().applyAllocation(requested, expectedRevision);
        String status = switch (o.result()) {
            case APPLIED -> "screen.signal.panel.status.applied";
            case UNCHANGED -> "screen.signal.panel.status.unchanged";
            case STALE_REVISION -> "screen.signal.panel.status.stale";
            case OVER_CAPACITY -> "screen.signal.panel.status.over_capacity";
        };
        if (o.result() == ApplyResult.APPLIED) {
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.7F);
            changed(level, data, o.delivered());
        }
        SignalNetwork.sendSnapshot(player, snapshot(data, player, pos, status), false);
        return PanelOutcome.DONE;
    }

    public static PanelOutcome installFuse(ServerPlayer player, BlockPos pos, int expectedRevision) {
        ServerLevel level = player.serverLevel();
        SignalCampaignData data = SignalCampaignData.get(level.getServer());
        if (!validPanelAction(player, level, data, pos)) {
            return PanelOutcome.REJECTED;
        }
        int slot = findFuse(player.getInventory());
        BellwetherSite.Outcome<RepairResult> o = data.site().installFuse(slot >= 0, expectedRevision);
        String status = switch (o.result()) {
            case INSTALLED -> "screen.signal.panel.status.fuse_installed";
            case ALREADY_INSTALLED -> "screen.signal.panel.status.fuse_already";
            case SUPPLY_LIVE -> "screen.signal.panel.status.fuse_live";
            case NO_FUSE -> "screen.signal.panel.status.fuse_missing";
            case STALE_REVISION -> "screen.signal.panel.status.stale";
        };
        if (o.result() == RepairResult.INSTALLED) {
            // Consume exactly one, only after every check passed.
            player.getInventory().getItem(slot).shrink(1);
            player.getInventory().setChanged();
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            changed(level, data, o.delivered());
        }
        SignalNetwork.sendSnapshot(player, snapshot(data, player, pos, status), false);
        return PanelOutcome.DONE;
    }

    private static boolean validPanelAction(ServerPlayer player, ServerLevel level, SignalCampaignData data, BlockPos pos) {
        if (!data.isActive() || !data.isParticipant(player.getUUID()) || level.dimension() != data.dimension()) {
            return false;
        }
        // Never let packet coordinates force-load chunks.
        if (!level.isLoaded(pos)) {
            return false;
        }
        if (!(level.getBlockState(pos).getBlock() instanceof DistributionPanelBlock)) {
            return false;
        }
        if (!BellwetherSite.NODE_PANEL.equals(boundNode(level, data, pos))) {
            return false;
        }
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_REACH_SQR) {
            return false;
        }
        long now = level.getGameTime();
        Long last = LAST_ACTION.get(player.getUUID());
        if (last != null && now - last < ACTION_COOLDOWN_TICKS && now >= last) {
            return false;
        }
        LAST_ACTION.put(player.getUUID(), now);
        return true;
    }

    public static void forgetPlayer(UUID id) {
        LAST_ACTION.remove(id);
    }

    // ----- shared -----

    /** The campaign node bound at {@code pos}, verified against the block entity's own identity. */
    @Nullable
    public static String boundNode(ServerLevel level, SignalCampaignData data, BlockPos pos) {
        if (!data.isActive() || level.dimension() != data.dimension()) {
            return null;
        }
        String node = data.nodeAt(pos);
        if (node == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SiteNodeBlockEntity site && node.equals(site.nodeId())) {
            return node;
        }
        return null;
    }

    public static int findFuse(Inventory inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(ModItems.REPLACEMENT_FUSE.get())) {
                return i;
            }
        }
        return -1;
    }

    public static PanelSnapshot snapshot(SignalCampaignData data, ServerPlayer player, BlockPos pos, String status) {
        BellwetherBus bus = data.site().bus();
        return new PanelSnapshot(pos, bus.revision(), bus.fuse() == io.github.b1tr0n1n.signal.domain.FuseState.INSTALLED,
                bus.isolated(), PanelSnapshot.mask(bus.configured()), bus.relayAligned(),
                findFuse(player.getInventory()) >= 0, status);
    }

    /** After any successful mutation: persist, project to the world, present one-shot consequences. */
    static void changed(ServerLevel level, SignalCampaignData data, java.util.List<io.github.b1tr0n1n.signal.domain.RadioMessage> delivered) {
        data.setDirty();
        SiteProjector.project(level, data);
        RadioPresenter.presentDelivered(level.getServer(), data, delivered);
    }

    private static void say(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static void warn(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
    }
}
