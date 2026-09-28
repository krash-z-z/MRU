package com.example.client.mixin;

import com.example.client.gui.hud.HudManager;
import com.example.client.gui.hud.HudOverlay;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void modOnKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        if (action == GLFW.GLFW_PRESS) {
            if (event.key() == GLFW.GLFW_KEY_O || event.key() == GLFW.GLFW_KEY_INSERT) {
                if (mc.gui.screen() == null || HudOverlay.INSTANCE.getEditorOpen()) {
                    HudOverlay.INSTANCE.toggleEditor();
                    ci.cancel();
                    return;
                }
            }

            if (event.key() == GLFW.GLFW_KEY_H && mc.gui.screen() == null && !HudOverlay.INSTANCE.getEditorOpen()) {
                HudManager.INSTANCE.toggle();
                ci.cancel();
                return;
            }

            if (HudOverlay.INSTANCE.getEditorOpen()) {
                if (HudOverlay.INSTANCE.onKeyPressed(event.key())) {
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
