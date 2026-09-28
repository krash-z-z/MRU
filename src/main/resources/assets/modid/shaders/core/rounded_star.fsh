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

float sdStar5(in vec2 p, in float r, in float rf, in float roundness) {
    const vec2 k1 = vec2(0.809016994375, -0.587785252292);
    const vec2 k2 = vec2(-k1.x, k1.y);
    p.x = abs(p.x);
    p -= 2.0 * max(dot(k1, p), 0.0) * k1;
    p -= 2.0 * max(dot(k2, p), 0.0) * k2;
    p.x = abs(p.x);
    p.y -= r;
    vec2 ba = rf * vec2(-k1.y, k1.x) - vec2(0.0, 1.0);
    float h = clamp(dot(p, ba) / dot(ba, ba), 0.0, r);
    return length(p - ba * h) * sign(p.y * ba.x - p.x * ba.y) - roundness;
}

void main() {
    float width = 1.0 / max(abs(dFdx(texCoord0.x)), 0.0001);
    float height = 1.0 / max(abs(dFdy(texCoord0.y)), 0.0001);
    
    // Local coords from center, flip Y so star tip points upward
    vec2 local = vec2((texCoord0.x - 0.5) * width, (0.5 - texCoord0.y) * height);
    
    float radius = min(width, height) * 0.44;
    float roundness = radius * 0.12;
    float dist = sdStar5(local, radius - roundness, 0.46, roundness);
    
    // If vertexColor.a is slightly modulated (e.g. outline mode if flagged, or normal fill)
    float alpha = 1.0 - smoothstep(-0.5, 0.5, dist);
    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
