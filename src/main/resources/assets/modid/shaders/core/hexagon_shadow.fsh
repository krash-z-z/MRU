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
    float baseBlur = floor(texCoord0.x);
    float blurRadius = max(baseBlur, 1.0);

    float u = clamp((texCoord0.x - baseBlur - 0.1) / 0.8, 0.0, 1.0);
    float v = clamp(texCoord0.y, 0.0, 1.0);

    float quadWidth = 0.8 / max(abs(dFdx(texCoord0.x)), 0.00001);
    float quadHeight = 1.0 / max(abs(dFdy(texCoord0.y)), 0.00001);
    float quadSide = min(quadWidth, quadHeight);

    float pad = blurRadius * 2.5;
    float hexRadius = max((quadSide - pad * 2.0) * 0.5 - 1.0, 0.0);
    vec2 local = (vec2(u, v) - 0.5) * quadSide;

    float dist = sdfHexagon(local, hexRadius * 0.866025);

    if (dist >= pad) {
        discard;
    }

    float sigma1 = max(blurRadius * 0.45, 0.5);
    float sigma2 = max(blurRadius * 0.85, 0.5);
    float shadowAlpha = mix(gaussianShadow(dist, sigma1), gaussianShadow(dist, sigma2), 0.4);

    if (shadowAlpha <= 0.001) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * shadowAlpha) * ColorModulator;
}
