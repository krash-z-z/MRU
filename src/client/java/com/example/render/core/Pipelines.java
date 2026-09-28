package com.example.render.core;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Central registry for all Blaze3D GPU render pipelines.
 */
public final class Pipelines {

    private Pipelines() {}

    public static final RenderPipeline ROUNDED_RECT = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/rounded_rect"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/rounded_rect"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline ROUNDED_OUTLINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/rounded_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/rounded_outline"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline SQUIRCLE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/squircle"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/squircle"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline CIRCLE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/circle"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/circle"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline HEXAGON = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/hexagon"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/hexagon"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline HEXAGON_OUTLINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/hexagon_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/hexagon_outline"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline GLOW = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/glow"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/glow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline GRADIENT_RECT = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/gradient_rect"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/gradient_rect"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline TINTED_TEXTURE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/tinted_texture"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/tinted_texture"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline ROUNDED_TEXTURE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/rounded_texture"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/rounded_texture"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline FONT_TEXT = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/font_text"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/font_text"))
            .build()
    );

    public static final RenderPipeline UNIVERSAL_SHADOW = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/universal_shadow"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/universal_shadow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );

    public static final RenderPipeline HUE_SPECTRUM = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/hue_spectrum"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/hue_spectrum"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build()
    );
}
