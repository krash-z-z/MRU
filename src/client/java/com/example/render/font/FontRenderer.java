package com.example.render.font;

import com.example.render.context.Color;
import com.example.render.core.Pipelines;
import com.example.render.util.Color4f;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * High-performance MSDF (Multi-channel Signed Distance Field) Font Renderer
 * with O(1) ASCII direct metrics lookups and LRU unscaled text measurement caching.
 */
public final class FontRenderer {

    public static final FontRenderer INSTANCE = new FontRenderer();

    private final Map<FontFace, FontData> cache = new HashMap<>();

    private FontRenderer() {}

    public void draw(GuiGraphicsExtractor graphics, FontFace face, String text, float x, float y, float size, int color) {
        if (((color >>> 24) & 0xFF) <= 0 || text == null || text.isEmpty()) return;

        FontData data = data(face);
        if (data == null) return;
        var texture = Minecraft.getInstance().getTextureManager().getTexture(face.getTextureId());
        TextureSetup textureSetup = TextureSetup.singleTexture(
            texture.getTextureView(),
            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
        );
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float cursor = x;
        float renderY = y;
        int previous = -1;
        int length = text.length();

        for (int index = 0; index < length; index++) {
            int code = text.charAt(index);
            Glyph glyph = data.getGlyph(code);
            if (glyph == null) continue;

            if (previous != -1) {
                Map<Integer, Float> kern = data.kernings.get(previous);
                if (kern != null) {
                    Float advance = kern.get(code);
                    if (advance != null) cursor += advance * size;
                }
            }

            if (glyph.hasBounds()) {
                float x0 = cursor + glyph.planeLeft() * size;
                float y0 = renderY + data.baseline * size - glyph.planeTop() * size;
                float x1 = cursor + glyph.planeRight() * size;
                float y1 = renderY + data.baseline * size - glyph.planeBottom() * size;

                graphics.guiRenderState.addGlyphToCurrentLayer(
                    new GlyphState(
                        Pipelines.FONT_TEXT,
                        textureSetup,
                        pose,
                        x0, y0, x1, y1,
                        glyph.minU(), glyph.minV(), glyph.maxU(), glyph.maxV(),
                        color,
                        graphics.scissorStack.peek()
                    )
                );
            }

            cursor += glyph.advance() * size;
            previous = code;
        }
    }

    public void draw(GuiGraphicsExtractor graphics, FontFace face, String text, float x, float y, float size, Color color) {
        if (color == null) return;
        draw(graphics, face, text, x, y, size, color.toArgb());
    }

    public void draw(GuiGraphicsExtractor graphics, FontFace face, String text, float x, float y, float size, Color4f color) {
        if (color == null) return;
        draw(graphics, face, text, x, y, size, color.toArgbInt());
    }

    public void drawCentered(GuiGraphicsExtractor graphics, FontFace face, String text, float centerX, float y, float size, int color) {
        draw(graphics, face, text, centerX - width(face, text, size) * 0.5f, y, size, color);
    }

    public void drawCentered(GuiGraphicsExtractor graphics, FontFace face, String text, float centerX, float y, float size, Color color) {
        drawCentered(graphics, face, text, centerX, y, size, color.toArgb());
    }

    public void drawRight(GuiGraphicsExtractor graphics, FontFace face, String text, float rightX, float y, float size, int color) {
        draw(graphics, face, text, rightX - width(face, text, size), y, size, color);
    }

    public void drawRight(GuiGraphicsExtractor graphics, FontFace face, String text, float rightX, float y, float size, Color color) {
        drawRight(graphics, face, text, rightX, y, size, color.toArgb());
    }

    public void drawWithShadow(GuiGraphicsExtractor graphics, FontFace face, String text, float x, float y, float size, int color, int shadowColor) {
        if (((shadowColor >>> 24) & 0xFF) > 0) {
            float shadowOffset = Math.max(0.6f, size * 0.05f);
            draw(graphics, face, text, x + shadowOffset, y + shadowOffset, size, shadowColor);
        }
        draw(graphics, face, text, x, y, size, color);
    }

    public void drawWithShadow(GuiGraphicsExtractor graphics, FontFace face, String text, float x, float y, float size, Color color, Color shadowColor) {
        drawWithShadow(graphics, face, text, x, y, size, color.toArgb(), shadowColor != null ? shadowColor.toArgb() : 0);
    }

    public float width(FontFace face, String text, float size) {
        if (text == null || text.isEmpty()) return 0.0f;
        FontData data = data(face);
        if (data == null) return text.length() * size * 0.45f;
        return data.getUnscaledWidth(text) * size;
    }

    public float height(float size) {
        return size;
    }

    private FontData data(FontFace face) {
        return cache.computeIfAbsent(face, this::load);
    }

    private FontData load(FontFace face) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(face.getDataId());
            if (resource.isEmpty()) return null;

