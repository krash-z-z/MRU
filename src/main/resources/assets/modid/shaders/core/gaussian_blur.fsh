#version 330

uniform sampler2D InputSampler;

layout(std140) uniform GaussianInfo {
    vec4 InputSizeDirection; // xy: inputSize, zw: direction
    vec4 Params;             // x: radius, y: sigma, z: offsetMult, w: reserved
};

in vec2 texCoord;
out vec4 fragColor;

float gaussianWeight(float x, float sigma, float maxR) {
    if (x >= maxR) return 0.0;
    float g = exp(-0.5 * (x * x) / (sigma * sigma));
    float win = 0.5 + 0.5 * cos(3.14159265359 * (x / maxR));
    return g * win;
}

void main() {
    vec2 inputSize = InputSizeDirection.xy;
    vec2 direction = InputSizeDirection.zw;
    float maxRadius = clamp(Params.x, 1.0, 48.0);
    float baseSigma = max(Params.y, 0.5);
    float sigma = max(baseSigma, maxRadius * 0.38);
    float offsetMult = max(Params.z, 0.5);
    vec2 texel = (direction / inputSize) * offsetMult;

    // Center tap
    float w0 = gaussianWeight(0.0, sigma, maxRadius);
    vec4 color = texture(InputSampler, texCoord) * w0;
    float totalWeight = w0;

    // Bilinear paired sampling: fetch pairs of texels using linear hardware filtering
    for (float i = 1.0; i <= 48.0; i += 2.0) {
        if (i > maxRadius) break;

        float i1 = i;
        float i2 = min(i + 1.0, maxRadius);

        float w1 = gaussianWeight(i1, sigma, maxRadius);
        float w2 = (i2 > i1) ? gaussianWeight(i2, sigma, maxRadius) : 0.0;
        float pairWeight = w1 + w2;

        if (pairWeight > 0.00001) {
            float pairOffset = (i1 * w1 + i2 * w2) / pairWeight;
            vec2 sampleOffset = texel * pairOffset;

            color += texture(InputSampler, texCoord + sampleOffset) * pairWeight;
            color += texture(InputSampler, texCoord - sampleOffset) * pairWeight;
            totalWeight += pairWeight * 2.0;
        }
    }

    vec4 finalColor = color / max(totalWeight, 0.0001);

    // Subtle triangular dither to eliminate 8-bit color banding on gradients
    float noise = fract(sin(dot(texCoord * inputSize, vec2(12.9898, 78.233))) * 43758.5453);
    float dither = (noise - 0.5) * (1.0 / 255.0);
    finalColor.rgb += dither;

    fragColor = finalColor;
}
