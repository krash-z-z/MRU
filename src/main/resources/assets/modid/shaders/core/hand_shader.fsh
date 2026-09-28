#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform HandShaderInfo {
    vec4 TintColor;
    vec4 Params;
    vec4 Flags;
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float gradientNoise(vec2 p) {
    const vec3 magic = vec3(0.06711056, 0.00583715, 52.9829189);
    return fract(magic.z * fract(dot(p, magic.xy)));
}

float handMask(vec2 uv, float threshold) {
    vec2 texel = 0.5 / vec2(textureSize(Sampler1, 0));
    vec2 safeUv = clamp(uv, texel, vec2(1.0) - texel);
    vec3 d = abs(texture(Sampler0, safeUv).rgb - texture(Sampler1, safeUv).rgb);
    float thresholdFloor = max(threshold, 0.015);
    return smoothstep(thresholdFloor, thresholdFloor + 0.12, max(d.r, max(d.g, d.b)) * 7.0);
}

float smoothHandMask(vec2 uv, float centerMask, float threshold) {
    vec2 texel = 0.75 / vec2(textureSize(Sampler1, 0));
    float mask = centerMask * 0.36;
    const float SQ2 = 0.70710678;
    mask += handMask(uv + vec2(texel.x, 0.0), threshold) * 0.10;
    mask += handMask(uv - vec2(texel.x, 0.0), threshold) * 0.10;
    mask += handMask(uv + vec2(0.0, texel.y), threshold) * 0.10;
    mask += handMask(uv - vec2(0.0, texel.y), threshold) * 0.10;
    mask += handMask(uv + vec2(texel.x, texel.y) * SQ2, threshold) * 0.06;
    mask += handMask(uv + vec2(-texel.x, texel.y) * SQ2, threshold) * 0.06;
    mask += handMask(uv + vec2(texel.x, -texel.y) * SQ2, threshold) * 0.06;
    mask += handMask(uv + vec2(-texel.x, -texel.y) * SQ2, threshold) * 0.06;
    return mask;
}

vec3 smoothBackgroundBlur(vec2 uv, float radius) {
    vec2 blurScale = radius / vec2(textureSize(Sampler1, 0));
    const int sampleCount = 64;
    const float GOLDEN_ANGLE = 2.39996322973;
    vec3 blurred = texture(Sampler1, uv).rgb * 1.5;
    float total = 1.5;

    vec2 pixel = floor(uv * vec2(textureSize(Sampler1, 0)));
    float jitter = (hash(pixel + vec2(29.0, 71.0)) - 0.5) * 0.15;

    for (int i = 0; i < sampleCount; i++) {
        float fi = float(i) + 0.5 + jitter;
        float normalizedRadius = sqrt(fi / float(sampleCount));
        float angle = fi * GOLDEN_ANGLE;
        vec2 direction = vec2(cos(angle), sin(angle));
        float weight = exp(-normalizedRadius * normalizedRadius * 2.4);
        blurred += texture(Sampler1, uv + direction * blurScale * normalizedRadius).rgb * weight;
        total += weight;
    }

    return blurred / total;
}

void main() {
    vec2 uv = texCoord;
    float blurRadius = clamp(Params.x, 1.0, 150.0);
    float threshold = max(Params.y, 0.003);
    float opacity = clamp(Params.z, 0.0, 1.0);

    float mask = handMask(uv, threshold);
    if (mask <= 0.001) {
        discard;
    }
    mask = smoothHandMask(uv, mask, threshold);
    if (mask <= 0.005) {
        discard;
    }

    float fillMask = smoothstep(0.0, 0.85, mask);
    vec3 blurred = smoothBackgroundBlur(uv, blurRadius);

    vec3 brightTint = TintColor.rgb;
    float tintMax = max(brightTint.r, max(brightTint.g, brightTint.b));
    if (tintMax > 0.01) {
        brightTint = (brightTint / tintMax) * max(0.95, tintMax);
    }

    vec3 fillCol = mix(blurred, brightTint, opacity);
    vec3 dither = (1.0 / 255.0) * vec3(gradientNoise(gl_FragCoord.xy) - 0.5);
    fillCol = clamp(fillCol + dither, 0.0, 1.0);

    float outA = fillMask * opacity;
    fragColor = vec4(fillCol, outA);
}