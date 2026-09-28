package com.example.client.mixin;

import com.example.render.blur.DoubleKawaseBlurRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void modOnRenderHead(CallbackInfo ci) {
        DoubleKawaseBlurRenderer.INSTANCE.setBlurRenderedThisFrame(false);
    }

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void modOnProcessBlurEffect(CallbackInfo ci) {
        ci.cancel();
        if (!DoubleKawaseBlurRenderer.INSTANCE.isBlurRenderedThisFrame()) {
            DoubleKawaseBlurRenderer.INSTANCE.renderIfVisible();
        }
    }
}
