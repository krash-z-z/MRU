package com.example.client.gui.hud;

import com.example.render.post.BlurUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.List;

public final class HudOverlay {
    public static final HudOverlay INSTANCE = new HudOverlay();

    private HudOverlay() {}

    public void extract(GuiGraphicsExtractor graphics) {
        HudManager.INSTANCE.extract(graphics);
    }

    public List<BlurUtil.Shape> blurBoxes(float guiScale) {
        return HudManager.INSTANCE.blurBoxes(guiScale);
    }

    public boolean toggleEditor() {
        return HudManager.INSTANCE.toggleEditor();
    }

    public void closeEditor() {
        HudManager.INSTANCE.closeEditor();
    }

    public boolean getEditorOpen() {
        return HudManager.INSTANCE.getEditorOpen();
    }

    public boolean mouseClicked(int button, int action) {
        return HudManager.INSTANCE.mouseClicked(button, action);
    }

    public boolean mouseReleased(int action) {
        return HudManager.INSTANCE.mouseReleased(action);
    }

    public boolean mouseScrolled(double horizontal, double vertical) {
        return HudManager.INSTANCE.mouseScrolled(horizontal, vertical);
    }

    public boolean onKeyPressed(int key) {
        return HudManager.INSTANCE.onKeyPressed(key);
    }
}
