package com.trainguy9512.animationoverhaul.mixin.debug;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MixinPlayerSkin.class)
public class MixinPlayerSkin {

    private static final ResourceLocation debugCapeLocation = new ResourceLocation("textures/testcape.png");

    @Inject(method = "capeTexture", at = @At("HEAD"), cancellable = true)
    private void useDebugCapeTexture(CallbackInfoReturnable<ResourceLocation> cir){
        cir.setReturnValue(debugCapeLocation);
    }
}
