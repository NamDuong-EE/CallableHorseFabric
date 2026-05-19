package club.someoneice.callablehorse.client;

import club.someoneice.callablehorse.core.HorseFeatures;
import club.someoneice.callablehorse.mixin.HorseModelAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.ChestedHorseModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;

public class HorseChestLayer extends RenderLayer<Horse, HorseModel<Horse>> {
    private static final ResourceLocation DONKEY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/horse/donkey.png");
    private final ModelPart leftChest;
    private final ModelPart rightChest;

    public HorseChestLayer(RenderLayerParent<Horse, HorseModel<Horse>> parent) {
        super(parent);
        ModelPart body = ChestedHorseModel.createBodyLayer().bakeRoot().getChild("body");
        this.leftChest = body.getChild("left_chest");
        this.rightChest = body.getChild("right_chest");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Horse horse, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!HorseFeatures.hasCustomChest(horse) || horse.isBaby()) return;

        poseStack.pushPose();
        ((HorseModelAccess) this.getParentModel()).getBody().translateAndRotate(poseStack);
        var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(DONKEY_TEXTURE));
        this.leftChest.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        this.rightChest.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
