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
    float distance = length(texCoord0 - vec2(0.5)) * 2.0;
    float alpha = 1.0 - smoothstep(0.12, 1.0, distance);
    alpha *= alpha;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
