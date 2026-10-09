package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.block.SiteNodeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SignalMod.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // null datafixer type is the standard idiom for mod block entities
    public static final RegistryObject<BlockEntityType<SiteNodeBlockEntity>> SITE_NODE = BLOCK_ENTITIES.register("site_node",
            () -> BlockEntityType.Builder.of(SiteNodeBlockEntity::new,
                    ModBlocks.GENERATOR.get(),
                    ModBlocks.DISTRIBUTION_PANEL.get(),
                    ModBlocks.PUMP_CONTROLLER.get(),
                    ModBlocks.RELAY_CONTROLLER.get(),
                    ModBlocks.SERVICE_LAMP.get()).build(null));

    private ModBlockEntities() {
    }
}
