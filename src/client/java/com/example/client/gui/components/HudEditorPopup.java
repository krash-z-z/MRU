package com.example.client.gui.components;

import com.example.client.gui.hud.HudWidget;
import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.Easing;
import com.example.render.post.BlurUtil;
import com.example.render.msdf.FontRenderer;
import com.example.render.shape.DrawUtility;
import com.example.render.util.ScissorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HudEditorPopup {
    private static class WidgetRowEntry {
        final HudWidget widget;
        final Animation hoverAnimation = new Animation(220L, Easing.SMOOTH_IN_OUT);
        final Animation activeAnimation = new Animation(280L, Easing.BAKEK);

        WidgetRowEntry(HudWidget widget) {
            this.widget = widget;
        }
    }

    private boolean open = false;
    private float x = 0.0f;
    private float y = 0.0f;
    private float width = 164.0f;
    private float height = 140.0f;

    private static final float MAX_VISIBLE_BOX_HEIGHT = 126.0f;
    private static final float ROW_HEIGHT = 18.0f;

    private float scrollTarget = 0.0f;
    private final Animation scrollAnim = new Animation(200L, Easing.SMOOTH_IN_OUT);
    private final Animation scrollbarAlphaAnim = new Animation(250L, Easing.SMOOTH_IN_OUT);
    private final Animation openAnim = new Animation(480L, Easing.HUD_APPEAR);
    private final List<WidgetRowEntry> entries = new ArrayList<>();

    public boolean isOpen() {
        return open;
    }

    public boolean shouldDraw() {
        return open || openAnim.getValue() > 0.005f;
    }

    public void open(List<HudWidget> widgets, float mouseX, float mouseY, float screenW, float screenH) {
        this.open = true;
        this.openAnim.setDuration(480L);
        this.openAnim.setEasing(Easing.HUD_APPEAR);
        this.openAnim.reset(0.0f);
        this.openAnim.update(1.0f);

        this.entries.clear();
        for (HudWidget w : widgets) {
            this.entries.add(new WidgetRowEntry(w));
        }

        this.scrollTarget = 0.0f;
        this.scrollAnim.reset(0.0f);

        this.width = 164.0f;
        this.height = calculateHeight();

        float spawnX = Math.max(10.0f, Math.min(screenW - width - 10.0f, mouseX - width * 0.5f));
        float spawnY = (mouseY + height + 10.0f <= screenH - 10.0f)
            ? mouseY + 6.0f
            : Math.max(10.0f, mouseY - height - 6.0f);

        this.x = spawnX;
        this.y = spawnY;
    }

    public void close() {
        if (!this.open) return;
        this.open = false;
        this.openAnim.setDuration(440L);
        this.openAnim.setEasing(Easing.HUD_DISAPPEAR);
        this.openAnim.update(0.0f);
    }

    private float getTotalListHeight() {
        return 6.0f + entries.size() * ROW_HEIGHT;
    }

    private float getListBoxHeight() {
        return Math.min(MAX_VISIBLE_BOX_HEIGHT, getTotalListHeight());
    }

    private float calculateHeight() {
        return 28.0f + getListBoxHeight() + 8.0f;
    }

    private float getMaxScroll() {
        return Math.max(0.0f, getTotalListHeight() - getListBoxHeight());
    }

    public boolean mouseScrolled(float mouseX, float mouseY, double amount) {
        if (!isOpen() || entries.isEmpty()) return false;
        boolean inside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (inside) {
            float max = getMaxScroll();
            if (max > 0.0f) {
                this.scrollTarget = Math.max(0.0f, Math.min(max, scrollTarget - (float) amount * ROW_HEIGHT));
                this.scrollAnim.update(scrollTarget);
                return true;
            }
        }
        return false;
    }

    public void update(float mouseX, float mouseY) {
        openAnim.update(open ? 1.0f : 0.0f);
        if (!shouldDraw() || entries.isEmpty()) return;

        this.height = calculateHeight();
        float maxScroll = getMaxScroll();
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
        scrollAnim.update(scrollTarget);
        float currentScroll = scrollAnim.getValue();

        float boxX = x + 5.0f;
        float boxY = y + 28.0f;
        float boxW = width - 10.0f;
        float boxH = getListBoxHeight();

        boolean boxHovered = mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH;
        scrollbarAlphaAnim.update(boxHovered || Math.abs(scrollAnim.getValue() - scrollTarget) > 0.5f);

        for (int i = 0; i < entries.size(); i++) {
            WidgetRowEntry entry = entries.get(i);
            float itemY = boxY + 3.0f + i * ROW_HEIGHT - currentScroll;
            boolean itemHovered = boxHovered && mouseX >= boxX + 2.0f && mouseX <= boxX + boxW - 2.0f &&
                                  mouseY >= Math.max(boxY, itemY) && mouseY <= Math.min(boxY + boxH, itemY + ROW_HEIGHT);
            entry.hoverAnimation.update(itemHovered);
            entry.activeAnimation.update(entry.widget.getEnabled());
        }
    }

    public void render(GuiGraphicsExtractor graphics, float mouseX, float mouseY) {
        if (!shouldDraw() || entries.isEmpty()) return;
        this.height = calculateHeight();

        openAnim.update(open ? 1.0f : 0.0f);
        float menuScale = openAnim.getValue();
        if (menuScale <= 0.005f) return;

        float boxRadius = 12.0f;
        float cornerSmoothing = 0.85f;

        float slideY = (1.0f - Math.min(1.0f, menuScale)) * 6.0f;
        float popupAlpha = Math.min(1.0f, Math.max(0.0f, menuScale));

        float currentW = width * menuScale;
        float currentH = height * menuScale;
        float currentX = x + (width - currentW) * 0.5f;
        float currentY = y + slideY + (height - currentH) * 0.5f;
        float currentRadius = boxRadius * menuScale;

        BlurUtil.INSTANCE.apply(currentX, currentY, currentW, currentH, currentRadius, cornerSmoothing, true, popupAlpha, true);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width * 0.5f, y + height * 0.5f + slideY);
        graphics.pose().scale(menuScale, menuScale);
        graphics.pose().translate(-(x + width * 0.5f), -(y + height * 0.5f));

        int boxBg = ((int) (95.0f * popupAlpha) << 24) | 0x000A0A0D;
        DrawUtility.drawSquircle(graphics, x, y, width, height, boxRadius, cornerSmoothing, boxBg);
        DrawUtility.drawRoundedOutline(graphics, x, y, width, height, boxRadius, cornerSmoothing, ((int) (40.0f * popupAlpha) << 24) | 0x00FFFFFF);

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfSemibold,
            "HUD Elements",
            x + 12.0f,
            y + 7.5f,
            8.5f,
            ((int) (255.0f * popupAlpha) << 24) | 0x00FFFFFF
        );

        long activeCount = entries.stream().filter(e -> e.widget.getEnabled()).count();
        String countText = activeCount + " of " + entries.size() + " active";
        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfMedium,
            countText,
            x + 12.0f,
            y + 17.5f,
            6.0f,
            UITheme.INSTANCE.accentAlpha(popupAlpha * 0.75f)
        );

        float boxX = x + 5.0f;
        float boxY = y + 28.0f;
        float boxW = width - 10.0f;
        float boxH = getListBoxHeight();

        DrawUtility.drawSquircle(graphics, boxX, boxY, boxW, boxH, 7.0f, 0.7f, 0x40111114);
        DrawUtility.drawRoundedOutline(graphics, boxX, boxY, boxW, boxH, 7.0f, 0.7f, 0x15FFFFFF);

        float currentScroll = scrollAnim.getValue();

        ScissorUtil.run(graphics, boxX, boxY + 1.0f, boxW, boxH - 2.0f, () -> {
            for (int i = 0; i < entries.size(); i++) {
                WidgetRowEntry entry = entries.get(i);
                float itemY = boxY + 3.0f + i * ROW_HEIGHT - currentScroll;
                if (itemY + ROW_HEIGHT < boxY || itemY > boxY + boxH) continue;

                float activeVal = entry.activeAnimation.getValue();
                float hoverVal = entry.hoverAnimation.getValue();

                if (hoverVal > 0.01f) {
                    int rowHoverAlpha = Math.max(0, Math.min(255, (int) (20.0f * hoverVal)));
                    DrawUtility.drawSquircle(graphics, boxX + 2.0f, itemY, boxW - 4.0f, ROW_HEIGHT, 5.0f, 0.6f, (rowHoverAlpha << 24) | 0x00FFFFFF);
                }

                int itemColor = UITheme.INSTANCE.highlightColor(hoverVal, activeVal);

                FontRenderer.INSTANCE.draw(
                    graphics,
                    FontRenderer.Face.SfRegular,
                    entry.widget.getTitle(),
                    boxX + 9.0f,
                    itemY + (ROW_HEIGHT - 7.5f) * 0.5f,
                    7.5f,
                    itemColor
                );

                float dotR = 2.5f;
                float dotX = boxX + boxW - 10.0f;
                float dotY = itemY + ROW_HEIGHT * 0.5f;
                if (activeVal > 0.01f) {
                    int dotAlpha = Math.max(0, Math.min(255, (int) (255.0f * activeVal)));
                    DrawUtility.drawCircle(graphics, dotX, dotY, dotR, (dotAlpha << 24) | (UITheme.INSTANCE.accent() & 0x00FFFFFF));
                } else {
                    DrawUtility.drawCircle(graphics, dotX, dotY, dotR, 0x33FFFFFF);
                }
            }
        });

        float maxScroll = getMaxScroll();
        if (maxScroll > 0.0f) {
            float sbAlpha = scrollbarAlphaAnim.getValue() * popupAlpha;
            if (sbAlpha > 0.01f) {
                float totalH = getTotalListHeight();
                float barH = Math.max(14.0f, boxH * (boxH / totalH));
                float barY = boxY + (boxH - barH) * (currentScroll / maxScroll);
                float barX = boxX + boxW - 3.5f;
                int barColor = ((int) (90.0f * sbAlpha) << 24) | 0x00FFFFFF;
                DrawUtility.drawSquircle(graphics, barX, barY, 2.0f, barH, 1.0f, 0.5f, barColor);
            }
        }

        graphics.pose().popMatrix();
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (!isOpen() || entries.isEmpty()) return false;

        boolean inside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float boxX = x + 5.0f;
            float boxY = y + 28.0f;
            float boxW = width - 10.0f;
            float boxH = getListBoxHeight();
            float currentScroll = scrollAnim.getValue();

            if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
                for (int i = 0; i < entries.size(); i++) {
                    WidgetRowEntry entry = entries.get(i);
                    float itemY = boxY + 3.0f + i * ROW_HEIGHT - currentScroll;
                    if (mouseY >= itemY && mouseY <= itemY + ROW_HEIGHT) {
                        entry.widget.setEnabled(!entry.widget.getEnabled());
                        return true;
                    }
                }
            }
            return inside;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (inside) return true;
            close();
            return false;
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        return false;
    }

    public boolean keyPressed(int key) {
        if (isOpen() && key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return false;
    }
}
