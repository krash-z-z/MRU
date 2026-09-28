#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(std140) uniform ScanUniforms {
    vec4 ScanBounds;
    vec4 ScanParams;
    vec4 ScanColorA;
    vec4 ScanColorB;
};

in vec2 scanUv;
in vec3 scanPosition;
in vec3 scanViewNormal;

out vec4 fragColor;

const float BAND_HALF = 0.02;
const float WAVE_AMP = 0.03;
const float TRAIL_LEN = 0.5;
const float TRAIL_STRENGTH = 0.5;
const float GLOW_LEN = 0.12;
const float GLOW_STRENGTH = 0.6;
const float AMBIENT_LEN = 0.55;
const float AMBIENT_STRENGTH = 0.12;
const float HOT_MIX = 0.7;
const float SWEEP_OVERSHOOT = 0.20;

void main() {
    float textureAlpha = texture(Sampler0, scanUv).a;
    if (textureAlpha <= 0.02) {
        discard;
    }

    float ScanMinY = ScanBounds.x;
    float ScanMaxY = ScanBounds.y;
    vec2 ScanCenter = ScanBounds.zw;

    float ScanProgress = ScanParams.x;
    float ScanDirection = ScanParams.y;
    float ScanTime = ScanParams.z;
    float ScanAlpha = ScanParams.w;

    float height = max(ScanMaxY - ScanMinY, 0.001);
    float modelY = clamp((scanPosition.y - ScanMinY) / height, 0.0, 1.0);
    float bandY = mix(ScanMinY - SWEEP_OVERSHOOT, ScanMaxY + SWEEP_OVERSHOOT, ScanProgress);
    vec2 radial = scanPosition.xz - ScanCenter;
    float theta = atan(radial.y, radial.x);
    float wave = WAVE_AMP * (sin(theta * 3.0 + ScanTime * 2.6)
            + 0.45 * sin(theta * 5.0 - ScanTime * 3.4));
    float d = scanPosition.y - (bandY + wave);
    float dist = abs(d);

    float aa = max(fwidth(d) * 0.55, 0.0005);
    float core = 1.0 - smoothstep(BAND_HALF * 0.7 - aa, BAND_HALF + aa, dist);
    float hot = 1.0 - smoothstep(BAND_HALF * 0.25 - aa, BAND_HALF * 0.45 + aa, dist);
    float glow = exp(-dist / GLOW_LEN) * GLOW_STRENGTH;
    float ambient = exp(-dist / AMBIENT_LEN) * AMBIENT_STRENGTH;

    float direction = sign(ScanDirection);
    float behind = -d * direction - BAND_HALF;
    float trail = 0.0;
    if (behind > 0.0) {
        float trailLength = TRAIL_LEN * (0.25 + 0.75 * abs(ScanDirection));
        trail = (1.0 - clamp(behind / trailLength, 0.0, 1.0)) * TRAIL_STRENGTH;
    }

    float rim = pow(1.0 - clamp(abs(scanViewNormal.z), 0.0, 1.0), 1.35);
    float strength = clamp(core + glow + ambient + trail, 0.0, 1.0);
    strength *= mix(0.9, 1.12, rim);

    if (strength <= 0.012) {
        discard;
    }

    float colorMix = 0.5 + 0.5 * sin(theta + ScanTime * 0.35);
    vec3 color = mix(ScanColorA.rgb, ScanColorB.rgb, colorMix);
    vec3 hotColor = mix(color, vec3(1.0), HOT_MIX);
    vec3 rgb = color * (core + glow + ambient + trail) + hotColor * hot;
    float alpha = clamp((core + glow * 0.72 + ambient * 0.35 + trail) * ScanAlpha, 0.0, 1.0);
    fragColor = vec4(rgb * ColorModulator.rgb, alpha * textureAlpha * ColorModulator.a);
}
