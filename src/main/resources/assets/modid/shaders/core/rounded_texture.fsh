#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

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
    vec2 v = max(abs(pos) - size + radius, 0.0);
    float p = (smoothing <= 0.6)
        ? mix(2.0, 3.2, clamp(smoothing / 0.6, 0.0, 1.0))
        : mix(3.2, 4.8, clamp((smoothing - 0.6) / 0.4, 0.0, 1.0));
    if (v.x <= 0.0 && v.y <= 0.0) {
        vec2 inside = abs(pos) - size;
        return max(inside.x, inside.y);
    }
    vec2 u = v / max(radius, 0.0001);
    float norm = pow(pow(u.x, p) + pow(u.y, p), 1.0 / p);
    return (norm - 1.0) * radius;
}

float rdist(vec2 pos, vec2 size, float radius, float smoothing) {
    return (smoothing > 0.001) ? rdist_smooth(pos, size, radius, smoothing) : rdist_classic(pos, size, radius);
}

void main() {
    float width = 1.0 / max(abs(dFdx(texCoord0.x)), 0.00001);
    float height = 1.0 / max(abs(dFdy(texCoord0.y)), 0.00001);

    bool isSubregion = (vertexColor.b > 0.001);
    float localX, localY;
    float uPixels = 0.0;
    float vPixels = 0.0;
    if (isSubregion) {
        localX = clamp(texCoord0.x - floor(texCoord0.x), 0.0, 1.0);
        localY = clamp(texCoord0.y - floor(texCoord0.y), 0.0, 1.0);
        uPixels = floor(texCoord0.x);
        vPixels = floor(texCoord0.y);
    } else {
        localX = clamp(texCoord0.x, 0.0, 1.0);
        localY = clamp(texCoord0.y, 0.0, 1.0);
    }

    float rRatio = vertexColor.r;
    float smoothing = vertexColor.g;
    float regionPixels = max(1.0, floor(vertexColor.b * 255.0 + 0.5));

    vec2 halfSize = vec2(width, height) * 0.5;
    vec2 local = (vec2(localX, localY) - 0.5) * vec2(width, height);
    float halfShortSide = min(halfSize.x, halfSize.y);
    float radius = halfShortSide * clamp(rRatio, 0.0, 1.0);

    vec2 targetHalfSize = halfSize;
    float distance = rdist(local, targetHalfSize, radius, smoothing);
    float afwidth = max(fwidth(distance) * 0.5, 0.5);
    float alpha = 1.0 - smoothstep(-afwidth, afwidth, distance);
    if (alpha <= 0.001) {
        discard;
    }

    vec2 skinUv;
    if (vertexColor.b <= 0.001) {
        skinUv = vec2(localX, localY);
    } else {
        vec2 texSize = vec2(textureSize(Sampler0, 0));
        if (texSize.x <= 0.0 || texSize.y <= 0.0) {
            texSize = vec2(64.0, 64.0);
        }
        vec2 uvInRegion = clamp(vec2(localX, localY) * regionPixels, vec2(0.0001), vec2(regionPixels - 0.0001));
        skinUv = (vec2(uPixels, vPixels) + uvInRegion) / texSize;
    }
    vec4 color = texture(Sampler0, skinUv);
    if (color.a <= 0.01) {
        discard;
    }
    fragColor = vec4(color.rgb, color.a * alpha * vertexColor.a) * ColorModulator;
}
