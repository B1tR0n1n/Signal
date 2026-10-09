package io.github.b1tr0n1n.signal.campaign;

import io.github.b1tr0n1n.signal.block.DistributionPanelBlock;
import io.github.b1tr0n1n.signal.block.GeneratorBlock;
import io.github.b1tr0n1n.signal.block.PumpControllerBlock;
import io.github.b1tr0n1n.signal.block.RelayControllerBlock;
import io.github.b1tr0n1n.signal.block.RelayState;
import io.github.b1tr0n1n.signal.block.ServiceLampBlock;
import io.github.b1tr0n1n.signal.block.SiteNodeBlockEntity;
import io.github.b1tr0n1n.signal.domain.BellwetherBus;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.domain.FuseState;
import io.github.b1tr0n1n.signal.domain.Service;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Projects authoritative campaign state onto physical blocks. One direction only: blocks never feed
 * state back. Only positions in loaded chunks are touched (never force-loads), so an unloaded site
 * simply catches up the next time it is projected.
 */
public final class SiteProjector {
    /** Binding id for the header-tank cauldron(s) whose water level mirrors pump pressure. */
    public static final String NODE_PUMP_TANK = "bellwether/pump_tank";

    private SiteProjector() {
    }

    public static void project(ServerLevel level, SignalCampaignData data) {
        if (!data.isActive() || level.dimension() != data.dimension()) {
            return;
        }
        for (Map.Entry<BlockPos, String> e : data.bindings().entrySet()) {
            BlockPos pos = e.getKey();
            if (!level.isLoaded(pos)) {
                continue;
            }
            projectOne(level, data, pos, e.getValue());
        }
    }

    private static void projectOne(ServerLevel level, SignalCampaignData data, BlockPos pos, String node) {
        BlockState current = level.getBlockState(pos);
        BellwetherBus bus = data.site().bus();
        BlockState desired = current;

        if (node.equals(NODE_PUMP_TANK)) {
            int p = data.pumpPressure();
            if (current.is(Blocks.CAULDRON) || current.is(Blocks.WATER_CAULDRON)) {
                desired = p == 0 ? Blocks.CAULDRON.defaultBlockState()
                        : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, p);
            }
        } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SiteNodeBlockEntity node0) || !node0.nodeId().equals(node)) {
                return; // physical block no longer matches its binding; never overwrite foreign blocks
            }
            Block block = current.getBlock();
            if (block instanceof GeneratorBlock) {
                desired = current.setValue(GeneratorBlock.RUNNING, bus.busLive());
            } else if (block instanceof DistributionPanelBlock) {
                desired = current.setValue(DistributionPanelBlock.FAULT, bus.fuse() == FuseState.DAMAGED);
            } else if (block instanceof PumpControllerBlock) {
                desired = current.setValue(PumpControllerBlock.PRESSURE, data.pumpPressure());
            } else if (block instanceof RelayControllerBlock) {
                desired = current.setValue(RelayControllerBlock.RELAY, relayState(bus));
            } else if (block instanceof ServiceLampBlock) {
                desired = current.setValue(ServiceLampBlock.LIT, nodeLive(bus, node));
            }
        }
        if (desired != current) {
            level.setBlock(pos, desired, Block.UPDATE_CLIENTS);
        }
    }

    public static RelayState relayState(BellwetherBus bus) {
        if (bus.relayReachable()) {
            return RelayState.LINKED;
        }
        return bus.isRunning(Service.REGIONAL_RELAY) ? RelayState.POWERED : RelayState.OFF;
    }

    /** Whether an indicator bound to {@code node} should be lit. */
    public static boolean nodeLive(BellwetherBus bus, String node) {
        return switch (node) {
            case BellwetherSite.NODE_GENERATOR -> bus.busLive();
            case BellwetherSite.NODE_PUMP -> bus.isRunning(Service.PUMP);
            case BellwetherSite.NODE_LOCAL_TX -> bus.isRunning(Service.LOCAL_TRANSMITTER);
            case BellwetherSite.NODE_RELAY -> bus.isRunning(Service.REGIONAL_RELAY);
            default -> false;
        };
    }
}
