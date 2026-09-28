#version 330

uniform sampler2D InputSampler;

layout(std140) uniform AcrylicInfo {
    vec4 InputSizeRadius; // xy: inputSize, z: radius, w: chromatic
    vec4 ExtraParams;     // x: haze, y: grain, z: reserved, w: reserved
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

vec2 hash2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float frostNoise(vec2 p) {
    float n1 = hash(p);
    float n2 = hash(p * 1.618 + vec2(19.1, 47.7));
    float n3 = hash(p * 2.37 - vec2(31.4, 11.9));
    return (n1 * 0.50 + n2 * 0.35 + n3 * 0.15) - 0.50;
}

void main() {
    vec2 inputSize = InputSizeRadius.xy;
    float radius = clamp(InputSizeRadius.z, 0.5, 120.0);
    float chromatic = clamp(InputSizeRadius.w, 0.0, 1.0);
    float hazeAmount = ExtraParams.x;
    float grainAmount = ExtraParams.y;

    vec2 texSize = inputSize;
    vec2 blurScale = (radius * 0.90) / texSize;

    const int SAMPLES = 48;
    const float GOLDEN_ANGLE = 2.39996323;

    vec2 pixel = floor(texCoord * texSize);
    vec2 rnd = hash2(pixel + vec2(41.0, 79.0));
    float rotAngle = rnd.x * 6.2831853;
    mat2 rotMat = mat2(cos(rotAngle), -sin(rotAngle), sin(rotAngle), cos(rotAngle));

    vec4 colorAcc = vec4(0.0);
    float totalWeight = 0.0;

    // Center tap with balanced frosted weight
    float centerWeight = max(0.60 - radius * 0.015, 0.15);
    vec4 centerSample = texture(InputSampler, texCoord);
    colorAcc += centerSample * centerWeight;
    totalWeight += centerWeight;

    for (int i = 0; i < SAMPLES; i++) {
        float fi = float(i) + 0.5;
        float normDist = fi / float(SAMPLES);
        float r = sqrt(normDist);
        float theta = fi * GOLDEN_ANGLE;

        // Vogel spiral sample position
        vec2 samplePos = vec2(cos(theta), sin(theta)) * r;
        samplePos = rotMat * samplePos;

        // Micro-roughness sandblasted perturbation per tap
        vec2 tapJitter = (hash2(pixel + vec2(float(i) * 13.17, float(i) * 37.83)) - 0.5) * (0.35 / float(SAMPLES));
        vec2 dir = (samplePos + tapJitter) * blurScale;

        // Spectral micro-dispersion across frosted diffusion cone
        float microDisp = chromatic * (r * 0.35);
        vec2 rDir = dir * (1.0 + microDisp);
        vec2 gDir = dir;
        vec2 bDir = dir * (1.0 - microDisp);

        // Smooth frosted diffusion curve (wide Gaussian scattering)
        float weight = exp(-normDist * 2.0);

        colorAcc.r += texture(InputSampler, clamp(texCoord + rDir, 0.001, 0.999)).r * weight;
        colorAcc.g += texture(InputSampler, clamp(texCoord + gDir, 0.001, 0.999)).g * weight;
        colorAcc.b += texture(InputSampler, clamp(texCoord + bDir, 0.001, 0.999)).b * weight;
        colorAcc.a += texture(InputSampler, clamp(texCoord + gDir, 0.001, 0.999)).a * weight;
        totalWeight += weight;
    }

    vec4 blurredCol = colorAcc / max(totalWeight, 0.0001);

    // Frosted diffuse scattering & milky haze
    float bgLuma = dot(blurredCol.rgb, vec3(0.2126, 0.7152, 0.0722));
    float frostHazeFactor = clamp(radius / 40.0, 0.0, 1.0) * hazeAmount;
    vec3 milkyGlow = vec3(0.96, 0.97, 1.0) * (bgLuma * 0.35 + 0.45);
    vec3 frostedBase = mix(blurredCol.rgb, mix(blurredCol.rgb, milkyGlow, 0.22), frostHazeFactor);

    // Fine tactile frosted grain overlay
    float grain = frostNoise(texCoord * texSize);
    float frostRoughness = clamp(radius / 35.0, 0.15, 1.0);
    float fineGrainOverlay = 1.0 + grain * (0.045 * frostRoughness * grainAmount);

    fragColor = vec4(frostedBase * fineGrainOverlay, blurredCol.a);
}
