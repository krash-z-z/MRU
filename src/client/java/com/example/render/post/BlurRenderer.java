package com.example.render.post;

import com.example.client.gui.HudConfig;
import com.example.client.gui.hud.HudOverlay;
import com.example.render.blur.ShadowBlurUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * High-performance fullscreen multi-pass blur and composite post-processing renderer.
 */
public final class BlurRenderer {
    public static final BlurRenderer INSTANCE = new BlurRenderer();

    private static final int GAUSSIAN_UBO_SIZE = 32;
    private static final int ACRYLIC_UBO_SIZE = 32;
    private static final int MAX_BLUR_BOXES = 128;
    private static final int COMPOSITE_UBO_SIZE = 96 + MAX_BLUR_BOXES * 48;
    private static final int LIQUID_GLASS_UBO_SIZE = 64 + MAX_BLUR_BOXES * 48;

    private static final BindGroupLayout GAUSSIAN_LAYOUT = BindGroupLayout.builder()
        .withSampler("InputSampler")
        .withUniform("GaussianInfo", UniformType.UNIFORM_BUFFER)
        .build();

    private static final BindGroupLayout ACRYLIC_LAYOUT = BindGroupLayout.builder()
        .withSampler("InputSampler")
        .withUniform("AcrylicInfo", UniformType.UNIFORM_BUFFER)
        .build();

    private static final BindGroupLayout COMPOSITE_LAYOUT = BindGroupLayout.builder()
        .withSampler("OriginalSampler")
        .withSampler("GaussianSampler")
        .withSampler("AcrylicSampler")
        .withUniform("CompositeInfo", UniformType.UNIFORM_BUFFER)
        .build();

    private static final BindGroupLayout LIQUID_GLASS_LAYOUT = BindGroupLayout.builder()
        .withSampler("OriginalSampler")
        .withSampler("GaussianSampler")
        .withSampler("AcrylicSampler")
        .withUniform("LiquidGlassInfo", UniformType.UNIFORM_BUFFER)
        .build();

