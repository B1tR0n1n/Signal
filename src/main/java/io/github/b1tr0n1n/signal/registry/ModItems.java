package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.item.HandheldRadioItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SignalMod.MOD_ID);

    public static final DeferredItem<HandheldRadioItem> HANDHELD_RADIO = ITEMS.register("handheld_radio",
            () -> new HandheldRadioItem(new Item.Properties().stacksTo(1)));
    /** One explicit repair component. Consumed only by a validated fuse installation. */
    public static final DeferredItem<Item> REPLACEMENT_FUSE = ITEMS.register("replacement_fuse",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> GENERATOR = ITEMS.register("generator",
            () -> new BlockItem(ModBlocks.GENERATOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DISTRIBUTION_PANEL = ITEMS.register("distribution_panel",
            () -> new BlockItem(ModBlocks.DISTRIBUTION_PANEL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PUMP_CONTROLLER = ITEMS.register("pump_controller",
            () -> new BlockItem(ModBlocks.PUMP_CONTROLLER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RELAY_CONTROLLER = ITEMS.register("relay_controller",
            () -> new BlockItem(ModBlocks.RELAY_CONTROLLER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SERVICE_LAMP = ITEMS.register("service_lamp",
            () -> new BlockItem(ModBlocks.SERVICE_LAMP.get(), new Item.Properties()));

    private ModItems() {
    }
}
