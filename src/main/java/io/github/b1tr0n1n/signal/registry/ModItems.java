package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.item.HandheldRadioItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SignalMod.MOD_ID);

    public static final RegistryObject<HandheldRadioItem> HANDHELD_RADIO = ITEMS.register("handheld_radio",
            () -> new HandheldRadioItem(new Item.Properties().stacksTo(1)));
    /** One explicit repair component. Consumed only by a validated fuse installation. */
    public static final RegistryObject<Item> REPLACEMENT_FUSE = ITEMS.register("replacement_fuse",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<BlockItem> GENERATOR = ITEMS.register("generator",
            () -> new BlockItem(ModBlocks.GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> DISTRIBUTION_PANEL = ITEMS.register("distribution_panel",
            () -> new BlockItem(ModBlocks.DISTRIBUTION_PANEL.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> PUMP_CONTROLLER = ITEMS.register("pump_controller",
            () -> new BlockItem(ModBlocks.PUMP_CONTROLLER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> RELAY_CONTROLLER = ITEMS.register("relay_controller",
            () -> new BlockItem(ModBlocks.RELAY_CONTROLLER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> SERVICE_LAMP = ITEMS.register("service_lamp",
            () -> new BlockItem(ModBlocks.SERVICE_LAMP.get(), new Item.Properties()));

    private ModItems() {
    }
}
