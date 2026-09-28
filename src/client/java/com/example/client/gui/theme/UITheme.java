package com.example.client.gui.theme;

public final class UITheme {
    public static final UITheme INSTANCE = new UITheme();

    private UITheme() {}

    private int accentColor = 0xFF6366F1;

    public int accent() {
        return accentColor;
    }

    public void setAccent(int color) {
        this.accentColor = color;
    }

    public int accentAlpha(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (255.0f * alpha)));
        return (accentColor & 0x00FFFFFF) | (a << 24);
    }

    public int secondaryAccent() {
        return mixRgb(accentColor, 0xFFFFFFFF, 0.35f);
    }

    public int secondaryAccentAlpha(float alpha) {
        int sec = secondaryAccent();
        int a = Math.max(0, Math.min(255, (int) (255.0f * alpha)));
        return (sec & 0x00FFFFFF) | (a << 24);
    }

    public int sliderTrack() {
        return mixRgb(0xFF18181B, accentColor, 0.25f);
    }

    public int highlightColor(float hover) {
        return highlightColor(hover, 0.0f);
    }

    public int highlightColor(float hover, float active) {
        float h = Math.max(0.0f, Math.min(1.0f, hover));
        int acc = accent();
        int ar = (acc >>> 16) & 0xFF;
        int ag = (acc >>> 8) & 0xFF;
        int ab = acc & 0xFF;

        int r = (int) (255.0f + (ar - 255.0f) * h);
        int g = (int) (255.0f + (ag - 255.0f) * h);
        int b = (int) (255.0f + (ab - 255.0f) * h);
        int a = Math.min(255, Math.max(0, (int) (255.0f * (0.75f + 0.25f * Math.max(active, h)))));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int mixRgb(int from, int towards, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int r = (int) (((from >>> 16) & 0xFF) + (((towards >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = (int) (((from >>> 8) & 0xFF) + (((towards >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((towards & 0xFF) - (from & 0xFF)) * t);
        return (from & 0xFF000000)
            | (Math.max(0, Math.min(255, r)) << 16)
            | (Math.max(0, Math.min(255, g)) << 8)
            | Math.max(0, Math.min(255, b));
    }
}