    private static final RenderPipeline GAUSSIAN_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/gaussian_blur"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/gaussian_blur"))
            .withBindGroupLayout(GAUSSIAN_LAYOUT)
            .build()
    );

    private static final RenderPipeline ACRYLIC_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/acrylic_blur"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/acrylic_blur"))
            .withBindGroupLayout(ACRYLIC_LAYOUT)
            .build()
    );

    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/double_kawase_composite"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/double_kawase_composite"))
            .withBindGroupLayout(COMPOSITE_LAYOUT)
            .build()
    );

    private static final RenderPipeline LIQUID_GLASS_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/liquid_glass"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/liquid_glass"))
            .withBindGroupLayout(LIQUID_GLASS_LAYOUT)
            .build()
    );

    private TextureTarget original = null;
    private TextureTarget gaussianTarget = null;
    private TextureTarget gaussianTemp = null;
    private TextureTarget acrylicTarget = null;
    private int targetWidth = 0;
    private int targetHeight = 0;

    private GpuBuffer[] gaussianUbos = new GpuBuffer[BlurUtil.MAX_GAUSSIAN_PASSES * 2];
    private GpuBuffer acrylicUbo = null;
    private GpuBuffer compositeUbo = null;
    private GpuBuffer liquidGlassUbo = null;

    private final ByteBuffer gaussianInfoBuffer = reusableBuffer(GAUSSIAN_UBO_SIZE);
    private final ByteBuffer acrylicInfoBuffer = reusableBuffer(ACRYLIC_UBO_SIZE);
    private final ByteBuffer compositeInfoBuffer = reusableBuffer(COMPOSITE_UBO_SIZE);
    private final ByteBuffer liquidGlassInfoBuffer = reusableBuffer(LIQUID_GLASS_UBO_SIZE);

    private boolean blurRenderedThisFrame = false;

    private BlurRenderer() {}

    public boolean isBlurRenderedThisFrame() {
        return this.blurRenderedThisFrame;
    }

    public void setBlurRenderedThisFrame(boolean rendered) {
        this.blurRenderedThisFrame = rendered;
    }

    public void renderIfVisible() {
        if (blurRenderedThisFrame) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null && mc.gui.screen() == null) return;

        float guiScale = (float) mc.getWindow().getGuiScale();
        List<BlurUtil.Shape> boxes = postProcessingBoxes(guiScale);
        if (boxes.isEmpty()) return;

        boolean bgBlurEnabled = HudConfig.INSTANCE.features.isEnabled("Background Blur");
        if (!bgBlurEnabled) {
            boxes = boxes.stream().filter(BlurUtil.Shape::liquidGlass).toList();
            if (boxes.isEmpty()) return;
        }

        prepareAndRenderBlur(boxes);
        blurRenderedThisFrame = true;
    }

    private List<BlurUtil.Shape> postProcessingBoxes(float guiScale) {
        List<BlurUtil.Shape> result = new ArrayList<>(MAX_BLUR_BOXES);
        result.addAll(BlurUtil.INSTANCE.drainShapes(guiScale));
        result.addAll(HudOverlay.INSTANCE.blurBoxes(guiScale));
        if (result.size() > MAX_BLUR_BOXES) {
            return result.subList(0, MAX_BLUR_BOXES);
        }
        return result;
    }

    private void prepareAndRenderBlur(List<BlurUtil.Shape> boxes) {
        Minecraft minecraft = Minecraft.getInstance();
        var mainTarget = minecraft.gameRenderer.mainRenderTarget();
        if (mainTarget == null) return;
        int width = mainTarget.width;
        int height = mainTarget.height;
        if (width <= 0 || height <= 0) return;

        ensureTargets(width, height);

        if (original == null || gaussianTarget == null || gaussianTemp == null || acrylicTarget == null) return;

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var mainColor = mainTarget.getColorTexture();
        if (mainColor == null) return;
        var originalColor = original.getColorTexture();
        if (originalColor == null) return;
        encoder.copyTextureToTexture(mainColor, originalColor, 0, 0, 0, 0, 0, width, height);

        List<BlurUtil.Shape> liquidGlassBoxes = new ArrayList<>();
        List<BlurUtil.Shape> compositeBoxes = new ArrayList<>();

        for (BlurUtil.Shape box : boxes) {
            if (box.liquidGlass()) {
                liquidGlassBoxes.add(box);
            } else {
                compositeBoxes.add(box);
            }
        }

        HudConfig cfg = HudConfig.INSTANCE;
        boolean bgBlurEnabled = cfg.features.isEnabled("Background Blur");
        boolean hasGaussian = false;
        boolean hasAcrylic = false;

        if (bgBlurEnabled) {
            for (BlurUtil.Shape box : boxes) {
                if (box.blurStrength() > 0.001f) {
                    if (box.blurType() == ShadowBlurUtil.BlurType.ACRYLIC) {
                        hasAcrylic = true;
                    } else if (box.blurType() == ShadowBlurUtil.BlurType.GAUSSIAN) {
                        hasGaussian = true;
                    }
                }
            }

            if (!compositeBoxes.isEmpty() && !hasGaussian && !hasAcrylic) {
                if (cfg.blurType.getValue() == ShadowBlurUtil.BlurType.ACRYLIC) {
                    hasAcrylic = true;
                } else if (cfg.blurType.getValue() == ShadowBlurUtil.BlurType.GAUSSIAN) {
                    hasGaussian = true;
                }
            }
        }

        if (hasGaussian) {
            renderGaussian(
                original,
                gaussianTemp,
                gaussianTarget,
                (int) Math.round(cfg.gaussianPasses.getValue()),
                (int) Math.round(cfg.gaussianRadius.getValue()),
                cfg.gaussianSigma.getValue(),
                cfg.gaussianOffset.getValue()
            );
        }

        if (hasAcrylic) {
            renderAcrylic(
                original,
                acrylicTarget,
                cfg.acrylicRadius.getValue(),
                cfg.acrylicChromatic.getValue(),
                cfg.acrylicHaze.getValue(),
                cfg.acrylicGrain.getValue()
            );
        }

        if (!compositeBoxes.isEmpty() && bgBlurEnabled) {
            float tint = cfg.blurOpacity.getValue();
            renderComposite(
                original,
                hasGaussian ? gaussianTarget : original,
                hasAcrylic ? acrylicTarget : original,
                mainTarget,
                tint,
                compositeBoxes
            );
        }

        if (!liquidGlassBoxes.isEmpty()) {
            BlurUtil.Shape primary = liquidGlassBoxes.get(0);
            renderLiquidGlass(
                original,
                hasGaussian ? gaussianTarget : original,
                hasAcrylic ? acrylicTarget : original,
                mainTarget,
                1.0f,
                primary.refraction(),
                primary.chromaticAberration(),
                1.0f,
                primary.highlight(),
                primary.sampleEscape(),
                true,
                liquidGlassBoxes
            );
        }
    }

    private void ensureTargets(int width, int height) {
        if (width == targetWidth && height == targetHeight && original != null && gaussianTarget != null && gaussianTemp != null && acrylicTarget != null && compositeUbo != null && acrylicUbo != null && liquidGlassUbo != null) return;
        closeTargets();
        targetWidth = width;
        targetHeight = height;

        original = new TextureTarget("mod-original", width, height, false, GpuFormat.RGBA8_UNORM);
        gaussianTarget = new TextureTarget("mod-gaussian-target", width, height, false, GpuFormat.RGBA8_UNORM);
        gaussianTemp = new TextureTarget("mod-gaussian-temp", width, height, false, GpuFormat.RGBA8_UNORM);
        acrylicTarget = new TextureTarget("mod-acrylic-target", width, height, false, GpuFormat.RGBA8_UNORM);

        var device = RenderSystem.getDevice();
        gaussianUbos = new GpuBuffer[BlurUtil.MAX_GAUSSIAN_PASSES * 2];
        for (int i = 0; i < gaussianUbos.length; i++) {
            final int idx = i;
            gaussianUbos[i] = device.createBuffer(() -> "Mod Gaussian UBO " + idx, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, (long) GAUSSIAN_UBO_SIZE);
        }
        acrylicUbo = device.createBuffer(() -> "Mod Acrylic UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, (long) ACRYLIC_UBO_SIZE);
        compositeUbo = device.createBuffer(() -> "Mod Composite UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, (long) COMPOSITE_UBO_SIZE);
        liquidGlassUbo = device.createBuffer(() -> "Mod Liquid Glass UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, (long) LIQUID_GLASS_UBO_SIZE);
    }

    public void closeTargets() {
        if (original != null) original.destroyBuffers();
        if (gaussianTarget != null) gaussianTarget.destroyBuffers();
        if (gaussianTemp != null) gaussianTemp.destroyBuffers();
        if (acrylicTarget != null) acrylicTarget.destroyBuffers();
        original = null; gaussianTarget = null; gaussianTemp = null; acrylicTarget = null;
        for (GpuBuffer ubo : gaussianUbos) if (ubo != null) ubo.close();
        gaussianUbos = new GpuBuffer[BlurUtil.MAX_GAUSSIAN_PASSES * 2];
        if (acrylicUbo != null) acrylicUbo.close();
        acrylicUbo = null;
        if (compositeUbo != null) compositeUbo.close();
        compositeUbo = null;
        if (liquidGlassUbo != null) liquidGlassUbo.close();
        liquidGlassUbo = null;
    }

    private RenderTarget renderGaussian(RenderTarget source, RenderTarget temp, RenderTarget destination, int passes, int radius, float sigma, float offset) {
        int passCount = Math.max(1, Math.min(BlurUtil.MAX_GAUSSIAN_PASSES, passes));
        RenderTarget currentIn = source;
        for (int p = 0; p < passCount; p++) {
            renderGaussianPass(p * 2, currentIn, temp, 1.0f, 0.0f, radius, sigma, offset);
            renderGaussianPass(p * 2 + 1, temp, destination, 0.0f, 1.0f, radius, sigma, offset);
            currentIn = destination;
        }
        return destination;
    }

    private void renderGaussianPass(int index, RenderTarget source, RenderTarget destination, float directionX, float directionY, int radius, float sigma, float offset) {
        if (index < 0 || index >= gaussianUbos.length) return;
        GpuBuffer ubo = gaussianUbos[index];
        if (ubo == null) return;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(ubo.slice(), writeGaussianInfoBuffer((float) source.width, (float) source.height, directionX, directionY, (float) radius, sigma, offset));
        var destView = destination.getColorTextureView();
        if (destView == null) return;
        try (RenderPass pass = encoder.createRenderPass(() -> "Gaussian blur pass " + index, destView, Optional.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setPipeline(GAUSSIAN_PIPELINE);
            var sourceView = source.getColorTextureView();
            if (sourceView == null) return;
            pass.bindTexture("InputSampler", sourceView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("GaussianInfo", ubo);
            pass.draw(3, 1, 0, 0);
        }
    }

    private RenderTarget renderAcrylic(RenderTarget source, RenderTarget destination, float radius, float chromatic, float haze, float grain) {
        GpuBuffer ubo = acrylicUbo;
        if (ubo == null) return source;
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(ubo.slice(), writeAcrylicInfoBuffer((float) source.width, (float) source.height, radius, chromatic, haze, grain));
        var destView = destination.getColorTextureView();
        if (destView == null) return source;
        try (RenderPass pass = encoder.createRenderPass(() -> "Acrylic blur pass", destView, Optional.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setPipeline(ACRYLIC_PIPELINE);
            var sourceView = source.getColorTextureView();
            if (sourceView == null) return source;
            pass.bindTexture("InputSampler", sourceView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("AcrylicInfo", ubo);
            pass.draw(3, 1, 0, 0);
        }
        return destination;
    }

    private void renderComposite(RenderTarget originalTarget, RenderTarget gaussianTarget, RenderTarget acrylicTarget, RenderTarget destination, float tintStrength, List<BlurUtil.Shape> blurBoxes) {
        GpuBuffer ubo = compositeUbo;
        if (ubo == null) return;

        HudConfig cfg = HudConfig.INSTANCE;
        boolean shadowEnabled = cfg.features.isEnabled("Drop Shadows");
        float shadowRadius = cfg.shadowBlur.getValue();
        float shadowOpacity = cfg.shadowOpacity.getValue();

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(
            ubo.slice(),
            writeCompositeInfoBuffer(
                (float) destination.width,
                (float) destination.height,
                0.0f, 0.0f, 0.0f, 0.0f,
                1.0f,
                tintStrength,
                shadowEnabled,
                shadowRadius,
                shadowOpacity,
                blurBoxes
            )
        );

        var destView = destination.getColorTextureView();
        if (destView == null) return;
        try (RenderPass pass = encoder.createRenderPass(() -> "Double Kawase composite", destView, Optional.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setPipeline(COMPOSITE_PIPELINE);
            var originalView = originalTarget.getColorTextureView();
            if (originalView == null) return;
            pass.bindTexture("OriginalSampler", originalView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            var gaussianView = gaussianTarget.getColorTextureView();
            if (gaussianView == null) return;
            pass.bindTexture("GaussianSampler", gaussianView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            var acrylicView = acrylicTarget.getColorTextureView();
            if (acrylicView == null) return;
            pass.bindTexture("AcrylicSampler", acrylicView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("CompositeInfo", ubo);
            pass.draw(3, 1, 0, 0);
        }
    }

    private void renderLiquidGlass(
        RenderTarget originalTarget,
        RenderTarget gaussianTarget,
        RenderTarget acrylicTarget,
        RenderTarget destination,
        float opacity,
        float refraction,
        float chromaticAberration,
        float saturation,
        float highlight,
        float sampleEscape,
        boolean isSquircle,
        List<BlurUtil.Shape> glassBoxes
    ) {
        GpuBuffer ubo = liquidGlassUbo;
        if (ubo == null) return;

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(
            ubo.slice(),
            writeLiquidGlassInfoBuffer(
                (float) destination.width,
                (float) destination.height,
                opacity,
                refraction,
                chromaticAberration,
                saturation,
                highlight,
                sampleEscape,
                isSquircle,
                glassBoxes
            )
        );

        var destView = destination.getColorTextureView();
        if (destView == null) return;
        try (RenderPass pass = encoder.createRenderPass(() -> "Liquid Glass Composite Pass", destView, Optional.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setPipeline(LIQUID_GLASS_PIPELINE);
            var originalView = originalTarget.getColorTextureView();
            if (originalView == null) return;
            pass.bindTexture("OriginalSampler", originalView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            var gaussianView = (gaussianTarget != null ? gaussianTarget : originalTarget).getColorTextureView();
            if (gaussianView == null) return;
            pass.bindTexture("GaussianSampler", gaussianView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            var acrylicView = (acrylicTarget != null ? acrylicTarget : originalTarget).getColorTextureView();
            if (acrylicView == null) return;
            pass.bindTexture("AcrylicSampler", acrylicView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setUniform("LiquidGlassInfo", ubo);
            pass.draw(3, 1, 0, 0);
        }
    }

    private ByteBuffer writeAcrylicInfoBuffer(float width, float height, float radius, float chromatic, float haze, float grain) {
        acrylicInfoBuffer.clear();
        acrylicInfoBuffer.putFloat(width);
        acrylicInfoBuffer.putFloat(height);
        acrylicInfoBuffer.putFloat(radius);
        acrylicInfoBuffer.putFloat(chromatic);
        acrylicInfoBuffer.putFloat(haze);
        acrylicInfoBuffer.putFloat(grain);
        acrylicInfoBuffer.putFloat(0.0f);
        acrylicInfoBuffer.putFloat(0.0f);
        acrylicInfoBuffer.flip();
        return acrylicInfoBuffer;
    }

    private ByteBuffer writeGaussianInfoBuffer(float width, float height, float dirX, float dirY, float radius, float sigma, float offset) {
        gaussianInfoBuffer.clear();
        gaussianInfoBuffer.putFloat(width);
        gaussianInfoBuffer.putFloat(height);
        gaussianInfoBuffer.putFloat(dirX);
        gaussianInfoBuffer.putFloat(dirY);
        gaussianInfoBuffer.putFloat(radius);
        gaussianInfoBuffer.putFloat(sigma);
        gaussianInfoBuffer.putFloat(offset);
        gaussianInfoBuffer.putFloat(0.0f);
        gaussianInfoBuffer.flip();
        return gaussianInfoBuffer;
    }

    private ByteBuffer writeLiquidGlassInfoBuffer(
        float outputWidth,
        float outputHeight,
        float opacity,
        float refraction,
        float chromaticAberration,
        float saturation,
        float highlight,
        float sampleEscape,
        boolean isSquircle,
        List<BlurUtil.Shape> glassBoxes
    ) {
        liquidGlassInfoBuffer.clear();
        int boxCount = Math.min(glassBoxes.size(), MAX_BLUR_BOXES);

        liquidGlassInfoBuffer.putFloat(outputWidth);
        liquidGlassInfoBuffer.putFloat(outputHeight);
        liquidGlassInfoBuffer.putFloat(1.0f / outputWidth);
        liquidGlassInfoBuffer.putFloat(1.0f / outputHeight);

        liquidGlassInfoBuffer.putFloat(saturation);
        liquidGlassInfoBuffer.putFloat(opacity);
        liquidGlassInfoBuffer.putFloat(refraction);
        liquidGlassInfoBuffer.putFloat(highlight);

        liquidGlassInfoBuffer.putFloat(1.0f);
        liquidGlassInfoBuffer.putFloat(1.0f);
        liquidGlassInfoBuffer.putFloat(1.0f);
        liquidGlassInfoBuffer.putFloat(chromaticAberration);

        liquidGlassInfoBuffer.putFloat((float) boxCount);
        liquidGlassInfoBuffer.putFloat(0.0f);
        liquidGlassInfoBuffer.putFloat(sampleEscape);
        liquidGlassInfoBuffer.putFloat(isSquircle ? 1.0f : 0.0f);

        for (int i = 0; i < MAX_BLUR_BOXES; i++) {
            if (i < boxCount) {
                BlurUtil.Shape box = glassBoxes.get(i);
                liquidGlassInfoBuffer.putFloat(box.x());
                liquidGlassInfoBuffer.putFloat(box.y());
                liquidGlassInfoBuffer.putFloat(box.width());
                liquidGlassInfoBuffer.putFloat(box.height());
            } else {
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
            }
        }

        for (int i = 0; i < MAX_BLUR_BOXES; i++) {
            if (i < boxCount) {
                BlurUtil.Shape box = glassBoxes.get(i);
                float radius = box.radius();
                float smoothing = box.smoothing() >= 0.0f ? box.smoothing() : 0.6f;
                float shapeMode = (float) box.shapeMode();
                if (shapeMode == 0.0f && box.squircle() != null && box.squircle()) {
                    shapeMode = 1.0f;
                }
                float effTint = box.tintStrength() >= 0.0f ? box.tintStrength() : 0.0f;

                liquidGlassInfoBuffer.putFloat(radius);
                liquidGlassInfoBuffer.putFloat(smoothing);
                liquidGlassInfoBuffer.putFloat(shapeMode);
                liquidGlassInfoBuffer.putFloat(effTint);
            } else {
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
            }
        }

        boolean bgBlurEnabled = HudConfig.INSTANCE.features.isEnabled("Background Blur");
        for (int i = 0; i < MAX_BLUR_BOXES; i++) {
            if (i < boxCount) {
                BlurUtil.Shape box = glassBoxes.get(i);
                float blurStr = box.blurStrength() >= 0.0f ? box.blurStrength() : 1.0f;
                float blurTypeVal;
                if (!bgBlurEnabled || box.blurType() == null || box.blurType() == ShadowBlurUtil.BlurType.NONE || blurStr <= 0.001f) {
                    blurTypeVal = -1.0f;
                } else if (box.blurType() == ShadowBlurUtil.BlurType.ACRYLIC) {
                    blurTypeVal = 1.0f;
                } else {
                    blurTypeVal = 0.0f;
                }
                float boxRefraction = box.refraction() >= 0.0f ? box.refraction() : refraction;
                float boxEscape = box.sampleEscape() >= 0.0f ? box.sampleEscape() : sampleEscape;

                liquidGlassInfoBuffer.putFloat(blurStr);
                liquidGlassInfoBuffer.putFloat(blurTypeVal);
                liquidGlassInfoBuffer.putFloat(boxRefraction);
                liquidGlassInfoBuffer.putFloat(boxEscape);
            } else {
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
                liquidGlassInfoBuffer.putFloat(0.0f);
            }
        }

        liquidGlassInfoBuffer.flip();
        return liquidGlassInfoBuffer;
    }

    private ByteBuffer writeCompositeInfoBuffer(
        float outputWidth, float outputHeight,
        float rectX, float rectY, float rectW, float rectH,
        float opacity, float tintStrength,
        boolean shadowEnabled, float shadowRadius, float shadowOpacity,
        List<BlurUtil.Shape> boxes
    ) {
        compositeInfoBuffer.clear();
        compositeInfoBuffer.putFloat(outputWidth);
        compositeInfoBuffer.putFloat(outputHeight);
        compositeInfoBuffer.putFloat(0.0f);
        compositeInfoBuffer.putFloat(0.0f);

        compositeInfoBuffer.putFloat(rectX);
        compositeInfoBuffer.putFloat(rectY);
        compositeInfoBuffer.putFloat(rectW);
        compositeInfoBuffer.putFloat(rectH);

        compositeInfoBuffer.putFloat(tintStrength);
        compositeInfoBuffer.putFloat(opacity);
        compositeInfoBuffer.putFloat(HudConfig.INSTANCE.blurType.getValue() == ShadowBlurUtil.BlurType.ACRYLIC ? 1.0f : 0.0f);
        compositeInfoBuffer.putFloat(1.0f);

        compositeInfoBuffer.putFloat(12.0f);
        compositeInfoBuffer.putFloat(0.0f);
        compositeInfoBuffer.putFloat(0.0f);
        compositeInfoBuffer.putFloat(0.65f);

        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float effShadowOffsetX = 0.0f;
        float effShadowOffsetY = 2.0f * guiScale;
        float effShadowRadius = (shadowRadius > 0.01f ? shadowRadius : 14.0f) * guiScale;
        float effShadowOpacity = shadowEnabled ? (shadowOpacity > 0.001f ? shadowOpacity : 0.35f) : 0.0f;

        compositeInfoBuffer.putFloat(effShadowOffsetX);
        compositeInfoBuffer.putFloat(effShadowOffsetY);
        compositeInfoBuffer.putFloat(effShadowRadius);
        compositeInfoBuffer.putFloat(effShadowOpacity);

        int count = Math.min(boxes.size(), MAX_BLUR_BOXES);
        compositeInfoBuffer.putFloat((float) count);
        compositeInfoBuffer.putFloat(0.0f);
        compositeInfoBuffer.putFloat(0.0f);
        compositeInfoBuffer.putFloat(0.0f);

        for (int i = 0; i < MAX_BLUR_BOXES; i++) {
            if (i < count) {
                BlurUtil.Shape box = boxes.get(i);
                compositeInfoBuffer.putFloat(box.x());
                compositeInfoBuffer.putFloat(box.y());
                compositeInfoBuffer.putFloat(box.width());
                compositeInfoBuffer.putFloat(box.height());
            } else {
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
            }
        }

        for (int i = 0; i < MAX_BLUR_BOXES; i++) {
            if (i < count) {
                BlurUtil.Shape box = boxes.get(i);
                float smoothing = box.smoothing() >= 0.0f ? box.smoothing() : 0.8f;
                float shapeMode = (float) box.shapeMode();
                if (shapeMode == 0.0f && box.squircle() != null && box.squircle()) {
                    shapeMode = 1.0f;
                }
                float blurTypeVal;
                if (box.blurType() == null || box.blurType() == ShadowBlurUtil.BlurType.NONE) {
                    blurTypeVal = -1.0f;
                } else if (box.blurType() == ShadowBlurUtil.BlurType.ACRYLIC) {
                    blurTypeVal = 1.0f + (box.shadow() ? 10.0f : 0.0f);
                } else {
                    blurTypeVal = 0.0f + (box.shadow() ? 10.0f : 0.0f);
                }
                float blurStr = box.blurStrength() >= 0.0f ? box.blurStrength() : 1.0f;
                compositeInfoBuffer.putFloat(smoothing);
                compositeInfoBuffer.putFloat(shapeMode);
                compositeInfoBuffer.putFloat(blurStr);
                compositeInfoBuffer.putFloat(blurTypeVal);
            } else {
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
            }
        }

        for (int i = 0; i < MAX_BLUR_BOXES; i++) {
            if (i < count) {
                BlurUtil.Shape box = boxes.get(i);
                compositeInfoBuffer.putFloat(box.rTL());
                compositeInfoBuffer.putFloat(box.rTR());
                compositeInfoBuffer.putFloat(box.rBR());
                compositeInfoBuffer.putFloat(box.rBL());
            } else {
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
                compositeInfoBuffer.putFloat(0.0f);
            }
        }

        compositeInfoBuffer.flip();
        return compositeInfoBuffer;
    }

    private static ByteBuffer reusableBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }
}
