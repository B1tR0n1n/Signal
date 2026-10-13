package io.github.b1tr0n1n.signal.client.render;

import io.github.b1tr0n1n.signal.SignalMod;
import io.github.b1tr0n1n.signal.entity.DrawnEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Drawn: a hunched, too-tall figure on bent legs, arms hanging past its knees, an eyeless head carrying
 * two thin membranes like listening dishes that turn toward sound. Original model; texture UVs match
 * {@code tools/gen_textures.py#drawn_entity}.
 */
public final class DrawnModel extends HierarchicalModel<DrawnEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(SignalMod.MOD_ID, "drawn"), "main");

    private final ModelPart root, torso, neck, head, leftEar, rightEar, leftArm, rightArm, leftForearm, rightForearm,
            leftLeg, rightLeg, leftShin, rightShin;

    public DrawnModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.neck = torso.getChild("neck");
        this.head = neck.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.leftArm = torso.getChild("left_arm");
        this.rightArm = torso.getChild("right_arm");
        this.leftForearm = leftArm.getChild("forearm");
        this.rightForearm = rightArm.getChild("forearm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftShin = leftLeg.getChild("shin");
        this.rightShin = rightLeg.getChild("shin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        for (int side : new int[]{-1, 1}) {
            String name = side < 0 ? "right_leg" : "left_leg";
            PartDefinition leg = r.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 16).mirror(side > 0)
                    .addBox(-1.5F, 0F, -1.5F, 3, 9, 3), PartPose.offsetAndRotation(side * 2.0F, 7.5F, 0F, -0.35F, 0F, 0F));
            leg.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(12, 16).mirror(side > 0)
                    .addBox(-1F, 0F, -1F, 2, 9, 2), PartPose.offsetAndRotation(0F, 8.5F, 0F, 0.6F, 0F, 0F));
        }
        PartDefinition torso = r.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, -12F, -2F, 7, 12, 4), PartPose.offsetAndRotation(0F, 8F, 0F, 0.62F, 0F, 0F));
        PartDefinition neck = torso.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1F, -4F, -1F, 2, 4, 2), PartPose.offsetAndRotation(0F, -12F, -0.5F, 0.55F, 0F, 0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-2.5F, -5F, -4F, 5, 5, 6), PartPose.offsetAndRotation(0F, -3.5F, 0F, -1.05F, 0F, 0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0F, -9F, -5F, 0, 10, 10), PartPose.offsetAndRotation(2.5F, -2F, 0F, 0F, 0.7F, -0.35F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 32).mirror()
                .addBox(0F, -9F, -5F, 0, 10, 10), PartPose.offsetAndRotation(-2.5F, -2F, 0F, 0F, -0.7F, 0.35F));
        for (int side : new int[]{-1, 1}) {
            String name = side < 0 ? "right_arm" : "left_arm";
            PartDefinition arm = torso.addOrReplaceChild(name, CubeListBuilder.create().texOffs(20, 16).mirror(side > 0)
                    .addBox(-1F, 0F, -1F, 2, 11, 2), PartPose.offsetAndRotation(side * 4.2F, -11F, 0F, -0.75F, 0F, side * -0.12F));
            arm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(28, 16).mirror(side > 0)
                    .addBox(-1F, 0F, -1F, 2, 12, 2).texOffs(36, 16).addBox(-1.5F, 12F, -1.5F, 3, 3, 1),
                    PartPose.offsetAndRotation(0F, 10.5F, 0F, 0.15F, 0F, 0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(DrawnEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        float walk = limbSwing * 0.55F, amt = Math.min(1F, limbSwingAmount * 1.4F);
        leftLeg.xRot += Mth.cos(walk) * 0.9F * amt;
        rightLeg.xRot += Mth.cos(walk + Mth.PI) * 0.9F * amt;
        leftShin.xRot += Math.max(0F, Mth.sin(walk)) * 0.6F * amt;
        rightShin.xRot += Math.max(0F, Mth.sin(walk + Mth.PI)) * 0.6F * amt;
        // Arms hang and sway out of step, a little late, like dead weight.
        leftArm.xRot += Mth.cos(walk + Mth.PI - 0.6F) * 0.5F * amt + Mth.sin(ageInTicks * 0.05F) * 0.04F;
        rightArm.xRot += Mth.cos(walk - 0.6F) * 0.5F * amt - Mth.sin(ageInTicks * 0.05F) * 0.04F;
        torso.xRot += Mth.sin(ageInTicks * 0.04F) * 0.03F + amt * 0.12F;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.7F;
        neck.xRot += headPitch * Mth.DEG_TO_RAD * 0.4F;
        // The membranes never stop moving; when it hunts they flare wide and tremble.
        boolean hunting = e.getTarget() != null || e.heard() != null;
        float flare = hunting ? 0.35F : 0F, tremble = Mth.sin(ageInTicks * (hunting ? 1.3F : 0.25F)) * (hunting ? 0.09F : 0.05F);
        leftEar.yRot += flare + tremble;
        rightEar.yRot -= flare + tremble;
        leftEar.zRot -= tremble * 0.5F;
        rightEar.zRot += tremble * 0.5F;
        if (attackTime > 0) {
            float a = Mth.sin(attackTime * Mth.PI);
            leftArm.xRot -= a * 1.8F;
            rightArm.xRot -= a * 1.6F;
        }
    }
}
