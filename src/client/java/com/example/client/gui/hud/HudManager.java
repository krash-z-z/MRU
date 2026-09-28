package com.example.client.gui.hud;

import com.example.client.gui.HudConfig;
import com.example.client.gui.components.HudEditorPopup;
import com.example.client.gui.components.WidgetSettingsPopup;
import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.Easing;
import com.example.render.post.BlurUtil;
import com.example.render.blur.ShadowBlurUtil;
import com.example.render.msdf.FontRenderer;
import com.example.render.shape.DrawUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class HudManager {
    public static final HudManager INSTANCE = new HudManager();

    private HudManager() {}

    private final List<HudWidget> widgets = new ArrayList<>(List.of(
        new TargetInfoWidget(),
        new LinearGradientWidget(),
        new SunsetPeachWidget(),
        new LiquidGlassWidget()
    ));

    private final Map<HudWidget, WidgetAnimationState> widgetAnimationStates = new HashMap<>();
    private final Map<HudWidget, Float> dragScales = new HashMap<>();
    private final Animation editorLinesFadeAnim = new Animation(240L, Easing.QUAD_OUT);
    private final Map<HudWidget, Animation> widgetLineBrightAnims = new HashMap<>();
    private final Animation snapLinesFadeAnim = new Animation(160L, Easing.QUAD_OUT);
    private final List<Float> lastSnapLinesX = new ArrayList<>();
    private final List<Float> lastSnapLinesY = new ArrayList<>();

    private static final float DRAG_SCALE_TARGET = 1.03f;
    private static final float DRAG_SCALE_ENGAGE_LERP = 0.12f;
    private static final float DRAG_SCALE_RELEASE_LERP = 0.08f;

    private boolean editorOpen = false;
    private boolean hudEnabled = true;

    private HudWidget dragging = null;
    private float dragOffsetX = 0.0f;
    private float dragOffsetY = 0.0f;
    private final List<Float> activeAlignedLinesX = new ArrayList<>();
    private final List<Float> activeAlignedLinesY = new ArrayList<>();

    private final WidgetSettingsPopup settingsPopup = new WidgetSettingsPopup();
    private final HudEditorPopup editorPopup = new HudEditorPopup();

    public boolean getEditorOpen() {
        return editorOpen;
    }

    public boolean isHudEnabled() {
        return hudEnabled;
    }

    public void setHudEnabled(boolean enabled) {
        this.hudEnabled = enabled;
    }

    public void toggle() {
        this.hudEnabled = !this.hudEnabled;
    }

    public boolean isDragging(HudWidget widget) {
        return dragging == widget;
    }

    public HudWidget getDragging() {
        return dragging;
    }

    public List<HudWidget> getWidgets() {
        return widgets;
    }

    public boolean toggleEditor() {
        Minecraft mc = Minecraft.getInstance();
        editorOpen = !editorOpen;
        dragging = null;
        activeAlignedLinesX.clear();
        activeAlignedLinesY.clear();
        if (!editorOpen) {
            settingsPopup.close();
            editorPopup.close();
        }
        if (editorOpen) {
            mc.mouseHandler.releaseMouse();
        } else if (mc.gui.screen() == null) {
            mc.mouseHandler.grabMouse();
        }
        return true;
    }

    public void closeEditor() {
        if (!editorOpen) return;
        editorOpen = false;
        dragging = null;
        activeAlignedLinesX.clear();
        activeAlignedLinesY.clear();
        settingsPopup.close();
        editorPopup.close();
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() == null) {
            mc.mouseHandler.grabMouse();
        }
    }

    public void extract(GuiGraphicsExtractor graphics) {
        if (!hudEnabled && !editorOpen) return;

        Minecraft mc = Minecraft.getInstance();
        float mouseX = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float mouseY = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());

        updateDragging(mc, mouseX, mouseY);
        updateDragScales();
        if (settingsPopup.isOpen()) {
            settingsPopup.update(mouseX, mouseY);
        }
        if (editorPopup.isOpen()) {
            editorPopup.update(mouseX, mouseY);
        }

        editorLinesFadeAnim.update(editorOpen ? 1.0f : 0.0f);
        float editorFade = editorLinesFadeAnim.getValue();
        if (editorFade > 0.005f) {
            renderEditorAlignmentGuides(graphics, mc, editorFade);
            renderEditorHeader(graphics, mc, editorFade);
        }

        for (HudWidget widget : widgets) {
            boolean shouldRender = widget.visible(mc, editorOpen);
            WidgetAnimationState state = widgetAnimationStates.computeIfAbsent(widget, w -> new WidgetAnimationState(shouldRender));
            state.update(shouldRender);
            if (state.shouldDraw()) {
                renderAnimatedWidget(graphics, mc, widget, state);
            }
        }
        widgetAnimationStates.entrySet().removeIf(e -> !e.getValue().shouldDraw());

        if (settingsPopup.shouldDraw()) {
            settingsPopup.render(graphics, mouseX, mouseY);
        }
        if (editorPopup.shouldDraw()) {
            editorPopup.render(graphics, mouseX, mouseY);
        }
    }

    public List<BlurUtil.Shape> blurBoxes(float guiScale) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return List.of();
        List<BlurUtil.Shape> result = new ArrayList<>();
        float tintStrength = HudConfig.INSTANCE.blurOpacity.getValue();
        for (HudWidget widget : widgets) {
            WidgetAnimationState state = widgetAnimationStates.computeIfAbsent(widget, w -> new WidgetAnimationState(w.visible(mc, editorOpen)));
            boolean drawable = state.shouldDraw();
            if (!drawable) continue;
            result.addAll(animatedBlurBoxes(widget, guiScale, tintStrength, state));
        }
        return result;
    }

    private List<BlurUtil.Shape> animatedBlurBoxes(HudWidget widget, float guiScale, float tintStrength, WidgetAnimationState state) {
        List<BlurUtil.Shape> boxes = widget.blurBoxes(guiScale, tintStrength);
        if (boxes.isEmpty()) return List.of();
        float scale = state != null ? Math.max(0.0f, state.visibility.getValue()) : 1.0f;
        float dScale = dragScales.getOrDefault(widget, 1.0f);
        float widgetScale = widget.getScale();
        float combinedScale = scale * dScale * widgetScale;
        if (combinedScale < 0.005f) return List.of();

        float width = Math.max(1.0f, widget.bounds.width);
        float height = Math.max(1.0f, widget.bounds.height);
        float pivotX = widget.bounds.x + width * 0.5f;
        float pivotY = widget.bounds.y + height * 0.5f;

        float animAlpha = Math.max(0.0f, Math.min(1.0f, scale));
        float slideY = (1.0f - Math.min(1.0f, scale)) * 3.0f;

        List<BlurUtil.Shape> result = new ArrayList<>(boxes.size());
        for (BlurUtil.Shape box : boxes) {
            float boxX = box.x() / guiScale;
            float boxY = box.y() / guiScale;
            float boxW = box.width() / guiScale;
            float boxH = box.height() / guiScale;
            float boxRTL = box.rTL() / guiScale;
            float boxRTR = box.rTR() / guiScale;
            float boxRBR = box.rBR() / guiScale;
            float boxRBL = box.rBL() / guiScale;

            float scaledX = (pivotX + (boxX - pivotX) * combinedScale) * guiScale;
            float scaledY = (pivotY + slideY + (boxY - pivotY) * combinedScale) * guiScale;
            float scaledW = boxW * combinedScale * guiScale;
            float scaledH = boxH * combinedScale * guiScale;

            result.add(new BlurUtil.Shape(
                scaledX,
                scaledY,
                scaledW,
                scaledH,
                boxRTL * combinedScale * guiScale,
                boxRTR * combinedScale * guiScale,
                boxRBR * combinedScale * guiScale,
                boxRBL * combinedScale * guiScale,
                box.smoothing(),
                box.squircle(),
                box.tintStrength() * animAlpha,
                box.shadow(),
                box.blurType(),
                box.liquidGlass(),
                box.refraction(),
                box.chromaticAberration(),
                box.highlight(),
                box.sampleEscape() * combinedScale,
                box.shapeMode(),
                box.blurStrength()
            ));
        }
        return result;
    }

    private void renderEditorHeader(GuiGraphicsExtractor graphics, Minecraft mc, float alpha) {
        float screenW = mc.getWindow().getGuiScaledWidth();
        float topBarH = 28.0f;
        float y = 10.0f;
        float x = (screenW - 400.0f) * 0.5f;
        float w = 400.0f;

        float slideY = (1.0f - alpha) * -20.0f;
        float curX = x + (w - w * alpha) * 0.5f;
        float curY = y + slideY;
        float curW = w * alpha;
        float curH = topBarH * alpha;
        float curR = 14.0f * alpha;

        if (alpha > 0.01f) {
            BlurUtil.INSTANCE.apply(curX, curY, curW, curH, curR, 0.85f, true, alpha, true);
        }

        int bgAlpha = Math.max(0, Math.min(255, (int) (90.0f * alpha)));
        int outlineAlpha = Math.max(0, Math.min(255, (int) (38.0f * alpha)));
        int titleAlpha = Math.max(0, Math.min(255, (int) (255.0f * alpha)));
        int hintAlpha = Math.max(0, Math.min(255, (int) (150.0f * alpha)));
        int dotAlpha = Math.max(0, Math.min(255, (int) (200.0f * alpha)));

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + w * 0.5f, y + topBarH * 0.5f + slideY);
        graphics.pose().scale(alpha, alpha);
        graphics.pose().translate(-(x + w * 0.5f), -(y + topBarH * 0.5f));

        DrawUtility.drawSquircle(graphics, x, y, w, topBarH, 14.0f, 0.85f, (bgAlpha << 24) | 0x000A0A0D);
        DrawUtility.drawRoundedOutline(graphics, x, y, w, topBarH, 14.0f, 0.85f, (outlineAlpha << 24) | 0x00FFFFFF);

        DrawUtility.drawCircle(graphics, x + 16.0f, y + topBarH * 0.5f, 3.0f, UITheme.INSTANCE.accentAlpha(alpha));

        String title = "HUD Editor";
        float titleFontSize = 8.5f;
        float titleW = FontRenderer.INSTANCE.width(FontRenderer.Face.SfSemibold, title, titleFontSize);
        FontRenderer.INSTANCE.draw(graphics, FontRenderer.Face.SfSemibold, title, x + 26.0f, y + (topBarH - titleFontSize) * 0.5f, titleFontSize, (titleAlpha << 24) | 0x00FFFFFF);

        float sepX = x + 26.0f + titleW + 7.0f;
        DrawUtility.drawCircle(graphics, sepX, y + topBarH * 0.5f, 1.5f, (dotAlpha << 24) | 0x00FFFFFF);

        FontRenderer.INSTANCE.draw(graphics, FontRenderer.Face.SfRegular, "Drag · Right-click for settings · Empty space for list", sepX + 8.0f, y + (topBarH - 7.0f) * 0.5f, 7.0f, (hintAlpha << 24) | 0x00FFFFFF);

        graphics.pose().popMatrix();
    }

    private void renderAnimatedWidget(GuiGraphicsExtractor graphics, Minecraft mc, HudWidget widget, WidgetAnimationState state) {
        float width = Math.max(1.0f, widget.bounds.width);
        float height = Math.max(1.0f, widget.bounds.height);
        float scale = Math.max(0.0f, state.visibility.getValue());
        float dScale = dragScales.getOrDefault(widget, 1.0f);
        float widgetScale = widget.getScale();
        float combinedScale = scale * dScale * widgetScale;
        if (combinedScale < 0.005f) return;

        float pivotX = widget.bounds.x + width * 0.5f;
        float pivotY = widget.bounds.y + height * 0.5f;

        float animAlpha = Math.max(0.0f, Math.min(1.0f, scale));
        widget.setAnimationAlpha(animAlpha);

        float slideY = (1.0f - Math.min(1.0f, scale)) * 3.0f;

        graphics.pose().pushMatrix();
        graphics.pose().translate(pivotX, pivotY + slideY);
        graphics.pose().scale(combinedScale, combinedScale);
        graphics.pose().translate(-pivotX, -pivotY);

        widget.render(graphics, mc, editorOpen);

        graphics.pose().popMatrix();
    }

    private void updateDragScales() {
        for (HudWidget widget : widgets) {
            float current = dragScales.getOrDefault(widget, 1.0f);
            boolean isDragging = (dragging == widget);
            float target = isDragging ? DRAG_SCALE_TARGET : 1.0f;
            float lerp = isDragging ? DRAG_SCALE_ENGAGE_LERP : DRAG_SCALE_RELEASE_LERP;
            float next = current + (target - current) * lerp;
            dragScales.put(widget, Math.abs(next - 1.0f) < 0.001f && !isDragging ? 1.0f : next);
        }
    }

    private void updateDragging(Minecraft mc, float mouseX, float mouseY) {
        if (dragging == null || !editorOpen) return;

        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();
        float widgetW = dragging.bounds.width;
        float widgetH = dragging.bounds.height;

        float targetX = mouseX - dragOffsetX;
        float targetY = mouseY - dragOffsetY;

        activeAlignedLinesX.clear();
        activeAlignedLinesY.clear();

        float snapThreshold = 4.0f;

        List<Float> refLinesX = new ArrayList<>();
        List<Float> refLinesY = new ArrayList<>();

        refLinesX.add(10.0f);
        refLinesX.add(screenW - 10.0f);
        refLinesX.add(screenW * 0.5f);

        refLinesY.add(10.0f);
        refLinesY.add(screenH - 10.0f);
        refLinesY.add(screenH * 0.5f);

        for (HudWidget other : widgets) {
            if (other == dragging || !other.visible(mc, true)) continue;
            refLinesX.add(other.bounds.x);
            refLinesX.add(other.bounds.x + other.bounds.width);
            refLinesX.add(other.bounds.x + other.bounds.width * 0.5f);

            refLinesY.add(other.bounds.y);
            refLinesY.add(other.bounds.y + other.bounds.height);
            refLinesY.add(other.bounds.y + other.bounds.height * 0.5f);
        }

        float[] candidateX = { targetX, targetX + widgetW, targetX + widgetW * 0.5f };
        float bestDiffX = snapThreshold;
        float snapShiftX = 0.0f;
        float matchedRefX = -1.0f;

        for (int i = 0; i < 3; i++) {
            float cand = candidateX[i];
            for (float ref : refLinesX) {
                float diff = Math.abs(cand - ref);
                if (diff < bestDiffX) {
                    bestDiffX = diff;
                    snapShiftX = ref - cand;
                    matchedRefX = ref;
                }
            }
        }
        if (matchedRefX >= 0.0f) {
            targetX += snapShiftX;
            activeAlignedLinesX.add(matchedRefX);
        }

        float[] candidateY = { targetY, targetY + widgetH, targetY + widgetH * 0.5f };
        float bestDiffY = snapThreshold;
        float snapShiftY = 0.0f;
        float matchedRefY = -1.0f;

        for (int i = 0; i < 3; i++) {
            float cand = candidateY[i];
            for (float ref : refLinesY) {
                float diff = Math.abs(cand - ref);
                if (diff < bestDiffY) {
                    bestDiffY = diff;
                    snapShiftY = ref - cand;
                    matchedRefY = ref;
                }
            }
        }
        if (matchedRefY >= 0.0f) {
            targetY += snapShiftY;
            activeAlignedLinesY.add(matchedRefY);
        }

        targetX = Math.max(2.0f, Math.min(screenW - widgetW - 2.0f, targetX));
        targetY = Math.max(2.0f, Math.min(screenH - widgetH - 2.0f, targetY));

        dragging.bounds.x = targetX;
        dragging.bounds.y = targetY;
    }

    private void renderEditorAlignmentGuides(GuiGraphicsExtractor graphics, Minecraft mc, float linesAlpha) {
        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();

        int accent1 = UITheme.INSTANCE.accent();
        int accent2 = UITheme.INSTANCE.accentAlpha(0.6f);

        for (HudWidget widget : widgets) {
            if (!widget.visible(mc, true)) continue;

            Animation brightAnim = widgetLineBrightAnims.computeIfAbsent(widget, k -> new Animation(180L, Easing.QUAD_OUT));
            brightAnim.update((dragging == widget) ? 1.0f : 0.0f);
            float brightP = brightAnim.getValue();

            int alpha = Math.round((28.0f + (140.0f - 28.0f) * brightP) * linesAlpha);
            if (alpha <= 0) continue;

            float x1 = widget.bounds.x;
            float x2 = widget.bounds.x + widget.bounds.width;
            float y1 = widget.bounds.y;
            float y2 = widget.bounds.y + widget.bounds.height;

            int col = (alpha << 24) | (accent1 & 0x00FFFFFF);
            DrawUtility.drawSquircle(graphics, x1, 0.0f, 1.0f, screenH, 0.0f, 0.0f, col);
            DrawUtility.drawSquircle(graphics, x2, 0.0f, 1.0f, screenH, 0.0f, 0.0f, col);
            DrawUtility.drawSquircle(graphics, 0.0f, y1, screenW, 1.0f, 0.0f, 0.0f, col);
            DrawUtility.drawSquircle(graphics, 0.0f, y2, screenW, 1.0f, 0.0f, 0.0f, col);
        }

        boolean hasSnaps = !activeAlignedLinesX.isEmpty() || !activeAlignedLinesY.isEmpty();
        if (hasSnaps) {
            lastSnapLinesX.clear();
            lastSnapLinesX.addAll(activeAlignedLinesX);
            lastSnapLinesY.clear();
            lastSnapLinesY.addAll(activeAlignedLinesY);
        }
        snapLinesFadeAnim.update(hasSnaps ? 1.0f : 0.0f);
        float snapAlpha = snapLinesFadeAnim.getValue() * linesAlpha;

        if (snapAlpha > 0.005f) {
            int glowCol = ((int) (90.0f * snapAlpha) << 24) | (accent1 & 0x00FFFFFF);
            int coreCol = ((int) (240.0f * snapAlpha) << 24) | 0x00FFFFFF;

            for (float alignX : lastSnapLinesX) {
                DrawUtility.drawSquircle(graphics, alignX - 1.0f, 0.0f, 3.0f, screenH, 0.0f, 0.0f, glowCol);
                DrawUtility.drawSquircle(graphics, alignX, 0.0f, 1.0f, screenH, 0.0f, 0.0f, coreCol);
            }

            for (float alignY : lastSnapLinesY) {
                DrawUtility.drawSquircle(graphics, 0.0f, alignY - 1.0f, screenW, 3.0f, 0.0f, 0.0f, glowCol);
                DrawUtility.drawSquircle(graphics, 0.0f, alignY, screenW, 1.0f, 0.0f, 0.0f, coreCol);
            }
        }
    }

    public boolean mouseClicked(int button, int action) {
        if (!editorOpen) return false;
        if (action != GLFW.GLFW_PRESS) return false;

        Minecraft mc = Minecraft.getInstance();
        float mouseX = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float mouseY = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());

        if (settingsPopup.isOpen() && settingsPopup.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (editorPopup.isOpen() && editorPopup.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = widgets.size() - 1; i >= 0; i--) {
                HudWidget w = widgets.get(i);
                if (!w.visible(mc, true)) continue;
                if (contains(mouseX, mouseY, w.bounds)) {
                    dragging = w;
                    dragOffsetX = mouseX - w.bounds.x;
                    dragOffsetY = mouseY - w.bounds.y;
                    return true;
                }
            }
            return false;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            HudWidget hit = null;
            for (int i = widgets.size() - 1; i >= 0; i--) {
                HudWidget w = widgets.get(i);
                if (!w.visible(mc, true)) continue;
                if (contains(mouseX, mouseY, w.bounds)) {
                    hit = w;
                    break;
                }
            }

            if (hit != null) {
                editorPopup.close();
                settingsPopup.open(hit, mouseX, mouseY, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                return true;
            } else {
                settingsPopup.close();
                editorPopup.open(widgets, mouseX, mouseY, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(int action) {
        if (!editorOpen) return false;
        if (action == GLFW.GLFW_RELEASE) {
            if (settingsPopup.isOpen()) {
                settingsPopup.mouseReleased(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }
            if (editorPopup.isOpen()) {
                editorPopup.mouseReleased(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }
            if (dragging != null) {
                dragging = null;
                activeAlignedLinesX.clear();
                activeAlignedLinesY.clear();
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double horizontal, double vertical) {
        if (!editorOpen) return false;
        Minecraft mc = Minecraft.getInstance();
        float mouseX = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float mouseY = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());

        if (settingsPopup.isOpen() && settingsPopup.mouseScrolled(mouseX, mouseY, vertical)) {
            return true;
        }
        if (editorPopup.isOpen() && editorPopup.mouseScrolled(mouseX, mouseY, vertical)) {
            return true;
        }
        return false;
    }

    public boolean onKeyPressed(int key) {
        if (settingsPopup.isOpen()) {
            return settingsPopup.keyPressed(key);
        }
        if (editorPopup.isOpen()) {
            return editorPopup.keyPressed(key);
        }
        if (editorOpen && key == GLFW.GLFW_KEY_ESCAPE) {
            toggleEditor();
            return true;
        }
        return false;
    }

    private boolean contains(float mouseX, float mouseY, HudBounds bounds) {
        return mouseX >= bounds.x && mouseX <= bounds.x + bounds.width &&
               mouseY >= bounds.y && mouseY <= bounds.y + bounds.height;
    }

    private static final class WidgetAnimationState {
        final Animation visibility = new Animation(400L, Easing.HUD_APPEAR);
        private boolean visible = false;

        WidgetAnimationState(boolean initialVisible) {
            this.visible = initialVisible;
            if (initialVisible) {
                this.visibility.setValue(1.0f);
            } else {
                this.visibility.setValue(0.0f);
            }
        }

        void update(boolean shouldBeVisible) {
            if (shouldBeVisible != visible) {
                visible = shouldBeVisible;
                if (shouldBeVisible) {
                    visibility.setDuration(400L);
                    visibility.setEasing(Easing.HUD_APPEAR);
                } else {
                    visibility.setDuration(350L);
                    visibility.setEasing(Easing.HUD_DISAPPEAR);
                }
            }
            visibility.update(shouldBeVisible ? 1.0f : 0.0f);
        }

        boolean shouldDraw() {
            return visible || visibility.getValue() > 0.005f;
        }
    }
}
