package com.example.render.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public final class ScissorUtil {

    private ScissorUtil() {}

    public static void push(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        if (graphics == null || width < 2.0f || height < 2.0f) return;
        int minX = (int) Math.floor(x);
        int minY = (int) Math.floor(y);
        int maxX = (int) Math.ceil(x + width);
        int maxY = (int) Math.ceil(y + height);
        if (maxX - minX <= 1 || maxY - minY <= 1) return;
        graphics.enableScissor(minX, minY, maxX, maxY);
    }

    public static void push(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 1 || height <= 1) return;
        graphics.enableScissor(x, y, x + width, y + height);
    }

    public static void pop(GuiGraphicsExtractor graphics) {
        if (graphics == null) return;
        graphics.disableScissor();
    }

    public static void run(GuiGraphicsExtractor graphics, float x, float y, float width, float height, Runnable action) {
        if (action == null) return;
        if (graphics == null || width < 2.0f || height < 2.0f) {
            action.run();
            return;
        }
        int minX = (int) Math.floor(x);
        int minY = (int) Math.floor(y);
        int maxX = (int) Math.ceil(x + width);
        int maxY = (int) Math.ceil(y + height);
        if (maxX - minX <= 1 || maxY - minY <= 1) {
            action.run();
            return;
        }

        ScreenRectangle current = graphics.scissorStack.peek();
        if (current != null) {
            int intMinX = Math.max(minX, current.left());
            int intMinY = Math.max(minY, current.top());
            int intMaxX = Math.min(maxX, current.right());
            int intMaxY = Math.min(maxY, current.bottom());
            if (intMaxX - intMinX <= 1 || intMaxY - intMinY <= 1) {
                action.run();
                return;
            }
        }

        graphics.enableScissor(minX, minY, maxX, maxY);
        try {
            action.run();
        } finally {
            graphics.disableScissor();
        }
    }

    public static void run(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Runnable action) {
        run(graphics, (float) x, (float) y, (float) width, (float) height, action);
    }

    public static boolean contains(float px, float py, float x, float y, float width, float height) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public static boolean isPointInActiveScissor(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (graphics == null) return true;
        return graphics.containsPointInScissor(mouseX, mouseY);
    }
}
