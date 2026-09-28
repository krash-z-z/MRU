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
    vec2 av = abs(v);
    if (av.x <= 0.0001 && av.y <= 0.0001) {
        return 0.0;
    }
    float vx = max(av.x, 0.0001);
    float vy = max(av.y, 0.0001);
    return pow(pow(vx, p) + pow(vy, p), 1.0 / p);
}

float squircleDist(vec2 pos, vec2 size, float radius, float smoothing) {
    if (radius <= 0.001) {
        vec2 v = abs(pos) - size;
        return max(v.x, v.y);
    }
    radius = min(radius, min(size.x, size.y));
    vec2 v = abs(pos) - size + radius;
    float p = (smoothing <= 0.6)
        ? mix(2.0, 3.2, clamp(smoothing / 0.6, 0.0, 1.0))
        : mix(3.2, 4.0, clamp((smoothing - 0.6) / 0.4, 0.0, 1.0));
    return min(max(v.x, v.y), 0.0) + lnorm(max(v, vec2(0.0)), p) - radius;
}

vec3 hueToRgb(float h) {
    float r = abs(h * 6.0 - 3.0) - 1.0;
    float g = 2.0 - abs(h * 6.0 - 2.0);
    float b = 2.0 - abs(h * 6.0 - 4.0);
    return clamp(vec3(r, g, b), 0.0, 1.0);
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
    vec2 targetHalfSize = max(halfSize - vec2(1.0), vec2(0.0));
    float maxRadius = min(targetHalfSize.x, targetHalfSize.y);
    float radius = min(maxRadius, halfShortSide * rRatio);

    float distance = squircleDist(local, targetHalfSize, radius, smoothing);

    float afwidth = max(fwidth(distance) * 0.5, 0.5);
    float alpha = 1.0 - smoothstep(-afwidth, afwidth, distance);
    if (alpha <= 0.001) {
        discard;
    }

    vec3 rgb = hueToRgb(u);
    fragColor = vec4(rgb, vertexColor.a * alpha) * ColorModulator;
}
