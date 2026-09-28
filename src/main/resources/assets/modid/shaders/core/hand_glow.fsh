#version 330

layout(std140) uniform HandGlowInfo {
    vec4 TintColor;     // rgb: primary color, a: opacity
    vec4 SecondaryTint; // rgb: secondary color, a: doublePhase
    vec4 GlowParams;    // x: scale (glow radius), y: mode (1: glow, 2: outline), z: pulseVal, w: unused
    vec4 ScreenInfo;    // x: width, y: height, z: isDouble, w: time
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1; // Hand difference mask (r/g/b/a = hand silhouette)

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

void main() {
    ivec2 maskSize = textureSize(Sampler1, 0);
    if (maskSize.x <= 2 || maskSize.y <= 2) {
        discard;
    }

    vec2 screenUv = gl_FragCoord.xy / vec2(maskSize);
    vec2 texel = 1.0 / vec2(maskSize);

    float centerMask = texture(Sampler1, screenUv).a;
    if (centerMask >= 0.5) {
        discard;
    }

    int mode = int(round(GlowParams.y));
    float opacity = clamp(TintColor.a, 0.0, 1.0);
    float pulseVal = clamp(GlowParams.z, 0.0, 2.0);
    float time = ScreenInfo.w;

    // Clean 2-segment linear gradient moving straight through the hand
    float linearCoord = screenUv.x * 0.75 + screenUv.y * 1.25;
    float movingPhase = linearCoord * 3.5 - time * 2.2;
    float gradT = sin(movingPhase) * 0.5 + 0.5;
    vec3 movingColor = mix(TintColor.rgb, SecondaryTint.rgb, gradT);

    // 1. Continuous gradient edge detection for crisp outline stroke
    float dX = texture(Sampler1, screenUv + vec2(texel.x * 1.25, 0.0)).a - texture(Sampler1, screenUv - vec2(texel.x * 1.25, 0.0)).a;
    float dY = texture(Sampler1, screenUv + vec2(0.0, texel.y * 1.25)).a - texture(Sampler1, screenUv - vec2(0.0, texel.y * 1.25)).a;
    vec2 edgeGrad = vec2(dX, dY);
    float edgeDist = length(edgeGrad);
    float outlineStroke = smoothstep(0.03, 0.65, edgeDist * 2.4);
    float coreLaser = smoothstep(0.20, 0.85, edgeDist * 3.0);

    float innerHalo = 0.0;
    float outerHalo = 0.0;

    if (mode == 1) {
        // Mode 1: Continuous jittered radial Gaussian bloom (48 samples)
        float scaleSetting = max(GlowParams.x, 0.1);
        float glowRadiusPixels = clamp(scaleSetting * 32.0, 6.0, 180.0);
        vec2 blurScale = glowRadiusPixels * texel;

        const int GLOW_SAMPLES = 48;
        const float GOLDEN_ANGLE = 2.39996323;

        float glowAcc = 0.0;
        float totalWeight = 0.0;
        float noiseAngle = hash(gl_FragCoord.xy) * 6.2831853;

        for (int i = 0; i < GLOW_SAMPLES; i++) {
            float fi = float(i) + 0.5;
            float r = sqrt(fi / float(GLOW_SAMPLES));
            float theta = fi * GOLDEN_ANGLE + noiseAngle;
            vec2 dir = vec2(cos(theta), sin(theta)) * blurScale * r;
            float weight = exp(-r * r * 1.65);

            float sampleA = texture(Sampler1, clamp(screenUv + dir, 0.001, 0.999)).a;
            glowAcc += sampleA * weight;
            totalWeight += weight;
        }

        float glowIntensity = totalWeight > 0.0 ? (glowAcc / totalWeight) : 0.0;
        innerHalo = pow(glowIntensity, 0.48) * 1.40;
        outerHalo = pow(glowIntensity, 0.85) * 0.75;
    }

    // Combine crisp outline edge + optional bloom atmosphere
    float totalGlow = (mode == 1)
        ? clamp(innerHalo * 0.70 + outerHalo * 0.50 + outlineStroke * 1.50, 0.0, 1.0)
        : clamp(outlineStroke * 1.85, 0.0, 1.0);

    float finalAlpha = clamp(totalGlow * opacity * pulseVal, 0.0, 1.0);
    if (finalAlpha <= 0.002) {
        discard;
    }

    vec3 dither = (1.0 / 255.0) * vec3(gradientNoise(gl_FragCoord.xy) - 0.5);
    vec3 finalColor = mix(movingColor, vec3(1.0), coreLaser * 0.35);
    finalColor = clamp(finalColor + dither, 0.0, 1.0);

    fragColor = vec4(finalColor, finalAlpha);
}
