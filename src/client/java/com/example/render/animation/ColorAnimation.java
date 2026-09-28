package com.example.render.animation;

public class ColorAnimation {
    private final Animation r;
    private final Animation g;
    private final Animation b;
    private final Animation a;

    public ColorAnimation(long duration, int initialColor, Easing easing) {
        this.r = new Animation(duration, (initialColor >> 16) & 0xFF, easing);
        this.g = new Animation(duration, (initialColor >> 8) & 0xFF, easing);
        this.b = new Animation(duration, initialColor & 0xFF, easing);
        this.a = new Animation(duration, (initialColor >>> 24) & 0xFF, easing);
    }

    public ColorAnimation(long duration, Easing easing) {
        this(duration, 0xFFFFFFFF, easing);
    }

    public void update(int targetColor) {
        r.update((targetColor >> 16) & 0xFF);
        g.update((targetColor >> 8) & 0xFF);
        b.update(targetColor & 0xFF);
        a.update((targetColor >>> 24) & 0xFF);
    }

    public void reset(int color) {
        r.reset((color >> 16) & 0xFF);
        g.reset((color >> 8) & 0xFF);
        b.reset(color & 0xFF);
        a.reset((color >>> 24) & 0xFF);
    }

    public int getColor() {
        int red = Math.max(0, Math.min(255, (int) r.getValue()));
        int green = Math.max(0, Math.min(255, (int) g.getValue()));
        int blue = Math.max(0, Math.min(255, (int) b.getValue()));
        int alpha = Math.max(0, Math.min(255, (int) a.getValue()));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public void setEasing(Easing easing) {
        r.setEasing(easing);
        g.setEasing(easing);
        b.setEasing(easing);
        a.setEasing(easing);
    }
}
