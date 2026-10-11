package io.github.b1tr0n1n.signal.registry;

import com.mojang.serialization.Codec;
import io.github.b1tr0n1n.signal.SignalMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SignalMod.MOD_ID);

    /** Rounds loaded in a gun. Read through {@code Magazine}, which clamps it; never trusted as-is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LOADED_ROUNDS =
            COMPONENTS.registerComponentType("loaded_rounds", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
