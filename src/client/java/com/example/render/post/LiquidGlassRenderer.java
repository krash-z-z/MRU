package com.example.render.post;

import com.example.render.context.Color;
import com.example.render.shape.CornerRadii;
import com.example.render.util.Color4f;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * GPU Optical Refraction Liquid Glass Shader Renderer.
 */
public final class LiquidGlassRenderer {
    public static final LiquidGlassRenderer INSTANCE = new LiquidGlassRenderer();

    private static final int LIQUID_GLASS_UBO_SIZE = 128; // 8 * vec4 (32 floats = 128 bytes)

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
    }

    public record GlassStyle(
        float thickness,
        float baseHeight,
        float ior,
        float dispersion,
        float blurRoughness,
        float reflectionIntensity,
        float liquidWobble,
        Color tint,
        Color specular
    ) {
        public static final GlassStyle DEFAULT = new GlassStyle(
            14.0f, 112.0f, 1.52f, 0.025f, 0.6f, 1.0f, 0.0f, Color.WHITE, Color.WHITE
        );
    }

    private static final BindGroupLayout LIQUID_GLASS_LAYOUT = BindGroupLayout.builder()
        .withSampler("OriginalSampler")
        .withSampler("BlurSampler")
        .withUniform("LiquidGlassInfo", UniformType.UNIFORM_BUFFER)
        .build();

    private static final RenderPipeline LIQUID_GLASS_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/liquid_glass_post"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/liquid_glass"))
            .withBindGroupLayout(LIQUID_GLASS_LAYOUT)
            .build()
    );

    private TextureTarget snapshotTarget = null;
    private int targetWidth = 0;
    private int targetHeight = 0;
    private GpuBuffer ubo = null;
    private final ByteBuffer infoBuffer = ByteBuffer.allocateDirect(LIQUID_GLASS_UBO_SIZE).order(ByteOrder.nativeOrder());

    private LiquidGlassRenderer() {}

    private void ensureTargets(int width, int height) {
        if (width == targetWidth && height == targetHeight && snapshotTarget != null && ubo != null) {
            return;
        }
        closeTargets();
        targetWidth = width;
        targetHeight = height;

        snapshotTarget = new TextureTarget("mod-liquid-glass-snapshot", width, height, false, GpuFormat.RGBA8_UNORM);
        var device = RenderSystem.getDevice();
        ubo = device.createBuffer(() -> "Mod Liquid Glass UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, (long) LIQUID_GLASS_UBO_SIZE);
    }

    public void closeTargets() {
        if (snapshotTarget != null) {
            snapshotTarget.destroyBuffers();
            snapshotTarget = null;
        }
        if (ubo != null) {
            ubo.close();
            ubo = null;
        }
    }

    public void renderLiquidGlass(
        ShapeType shapeType,
        float x, float y, float width, float height,
        CornerRadii radii,
        float rotation,
        GlassStyle style,
        float time
    ) {
        float rTL = radii != null ? radii.tl() : 0.0f;
        float rTR = radii != null ? radii.tr() : 0.0f;
        float rBR = radii != null ? radii.br() : 0.0f;
        float rBL = radii != null ? radii.bl() : 0.0f;

        renderLiquidGlass(
            shapeType, x, y, width, height, rTL, rTR, rBR, rBL, rotation,
            style.thickness(), style.baseHeight(), style.ior(), style.dispersion(),
            style.blurRoughness(), style.reflectionIntensity(), style.liquidWobble(), time,
            style.tint().toColor4f(), style.specular().toColor4f()
        );
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
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
        if (mainTarget == null || mainTarget.width <= 0 || mainTarget.height <= 0) return;

        ensureTargets(mainTarget.width, mainTarget.height);

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var mainColor = mainTarget.getColorTexture();
        if (mainColor == null) return;
        var snapshotColor = snapshotTarget.getColorTexture();
        if (snapshotColor == null) return;
        encoder.copyTextureToTexture(mainColor, snapshotColor, 0, 0, 0, 0, 0, mainTarget.width, mainTarget.height);

        float scale = (float) mc.getWindow().getGuiScale();
        float pixelX = x * scale;
        float pixelY = y * scale;
        float pixelW = width * scale;
        float pixelH = height * scale;
        float centerX = pixelX + pixelW * 0.5f;
        float centerY = pixelY + pixelH * 0.5f;
        float halfW = pixelW * 0.5f;
        float halfH = pixelH * 0.5f;

        float radTL = rTL * scale;
        float radTR = rTR * scale;
        float radBR = rBR * scale;
        float radBL = rBL * scale;

        infoBuffer.clear();
        infoBuffer.putFloat((float) mainTarget.width);
        infoBuffer.putFloat((float) mainTarget.height);
        infoBuffer.putFloat((float) shapeType.id);
        infoBuffer.putFloat(rotation);

        infoBuffer.putFloat(centerX);
        infoBuffer.putFloat(centerY);
        infoBuffer.putFloat(halfW);
        infoBuffer.putFloat(halfH);

        infoBuffer.putFloat(radTL);
        infoBuffer.putFloat(radTR);
        infoBuffer.putFloat(radBR);
        infoBuffer.putFloat(radBL);

        infoBuffer.putFloat(thickness * scale);
        infoBuffer.putFloat(baseHeight * scale);
        infoBuffer.putFloat(ior);
        infoBuffer.putFloat(dispersion);

        infoBuffer.putFloat(blurRoughness);
        infoBuffer.putFloat(reflectionIntensity);
        infoBuffer.putFloat(3.0f);
        infoBuffer.putFloat(64.0f);

        infoBuffer.putFloat(liquidWobble);
        infoBuffer.putFloat(time);
        infoBuffer.putFloat(1.0f);
        infoBuffer.putFloat(0.0f);

        infoBuffer.putFloat(tintColor.r);
        infoBuffer.putFloat(tintColor.g);
        infoBuffer.putFloat(tintColor.b);
        infoBuffer.putFloat(tintColor.a);

        infoBuffer.putFloat(specularColor.r);
        infoBuffer.putFloat(specularColor.g);
        infoBuffer.putFloat(specularColor.b);
        infoBuffer.putFloat(specularColor.a);

        infoBuffer.flip();

        encoder.writeToBuffer(ubo.slice(), infoBuffer);

        var destView = mainTarget.getColorTextureView();
        if (destView == null) return;

        try (RenderPass pass = encoder.createRenderPass(() -> "Liquid Glass Pass", destView, Optional.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setPipeline(LIQUID_GLASS_PIPELINE);
            var snapshotView = snapshotTarget.getColorTextureView();
            if (snapshotView == null) return;
            pass.bindTexture("OriginalSampler", snapshotView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.bindTexture("BlurSampler", snapshotView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("LiquidGlassInfo", ubo);
            pass.draw(3, 1, 0, 0);
        }
    }

    public void renderLiquidRoundedRect(float x, float y, float width, float height, float radius, Color4f tint) {
        renderLiquidGlass(
            ShapeType.ROUNDED_RECT,
            x, y, width, height,
            radius, radius, radius, radius,
            0.0f,
            14.0f, 112.0f,
            1.52f, 0.025f,
            0.6f, 1.0f,
            0.0f, 0.0f,
            tint,
            Color4f.WHITE
        );
    }
}
