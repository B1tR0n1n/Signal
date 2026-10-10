package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.block.SiteNodeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SignalMod.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // null datafixer type is the standard idiom for mod block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiteNodeBlockEntity>> SITE_NODE = BLOCK_ENTITIES.register("site_node",
            () -> BlockEntityType.Builder.of(SiteNodeBlockEntity::new,
                    ModBlocks.GENERATOR.get(),
                    ModBlocks.DISTRIBUTION_PANEL.get(),
                    ModBlocks.PUMP_CONTROLLER.get(),
                    ModBlocks.RELAY_CONTROLLER.get(),
                    ModBlocks.SERVICE_LAMP.get()).build(null));

    private ModBlockEntities() {
    }
}
