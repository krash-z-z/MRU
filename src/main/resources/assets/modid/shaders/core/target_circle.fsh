#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 p = (texCoord0 - vec2(0.5)) * 2.0;
    float dist = length(p);
    if (dist >= 1.0) {
        discard;
    }

    float edge = clamp(1.0 - dist, 0.0, 1.0);
    // Smooth, voluminous Gaussian bloom profile
    float core = exp(-dist * dist * 8.0);
    float halo = exp(-dist * dist * 2.8) * edge * 0.65;
    // White core blooms only on the head ring
    float whiteCore = exp(-dist * dist * 35.0) * smoothstep(0.35, 0.60, vertexColor.a);

    vec3 bloomColor = mix(vertexColor.rgb, vec3(1.0), whiteCore * 0.90);
    float totalIntensity = (core * 1.20 + halo) * smoothstep(0.0, 0.12, edge);
    float finalAlpha = totalIntensity * vertexColor.a;

    // High-frequency triangular dither to eliminate 8-bit monitor gradient banding
    float dither = (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    finalAlpha = clamp(finalAlpha + dither, 0.0, 1.0);

    vec3 finalRgb = bloomColor * finalAlpha * ColorModulator.rgb;
    fragColor = vec4(finalRgb, clamp(finalAlpha, 0.0, 1.0) * ColorModulator.a);
}
