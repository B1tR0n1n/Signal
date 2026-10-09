package io.github.b1tr0n1n.signal;

import io.github.b1tr0n1n.signal.campaign.CampaignEvents;
import io.github.b1tr0n1n.signal.network.SignalNetwork;
import io.github.b1tr0n1n.signal.registry.ModBlockEntities;
import io.github.b1tr0n1n.signal.registry.ModBlocks;
import io.github.b1tr0n1n.signal.registry.ModCreativeTabs;
import io.github.b1tr0n1n.signal.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * SIGNAL entry point. Wires registries, networking and server-side campaign events. Contains no game
 * rules itself; see the domain, campaign and scenario packages.
 */
@Mod(SignalMod.MOD_ID)
public final class SignalMod {
    public static final String MOD_ID = "signal";

    public SignalMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new CampaignEvents());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SignalNetwork::register);
    }
}
