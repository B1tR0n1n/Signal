package io.github.b1tr0n1n.signal.client;

import io.github.b1tr0n1n.signal.SignalMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Meridian haze for the vanilla renderer: when the camera is in a {@code #signal:meridian} biome the
 * terrain fog closes in (more in rain), easing in and out over a few seconds instead of snapping at
 * biome borders. Shader packs replace vanilla fog with their own volumetrics; packs that read the
 * vanilla fog uniforms will inherit these distances.
 */
@EventBusSubscriber(modid = SignalMod.MOD_ID, value = Dist.CLIENT)
public final class AtmosphereFog {
    public static final TagKey<Biome> MERIDIAN =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(SignalMod.MOD_ID, "meridian"));

    private static final float CLEAR_FAR = 176.0F;
    private static final float RAIN_FAR = 104.0F;
    private static final float NEAR_FRACTION = 0.08F;
    /** Fraction of the remaining distance covered per frame (~2 s to settle at 60 fps). */
    private static final float EASE = 0.03F;

    private static float intensity;
    private static float wetness;

    private AtmosphereFog() {
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        boolean inside = mc.level.getBiome(event.getCamera().getBlockPosition()).is(MERIDIAN);
        intensity += ((inside ? 1.0F : 0.0F) - intensity) * EASE;
        wetness += (mc.level.getRainLevel((float) event.getPartialTick()) - wetness) * EASE;
        if (intensity < 0.01F) {
            return;
        }
        float vanillaFar = event.getFarPlaneDistance();
        float hazeFar = Math.min(vanillaFar, CLEAR_FAR + (RAIN_FAR - CLEAR_FAR) * wetness);
        float far = vanillaFar + (hazeFar - vanillaFar) * intensity;
        float near = event.getNearPlaneDistance() + (far * NEAR_FRACTION - event.getNearPlaneDistance()) * intensity;
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(near);
        event.setCanceled(true); // required for NeoForge to apply the modified distances
    }
}
