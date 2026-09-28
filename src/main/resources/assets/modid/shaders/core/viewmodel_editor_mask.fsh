#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform EditorMaskUniforms {
    vec4 ScreenParams;
    vec4 StateParams;
    vec4 AccentColor;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 tc = texCoord;
    vec2 texel = 1.0 / max(ScreenParams.xy, vec2(1.0));

    vec3 withHand = texture(Sampler0, tc).rgb;
    vec3 withoutHand = texture(Sampler1, tc).rgb;
    float diff = length(withHand - withoutHand);
    float handMask = step(0.015, diff);

    float divider = StateParams.w > 0.01 ? StateParams.w : 0.5;
    float isLeft = step(tc.x, divider);
    float isRight = 1.0 - isLeft;

    float targetHand = isLeft * 2.0 + isRight * 1.0;
    float activeHand = StateParams.x;
    float hoveredHand = StateParams.y;

    float isActive = (activeHand > 0.5 && abs(activeHand - targetHand) < 0.5) ? 1.0 : 0.0;
    float isHover = (hoveredHand > 0.5 && abs(hoveredHand - targetHand) < 0.5) ? 1.0 : 0.0;

    float edge = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            if (x == 0 && y == 0) continue;
            vec2 offset = vec2(float(x), float(y)) * texel * 1.5;
            float nd = length(texture(Sampler0, tc + offset).rgb - texture(Sampler1, tc + offset).rgb);
            edge += step(0.015, nd);
        }
    }
    float outline = clamp(edge * (1.0 - handMask) * 0.15, 0.0, 1.0);

    float glow = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.785398;
        vec2 dir = vec2(cos(a), sin(a)) * texel * 4.5;
        float s = length(texture(Sampler0, tc + dir).rgb - texture(Sampler1, tc + dir).rgb);
        glow += step(0.015, s);
    }
    glow = clamp((glow / 8.0) * (1.0 - handMask), 0.0, 1.0);

    float pulse = 0.85 + 0.15 * sin(ScreenParams.z * 3.5);
    float intensity = isActive > 0.5 ? (1.35 * pulse) : (isHover > 0.5 ? (1.0 * pulse) : 0.45);

    vec3 baseColor = AccentColor.rgb;
    vec3 interior = baseColor * handMask * (isActive > 0.5 ? 0.18 : (isHover > 0.5 ? 0.08 : 0.03));
    vec3 outlineC = baseColor * outline * (1.2 * intensity);
    vec3 glowC = baseColor * glow * (0.45 * intensity);

    vec3 result = interior + outlineC + glowC;
    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}