package com.example.client.mixin;

import com.example.render.blur.DoubleKawaseBlurRenderer;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void modRenderDoubleKawaseBlur(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null || mc.gui.screen() != null) {
            DoubleKawaseBlurRenderer.INSTANCE.renderIfVisible();
        }
    }

    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void modPreventZeroScissor(ScreenRectangle rect, RenderPass renderPass, CallbackInfo ci) {
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
            renderPass.disableScissor();
            ci.cancel();
        }
    }
}
