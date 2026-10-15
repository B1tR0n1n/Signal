package io.github.b1tr0n1n.signal.client.render;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** The faint cold glow along a Drawn's head seam and ear veins: the one thing visible of it in the dark. */
public final class DrawnGlowLayer extends EyesLayer<DrawnEntity, DrawnModel> {
    private static final RenderType GLOW = RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(SignalMod.MOD_ID, "textures/entity/drawn_glow.png"));

    public DrawnGlowLayer(RenderLayerParent<DrawnEntity, DrawnModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return GLOW;
    }
}
