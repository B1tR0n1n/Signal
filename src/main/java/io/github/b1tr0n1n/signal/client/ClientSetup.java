package io.github.b1tr0n1n.signal.client;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.client.render.DrawnModel;
import io.github.b1tr0n1n.signal.client.render.DrawnRenderer;
import io.github.b1tr0n1n.signal.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.b1tr0n1n.signal.network.SignalNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client-only registration: entity models and renderers, and the reload key. */
@EventBusSubscriber(modid = SignalMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    /** R by default; rebindable under Options > Controls > Key Binds > SIGNAL. */
    public static final KeyMapping RELOAD = new KeyMapping("key.signal.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.signal");

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(RELOAD);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        while (RELOAD.consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                PacketDistributor.sendToServer(SignalNetwork.Reload.INSTANCE);
            }
        }
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