            try (var reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return parse(root);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private FontData parse(JsonObject root) {
        JsonObject atlas = root.getAsJsonObject("atlas");
        float atlasWidth = atlas.get("width").getAsFloat();
        float atlasHeight = atlas.get("height").getAsFloat();

        JsonObject metrics = root.getAsJsonObject("metrics");
        float baseline = metrics.has("baseline")
            ? metrics.get("baseline").getAsFloat()
            : (metrics.get("lineHeight").getAsFloat() + metrics.get("descender").getAsFloat());

        Map<Integer, Glyph> glyphs = new HashMap<>();
        JsonArray glyphArray = root.getAsJsonArray("glyphs");
        if (glyphArray != null) {
            for (JsonElement element : glyphArray) {
                JsonObject objectValue = element.getAsJsonObject();
                int code = objectValue.get("unicode").getAsInt();
                float advance = objectValue.get("advance").getAsFloat();
                JsonObject plane = objectValue.getAsJsonObject("planeBounds");
                JsonObject bounds = objectValue.getAsJsonObject("atlasBounds");

                if (plane != null && bounds != null) {
                    glyphs.put(code, new Glyph(
                        advance,
                        plane.get("left").getAsFloat(),
                        plane.get("top").getAsFloat(),
                        plane.get("right").getAsFloat(),
                        plane.get("bottom").getAsFloat(),
                        bounds.get("left").getAsFloat() / atlasWidth,
                        1.0f - bounds.get("top").getAsFloat() / atlasHeight,
                        bounds.get("right").getAsFloat() / atlasWidth,
                        1.0f - bounds.get("bottom").getAsFloat() / atlasHeight,
                        true
                    ));
                } else {
                    glyphs.put(code, new Glyph(advance, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false));
                }
            }
        }

        Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
        JsonArray kerningArray = root.getAsJsonArray("kerning");
        if (kerningArray != null) {
            for (JsonElement element : kerningArray) {
                JsonObject objectValue = element.getAsJsonObject();
                kernings.computeIfAbsent(objectValue.get("unicode1").getAsInt(), k -> new HashMap<>())
                    .put(objectValue.get("unicode2").getAsInt(), objectValue.get("advance").getAsFloat());
            }
        }

        return new FontData(baseline, glyphs, kernings);
    }

    private static final class FontData {
        private final float baseline;
        private final Map<Integer, Glyph> glyphs;
        private final Glyph[] asciiGlyphs = new Glyph[256];
        private final Glyph fallbackGlyph;
        private final Map<Integer, Map<Integer, Float>> kernings;
        private final Map<String, Float> widthCache = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Float> eldest) {
                return size() > 2048;
            }
        };

        private FontData(float baseline, Map<Integer, Glyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
            this.baseline = baseline;
            this.glyphs = glyphs;
            this.kernings = kernings;
            for (Map.Entry<Integer, Glyph> entry : glyphs.entrySet()) {
                int code = entry.getKey();
                if (code >= 0 && code < 256) {
                    this.asciiGlyphs[code] = entry.getValue();
                }
            }
            this.fallbackGlyph = glyphs.get((int) '?');
        }

        public Glyph getGlyph(int code) {
            if (code >= 0 && code < 256) {
                Glyph g = asciiGlyphs[code];
                if (g != null) return g;
            }
            Glyph g = glyphs.get(code);
            return g != null ? g : fallbackGlyph;
        }

        public float getUnscaledWidth(String text) {
            Float cached;
            synchronized (widthCache) {
                cached = widthCache.get(text);
            }
            if (cached != null) return cached;

            float total = 0.0f;
            int previous = -1;
            int len = text.length();
            for (int index = 0; index < len; index++) {
                int code = text.charAt(index);
                Glyph glyph = getGlyph(code);
                if (glyph == null) continue;

                if (previous != -1) {
                    Map<Integer, Float> kern = kernings.get(previous);
                    if (kern != null) {
                        Float advance = kern.get(code);
                        if (advance != null) total += advance;
                    }
                }

                total += glyph.advance();
                previous = code;
            }

            synchronized (widthCache) {
                widthCache.put(text, total);
            }
            return total;
        }
    }

    private record Glyph(
        float advance,
        float planeLeft,
        float planeTop,
        float planeRight,
        float planeBottom,
        float minU,
        float minV,
        float maxU,
        float maxV,
        boolean hasBounds
    ) {}

    private record GlyphState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float u0,
        float v0,
        float u1,
        float v1,
        int color,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            consumer.addVertexWith2DPose(pose, x0, y0).setUv(u0, v0).setColor(color);
            consumer.addVertexWith2DPose(pose, x0, y1).setUv(u0, v1).setColor(color);
            consumer.addVertexWith2DPose(pose, x1, y1).setUv(u1, v1).setColor(color);
            consumer.addVertexWith2DPose(pose, x1, y0).setUv(u1, v0).setColor(color);
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return textureSetup; }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            int left = (int) Math.min(x0, x1);
            int top = (int) Math.min(y0, y1);
            int right = (int) Math.max(x0, x1) + 1;
            int bottom = (int) Math.max(y0, y1) + 1;
            return new ScreenRectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
        }
    }
}
