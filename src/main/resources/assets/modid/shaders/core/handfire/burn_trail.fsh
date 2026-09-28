#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D TrailSampler;  // previous frame trail (ping-pong)
uniform sampler2D GlowSampler;   // burn_glow output
uniform sampler2D HandSampler;   // scene incl. hands
uniform sampler2D MaskSampler;   // hand mask (r channel)

layout(std140) uniform BurnData {
    vec4 res;        // x=width, y=height, z=time(sec), w=fillMode
    vec4 fire;       // x=radiusPx, y=strength, z=flameSpeed, w=intensity
    vec4 trailP;     // x=decay, y=flameHeightPx, z=flowSpeed, w=trailStrength
    vec4 fireColor;  // rgb=main hand / right color, w=glowStrength
    vec4 fireExtra;  // x=matchItemColor (1.0 or 0.0), y=dividerX, z=isRightMain, w=hasTwoHands
    vec4 fireColor2; // rgb=offhand / left color, w=unused
};

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
        mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

vec4 sampleTrail(vec2 coord) {
    return texture(TrailSampler, coord);
}

vec3 resolveHandColor(vec2 uv) {
    float dividerX = fireExtra.y > 0.01 ? fireExtra.y : 0.5;
    float isRightMain = fireExtra.z;
    float hasTwoHands = fireExtra.w;

    if (hasTwoHands > 0.5) {
        float t = smoothstep(dividerX - 0.08, dividerX + 0.08, uv.x);
        vec3 leftColor = isRightMain > 0.5 ? fireColor2.rgb : fireColor.rgb;
        vec3 rightColor = isRightMain > 0.5 ? fireColor.rgb : fireColor2.rgb;
        return mix(leftColor, rightColor, t);
    }
    return fireColor.rgb;
}

void main() {
    vec2 TexelSize = 1.0 / res.xy;
    float Time = res.z;
    float Intensity = clamp(fire.w, 0.0, 1.0);
    vec3 ThemeColor = resolveHandColor(texCoord);
    float Decay = trailP.x;
    float FlameHeight = trailP.y;
    float FlowSpeed = trailP.z;

    float flowTime = Time * FlowSpeed;
    float curlA = noise(texCoord * vec2(4.8, 7.2) + vec2(flowTime * 0.18, -flowTime * 0.28));
    float curlB = noise(texCoord * vec2(10.0, 13.0) + vec2(-flowTime * 0.22, flowTime * 0.16));
    float angle = (curlA * 2.0 - 1.0) * 2.15 + sin((texCoord.y + curlB) * 12.0 + flowTime * 1.7) * 0.74;
    float lift = mix(0.42, 1.28, clamp(FlameHeight / 56.0, 0.0, 1.0));
    vec2 flow = vec2(cos(angle) * 0.86, -lift + sin(angle) * 0.42) * TexelSize * FlowSpeed;

    vec4 previous = sampleTrail(texCoord + flow) * 0.58;
    previous += sampleTrail(texCoord + flow + vec2(TexelSize.x, 0.0) * 1.35) * 0.12;
    previous += sampleTrail(texCoord + flow - vec2(TexelSize.x, 0.0) * 1.35) * 0.12;
    previous += sampleTrail(texCoord + flow + vec2(0.0, TexelSize.y) * 1.15) * 0.09;
    previous += sampleTrail(texCoord + flow - vec2(0.0, TexelSize.y) * 1.15) * 0.09;
    previous.rgb *= Decay;
    previous.a *= Decay;

    vec4 glow = texture(GlowSampler, texCoord);
    float handMask = clamp(texture(MaskSampler, texCoord).r, 0.0, 1.0);

    float broken = noise(texCoord * vec2(16.0, 25.0) + vec2(flowTime * 0.35, -flowTime * 0.82));
    float ribbon = sin((texCoord.x - texCoord.y * 0.34) * 34.0 + flowTime * 4.2 + curlA * 5.5) * 0.5 + 0.5;
    float curlCut = smoothstep(0.20, 0.90, broken) * 0.55 + smoothstep(0.34, 0.98, ribbon) * 0.45;
    float fullSilhouetteAlpha = max(handMask * 0.85, glow.a) * (0.35 + curlCut * 0.45);
    float sourceAlpha = clamp(fullSilhouetteAlpha, 0.0, 0.74);

    float prevAlpha = previous.a * Decay;
    float outAlpha = clamp(prevAlpha + sourceAlpha * (1.0 - prevAlpha * 0.60), 0.0, 1.0);

    float coreHeat = clamp(pow(outAlpha, 1.35) * 1.35, 0.0, 1.0);
    vec3 flameColor = mix(ThemeColor, vec3(1.0), coreHeat * Intensity);

    fragColor = vec4(flameColor * outAlpha, outAlpha);
}
