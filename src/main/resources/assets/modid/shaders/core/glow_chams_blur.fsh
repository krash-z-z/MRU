#version 330

layout(std140) uniform ChamsBlurInfo {
    vec4 TintColor;     // rgb: primary accent / top color, a: opacity
    vec4 BlurParams;    // x: blurRadius, y: saturation, z: tintStrength, w: pulseVal
    vec4 ScreenInfo;    // x: width, y: height, z: isDouble, w: time
    vec4 SecondaryTint; // rgb: secondary accent / bottom color, a: doublePhase
    vec4 BlurModeInfo;  // x: blurType (0: Gaussian, 1: Acrylic), y: chromatic, z: haze, w: grain
};

uniform sampler2D Sampler0; // Background scene color before chams
uniform sampler2D Sampler1; // entityOutlineTarget player silhouette mask
uniform sampler2D Sampler2; // Hand mask view for first-person hand occlusion

in vec2 texCoord0;

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

float gradientNoise(vec2 p) {
    const vec3 magic = vec3(0.06711056, 0.00583715, 52.9829189);
    return fract(magic.z * fract(dot(p, magic.xy)));
}

vec4 sampleGaussian(vec2 uv, float radius, vec2 inputSize) {
    vec2 blurScale = (max(radius, 0.5) * 0.90) / inputSize;

    const int SAMPLES = 48;
    const float GOLDEN_ANGLE = 2.39996323;

    vec4 colorAcc = texture(Sampler0, clamp(uv, 0.001, 0.999)) * 1.5;
    float totalWeight = 1.5;

    if (radius > 0.5) {
        float noiseRot = hash(gl_FragCoord.xy) * 6.2831853;

        for (int i = 0; i < SAMPLES; i++) {
            float fi = float(i) + 0.5;
            float r = sqrt(fi / float(SAMPLES));
            float theta = fi * GOLDEN_ANGLE + noiseRot;
            vec2 dir = vec2(cos(theta), sin(theta)) * blurScale * r;
            float weight = exp(-r * r * 2.0);

            colorAcc += texture(Sampler0, clamp(uv + dir, 0.001, 0.999)) * weight;
            totalWeight += weight;
        }
    }

    return colorAcc / totalWeight;
}

vec4 sampleAcrylic(vec2 uv, float radius, vec2 inputSize, float chromatic, float hazeAmount, float grainAmount) {
    vec2 blurScale = (max(radius, 0.5) * 0.90) / inputSize;

    const int SAMPLES = 48;
    const float GOLDEN_ANGLE = 2.39996323;

    vec2 pixel = floor(uv * inputSize);
    vec2 rnd = hash2(pixel + vec2(41.0, 79.0));
    float rotAngle = rnd.x * 6.2831853;
    mat2 rotMat = mat2(cos(rotAngle), -sin(rotAngle), sin(rotAngle), cos(rotAngle));

    vec4 colorAcc = vec4(0.0);
    float totalWeight = 0.0;

    float centerWeight = max(0.60 - radius * 0.015, 0.15);
    vec4 centerSample = texture(Sampler0, clamp(uv, 0.001, 0.999));
    colorAcc += centerSample * centerWeight;
    totalWeight += centerWeight;

    for (int i = 0; i < SAMPLES; i++) {
        float fi = float(i) + 0.5;
        float normDist = fi / float(SAMPLES);
        float r = sqrt(normDist);
        float theta = fi * GOLDEN_ANGLE;

        vec2 samplePos = vec2(cos(theta), sin(theta)) * r;
        samplePos = rotMat * samplePos;

        vec2 tapJitter = (hash2(pixel + vec2(float(i) * 13.17, float(i) * 37.83)) - 0.5) * (0.35 / float(SAMPLES));
        vec2 dir = (samplePos + tapJitter) * blurScale;

        float microDisp = chromatic * (r * 0.35);
        vec2 rDir = dir * (1.0 + microDisp);
        vec2 gDir = dir;
        vec2 bDir = dir * (1.0 - microDisp);

        float weight = exp(-normDist * 2.0);

        colorAcc.r += texture(Sampler0, clamp(uv + rDir, 0.001, 0.999)).r * weight;
        colorAcc.g += texture(Sampler0, clamp(uv + gDir, 0.001, 0.999)).g * weight;
        colorAcc.b += texture(Sampler0, clamp(uv + bDir, 0.001, 0.999)).b * weight;
        colorAcc.a += texture(Sampler0, clamp(uv + gDir, 0.001, 0.999)).a * weight;
        totalWeight += weight;
    }

    vec4 blurredCol = colorAcc / max(totalWeight, 0.0001);

    // Subtle frosted scattering
    float frostHazeFactor = clamp(radius / 50.0, 0.0, 1.0) * hazeAmount * 0.12;
    vec3 frostedBase = mix(blurredCol.rgb, blurredCol.rgb * 1.05 + vec3(0.02), frostHazeFactor);

    // Fine tactile frosted grain overlay
    float grain = frostNoise(uv * inputSize);
    float frostRoughness = clamp(radius / 40.0, 0.15, 1.0);
    float fineGrainOverlay = 1.0 + grain * (0.035 * frostRoughness * grainAmount);

    return vec4(frostedBase * fineGrainOverlay, blurredCol.a);
}

