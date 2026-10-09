package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SignalMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SIGNAL = TABS.register("signal", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.signal"))
            .icon(() -> new ItemStack(ModItems.HANDHELD_RADIO.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.HANDHELD_RADIO.get());
                out.accept(ModItems.REPLACEMENT_FUSE.get());
                out.accept(ModItems.GENERATOR.get());
                out.accept(ModItems.DISTRIBUTION_PANEL.get());
                out.accept(ModItems.PUMP_CONTROLLER.get());
                out.accept(ModItems.RELAY_CONTROLLER.get());
                out.accept(ModItems.SERVICE_LAMP.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
