package com.example.render.core;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/**
 * GuiElementRenderState implementations for Blaze3D batching.
 */
public final class ShaderState {

    private ShaderState() {}

    public static int mixColor(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public record SquircleState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        float left, float top, float right, float bottom,
        float rRatioTL, float rRatioTR, float rRatioBR, float rRatioBL,
        float smoothing,
        int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight,
        float antiAliasWidth,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        public SquircleState(
            RenderPipeline pipeline,
            Matrix3x2f pose,
            float left, float top, float right, float bottom,
            float rRatio, float smoothing,
            int colorTopLeft, int colorBottomLeft, int colorBottomRight, int colorTopRight,
            float antiAliasWidth,
            ScreenRectangle scissorArea
        ) {
            this(pipeline, pose, left, top, right, bottom, rRatio, rRatio, rRatio, rRatio, smoothing, colorTopLeft, colorBottomLeft, colorBottomRight, colorTopRight, antiAliasWidth, scissorArea);
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float encTL = (float) Math.floor(Math.clamp(rRatioTL, 0.0f, 4.0f) * 64.0f + 0.5f);
            float encTR = (float) Math.floor(Math.clamp(rRatioTR, 0.0f, 4.0f) * 64.0f + 0.5f);
            float encBR = (float) Math.floor(Math.clamp(rRatioBR, 0.0f, 4.0f) * 64.0f + 0.5f);
            float encBL = (float) Math.floor(Math.clamp(rRatioBL, 0.0f, 4.0f) * 64.0f + 0.5f);
            float encodedSmoothing = (float) Math.floor(Math.clamp(smoothing, 0.0f, 1.0f) * 10.0f + 0.5f);

            if (encTL == encTR && encTR == encBR && encBR == encBL) {
                float u0 = encodedSmoothing + 0.1f;
                float u1 = encodedSmoothing + 0.9f;
                float v0 = encTL + 0.1f;
                float v1 = encTL + 0.9f;

                consumer.addVertexWith2DPose(pose, left, top).setUv(u0, v0).setColor(colorTopLeft);
                consumer.addVertexWith2DPose(pose, left, bottom).setUv(u0, v1).setColor(colorBottomLeft);
                consumer.addVertexWith2DPose(pose, right, bottom).setUv(u1, v1).setColor(colorBottomRight);
                consumer.addVertexWith2DPose(pose, right, top).setUv(u1, v0).setColor(colorTopRight);
            } else {
                float midX = (left + right) * 0.5f;
                float midY = (top + bottom) * 0.5f;

                float u0 = encodedSmoothing + 0.1f;
                float uMid = encodedSmoothing + 0.5f;
                float u1 = encodedSmoothing + 0.9f;

                int cTopMid = mixColor(colorTopLeft, colorTopRight, 0.5f);
                int cBotMid = mixColor(colorBottomLeft, colorBottomRight, 0.5f);
                int cLeftMid = mixColor(colorTopLeft, colorBottomLeft, 0.5f);
                int cRightMid = mixColor(colorTopRight, colorBottomRight, 0.5f);
                int cCenter = mixColor(cLeftMid, cRightMid, 0.5f);

                float v0TL = encTL + 0.1f;
                float vMidTL = encTL + 0.5f;
                consumer.addVertexWith2DPose(pose, left, top).setUv(u0, v0TL).setColor(colorTopLeft);
                consumer.addVertexWith2DPose(pose, left, midY).setUv(u0, vMidTL).setColor(cLeftMid);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidTL).setColor(cCenter);
                consumer.addVertexWith2DPose(pose, midX, top).setUv(uMid, v0TL).setColor(cTopMid);

                float vMidBL = encBL + 0.5f;
                float v1BL = encBL + 0.9f;
                consumer.addVertexWith2DPose(pose, left, midY).setUv(u0, vMidBL).setColor(cLeftMid);
                consumer.addVertexWith2DPose(pose, left, bottom).setUv(u0, v1BL).setColor(colorBottomLeft);
                consumer.addVertexWith2DPose(pose, midX, bottom).setUv(uMid, v1BL).setColor(cBotMid);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidBL).setColor(cCenter);

                float vMidBR = encBR + 0.5f;
                float v1BR = encBR + 0.9f;
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidBR).setColor(cCenter);
                consumer.addVertexWith2DPose(pose, midX, bottom).setUv(uMid, v1BR).setColor(cBotMid);
                consumer.addVertexWith2DPose(pose, right, bottom).setUv(u1, v1BR).setColor(colorBottomRight);
                consumer.addVertexWith2DPose(pose, right, midY).setUv(u1, vMidBR).setColor(cRightMid);

