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

float lnorm(vec2 v, float p) {
    v = abs(v);
    if (v.x <= 0.0 && v.y <= 0.0) {
        return 0.0;
    }
    return pow(pow(v.x, p) + pow(v.y, p), 1.0 / p);
}

float rdist_classic(vec2 pos, vec2 size, float radius) {
    radius = min(radius, min(size.x, size.y));
    vec2 v = abs(pos) - size + radius;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius;
}

float rdist_smooth(vec2 pos, vec2 size, float radius, float smoothing) {
    radius = min(radius, min(size.x, size.y));
    vec2 v = abs(pos) - size + radius;
    float p = (smoothing <= 0.6)
        ? mix(2.0, 3.2, clamp(smoothing / 0.6, 0.0, 1.0))
        : mix(3.2, 4.0, clamp((smoothing - 0.6) / 0.4, 0.0, 1.0));
    return min(max(v.x, v.y), 0.0) + lnorm(max(v, vec2(0.0)), p) - radius;
}

float rdist(vec2 pos, vec2 size, float radius, float smoothing) {
    return (smoothing > 0.001) ? rdist_smooth(pos, size, radius, smoothing) : rdist_classic(pos, size, radius);
}

void main() {
    float baseSmoothing = floor(texCoord0.x);
    float baseRadius = floor(texCoord0.y);

    float u = clamp((texCoord0.x - baseSmoothing - 0.1) / 0.8, 0.0, 1.0);
    float v = clamp((texCoord0.y - baseRadius - 0.1) / 0.8, 0.0, 1.0);

    float smoothing = clamp(baseSmoothing / 10.0, 0.0, 1.0);
    float rRatio = max(0.0, baseRadius / 64.0);

    float width = 0.8 / max(abs(dFdx(texCoord0.x)), 0.00001);
    float height = 0.8 / max(abs(dFdy(texCoord0.y)), 0.00001);

    vec2 uv = vec2(u, v);
    vec2 halfSize = vec2(width, height) * 0.5;
    vec2 local = (uv - 0.5) * vec2(width, height);

    float halfShortSide = min(halfSize.x, halfSize.y);
    float radius = halfShortSide * clamp(rRatio, 0.0, 1.0);

    vec2 targetHalfSize = max(halfSize - vec2(1.0), vec2(0.0));
    float distance = rdist(local, targetHalfSize, radius, smoothing);

    float strokeWidth = 1.0;
    float alpha = 1.0 - smoothstep(-0.5, 0.5, abs(distance) - strokeWidth * 0.5);
    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
