package io.github.b1tr0n1n.signal;

import io.github.b1tr0n1n.signal.campaign.CampaignEvents;
import io.github.b1tr0n1n.signal.network.SignalNetwork;
import io.github.b1tr0n1n.signal.registry.ModBlockEntities;
import io.github.b1tr0n1n.signal.registry.ModBlocks;
import io.github.b1tr0n1n.signal.registry.ModCreativeTabs;
import io.github.b1tr0n1n.signal.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * SIGNAL entry point. Wires registries, networking and server-side campaign events. Contains no game
 * rules itself; see the domain, campaign and scenario packages.
 */
@Mod(SignalMod.MOD_ID)
public final class SignalMod {
    public static final String MOD_ID = "signal";

    public SignalMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        modBus.addListener(SignalNetwork::register);
        NeoForge.EVENT_BUS.register(new CampaignEvents());
    }
}
