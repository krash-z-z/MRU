#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator; // x = time, y = strikeSeed, z = unused, w = fade
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float distToSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

void main() {
    // texCoord0.x in [0, 1] -> p.x in [-1, 1]
    // texCoord0.y in [0, 1] -> p.y in [0, 1] (0.0 = bottom, 1.0 = top)
    vec2 p = vec2((texCoord0.x - 0.5) * 2.0, texCoord0.y);
    float time = ColorModulator.x;
    float strikeSeed = ColorModulator.y;
    float fade = ColorModulator.w;

    if (fade <= 0.002) {
        discard;
    }

    // High frequency micro-jitter during the strike
    float microTime = floor(time * 30.0);
    float microSeed = microTime * 7.13 + strikeSeed;

    // 1. Primary vertical trunk (12 nodes, 11 segments)
    vec2 trunk[12];
    for (int i = 0; i < 12; i++) {
        float ny = 1.0 - float(i) / 11.0;
        float fi = float(i);
        // Base jagged kinks determined by strikeSeed
        float k1 = (hash11(fi * 13.17 + strikeSeed) - 0.5) * 0.30;
        // Secondary octave
        float k2 = (hash11(fi * 29.53 + strikeSeed * 2.1) - 0.5) * 0.12;
        // High frequency electric buzz
        float buzz = (hash11(fi * 7.7 + microSeed) - 0.5) * 0.025;
        // Convergence envelope
        float widthFactor = sin(ny * 3.14159265 * 0.85 + 0.25);
        float nx = (k1 + k2 + buzz) * widthFactor;
        trunk[i] = vec2(nx, ny);
    }

    // 2. Branch 1: Major right fork splitting from trunk[3] (y ~ 0.73)
    vec2 b1[6];
    b1[0] = trunk[3];
    for (int k = 1; k < 6; k++) {
        float fk = float(k);
        float by = trunk[3].y - fk * 0.12;
        float bx = trunk[3].x + fk * 0.09 + (hash11(fk * 11.19 + strikeSeed + 10.0) - 0.5) * 0.16;
        b1[k] = vec2(bx, by);
    }

    // 3. Branch 2: Major left fork splitting from trunk[5] (y ~ 0.55)
    vec2 b2[5];
    b2[0] = trunk[5];
    for (int k = 1; k < 5; k++) {
        float fk = float(k);
        float by = trunk[5].y - fk * 0.11;
        float bx = trunk[5].x - fk * 0.10 + (hash11(fk * 14.31 + strikeSeed + 20.0) - 0.5) * 0.16;
        b2[k] = vec2(bx, by);
    }

    // 4. Branch 3: Upper fork splitting from trunk[2] (y ~ 0.82)
    vec2 b3[4];
    b3[0] = trunk[2];
    for (int k = 1; k < 4; k++) {
        float fk = float(k);
        float by = trunk[2].y - fk * 0.09;
        float bx = trunk[2].x - fk * 0.08 + (hash11(fk * 8.47 + strikeSeed + 30.0) - 0.5) * 0.14;
        b3[k] = vec2(bx, by);
    }

    // 5. Branch 4: Lower branch splitting from trunk[7] (y ~ 0.36)
    vec2 b4[4];
    b4[0] = trunk[7];
    for (int k = 1; k < 4; k++) {
        float fk = float(k);
        float by = trunk[7].y - fk * 0.10;
        float bx = trunk[7].x + fk * 0.08 + (hash11(fk * 12.13 + strikeSeed + 40.0) - 0.5) * 0.14;
        b4[k] = vec2(bx, by);
    }

    // 6. Branch 5: Sub-branch from b1[2] (y ~ 0.49)
    vec2 b5[3];
    b5[0] = b1[2];
    for (int k = 1; k < 3; k++) {
        float fk = float(k);
        float by = b1[2].y - fk * 0.09;
        float bx = b1[2].x + fk * 0.07 + (hash11(fk * 9.71 + strikeSeed + 50.0) - 0.5) * 0.12;
        b5[k] = vec2(bx, by);
    }

    // Calculate minimum distances
    float dTrunk = 100.0;
    for (int i = 0; i < 11; i++) {
        dTrunk = min(dTrunk, distToSegment(p, trunk[i], trunk[i + 1]));
    }

    float dB1 = 100.0;
    for (int k = 0; k < 5; k++) {
        dB1 = min(dB1, distToSegment(p, b1[k], b1[k + 1]));
    }

    float dB2 = 100.0;
    for (int k = 0; k < 4; k++) {
        dB2 = min(dB2, distToSegment(p, b2[k], b2[k + 1]));
    }

    float dB3 = 100.0;
    for (int k = 0; k < 3; k++) {
        dB3 = min(dB3, distToSegment(p, b3[k], b3[k + 1]));
    }

    float dB4 = 100.0;
    for (int k = 0; k < 3; k++) {
        dB4 = min(dB4, distToSegment(p, b4[k], b4[k + 1]));
    }

    float dB5 = 100.0;
    for (int k = 0; k < 2; k++) {
        dB5 = min(dB5, distToSegment(p, b5[k], b5[k + 1]));
    }

    float dBranches = min(min(dB1, dB2), min(min(dB3, dB4), dB5));

    // Core and glow intensities
    float trunkCore = exp(-dTrunk * dTrunk * 5500.0);
    float trunkInner = exp(-dTrunk * 42.0) * 1.25;
    float trunkOuter = exp(-dTrunk * 10.0) * 0.55;

    float branchCore = exp(-dBranches * dBranches * 7000.0) * 0.85;
    float branchInner = exp(-dBranches * 50.0) * 0.95;
    float branchOuter = exp(-dBranches * 12.0) * 0.35;

    float totalCore = clamp(trunkCore + branchCore, 0.0, 1.0);
    float totalInner = trunkInner + branchInner;
    float totalOuter = trunkOuter + branchOuter;

    // Atmospheric storm cloud / purple ionization at the top (matching screenshot)
    float topHaze = smoothstep(0.55, 1.0, p.y) * exp(-abs(p.x) * 3.0) * 0.35;
    vec3 stormCloudColor = vec3(0.25, 0.18, 0.55);

    // Electric cyan and blue palette
    vec3 cyanColor = vertexColor.rgb;
    vec3 deepBlue = cyanColor * vec3(0.25, 0.50, 1.0);
    vec3 brightCyan = mix(cyanColor, vec3(0.65, 0.95, 1.0), 0.65);

    vec3 glowRgb = mix(deepBlue * totalOuter, brightCyan * totalInner, totalInner / (totalInner + totalOuter + 0.001));
    glowRgb += stormCloudColor * topHaze;

    // Combine white core and glow
    vec3 finalRgb = mix(glowRgb, vec3(1.0), totalCore);

    float totalIntensity = totalCore * 1.5 + totalInner + totalOuter * 0.8 + topHaze * 0.5;
    float finalAlpha = clamp(totalIntensity, 0.0, 1.0) * vertexColor.a * fade;

    if (finalAlpha <= 0.002) {
        discard;
    }

    fragColor = vec4(finalRgb * finalAlpha, finalAlpha);
}