void main() {
    ivec2 outlineSize = textureSize(Sampler1, 0);
    if (outlineSize.x <= 2 || outlineSize.y <= 2) {
        discard;
    }

    vec2 screenUv = gl_FragCoord.xy / vec2(outlineSize);
    vec2 texel = 1.0 / vec2(outlineSize);

    // First-person hand occlusion
    ivec2 handMaskSize = textureSize(Sampler2, 0);
    if (handMaskSize.x > 2 && handMaskSize.y > 2) {
        vec2 handUv = gl_FragCoord.xy / vec2(handMaskSize);
        float handVal = texture(Sampler2, handUv).r;
        if (handVal > 0.05) {
            discard;
        }
    }

    float mask = texture(Sampler1, screenUv).a;
    if (mask <= 0.01) {
        discard;
    }

    float blurRadius = clamp(BlurParams.x, 0.0, 150.0);
    float saturation = clamp(BlurParams.y, 0.0, 3.0);
    float tintStrength = clamp(BlurParams.z, 0.0, 1.0);
    float pulseVal = clamp(BlurParams.w, 0.0, 2.0);

    float opacity = clamp(TintColor.a * pulseVal, 0.0, 1.0);
    bool isDouble = ScreenInfo.z > 0.5;
    float doublePhase = SecondaryTint.a;
    int blurType = int(round(BlurModeInfo.x));

    vec2 inputSize = ScreenInfo.xy;
    vec4 blurredSample;
    if (blurType == 1) {
        // Mode 1: ACRYLIC (Frosted dispersion, subtle haze, fine tactile grain)
        float chromatic = clamp(BlurModeInfo.y, 0.0, 1.0);
        float haze = clamp(BlurModeInfo.z, 0.0, 1.0);
        float grain = clamp(BlurModeInfo.w, 0.0, 1.0);
        blurredSample = sampleAcrylic(screenUv, blurRadius, inputSize, chromatic, haze, grain);
    } else {
        // Mode 0: GAUSSIAN (Smooth radial Gaussian bokeh)
        blurredSample = sampleGaussian(screenUv, blurRadius, inputSize);
    }

    vec3 blurredCol = blurredSample.rgb;

    // Background saturation
    float bgLuma = dot(blurredCol, vec3(0.2126, 0.7152, 0.0722));
    vec3 saturatedCol = mix(vec3(bgLuma), blurredCol, saturation);

    // Theme tinting (clean multiplicative and weighted glass tint, no additive blowout)
    vec3 themeColor;
    if (isDouble) {
        float wave = sin(screenUv.x * 3.14159 + screenUv.y * 2.0 + doublePhase) * 0.5 + 0.5;
        themeColor = mix(TintColor.rgb, SecondaryTint.rgb, wave);
    } else {
        themeColor = mix(TintColor.rgb, SecondaryTint.rgb, clamp(1.0 - screenUv.y, 0.0, 1.0));
    }

    // Natural glass tint: balance between preserving scene detail and applying accent tint
    vec3 tintedCol = mix(saturatedCol, saturatedCol * themeColor * 1.25, tintStrength * 0.70);
    vec3 finalColor = mix(saturatedCol, mix(tintedCol, themeColor, tintStrength * 0.25), tintStrength);

    // Dither to eliminate banding
    vec3 dither = (1.0 / 255.0) * vec3(gradientNoise(gl_FragCoord.xy) - 0.5);
    finalColor = clamp(finalColor + dither, 0.0, 1.0);

    float alpha = mask * opacity;
    if (alpha <= 0.002) {
        discard;
    }

    fragColor = vec4(finalColor, alpha);
}
