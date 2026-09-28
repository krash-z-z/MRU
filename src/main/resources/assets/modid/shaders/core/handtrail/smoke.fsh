#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

layout(std140) uniform SmokeUniforms {
    vec4 SmokeParams1;
    vec4 SmokeParams2;
};

in vec2 texCoord;
out vec4 fragColor;

vec2 texelSize = vec2(0.0);

float sampleMask(vec2 uv) {
    return texture(Sampler1, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

float edgeMask(vec2 uv) {
    float c = sampleMask(uv);
    float e = 0.0;
    e += abs(c - sampleMask(uv + vec2( texelSize.x, 0.0)));
    e += abs(c - sampleMask(uv + vec2(-texelSize.x, 0.0)));
    e += abs(c - sampleMask(uv + vec2(0.0,  texelSize.y)));
    e += abs(c - sampleMask(uv + vec2(0.0, -texelSize.y)));
    return clamp(e * 0.9, 0.0, 1.0);
}

void main() {
    vec2 tc = texCoord;
    texelSize = 1.0 / max(SmokeParams1.xy, vec2(1.0));

    vec4 sharpTex = texture(Sampler2, tc);
    vec4 blurTex = texture(Sampler0, tc);

    float intensityC = clamp(SmokeParams1.z, 0.0, 1.5);
    float smokeSetting = clamp(SmokeParams1.w, 0.0, 0.8);
    float softness = clamp(SmokeParams2.x, 0.4, 2.5);
    float blurSetting = clamp(SmokeParams2.y, 0.2, 3.0);
    float activityC = clamp(SmokeParams2.z, 0.0, 1.0);

    float mask = sampleMask(tc);
    float edge = edgeMask(tc);

    float blurRatio = clamp(blurSetting * 0.28, 0.15, 0.75);
    vec4 combinedTex = mix(sharpTex, blurTex, blurRatio);

    float softMix = clamp((softness - 0.4) / 2.1, 0.0, 1.0);
    float bloom = pow(clamp(blurTex.a, 0.0, 1.0), mix(0.92, 0.76, softMix));
    bloom *= smoothstep(0.003, 0.05, blurTex.a);
    float aura = smoothstep(0.015, 0.35, blurTex.a) * smoothstep(0.003, 0.05, blurTex.a);

    float silhouetteAlpha = sharpTex.a * (0.85 + intensityC * 0.45);
    float smokeAlpha = clamp(max(silhouetteAlpha, max(bloom * 0.60, aura * 0.40)), 0.0, 0.95);
    smokeAlpha *= (0.85 + intensityC * 0.40 + smokeSetting * 0.25 + activityC * 0.15);
    smokeAlpha = clamp(smokeAlpha, 0.0, 0.95);

    // Cutoff low alpha so faint trails do not show
    const float LOW_ALPHA_CUTOFF = 0.032;
    if (smokeAlpha < LOW_ALPHA_CUTOFF) discard;
    smokeAlpha = smoothstep(LOW_ALPHA_CUTOFF, LOW_ALPHA_CUTOFF + 0.07, smokeAlpha) * smokeAlpha;

    smokeAlpha *= clamp(1.0 - mask * 0.95, 0.0, 1.0);

    vec3 smokeColor = combinedTex.rgb * (1.25 + smokeSetting * 0.30);
    float smokeLuma = dot(smokeColor, vec3(0.2126, 0.7152, 0.0722));
    if (smokeLuma < 0.12 && smokeLuma > 0.001) {
        smokeColor *= (0.12 / smokeLuma);
    }

    vec3 addColor = smokeColor * smokeAlpha;
    addColor += smokeColor * edge * bloom * (0.18 + intensityC * 0.15);

    fragColor = vec4(clamp(addColor, 0.0, 1.0), 0.0);
}