                float v0TR = encTR + 0.1f;
                float vMidTR = encTR + 0.5f;
                consumer.addVertexWith2DPose(pose, midX, top).setUv(uMid, v0TR).setColor(cTopMid);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidTR).setColor(cCenter);
                consumer.addVertexWith2DPose(pose, right, midY).setUv(u1, vMidTR).setColor(cRightMid);
                consumer.addVertexWith2DPose(pose, right, top).setUv(u1, v0TR).setColor(colorTopRight);
            }
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            float aa = antiAliasWidth + 1.0f;
            int x = (int) Math.floor(left - aa);
            int y = (int) Math.floor(top - aa);
            int width = Math.max(1, (int) Math.ceil((right - left) + aa * 2.0f));
            int height = Math.max(1, (int) Math.ceil((bottom - top) + aa * 2.0f));
            return new ScreenRectangle(x, y, width, height);
        }
    }

    public record ShaderRoundedRectState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        float left, float top, float right, float bottom,
        float rRatioTL, float rRatioTR, float rRatioBR, float rRatioBL,
        float smoothing,
        int color,
        float antiAliasWidth,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        public ShaderRoundedRectState(
            RenderPipeline pipeline,
            Matrix3x2f pose,
            float left, float top, float right, float bottom,
            float rRatio, float smoothing,
            int color,
            float antiAliasWidth,
            ScreenRectangle scissorArea
        ) {
            this(pipeline, pose, left, top, right, bottom, rRatio, rRatio, rRatio, rRatio, smoothing, color, antiAliasWidth, scissorArea);
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float encTL = (float) Math.floor(Math.clamp(rRatioTL, 0.0f, 1.0f) * 64.0f + 0.5f);
            float encTR = (float) Math.floor(Math.clamp(rRatioTR, 0.0f, 1.0f) * 64.0f + 0.5f);
            float encBR = (float) Math.floor(Math.clamp(rRatioBR, 0.0f, 1.0f) * 64.0f + 0.5f);
            float encBL = (float) Math.floor(Math.clamp(rRatioBL, 0.0f, 1.0f) * 64.0f + 0.5f);
            float encodedSmoothing = (float) Math.floor(Math.clamp(smoothing, 0.0f, 1.0f) * 10.0f + 0.5f);

            if (encTL == encTR && encTR == encBR && encBR == encBL) {
                float u0 = encodedSmoothing + 0.1f;
                float u1 = encodedSmoothing + 0.9f;
                float v0 = encTL + 0.1f;
                float v1 = encTL + 0.9f;

                consumer.addVertexWith2DPose(pose, left, top).setUv(u0, v0).setColor(color);
                consumer.addVertexWith2DPose(pose, left, bottom).setUv(u0, v1).setColor(color);
                consumer.addVertexWith2DPose(pose, right, bottom).setUv(u1, v1).setColor(color);
                consumer.addVertexWith2DPose(pose, right, top).setUv(u1, v0).setColor(color);
            } else {
                float midX = (left + right) * 0.5f;
                float midY = (top + bottom) * 0.5f;

                float u0 = encodedSmoothing + 0.1f;
                float uMid = encodedSmoothing + 0.5f;
                float u1 = encodedSmoothing + 0.9f;

                float v0TL = encTL + 0.1f;
                float vMidTL = encTL + 0.5f;
                consumer.addVertexWith2DPose(pose, left, top).setUv(u0, v0TL).setColor(color);
                consumer.addVertexWith2DPose(pose, left, midY).setUv(u0, vMidTL).setColor(color);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidTL).setColor(color);
                consumer.addVertexWith2DPose(pose, midX, top).setUv(uMid, v0TL).setColor(color);

                float vMidBL = encBL + 0.5f;
                float v1BL = encBL + 0.9f;
                consumer.addVertexWith2DPose(pose, left, midY).setUv(u0, vMidBL).setColor(color);
                consumer.addVertexWith2DPose(pose, left, bottom).setUv(u0, v1BL).setColor(color);
                consumer.addVertexWith2DPose(pose, midX, bottom).setUv(uMid, v1BL).setColor(color);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidBL).setColor(color);

                float vMidBR = encBR + 0.5f;
                float v1BR = encBR + 0.9f;
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidBR).setColor(color);
                consumer.addVertexWith2DPose(pose, midX, bottom).setUv(uMid, v1BR).setColor(color);
                consumer.addVertexWith2DPose(pose, right, bottom).setUv(u1, v1BR).setColor(color);
                consumer.addVertexWith2DPose(pose, right, midY).setUv(u1, vMidBR).setColor(color);

                float v0TR = encTR + 0.1f;
                float vMidTR = encTR + 0.5f;
                consumer.addVertexWith2DPose(pose, midX, top).setUv(uMid, v0TR).setColor(color);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(uMid, vMidTR).setColor(color);
                consumer.addVertexWith2DPose(pose, right, midY).setUv(u1, vMidTR).setColor(color);
                consumer.addVertexWith2DPose(pose, right, top).setUv(u1, v0TR).setColor(color);
            }
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            float aa = antiAliasWidth + 1.0f;
            int x = (int) Math.floor(left - aa);
            int y = (int) Math.floor(top - aa);
            int width = Math.max(1, (int) Math.ceil((right - left) + aa * 2.0f));
            int height = Math.max(1, (int) Math.ceil((bottom - top) + aa * 2.0f));
            return new ScreenRectangle(x, y, width, height);
        }
    }

    public record ShaderCircleState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        float left, float top, float right, float bottom,
        int color,
        float antiAliasWidth,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            consumer.addVertexWith2DPose(pose, left, top).setUv(0.0f, 0.0f).setColor(color);
            consumer.addVertexWith2DPose(pose, left, bottom).setUv(0.0f, 1.0f).setColor(color);
            consumer.addVertexWith2DPose(pose, right, bottom).setUv(1.0f, 1.0f).setColor(color);
            consumer.addVertexWith2DPose(pose, right, top).setUv(1.0f, 0.0f).setColor(color);
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            float aa = antiAliasWidth + 1.0f;
            int x = (int) Math.floor(left - aa);
            int y = (int) Math.floor(top - aa);
            int width = Math.max(1, (int) Math.ceil((right - left) + aa * 2.0f));
            int height = Math.max(1, (int) Math.ceil((bottom - top) + aa * 2.0f));
            return new ScreenRectangle(x, y, width, height);
        }
    }

    public record GlowState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        float left, float top, float right, float bottom,
        int color,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            consumer.addVertexWith2DPose(pose, left, top).setUv(0.0f, 0.0f).setColor(color);
            consumer.addVertexWith2DPose(pose, left, bottom).setUv(0.0f, 1.0f).setColor(color);
            consumer.addVertexWith2DPose(pose, right, bottom).setUv(1.0f, 1.0f).setColor(color);
            consumer.addVertexWith2DPose(pose, right, top).setUv(1.0f, 0.0f).setColor(color);
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            return new ScreenRectangle((int) left - 1, (int) top - 1, Math.max(1, (int) Math.ceil(right - left) + 2), Math.max(1, (int) Math.ceil(bottom - top) + 2));
        }
    }

    public record GradientRectState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        float left, float top, float right, float bottom,
        float rRatio, float smoothing,
        int mode, float angleDegrees,
        int colorStart, int colorEnd,
        float antiAliasWidth,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float encodedSmoothing = (float) Math.floor(Math.clamp(smoothing, 0.0f, 1.0f) * 10.0f + 0.5f) + (float) (mode & 3) * 16.0f;
            float encodedRadius = (float) Math.floor(Math.clamp(rRatio, 0.0f, 4.0f) * 64.0f + 0.5f);
            float u0 = encodedSmoothing + 0.1f;
            float u1 = encodedSmoothing + 0.9f;
            float v0 = encodedRadius + 0.1f;
            float v1 = encodedRadius + 0.9f;

            consumer.addVertexWith2DPose(pose, left, top).setUv(u0, v0).setColor(colorStart);
            consumer.addVertexWith2DPose(pose, left, bottom).setUv(u0, v1).setColor(colorStart);
            consumer.addVertexWith2DPose(pose, right, bottom).setUv(u1, v1).setColor(colorEnd);
            consumer.addVertexWith2DPose(pose, right, top).setUv(u1, v0).setColor(colorEnd);
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            float aa = antiAliasWidth + 1.0f;
            int x = (int) Math.floor(left - aa);
            int y = (int) Math.floor(top - aa);
            int width = Math.max(1, (int) Math.ceil((right - left) + aa * 2.0f));
            int height = Math.max(1, (int) Math.ceil((bottom - top) + aa * 2.0f));
            return new ScreenRectangle(x, y, width, height);
        }
    }

    public record TextureState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float left,
        float top,
        float right,
        float bottom,
        float u1,
        float v1,
        float u2,
        float v2,
        int colorLeft,
        int colorRight,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        public TextureState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            float left,
            float top,
            float right,
            float bottom,
            float u1,
            float v1,
            float u2,
            float v2,
            int color,
            ScreenRectangle scissorArea
        ) {
            this(pipeline, textureSetup, pose, left, top, right, bottom, u1, v1, u2, v2, color, color, scissorArea);
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            consumer.addVertexWith2DPose(pose, left, top).setUv(u1, v1).setColor(colorLeft);
            consumer.addVertexWith2DPose(pose, left, bottom).setUv(u1, v2).setColor(colorLeft);
            consumer.addVertexWith2DPose(pose, right, bottom).setUv(u2, v2).setColor(colorRight);
            consumer.addVertexWith2DPose(pose, right, top).setUv(u2, v1).setColor(colorRight);
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return textureSetup; }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            int x = (int) Math.floor(Math.min(left, right));
            int y = (int) Math.floor(Math.min(top, bottom));
            int width = Math.max(1, (int) Math.ceil(Math.abs(right - left)));
            int height = Math.max(1, (int) Math.ceil(Math.abs(bottom - top)));
            return new ScreenRectangle(x, y, width, height);
        }
    }

    public record RoundedTextureState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float left,
        float top,
        float right,
        float bottom,
        float rRatio,
        float smoothing,
        float uPixels,
        float vPixels,
        float regionPixels,
        int color,
        float antiAliasWidth,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            int encodedRadius = Math.max(0, Math.min(255, (int) Math.floor(Math.max(0.0f, Math.min(1.0f, rRatio)) * 255.0f + 0.5f)));
            int encodedSmoothing = Math.max(0, Math.min(255, (int) Math.floor(Math.max(0.0f, Math.min(1.0f, smoothing)) * 255.0f + 0.5f)));
            int encodedRegion = regionPixels > 0.01f ? Math.max(1, Math.min(255, (int) Math.round(regionPixels))) : 0;
            int encodedColor = (color & 0xFF000000) | (encodedRadius << 16) | (encodedSmoothing << 8) | encodedRegion;
            consumer.addVertexWith2DPose(pose, left, top).setUv(uPixels, vPixels).setColor(encodedColor);
            consumer.addVertexWith2DPose(pose, left, bottom).setUv(uPixels, vPixels + 1.0f).setColor(encodedColor);
            consumer.addVertexWith2DPose(pose, right, bottom).setUv(uPixels + 1.0f, vPixels + 1.0f).setColor(encodedColor);
            consumer.addVertexWith2DPose(pose, right, top).setUv(uPixels + 1.0f, vPixels).setColor(encodedColor);
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return textureSetup; }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            float aa = antiAliasWidth + 1.0f;
            int x = (int) Math.floor(Math.min(left, right) - aa);
            int y = (int) Math.floor(Math.min(top, bottom) - aa);
            int width = Math.max(1, (int) Math.ceil(Math.abs(right - left) + aa * 2.0f));
            int height = Math.max(1, (int) Math.ceil(Math.abs(bottom - top) + aa * 2.0f));
            return new ScreenRectangle(x, y, width, height);
        }
    }

    public record UniversalShadowState(
        RenderPipeline pipeline,
        Matrix3x2f pose,
        float left, float top, float right, float bottom,
        float rTL, float rTR, float rBR, float rBL,
        float smoothing,
        int shapeMode,
        float blurRadius,
        int color,
        ScreenRectangle scissorArea
    ) implements GuiElementRenderState {

        public UniversalShadowState(
            RenderPipeline pipeline,
            Matrix3x2f pose,
            float left, float top, float right, float bottom,
            float radius,
            float smoothing,
            int shapeMode,
            float blurRadius,
            int color,
            ScreenRectangle scissorArea
        ) {
            this(pipeline, pose, left, top, right, bottom, radius, radius, radius, radius, smoothing, shapeMode, blurRadius, color, scissorArea);
        }

        private static int encodeShadowColor(float radius, float smoothing, int shapeMode, float blurRadius, int shadowColor) {
            int rByte = (int) Math.clamp(radius, 0.0f, 255.0f);
            int sBits = (int) Math.clamp(smoothing * 10.0f + 0.5f, 0, 10);
            int mBits = Math.clamp(shapeMode, 0, 15);
            int gByte = sBits | (mBits << 4);
            int bByte = (int) Math.clamp(blurRadius, 1.0f, 255.0f);
            int aByte = (shadowColor >>> 24) & 0xFF;
            return (aByte << 24) | (rByte << 16) | (gByte << 8) | bByte;
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float encTL = (float) Math.floor(Math.clamp(rTL, 0.0f, 255.0f) + 0.5f);
            float encTR = (float) Math.floor(Math.clamp(rTR, 0.0f, 255.0f) + 0.5f);
            float encBR = (float) Math.floor(Math.clamp(rBR, 0.0f, 255.0f) + 0.5f);
            float encBL = (float) Math.floor(Math.clamp(rBL, 0.0f, 255.0f) + 0.5f);

            if (encTL == encTR && encTR == encBR && encBR == encBL) {
                int col = encodeShadowColor(encTL, smoothing, shapeMode, blurRadius, color);
                consumer.addVertexWith2DPose(pose, left, top).setUv(0.0f, 0.0f).setColor(col);
                consumer.addVertexWith2DPose(pose, left, bottom).setUv(0.0f, 1.0f).setColor(col);
                consumer.addVertexWith2DPose(pose, right, bottom).setUv(1.0f, 1.0f).setColor(col);
                consumer.addVertexWith2DPose(pose, right, top).setUv(1.0f, 0.0f).setColor(col);
            } else {
                float midX = (left + right) * 0.5f;
                float midY = (top + bottom) * 0.5f;

                int colTL = encodeShadowColor(encTL, smoothing, shapeMode, blurRadius, color);
                int colBL = encodeShadowColor(encBL, smoothing, shapeMode, blurRadius, color);
                int colBR = encodeShadowColor(encBR, smoothing, shapeMode, blurRadius, color);
                int colTR = encodeShadowColor(encTR, smoothing, shapeMode, blurRadius, color);

                // Top-Left quadrant
                consumer.addVertexWith2DPose(pose, left, top).setUv(0.0f, 0.0f).setColor(colTL);
                consumer.addVertexWith2DPose(pose, left, midY).setUv(0.0f, 0.5f).setColor(colTL);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(0.5f, 0.5f).setColor(colTL);
                consumer.addVertexWith2DPose(pose, midX, top).setUv(0.5f, 0.0f).setColor(colTL);

                // Bottom-Left quadrant
                consumer.addVertexWith2DPose(pose, left, midY).setUv(0.0f, 0.5f).setColor(colBL);
                consumer.addVertexWith2DPose(pose, left, bottom).setUv(0.0f, 1.0f).setColor(colBL);
                consumer.addVertexWith2DPose(pose, midX, bottom).setUv(0.5f, 1.0f).setColor(colBL);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(0.5f, 0.5f).setColor(colBL);

                // Bottom-Right quadrant
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(0.5f, 0.5f).setColor(colBR);
                consumer.addVertexWith2DPose(pose, midX, bottom).setUv(0.5f, 1.0f).setColor(colBR);
                consumer.addVertexWith2DPose(pose, right, bottom).setUv(1.0f, 1.0f).setColor(colBR);
                consumer.addVertexWith2DPose(pose, right, midY).setUv(1.0f, 0.5f).setColor(colBR);

                // Top-Right quadrant
                consumer.addVertexWith2DPose(pose, midX, top).setUv(0.5f, 0.0f).setColor(colTR);
                consumer.addVertexWith2DPose(pose, midX, midY).setUv(0.5f, 0.5f).setColor(colTR);
                consumer.addVertexWith2DPose(pose, right, midY).setUv(1.0f, 0.5f).setColor(colTR);
                consumer.addVertexWith2DPose(pose, right, top).setUv(1.0f, 0.0f).setColor(colTR);
            }
        }

        @Override public RenderPipeline pipeline() { return pipeline; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return scissorArea; }

        @Override
        public ScreenRectangle bounds() {
            int x = (int) Math.floor(left);
            int y = (int) Math.floor(top);
            int width = Math.max(1, (int) Math.ceil(right - left));
            int height = Math.max(1, (int) Math.ceil(bottom - top));
            return new ScreenRectangle(x, y, width, height);
        }
    }
}
