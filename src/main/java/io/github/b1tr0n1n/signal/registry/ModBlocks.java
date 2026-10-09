package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.block.DistributionPanelBlock;
import io.github.b1tr0n1n.signal.block.GeneratorBlock;
import io.github.b1tr0n1n.signal.block.PumpControllerBlock;
import io.github.b1tr0n1n.signal.block.RelayControllerBlock;
import io.github.b1tr0n1n.signal.block.ServiceLampBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SignalMod.MOD_ID);

    /**
     * Scenario equipment is unbreakable outside Creative so the authored site cannot be destroyed by
     * accident; damaging equipment would need to be a supported action with consequences.
     */
    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.METAL)
                .noLootTable()
                .pushReaction(PushReaction.BLOCK);
    }

    public static final RegistryObject<GeneratorBlock> GENERATOR = BLOCKS.register("generator",
            () -> new GeneratorBlock(machine().lightLevel(s -> s.getValue(GeneratorBlock.RUNNING) ? 7 : 0)));
    public static final RegistryObject<DistributionPanelBlock> DISTRIBUTION_PANEL = BLOCKS.register("distribution_panel",
            () -> new DistributionPanelBlock(machine()));
    public static final RegistryObject<PumpControllerBlock> PUMP_CONTROLLER = BLOCKS.register("pump_controller",
            () -> new PumpControllerBlock(machine()));
    public static final RegistryObject<RelayControllerBlock> RELAY_CONTROLLER = BLOCKS.register("relay_controller",
            () -> new RelayControllerBlock(machine()));
    public static final RegistryObject<ServiceLampBlock> SERVICE_LAMP = BLOCKS.register("service_lamp",
            () -> new ServiceLampBlock(machine().mapColor(MapColor.TERRACOTTA_ORANGE).sound(SoundType.LANTERN)
                    .lightLevel(s -> s.getValue(ServiceLampBlock.LIT) ? 13 : 0)));

    private ModBlocks() {
    }
}
