package com.example.render.blur;

import com.example.render.post.BlurRenderer;

/**
 * Backward-compatibility proxy delegating to {@link BlurRenderer}.
 */
public final class DoubleKawaseBlurRenderer {
    public static final DoubleKawaseBlurRenderer INSTANCE = new DoubleKawaseBlurRenderer();

    private DoubleKawaseBlurRenderer() {}

    public boolean isBlurRenderedThisFrame() {
        return BlurRenderer.INSTANCE.isBlurRenderedThisFrame();
    }

    public void setBlurRenderedThisFrame(boolean rendered) {
        BlurRenderer.INSTANCE.setBlurRenderedThisFrame(rendered);
    }

    public void renderIfVisible() {
        BlurRenderer.INSTANCE.renderIfVisible();
    }

    public void closeTargets() {
        BlurRenderer.INSTANCE.closeTargets();
    }
}
