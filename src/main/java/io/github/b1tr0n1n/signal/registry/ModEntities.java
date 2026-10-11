package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, SignalMod.MOD_ID);

    /** The Drawn: tall, blind, listening. */
    public static final DeferredHolder<EntityType<?>, EntityType<DrawnEntity>> DRAWN = ENTITIES.register("drawn",
            () -> EntityType.Builder.of(DrawnEntity::new, MobCategory.MONSTER).sized(0.7F, 2.3F).eyeHeight(2.05F)
                    .clientTrackingRange(10).build("drawn"));

    private ModEntities() {
    }
}
