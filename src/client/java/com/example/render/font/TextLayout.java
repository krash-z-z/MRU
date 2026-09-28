package com.example.render.font;

import java.util.ArrayList;
import java.util.List;

/**
 * Text layout and measurement utilities for MSDF typography.
 */
public final class TextLayout {

    private TextLayout() {}

    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    /**
     * Truncates text with an ellipsis if it exceeds the specified maximum width.
     */
    public static String trimToWidth(FontFace face, String text, float size, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (FontRenderer.INSTANCE.width(face, text, size) <= maxWidth) return text;

        String ellipsis = "...";
        float ellipsisWidth = FontRenderer.INSTANCE.width(face, ellipsis, size);
        if (ellipsisWidth >= maxWidth) return "";

        float availableWidth = maxWidth - ellipsisWidth;
        int low = 0;
        int high = text.length();
        int best = 0;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            String sub = text.substring(0, mid);
            if (FontRenderer.INSTANCE.width(face, sub, size) <= availableWidth) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return text.substring(0, best) + ellipsis;
    }

    /**
     * Wraps text into multiple lines fitting within the given maximum width.
     */
    public static List<String> wrapWords(FontFace face, String text, float size, float maxWidth) {
        if (text == null || text.isEmpty()) return List.of();
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (FontRenderer.INSTANCE.width(face, candidate, size) <= maxWidth) {
                currentLine.append(currentLine.isEmpty() ? word : " " + word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }
                currentLine.append(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
