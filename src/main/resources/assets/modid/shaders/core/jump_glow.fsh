#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 p = (texCoord0 - vec2(0.5)) * 2.0;
    float dist = length(p);
    if (dist >= 1.0) {
        discard;
    }

    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a <= 0.001) {
        discard;
    }

    // Smooth edge fade on quad boundaries to guarantee zero cutoff at quad edges
    float edge = clamp(1.0 - dist, 0.0, 1.0);
    float edgeFade = smoothstep(0.0, 0.12, edge);

    // Luminous bloom and energetic glow
    float texAlpha = tex.a * edgeFade;
    float glowIntensity = pow(texAlpha, 1.15) * 1.25;

    // Hot luminous core along peak density regions
    float hotCore = pow(texAlpha, 3.2) * 0.70;
    vec3 baseColor = vertexColor.rgb * tex.rgb;
    vec3 bloomColor = mix(baseColor, vec3(1.0), hotCore);

    float finalAlpha = clamp(glowIntensity * vertexColor.a, 0.0, 1.0);

    // High-frequency triangular dither to eliminate 8-bit monitor gradient banding
    float dither = (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    finalAlpha = clamp(finalAlpha + dither, 0.0, 1.0);

    if (finalAlpha <= 0.001) {
        discard;
    }

    fragColor = vec4(bloomColor, finalAlpha) * ColorModulator;
}
