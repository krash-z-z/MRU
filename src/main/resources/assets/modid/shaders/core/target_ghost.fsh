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
    float halo = pow(edge, 2.0) * exp(-dist * 2.5) * 0.75;
    float core = exp(-dist * dist * 16.0);
    float whiteCore = exp(-dist * dist * 64.0) * clamp(vertexColor.a * 1.5, 0.0, 1.0);
    vec3 bloomColor = mix(vertexColor.rgb, vec3(1.0), whiteCore * 0.85);
    float totalIntensity = (core * 1.25 + halo) * smoothstep(0.0, 0.15, edge);
    float finalAlpha = totalIntensity * vertexColor.a;
    vec3 finalRgb = bloomColor * finalAlpha * ColorModulator.rgb;

    fragColor = vec4(finalRgb, clamp(finalAlpha, 0.0, 1.0) * ColorModulator.a);
}
