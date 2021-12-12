package com.trainguy.animationoverhaul.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.trainguy.animationoverhaul.AnimationOverhaul;
import com.trainguy.animationoverhaul.access.EntityAccess;
import com.trainguy.animationoverhaul.access.LivingEntityAccess;
import com.trainguy.animationoverhaul.animations.LivingEntityAnimator;
import com.trainguy.animationoverhaul.util.animation.LocatorRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {
    protected MixinLivingEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Shadow protected abstract float getBob(T livingEntity, float f);
    @Shadow protected M model;
    @Shadow public abstract M getModel();

    @Shadow protected abstract void setupRotations(T livingEntity, PoseStack poseStack, float f, float g, float h);

    @Shadow protected abstract float getFlipDegrees(T livingEntity);

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;isBodyVisible(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private void setPartController(T livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci){
        ResourceLocation entityAnimatorResourceLocation = new ResourceLocation(livingEntity.getType().toShortString());
        if(AnimationOverhaul.ENTITY_ANIMATORS.containsKey(entityAnimatorResourceLocation)){
            LivingEntityAnimator<T, M> livingEntityAnimator = AnimationOverhaul.ENTITY_ANIMATORS.get(new ResourceLocation(livingEntity.getType().toShortString()));
            assert livingEntityAnimator != null;
            livingEntityAnimator.setProperties(livingEntity, this.model, g);
            livingEntityAnimator.animate();
        }
    }



    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"))
    private void overwriteSetupRotations(LivingEntityRenderer<T,M> instance, T livingEntity, PoseStack poseStack, float bob, float bodyRot, float frameTime){

        //poseStack.translate(Mth.sin(bob / 6), 0, 0);
        //poseStack.mulPose(Vector3f.ZP.rotation(Mth.sin(bob / 6) / 4));


        if(shouldUseAlternateRotations(livingEntity)){

            poseStack.popPose();
            poseStack.pushPose();

            if(livingEntity.getPose() == Pose.SLEEPING){
                Direction i = ((LivingEntity)livingEntity).getBedOrientation();
                float j = i != null ? sleepDirectionToRotation(i) : bodyRot;
                poseStack.mulPose(Vector3f.YP.rotationDegrees(j - 90));
            } else {
                if(livingEntity.isPassenger() && livingEntity.getVehicle() instanceof AbstractMinecart){
                    bodyRot = Mth.rotLerp(frameTime, ((LivingEntity)livingEntity).yHeadRotO, ((LivingEntity)livingEntity).yHeadRot);
                }
                poseStack.mulPose(Vector3f.YP.rotationDegrees(180 - bodyRot));
            }

        } else {
            this.setupRotations(livingEntity, poseStack, bob, bodyRot, frameTime);
        }
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0))
    private void removeBedTranslation(PoseStack instance, double d, double e, double f, T livingEntity){
        if(shouldUseAlternateRotations(livingEntity)){

        } else {
            instance.translate(d, e, f);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private void translateAndRotateAfterScale(T livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci){
        if(shouldUseAlternateRotations(livingEntity)){
            LocatorRig locatorRig = ((LivingEntityAccess)livingEntity).getLocatorRig();

            locatorRig.getLocator("master", false).translateAndRotatePoseStack(poseStack);
        }
    }

    private boolean shouldUseAlternateRotations(LivingEntity livingEntity){
        LocatorRig locatorRig = ((LivingEntityAccess)livingEntity).getLocatorRig();
        if(locatorRig != null){
            if(locatorRig.containsLocator("master")){
                return true;
            }
        }
        return false;
    }

    private static float sleepDirectionToRotation(Direction direction) {
        switch (direction) {
            case SOUTH: {
                return 90.0f;
            }
            case WEST: {
                return 0.0f;
            }
            case NORTH: {
                return 270.0f;
            }
            case EAST: {
                return 180.0f;
            }
        }
        return 0.0f;
    }
}
