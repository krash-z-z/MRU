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

float rdist_classic(vec2 pos, vec2 size, float radius) {
    if (radius <= 0.001) {
        vec2 v = abs(pos) - size;
        return max(v.x, v.y);
    }
    radius = min(radius, min(size.x, size.y));
    vec2 v = abs(pos) - size + radius;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius;
}

float sdfHexagon(vec2 p, float r) {
    const vec3 k = vec3(-0.866025404, 0.5, 0.577350269);
    p = abs(p);
    p -= 2.0 * min(dot(k.xy, p), 0.0) * k.xy;
    p -= vec2(clamp(p.x, -k.z * r, k.z * r), r);
    return length(p) * sign(p.y);
}

// High-accuracy error function approximation (Winitzki / Abramowitz-Stegun)
float erfApprox(float x) {
    float x2 = x * x;
    const float a = 0.147;
    float num = (4.0 / 3.141592653589793) + a * x2;
    float den = 1.0 + a * x2;
    float inner = -x2 * (num / den);
    return sign(x) * sqrt(max(1.0 - exp(inner), 0.0));
}

// True Gaussian cumulative distribution function for soft shadow penumbra
float gaussianShadow(float dist, float sigma) {
    float z = dist / (max(sigma, 0.001) * 1.41421356237);
    return clamp(0.5 - 0.5 * erfApprox(z), 0.0, 1.0);
}

void main() {
    float radius = vertexColor.r * 255.0;
    int gByte = int(vertexColor.g * 255.0 + 0.5);
    float smoothing = float(gByte & 15) / 10.0;
    int shapeMode = gByte >> 4;
    float blurRadius = max(vertexColor.b * 255.0, 1.0);
    float shadowOpacity = vertexColor.a;

    float quadWidth = 1.0 / max(abs(dFdx(texCoord0.x)), 0.00001);
    float quadHeight = 1.0 / max(abs(dFdy(texCoord0.y)), 0.00001);

    float pad = blurRadius * 2.5;
    vec2 boxHalfSize = max(vec2(quadWidth - pad * 2.0, quadHeight - pad * 2.0) * 0.5, vec2(0.0));
    vec2 local = (texCoord0 - 0.5) * vec2(quadWidth, quadHeight);

    float dist;
    if (shapeMode == 4) {
        // Hexagon: regular hexagon
        float r = min(boxHalfSize.x, boxHalfSize.y);
        dist = sdfHexagon(local, r * 0.866025);
    } else if (shapeMode == 3) {
        // Circle: exact Euclidean distance
        float r = min(boxHalfSize.x, boxHalfSize.y);
        dist = length(local) - r;
    } else if (shapeMode == 2) {
        // Pill / Capsule
        float r = min(boxHalfSize.x, boxHalfSize.y);
        vec2 d = abs(local) - max(boxHalfSize - vec2(r), vec2(0.0));
        dist = min(max(d.x, d.y), 0.0) + length(max(d, vec2(0.0))) - r;
    } else if (shapeMode == 1) {
        // Continuous squircle
        dist = squircleDist(local, boxHalfSize, radius, smoothing);
    } else {
        // Rounded rectangle
        dist = rdist_classic(local, boxHalfSize, radius);
    }

    if (dist >= pad) {
        discard;
    }

    // Realistic multi-lobe Gaussian falloff (contact core + ambient dispersion)
    float sigma1 = max(blurRadius * 0.45, 0.5);
    float sigma2 = max(blurRadius * 0.85, 0.5);
    float shadowAlpha = mix(gaussianShadow(dist, sigma1), gaussianShadow(dist, sigma2), 0.4);

    if (shadowAlpha <= 0.001) {
        discard;
    }

    fragColor = vec4(0.0, 0.0, 0.0, shadowOpacity * shadowAlpha) * ColorModulator;
}
