#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D MaskSampler;   // hand mask (r channel)
uniform sampler2D GlowSampler;   // burn_glow output
uniform sampler2D TrailSampler;  // current trail

layout(std140) uniform BurnData {
    vec4 res;        // x=width, y=height, z=time(sec), w=fillMode
    vec4 fire;       // x=radiusPx, y=strength, z=flameSpeed, w=intensity
    vec4 trailP;     // x=decay, y=flameHeightPx, z=flowSpeed, w=trailStrength
    vec4 fireColor;  // rgb=main hand / right color, w=glowStrength
    vec4 fireExtra;  // x=matchItemColor (1.0 or 0.0), y=dividerX, z=isRightMain, w=hasTwoHands
    vec4 fireColor2; // rgb=offhand / left color, w=unused
};

float sampleMask(vec2 coord) {
    return clamp(texture(MaskSampler, coord).r, 0.0, 1.0);
}

void main() {
    vec2 TexelSize = 1.0 / res.xy;
    float FillMode = res.w;
    float TrailStrength = trailP.w;
    float GlowStrength = fireColor.w;

    vec4 glow = texture(GlowSampler, texCoord);
    vec4 trail = texture(TrailSampler, texCoord);

    float handMask = sampleMask(texCoord);
    float outsideMask = 1.0 - smoothstep(0.015, 0.20, handMask);
    // In outline mode (FillMode == 0), the current hand model is protected so fire doesn't
    // burn through the active item, but in the air outside the current hand (outsideMask == 1),
    // the full solid silhouette trail renders completely.
    float fireGate = mix(outsideMask, 1.0, clamp(FillMode, 0.0, 1.0));

    // additive pass: output only what gets added on top of the scene
    vec3 addition = vec3(0.0);
    addition += trail.rgb * fireGate * TrailStrength * 0.86;

    float glowAlpha = clamp(glow.a * 0.94 * fireGate, 0.0, 0.76);
    addition += glow.rgb * glowAlpha * (0.72 + GlowStrength * 0.26);

    fragColor = vec4(clamp(addition, 0.0, 1.0), 1.0);
}
