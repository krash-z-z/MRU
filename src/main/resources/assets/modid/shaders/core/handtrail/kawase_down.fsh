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

    vec4 sum = texture(Sampler0, uv) * 4.0;
    sum += texture(Sampler0, uv - halfPixel);
    sum += texture(Sampler0, uv + halfPixel);
    sum += texture(Sampler0, uv + vec2(halfPixel.x, -halfPixel.y));
    sum += texture(Sampler0, uv - vec2(halfPixel.x, -halfPixel.y));

    fragColor = sum / 8.0;
}