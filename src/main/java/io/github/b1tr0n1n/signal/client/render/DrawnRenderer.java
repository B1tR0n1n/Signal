package io.github.b1tr0n1n.signal.client.render;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class DrawnRenderer extends MobRenderer<DrawnEntity, DrawnModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SignalMod.MOD_ID, "textures/entity/drawn.png");

    public DrawnRenderer(EntityRendererProvider.Context context) {
        super(context, new DrawnModel(context.bakeLayer(DrawnModel.LAYER)), 0.55F);
        addLayer(new DrawnGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(DrawnEntity entity) {
        return TEXTURE;
    }
}
