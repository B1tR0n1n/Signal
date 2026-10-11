package io.github.b1tr0n1n.signal.client;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.client.render.DrawnModel;
import io.github.b1tr0n1n.signal.client.render.DrawnRenderer;
import io.github.b1tr0n1n.signal.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registration: entity models and renderers. */
@EventBusSubscriber(modid = SignalMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DrawnModel.LAYER, DrawnModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DRAWN.get(), DrawnRenderer::new);
    }
}
