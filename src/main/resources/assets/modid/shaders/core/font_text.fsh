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

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec3 sampleColor = texture(Sampler0, texCoord0).rgb;
    float signedDistance = median(sampleColor.r, sampleColor.g, sampleColor.b) - 0.5;
    float screenDistance = signedDistance / max(fwidth(signedDistance), 0.0001);
    float alpha = clamp(screenDistance + 0.5, 0.0, 1.0);

    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
