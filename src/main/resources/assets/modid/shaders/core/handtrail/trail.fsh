#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

layout(std140) uniform TrailUniforms {
    vec4 GlowColor;
    vec4 GlowColor2;
    vec4 TrailParams1;
    vec4 TrailParams2;
    vec4 TrailParams3;
};

in vec2 texCoord;
out vec4 fragColor;

vec2 texelSize = vec2(0.0);

float sampleMask(vec2 uv) {
    return texture(Sampler2, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;

    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = p * 2.04 + vec2(19.17, 7.31);
        a *= 0.5;
    }

    return v;
}

float maskWithin(vec2 uv, float radius) {
    float near = sampleMask(uv);
    for (int i = 0; i < 12; i++) {
        float ring = sqrt((float(i) + 0.5) / 12.0);
        float angle = float(i) * 2.39996323;
        vec2 dir = vec2(cos(angle), sin(angle));
        near = max(near, sampleMask(uv + dir * texelSize * radius * ring));
    }
    return near;
}

vec3 themeTint(vec3 sceneColor, vec2 uv) {
    float luma = clamp(dot(sceneColor, vec3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);

    vec3 c1 = GlowColor.rgb;
    float m1 = max(c1.r, max(c1.g, c1.b));
    if (m1 > 0.01) c1 = (c1 / m1) * max(0.95, m1);

    vec3 c2 = GlowColor2.rgb;
    float m2 = max(c2.r, max(c2.g, c2.b));
    if (m2 > 0.01) c2 = (c2 / m2) * max(0.95, m2);

    if (GlowColor.a > 0.5) {
        // Double color mode:
        // One end (hand emitter) starts with Color 1, other end (trailing wisps) starts with Color 2
        // Along the trail, a smooth gradient connects them, and they slowly change into each other over time!
        float body = sampleMask(uv);
        float trailPrevAlpha = clamp(texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).a, 0.0, 1.0);
        float trailAge = clamp((1.0 - body) * (0.35 + 0.65 * (1.0 - trailPrevAlpha)), 0.0, 1.0);
        float screenProgress = clamp((uv.y + (1.0 - uv.x)) * 0.5, 0.0, 1.0);
        float trailT = clamp(mix(trailAge, screenProgress, 0.35), 0.0, 1.0);

        float slowCycle = GlowColor2.a;
        float slowShift = 0.5 + 0.5 * sin(slowCycle);

        float colorT = clamp(mix(slowShift, 1.0 - slowShift, trailT), 0.0, 1.0);
        vec3 tint = mix(c1, c2, colorT);
        return clamp(tint * (0.95 + luma * 0.45), 0.0, 1.0);
    } else {
        vec3 tint = mix(c1, c2, smoothstep(0.12, 0.88, luma));
        return clamp(tint * (0.92 + luma * 0.50), 0.0, 1.0);
    }
}

void addSource(vec2 sourceUv, float weight, inout vec3 color, inout float alpha) {
    float mask = sampleMask(sourceUv);
    if (mask <= 0.001) return;

    vec3 sceneColor = texture(Sampler1, clamp(sourceUv, vec2(0.0), vec2(1.0))).rgb;
    float sourceWeight = mask * weight;
    color += themeTint(sceneColor, sourceUv) * sourceWeight;
    alpha += sourceWeight;
}

