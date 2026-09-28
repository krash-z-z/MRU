#version 330

uniform sampler2D Sampler0;

layout(std140) uniform KawaseUniforms {
    vec4 OffsetHalfPixel;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec2 halfPixel = OffsetHalfPixel.zw * OffsetHalfPixel.xy;

    vec4 sum = texture(Sampler0, uv + vec2(-halfPixel.x * 2.0, 0.0));
    sum += texture(Sampler0, uv + vec2(-halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture(Sampler0, uv + vec2(0.0, halfPixel.y * 2.0));
    sum += texture(Sampler0, uv + vec2(halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture(Sampler0, uv + vec2(halfPixel.x * 2.0, 0.0));
    sum += texture(Sampler0, uv + vec2(halfPixel.x, -halfPixel.y)) * 2.0;
    sum += texture(Sampler0, uv + vec2(0.0, -halfPixel.y * 2.0));
    sum += texture(Sampler0, uv + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;

    fragColor = sum / 12.0;
}