package com.example.client.mixin;

import com.example.client.gui.hud.HudOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void modTurnPlayer(double d, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() != null) return;
        if (HudOverlay.INSTANCE.getEditorOpen()) {
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void modGrabMouse(CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() != null) return;
        if (HudOverlay.INSTANCE.getEditorOpen()) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void modOnButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() != null) return;

        if (HudOverlay.INSTANCE.getEditorOpen()) {
            int button = buttonInfo.button();

            if (action == GLFW.GLFW_RELEASE) {
                HudOverlay.INSTANCE.mouseReleased(action);
                ci.cancel();
                return;
            }

            if (action == GLFW.GLFW_PRESS) {
                if (HudOverlay.INSTANCE.mouseClicked(button, action)) {
                    ci.cancel();
                    return;
                }
                ci.cancel();
            }
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void modOnScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() != null) return;

        if (HudOverlay.INSTANCE.getEditorOpen()) {
            HudOverlay.INSTANCE.mouseScrolled(horizontal, vertical);
            ci.cancel();
        }
    }
}