void main() {
    vec2 tc = texCoord;
    vec2 texSize = TrailParams1.xy;
    float time = TrailParams1.z;
    float intensity = TrailParams1.w;

    float speed = TrailParams2.x;
    float trailLength = TrailParams2.y;
    float trailSoftness = TrailParams2.z;
    float trailBlur = TrailParams2.w;

    float smoke = TrailParams3.x;
    float activity = TrailParams3.y;
    float motion = TrailParams3.z;
    float trailFade = TrailParams3.w;

    texelSize = 1.0 / max(texSize, vec2(1.0));

    float intensityC = clamp(intensity, 0.0, 1.5);
    float speedC = clamp(speed, 0.35, 2.4);
    float lengthC = clamp(trailLength, 0.0, 1.0);
    float softness = clamp(trailSoftness, 0.55, 2.0);
    float blurRadius = clamp(trailBlur, 0.45, 2.5);
    float smokeC = clamp(smoke, 0.0, 0.8);
    float activityC = clamp(activity, 0.0, 1.0);
    float fadeSetting = clamp(trailFade, 0.55, 0.98);

    float spread = 1.0 + blurRadius * 2.85 + lengthC * 8.5;
    float sourceReach = 1.1 + 17.0 * spread / 7.2 + 4.0;

    float n = fbm(tc * vec2(34.0, 29.0) + vec2(time * 0.13 * speedC, -time * 0.10 * speedC));
    vec2 curl = vec2(
        fbm(tc * 28.0 + vec2(time * 0.20 * speedC, 3.1)),
        fbm(tc * 31.0 + vec2(8.4, -time * 0.17 * speedC))
    ) - 0.5;

    vec2 prevUv = tc;
    prevUv += curl * texelSize * (1.9 + blurRadius * 1.35) * mix(0.4, 1.0, lengthC);

    vec2 sway = vec2(
        sin(time * 0.85 * speedC),
        cos(time * 0.55 * speedC) * 0.32
    );
    prevUv += sway * texelSize * (mix(1.5, 6.5, lengthC) + blurRadius * 5.5 + lengthC * 4.0);

    vec4 prev = texture(Sampler0, clamp(prevUv, vec2(0.0), vec2(1.0)));
    vec2 edge = step(vec2(0.0), prevUv) * step(prevUv, vec2(1.0));
    prev *= edge.x * edge.y;

    float fade = mix(max(0.78, fadeSetting - 0.02), fadeSetting, clamp(softness / 1.8, 0.0, 1.0));
    prev *= fade;
    if (prev.a < 0.02) {
        prev = vec4(0.0);
    }

    float body = sampleMask(tc);

    if (prev.a <= 0.0 && body <= 0.001 && maskWithin(tc, sourceReach) <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 sourceColor = vec3(0.0);
    float sourceAlpha = 0.0;

    addSource(tc, 1.5, sourceColor, sourceAlpha);

    for (int i = 0; i < 18; i++) {
        float fi = float(i);
        float angle = fi * 2.399963 + time * (0.18 + speedC * 0.09) + n * 2.6;
        vec2 dir = vec2(cos(angle), sin(angle));
        float dist = 1.1 + fi * spread / 7.2;
        vec2 sourceUv = tc - dir * texelSize * dist - curl * texelSize * (2.0 + fi * 0.26);
        float weight = (18.0 - fi) / 18.0;
        addSource(sourceUv, weight * 0.4, sourceColor, sourceAlpha);
    }

    float wisps = mix(0.85, 1.15, fbm(tc * 64.0 + vec2(-time * 0.34 * speedC, time * 0.23 * speedC)));

    float newAlpha = max(body * 0.95, smoothstep(0.015, 0.20, sourceAlpha / 3.0));
    newAlpha *= wisps;
    newAlpha *= (0.35 + intensityC * 0.45 + smokeC * 0.25 + activityC * 0.15);

    vec3 fallbackColor;
    if (GlowColor.a > 0.5) {
        float trailAge = clamp((1.0 - body) * (0.35 + 0.65 * (1.0 - clamp(prev.a, 0.0, 1.0))), 0.0, 1.0);
        float screenProgress = clamp((tc.y + (1.0 - tc.x)) * 0.5, 0.0, 1.0);
        float trailT = clamp(mix(trailAge, screenProgress, 0.35), 0.0, 1.0);
        float slowCycle = GlowColor2.a;
        float slowShift = 0.5 + 0.5 * sin(slowCycle);
        float colorT = clamp(mix(slowShift, 1.0 - slowShift, trailT), 0.0, 1.0);
        fallbackColor = mix(GlowColor.rgb, GlowColor2.rgb, colorT);
    } else {
        fallbackColor = GlowColor.rgb;
    }

    vec3 newColor = sourceAlpha > 0.001 ? sourceColor / sourceAlpha : fallbackColor;
    vec3 guidedPrev = GlowColor.a > 0.5 ? mix(prev.rgb, fallbackColor, 0.06) : prev.rgb;
    vec3 outColor = mix(guidedPrev, newColor, clamp(newAlpha * 2.9, 0.0, 0.92));
    float outAlpha = clamp(prev.a + newAlpha * (1.0 - prev.a), 0.0, 0.95);
    if (outAlpha < 0.02) {
        outColor = vec3(0.0);
        outAlpha = 0.0;
    }

    fragColor = vec4(outColor, outAlpha);
}