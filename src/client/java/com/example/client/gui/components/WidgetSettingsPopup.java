package com.example.client.gui.components;

import com.example.client.gui.components.settings.*;
import com.example.client.gui.hud.HudWidget;
import com.example.client.gui.theme.UITheme;
import com.example.render.animation.Animation;
import com.example.render.animation.Easing;
import com.example.render.post.BlurUtil;
import com.example.render.msdf.FontRenderer;
import com.example.render.setting.*;
import com.example.render.shape.DrawUtility;
import com.example.render.util.ScissorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class WidgetSettingsPopup {
    private HudWidget targetWidget = null;
    private boolean open = false;

    private float x = 0.0f;
    private float y = 0.0f;
    private float width = 162.0f;
    private float height = 180.0f;

    private static final float MAX_VISIBLE_BODY_HEIGHT = 180.0f;
    private static final float SECTION_HEIGHT = 14.0f;

    private static final String SECTION_SHAPE = "Shape";
    private static final String SECTION_BACKGROUND = "Background";
    private static final String SECTION_SHADOW = "Shadow";
    private static final String SECTION_TINT = "Tint";
    private static final String SECTION_WIDGET = "Widget";
    private static final String SECTION_GENERAL = "General";

    private float scrollTarget = 0.0f;
    private final Animation scrollAnim = new Animation(200L, Easing.SMOOTH_IN_OUT);
    private final Animation scrollbarAlphaAnim = new Animation(250L, Easing.SMOOTH_IN_OUT);
    private final Animation openAnim = new Animation(480L, Easing.HUD_APPEAR);

    private final List<Object> settingComponents = new ArrayList<>();

    public boolean isOpen() {
        return open;
    }

    public boolean shouldDraw() {
        return open || openAnim.getValue() > 0.005f;
    }

    public HudWidget getTargetWidget() {
        return targetWidget;
    }

    public void open(HudWidget widget, float mouseX, float mouseY, float screenW, float screenH) {
        this.targetWidget = widget;
        this.open = true;
        this.openAnim.setDuration(480L);
        this.openAnim.setEasing(Easing.HUD_APPEAR);
        this.openAnim.reset(0.0f);
        this.openAnim.update(1.0f);

        this.scrollTarget = 0.0f;
        this.scrollAnim.reset(0.0f);

        buildComponents(widget);

        this.width = 162.0f;
        this.height = calculateHeight();

        float spawnX = Math.max(10.0f, Math.min(screenW - width - 10.0f, mouseX - width * 0.5f));
        float spawnY = (mouseY + height + 10.0f <= screenH - 10.0f)
            ? mouseY + 6.0f
            : Math.max(10.0f, mouseY - height - 6.0f);

        this.x = spawnX;
        this.y = spawnY;
    }

    public void close() {
        if (!open) return;
        this.open = false;
        this.openAnim.setDuration(440L);
        this.openAnim.setEasing(Easing.HUD_DISAPPEAR);
        this.openAnim.update(0.0f);
    }

    private void buildComponents(HudWidget widget) {
        settingComponents.clear();
        if (widget == null) return;

        List<ModuleSetting<?>> all = widget.getSettings();

        List<ModuleSetting<?>> shapeGroup = new ArrayList<>();
        List<ModuleSetting<?>> bgGroup = new ArrayList<>();
        List<ModuleSetting<?>> shadowGroup = new ArrayList<>();
        List<ModuleSetting<?>> tintGroup = new ArrayList<>();
        List<ModuleSetting<?>> generalGroup = new ArrayList<>();
        List<ModuleSetting<?>> widgetGroup = new ArrayList<>();

        for (ModuleSetting<?> s : all) {
            String name = s.getName();
            if (name.equals("Shape") || name.equals("Corner Radius") || name.equals("Separate Corners") ||
                name.startsWith("Radius ") || name.equals("Corner Smoothing")) {
                shapeGroup.add(s);
            } else if (name.equals("Opacity") || name.equals("Blur Mode") ||
                       name.equals("Blur Strength") || name.equals("Outline")) {
                bgGroup.add(s);
            } else if (name.equals("Shadow") || name.startsWith("Shadow ")) {
                shadowGroup.add(s);
            } else if (name.equals("Tint Overlay") || name.equals("Tint Strength") || name.equals("Tint Color")) {
                tintGroup.add(s);
            } else if (name.equals("Scale")) {
                generalGroup.add(s);
            } else {
                widgetGroup.add(s);
            }
        }

        addSection(SECTION_SHAPE, shapeGroup);
        addSection(SECTION_BACKGROUND, bgGroup);
        addSection(SECTION_SHADOW, shadowGroup);
        addSection(SECTION_TINT, tintGroup);
        if (!generalGroup.isEmpty()) addSection(SECTION_GENERAL, generalGroup);
        if (!widgetGroup.isEmpty()) addSection(SECTION_WIDGET, widgetGroup);
    }

    private void addSection(String label, List<ModuleSetting<?>> settings) {
        if (settings == null || settings.isEmpty()) return;
        settingComponents.add(label);
        for (ModuleSetting<?> s : settings) {
            if (s instanceof BooleanSetting bs) settingComponents.add(new BooleanSettingComponent(bs));
            else if (s instanceof ColorSetting cs) settingComponents.add(new ColorSettingComponent(cs));
            else if (s instanceof FloatSetting fs) settingComponents.add(new SliderSettingComponent(fs));
            else if (s instanceof EnumSetting<?> es) settingComponents.add(new ModeSettingComponent(es));
            else if (s instanceof MultiBooleanSetting mbs) settingComponents.add(new SelectSettingComponent(mbs));
        }
    }

    private float getTotalSettingsHeight() {
        float h = 0.0f;
        for (Object comp : settingComponents) {
            h += getCompHeight(comp) + 2.0f;
        }
        return h;
    }

    private float getVisibleBodyHeight() {
        return Math.min(MAX_VISIBLE_BODY_HEIGHT, getTotalSettingsHeight());
    }

    private float calculateHeight() {
        return 24.0f + getVisibleBodyHeight() + 6.0f;
    }

    private float getMaxScroll() {
        return Math.max(0.0f, getTotalSettingsHeight() - getVisibleBodyHeight());
    }

    public boolean mouseScrolled(float mouseX, float mouseY, double amount) {
        if (!isOpen() || settingComponents.isEmpty()) return false;
        boolean inside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (inside) {
            float max = getMaxScroll();
            if (max > 0.0f) {
                this.scrollTarget = Math.max(0.0f, Math.min(max, scrollTarget - (float) amount * 20.0f));
                this.scrollAnim.update(scrollTarget);
                return true;
            }
        }
        return false;
    }

    public void update(float mouseX, float mouseY) {
        if (!shouldDraw() || targetWidget == null) return;
        openAnim.update(open ? 1.0f : 0.0f);
        this.height = calculateHeight();

        float maxScroll = getMaxScroll();
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
        scrollAnim.update(scrollTarget);
        float currentScroll = scrollAnim.getValue();

        float bodyX = x + 3.0f;
        float bodyY = y + 24.0f;
        float bodyW = width - 6.0f;
        float bodyH = getVisibleBodyHeight();

        boolean bodyHovered = mouseX >= bodyX && mouseX <= bodyX + bodyW && mouseY >= bodyY && mouseY <= bodyY + bodyH;
        scrollbarAlphaAnim.update(bodyHovered || Math.abs(scrollAnim.getValue() - scrollTarget) > 0.5f);

        float itemX = x + 3.0f;
        float itemW = width - 6.0f;
        float curY = bodyY - currentScroll;

        for (Object comp : settingComponents) {
            float compH = getCompHeight(comp);
            float itemY = curY;
            float virtualMouseY = (bodyHovered && mouseY >= bodyY && mouseY <= bodyY + bodyH) ? mouseY : -9999.0f;

            if (comp instanceof BooleanSettingComponent b) {
                b.setBounds(itemX, itemY, itemW, compH);
                b.update(mouseX, virtualMouseY);
            } else if (comp instanceof ColorSettingComponent c) {
                c.setBounds(itemX, itemY, itemW, compH);
                c.update(mouseX, virtualMouseY);
            } else if (comp instanceof SliderSettingComponent s) {
                s.setBounds(itemX, itemY, itemW, compH);
                s.update(mouseX, virtualMouseY);
            } else if (comp instanceof ModeSettingComponent m) {
                m.setBounds(itemX, itemY, itemW, compH);
                m.update(mouseX, virtualMouseY);
            } else if (comp instanceof SelectSettingComponent sel) {
                sel.setBounds(itemX, itemY, itemW, compH);
                sel.update(mouseX, virtualMouseY);
            }
            curY += compH + 2.0f;
        }
    }

    private float getCompHeight(Object comp) {
        if (comp instanceof String) return SECTION_HEIGHT;
        if (comp instanceof BooleanSettingComponent b) return b.getHeight();
        if (comp instanceof ColorSettingComponent c) return c.getHeight();
        if (comp instanceof SliderSettingComponent s) return s.getHeight();
        if (comp instanceof ModeSettingComponent m) return m.getHeight();
        if (comp instanceof SelectSettingComponent sel) return sel.getHeight();
        return 18.0f;
    }

    public void render(GuiGraphicsExtractor graphics, float mouseX, float mouseY) {
        if (!shouldDraw() || targetWidget == null) return;
        this.height = calculateHeight();

        openAnim.update(open ? 1.0f : 0.0f);
        float menuScale = openAnim.getValue();
        if (menuScale <= 0.005f) return;

        float boxRadius = 12.0f;
        float cornerSmoothing = 0.85f;

        float slideY = (1.0f - Math.min(1.0f, menuScale)) * 5.0f;
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

        DrawUtility.drawCircle(graphics, x + 13.0f, y + 12.0f, 3.0f, UITheme.INSTANCE.accentAlpha(popupAlpha));

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfSemibold,
            targetWidget.getTitle(),
            x + 21.0f,
            y + 7.5f,
            8.5f,
            ((int) (255.0f * popupAlpha) << 24) | 0x00FFFFFF
        );

        FontRenderer.INSTANCE.draw(
            graphics,
            FontRenderer.Face.SfMedium,
            "Settings",
            x + 21.0f,
            y + 17.0f,
            6.0f,
            ((int) (110.0f * popupAlpha) << 24) | 0x00FFFFFF
        );

        float bodyY = y + 24.0f;
        float bodyW = width - 6.0f;
        float bodyH = getVisibleBodyHeight();
        float currentScroll = scrollAnim.getValue();

        ScissorUtil.run(graphics, x + 2.0f, bodyY, bodyW + 2.0f, bodyH, () -> {
            float curY = bodyY - currentScroll;
            float itemX = x + 3.0f;
            float itemW = width - 6.0f;

            for (Object comp : settingComponents) {
                float compH = getCompHeight(comp);
                boolean visible = curY + compH >= bodyY && curY <= bodyY + bodyH;

                if (comp instanceof String sectionLabel) {
                    if (visible) {
                        int labelAlpha = (int) (140.0f * popupAlpha);
                        FontRenderer.INSTANCE.draw(
                            graphics,
                            FontRenderer.Face.SfMedium,
                            sectionLabel,
                            itemX + 6.0f,
                            curY + 2.5f,
                            6.0f,
                            (Math.max(0, Math.min(255, labelAlpha)) << 24) | (UITheme.INSTANCE.accent() & 0x00FFFFFF)
                        );
                        float lineX = itemX + 6.0f + FontRenderer.INSTANCE.width(FontRenderer.Face.SfMedium, sectionLabel, 6.0f) + 5.0f;
                        float lineY = curY + SECTION_HEIGHT * 0.5f;
                        DrawUtility.drawSquircle(graphics, lineX, lineY, itemW - lineX + itemX - 2.0f, 0.5f, 0.0f, 0.0f, (Math.max(0, Math.min(255, (int) (40.0f * popupAlpha))) << 24) | 0x00FFFFFF);
                    }
                } else if (visible) {
                    if (comp instanceof BooleanSettingComponent b) {
                        b.setBounds(itemX, curY, itemW, compH);
                        b.render(graphics, mouseX, mouseY);
                    } else if (comp instanceof ColorSettingComponent c) {
                        c.setBounds(itemX, curY, itemW, compH);
                        c.render(graphics, mouseX, mouseY);
                    } else if (comp instanceof SliderSettingComponent s) {
                        s.setBounds(itemX, curY, itemW, compH);
                        s.render(graphics, mouseX, mouseY);
                    } else if (comp instanceof ModeSettingComponent m) {
                        m.setBounds(itemX, curY, itemW, compH);
                        m.render(graphics, mouseX, mouseY);
                    } else if (comp instanceof SelectSettingComponent sel) {
                        sel.setBounds(itemX, curY, itemW, compH);
                        sel.render(graphics, mouseX, mouseY);
                    }
                }
                curY += compH + 2.0f;
            }
        });

        float maxScroll = getMaxScroll();
        if (maxScroll > 0.0f) {
            float sbAlpha = scrollbarAlphaAnim.getValue() * popupAlpha;
            if (sbAlpha > 0.01f) {
                float totalH = getTotalSettingsHeight();
                float barH = Math.max(14.0f, bodyH * (bodyH / totalH));
                float barY = bodyY + (bodyH - barH) * (currentScroll / maxScroll);
                float barX = x + width - 3.5f;
                int barColor = ((int) (90.0f * sbAlpha) << 24) | 0x00FFFFFF;
                DrawUtility.drawSquircle(graphics, barX, barY, 2.0f, barH, 1.0f, 0.5f, barColor);
            }
        }

        graphics.pose().popMatrix();
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (!isOpen() || targetWidget == null) return false;

        boolean inside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float bodyY = y + 24.0f;
            float bodyH = getVisibleBodyHeight();
            if (mouseY >= bodyY && mouseY <= bodyY + bodyH) {
                for (Object comp : new ArrayList<>(settingComponents)) {
                    if (comp instanceof BooleanSettingComponent b && b.mouseClicked(mouseX, mouseY, button)) {
                        buildComponents(targetWidget);
                        return true;
                    }
                    if (comp instanceof ModeSettingComponent m && m.mouseClicked(mouseX, mouseY, button)) {
                        buildComponents(targetWidget);
                        return true;
                    }
                    if (comp instanceof SelectSettingComponent sel && sel.mouseClicked(mouseX, mouseY, button)) {
                        buildComponents(targetWidget);
                        return true;
                    }
                    if (comp instanceof ColorSettingComponent c && c.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                    if (comp instanceof SliderSettingComponent s && s.mouseClicked(mouseX, mouseY, button)) {
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
        if (!isOpen()) return false;
        for (Object comp : settingComponents) {
            if (comp instanceof SliderSettingComponent s) {
                s.mouseReleased(0, 0, button);
            } else if (comp instanceof ColorSettingComponent c) {
                c.mouseReleased(button);
            }
        }
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

