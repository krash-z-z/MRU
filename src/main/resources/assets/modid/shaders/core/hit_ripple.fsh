#version 330

uniform sampler2D Scene;
uniform sampler2D DepthSampler;

layout(std140) uniform Ripples {
    vec4 header;
    vec4 header2;
    vec4 header3;
    mat4 invViewProj;
    vec4 data[32];
    vec4 colors[64];
};

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;
const float TAU = 6.28318531;

vec3 worldFromDepth(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 world = invViewProj * clip;
    return world.xyz / world.w;
}

vec3 ringColorByAngle(int i, float ang) {
    int base = i * 4;
    float t = (ang < 0.0 ? ang + TAU : ang) / TAU * 4.0;
    int seg = int(floor(t)) & 3;
    float f = fract(t);
    vec3 a = colors[base + seg].rgb;
    vec3 b = colors[base + ((seg + 1) & 3)].rgb;
    return mix(a, b, f);
}

vec3 ringColor(int i, vec2 dir) {
    return ringColorByAngle(i, atan(dir.y, dir.x));
}

void main() {
    vec2 uv = texCoord;
    int count = int(header.x + 0.5);
    float aspect = header.y;
    float time = header.z;
    float warpAmp = header.w;
    float tintAmount = header2.x;
    float satFactor = header2.y;
    float glowOn = header2.z;
    float glowIntensity = header2.w;

    float sceneDepth = texture(DepthSampler, uv).r;
    bool hasScene = sceneDepth < 1.0;
    vec3 scenePos = worldFromDepth(uv, hasScene ? sceneDepth : 1.0);

    vec2 offset = vec2(0.0);
    float waveInfluence = 0.0;
    float tintAccum = 0.0;
    vec3 tintColor = vec3(0.0);
    float glowAccum = 0.0;
    vec3 glowColor = vec3(0.0);

    if (hasScene && count > 0) {
        for (int i = 0; i < count; i++) {
            vec3 center = data[i * 2].xyz;
            float ringRadius = data[i * 2].w;
            float ringWidth = max(data[i * 2 + 1].x, 1e-4);
            float amp = data[i * 2 + 1].y;
            float maxRadius = data[i * 2 + 1].z;
            float env = data[i * 2 + 1].w;

            vec3 delta = scenePos - center;
            float yDiff = delta.y;

            // Height bounding with smooth falloff to avoid infinite vertical artifacts
            if (abs(yDiff) < (maxRadius * 0.85 + 1.2)) {
                float hFade = smoothstep(maxRadius * 0.85 + 1.2, 0.0, abs(yDiff));
                float horizDist = length(delta.xz);
                float sphereDist = length(delta);
                // Hybrid spherical/planar distance for optimal voxel hugging on ground and obstacles
                float dist = mix(sphereDist, horizDist, 0.4);

                float w = (dist - ringRadius) / ringWidth;
                if (abs(w) < 1.0) {
                    float band = smoothstep(0.0, 1.0, 1.0 - abs(w)) * env * hFade;
                    float crest = pow(1.0 - abs(w), 2.2) * env * hFade;

                    waveInfluence = waveInfluence + band - waveInfluence * band;
                    vec2 dDir = horizDist > 1e-5 ? delta.xz / horizDist : vec2(1.0, 0.0);
                    vec3 colTheme = ringColor(i, dDir);

                    tintColor += colTheme * band;
                    tintAccum += band;

                    if (glowOn > 0.5) {
                        float runePulse = 0.85 + 0.15 * sin(atan(dDir.y, dDir.x) * 6.0 + time * 4.0);
                        glowColor += colTheme * (crest * runePulse);
                        glowAccum += crest * runePulse;
                    }

                    float wave = sin(w * PI) * (1.0 - abs(w));
                    vec2 screenDir = dDir;
                    screenDir.x /= aspect;
                    offset += screenDir * wave * amp * hFade;

                    if (warpAmp > 0.0) {
                        vec2 turb = vec2(
                            sin(scenePos.z * 3.0 + time * 4.0),
                            cos(scenePos.x * 3.0 - time * 4.0)
                        );
                        turb.x /= aspect;
                        offset += turb * (warpAmp * 0.006 * band);
                    }
                }
            }
        }
    }

    // Chromatic aberration sampling
    vec2 offsetR = offset * 1.08;
    vec2 offsetG = offset;
    vec2 offsetB = offset * 0.92;

    vec3 col;
    col.r = texture(Scene, uv + offsetR).r;
    col.g = texture(Scene, uv + offsetG).g;
    col.b = texture(Scene, uv + offsetB).b;

    if (abs(satFactor - 1.0) > 0.001 && waveInfluence > 0.001) {
        float f = mix(1.0, satFactor, waveInfluence);
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        col = max(mix(vec3(lum), col, f), vec3(0.0));
    }

    if (tintAmount > 0.001 && tintAccum > 0.001) {
        vec3 tint = tintColor / tintAccum;
        col += tint * (tintAmount * waveInfluence);
    }

    if (glowOn > 0.5 && glowAccum > 0.001) {
        vec3 glow = glowColor / max(glowAccum, 1e-4);
        col += glow * (glowIntensity * min(glowAccum, 1.5));
    }

    fragColor = vec4(min(col, vec3(1.0)), 1.0);
}
