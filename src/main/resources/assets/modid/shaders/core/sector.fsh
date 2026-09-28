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

void main() {
    float u = texCoord0.x - floor(texCoord0.x);
    if (u <= 0.0001 && texCoord0.x >= 1.0) u = 1.0;
    float v = texCoord0.y - floor(texCoord0.y);
    if (v <= 0.0001 && texCoord0.y >= 1.0) v = 1.0;
    u = clamp(u, 0.0, 1.0);
    v = clamp(v, 0.0, 1.0);

    float encX = floor(texCoord0.x - u + 0.5);
    float encY = floor(texCoord0.y - v + 0.5);

    float halfAngleDeg = encX * 0.5;
    float thicknessRatio = encY / 200.0;

    float width = 1.0 / max(length(vec2(dFdx(u), dFdy(u))), 0.0001);
    float height = 1.0 / max(length(vec2(dFdx(v), dFdy(v))), 0.0001);
    float side = (width + height) * 0.5;

    vec2 localPos = (vec2(u, v) - 0.5) * side;
    vec2 p = vec2(localPos.x, -localPos.y);

    float outerRadius = side * 0.5 - 2.0;
    float thickness = outerRadius * thicknessRatio;
    float innerRadius = max(outerRadius - thickness, 0.0);

    float halfAngle = radians(halfAngleDeg);

    float radial = length(p);
    float ringMid = (outerRadius + innerRadius) * 0.5;
    float ringHalf = max((outerRadius - innerRadius) * 0.5, 0.001);
    float ring = abs(radial - ringMid) - ringHalf;
    float wedge = abs(p.x) * cos(halfAngle) - p.y * sin(halfAngle);

    float corner = clamp((outerRadius - innerRadius) * 0.12, 3.0, 6.0);
    float rr = min(corner, ringHalf - 0.05);
    rr = max(rr, 0.0);

    vec2 dd = vec2(ring + rr, wedge + rr);
    float signedEdge = min(max(dd.x, dd.y), 0.0) + length(max(dd, vec2(0.0))) - rr;

    float afwidth = max(fwidth(signedEdge) * 0.75, 0.75);
    float alpha = 1.0 - smoothstep(-afwidth, afwidth, signedEdge);
    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
