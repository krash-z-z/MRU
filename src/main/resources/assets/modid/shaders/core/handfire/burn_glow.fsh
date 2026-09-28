#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D HandSampler;   // scene incl. hands
uniform sampler2D MaskSampler;   // hand mask (r channel)

layout(std140) uniform BurnData {
    vec4 res;        // x=width, y=height, z=time(sec), w=fillMode
    vec4 fire;       // x=radiusPx, y=strength, z=flameSpeed, w=intensity
    vec4 trailP;     // x=decay, y=flameHeightPx, z=flowSpeed, w=trailStrength
    vec4 fireColor;  // rgb=main hand / right color, w=glowStrength
    vec4 fireExtra;  // x=matchItemColor (1.0 or 0.0), y=dividerX, z=isRightMain, w=hasTwoHands
    vec4 fireColor2; // rgb=offhand / left color, w=unused
};

const int RINGS = 6;
const int DIRS = 12;
const float TAU = 6.28318530718;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
        mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

vec3 resolveHandColor(vec2 uv) {
    float dividerX = fireExtra.y > 0.01 ? fireExtra.y : 0.5;
    float isRightMain = fireExtra.z;
    float hasTwoHands = fireExtra.w;

    if (hasTwoHands > 0.5) {
        float t = smoothstep(dividerX - 0.08, dividerX + 0.08, uv.x);
        vec3 leftColor = isRightMain > 0.5 ? fireColor2.rgb : fireColor.rgb;
        vec3 rightColor = isRightMain > 0.5 ? fireColor.rgb : fireColor2.rgb;
        return mix(leftColor, rightColor, t);
    }
    return fireColor.rgb;
}

void main() {
    vec2 TexelSize = 1.0 / res.xy;
    float Time = res.z;
    float Radius = fire.x;
    float Strength = fire.y;
    float FlameSpeed = fire.z;
    float Intensity = clamp(fire.w, 0.0, 1.0);
    vec3 ThemeColor = resolveHandColor(texCoord);

    float flowTime = Time * FlameSpeed;
    float energy = 0.0;

    for (int ring = 1; ring <= RINGS; ring++) {
        float rp = float(ring) / float(RINGS);
        float distancePx = Radius * rp;
        float ringWeight = pow(1.0 - rp * 0.86, 1.45);

        for (int dir = 0; dir < DIRS; dir++) {
            float dp = float(dir) / float(DIRS);
            float warp = noise(texCoord * 9.0 + vec2(float(ring) * 3.4, float(dir) * 2.2 + flowTime * 0.16));
            float angle = dp * TAU + (warp - 0.5) * 0.34;
            float dist = distancePx * (0.90 + warp * 0.22);
            vec2 offset = vec2(cos(angle), sin(angle)) * TexelSize * dist;
            vec2 coord = texCoord + offset;
            float sampleAlpha = clamp(texture(MaskSampler, coord).r, 0.0, 1.0);
            energy += sampleAlpha * ringWeight;
        }
    }

    energy = energy / float(RINGS * DIRS) * 4.25;
    float softHalo = smoothstep(0.018, 0.58, energy);
    float edgeNoise = noise(texCoord * vec2(15.0, 21.0) + vec2(flowTime * 0.20, -flowTime * 0.34));
    float alpha = softHalo * Strength * (0.86 + edgeNoise * 0.18);
    alpha = clamp(alpha, 0.0, 0.72);

    float heat = clamp(alpha * 1.6, 0.0, 1.0);
    vec3 glowColor = mix(ThemeColor, vec3(1.0), heat * Intensity);
    fragColor = vec4(glowColor * (0.88 + alpha * 0.42), alpha);
}
