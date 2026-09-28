package com.example.render.glass;

import com.example.render.util.Color4f;

/**
 * Backward-compatibility proxy delegating to {@link com.example.render.post.LiquidGlassRenderer}.
 */
public final class LiquidGlassRenderer {
    public static final LiquidGlassRenderer INSTANCE = new LiquidGlassRenderer();

    public enum ShapeType {
        ROUNDED_RECT(0),
        CIRCLE(1),
        PILL(2),
        SQUIRCLE(3),
        HEXAGON(4),
        STAR(5);

        public final int id;
        ShapeType(int id) {
            this.id = id;
        }

        public com.example.render.post.LiquidGlassRenderer.ShapeType toPostShapeType() {
            return com.example.render.post.LiquidGlassRenderer.ShapeType.valueOf(this.name());
        }
    }

    private LiquidGlassRenderer() {}

    public void closeTargets() {
        com.example.render.post.LiquidGlassRenderer.INSTANCE.closeTargets();
    }

    public void renderLiquidGlass(
        ShapeType shapeType,
        float x, float y, float width, float height,
        float rTL, float rTR, float rBR, float rBL,
        float rotation,
        float thickness,
        float baseHeight,
        float ior,
        float dispersion,
        float blurRoughness,
        float reflectionIntensity,
        float liquidWobble,
        float time,
        Color4f tintColor,
        Color4f specularColor
    ) {
        com.example.render.post.LiquidGlassRenderer.INSTANCE.renderLiquidGlass(
            shapeType.toPostShapeType(),
            x, y, width, height,
            rTL, rTR, rBR, rBL,
            rotation,
            thickness,
            baseHeight,
            ior,
            dispersion,
            blurRoughness,
            reflectionIntensity,
            liquidWobble,
            time,
            tintColor,
            specularColor
        );
    }

    public void renderLiquidRoundedRect(float x, float y, float width, float height, float radius, Color4f tint) {
        com.example.render.post.LiquidGlassRenderer.INSTANCE.renderLiquidRoundedRect(x, y, width, height, radius, tint);
    }
}
