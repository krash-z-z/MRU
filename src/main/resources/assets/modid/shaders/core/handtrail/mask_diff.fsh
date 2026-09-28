#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D DepthSampler0;
uniform sampler2D DepthSampler1;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 before = texture(Sampler0, texCoord);
    vec4 after = texture(Sampler1, texCoord);

    float dBefore = texture(DepthSampler0, texCoord).r;
    float dAfter = texture(DepthSampler1, texCoord).r;

    vec3 colorDiff = abs(after.rgb - before.rgb);
    float maxColorDiff = max(max(colorDiff.r, colorDiff.g), colorDiff.b);
    float colorHand = smoothstep(0.02, 0.06, maxColorDiff);

    // Depth difference: hands write closer / different depth than the background scene
    float depthDiff = abs(dAfter - dBefore);
    float depthHand = step(0.000001, depthDiff);

    float isHand = max(depthHand, colorHand);

    fragColor = vec4(isHand, isHand, isHand, isHand);
}