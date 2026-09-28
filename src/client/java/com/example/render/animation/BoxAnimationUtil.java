package com.example.render.animation;

public final class BoxAnimationUtil {
    public static final BoxAnimationUtil INSTANCE = new BoxAnimationUtil();

    private BoxAnimationUtil() {}

    public float progress(double startMs, float durationMs) {
        float elapsed = (float) ((System.nanoTime() / 1_000_000.0 - startMs) / durationMs);
        return AnimationUtil.INSTANCE.clamp01(elapsed);
    }

    public float fade(float progress) {
        return AnimationUtil.INSTANCE.apply(AnimationUtil.Mode.FADE, progress);
    }

    public float bounce(float progress) {
        return AnimationUtil.INSTANCE.apply(AnimationUtil.Mode.BOUNCE, progress);
    }

    public float elastic(float progress) {
        return AnimationUtil.INSTANCE.apply(AnimationUtil.Mode.ELASTIC, progress);
    }

    public float size(float from, float to, float progress) {
        return size(from, to, progress, AnimationUtil.Mode.FADE);
    }

    public float size(float from, float to, float progress, AnimationUtil.Mode mode) {
        float t = AnimationUtil.INSTANCE.apply(mode, progress);
        return from + (to - from) * t;
    }

    public float lerp(float from, float to, float progress) {
        return lerp(from, to, progress, AnimationUtil.Mode.FADE);
    }

    public float lerp(float from, float to, float progress, AnimationUtil.Mode mode) {
        float t = AnimationUtil.INSTANCE.apply(mode, progress);
        return from + (to - from) * t;
    }

    public float[] centerScale(float left, float top, float width, float height, float scale) {
        float safeScale = Math.max(0.0f, Math.min(scale, 1.25f));
        float scaledWidth = width * safeScale;
        float scaledHeight = height * safeScale;
        return new float[] {
            left + (width - scaledWidth) * 0.5f,
            top + (height - scaledHeight) * 0.5f,
            scaledWidth,
            scaledHeight,
        };
    }
}
