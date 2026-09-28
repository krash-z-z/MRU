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

const float PI = 3.14159265358979323846;
const float TWO_PI = 6.28318530717958647692;

float normalizeAngle(float a) {
    return mod(mod(a, TWO_PI) + TWO_PI, TWO_PI);
}

void main() {
    float encX = floor(texCoord0.x);
    float encY = floor(texCoord0.y);

    float localU = clamp((texCoord0.x - encX) / 0.9999, 0.0, 1.0);
    float localV = clamp((texCoord0.y - encY) / 0.9999, 0.0, 1.0);

    float startAngleDeg = floor(encX / 100.0);
    float thicknessRatio = clamp(mod(encX, 100.0) / 99.0, 0.0, 1.0);
    float sweepDeg = encY / 20.0;

    float width = 1.0 / max(abs(dFdx(localU)), 0.0001);
    float height = 1.0 / max(abs(dFdy(localV)), 0.0001);

    float side = min(width, height);
    vec2 localPos = (vec2(localU, localV) - 0.5) * side;
    float distToCenter = length(localPos);

    float outerRadius = side * 0.5 - 2.0;
    float thickness = outerRadius * thicknessRatio;
    float midRadius = outerRadius - thickness * 0.5;

    float startRad = radians(startAngleDeg);
    float sweepRad = radians(clamp(sweepDeg, 0.0, 360.0));

    float halfThickness = thickness * 0.5;
    float rIn = midRadius - halfThickness;
    float rOut = midRadius + halfThickness;

    float sdf;

    if (sweepRad >= TWO_PI - 0.005) {
        sdf = abs(distToCenter - midRadius) - halfThickness;
    } else {
        float phi = atan(localPos.y, localPos.x);
        float dAngle = normalizeAngle(phi - startRad);

        vec2 capStart = midRadius * vec2(cos(startRad), sin(startRad));
        vec2 capEnd = midRadius * vec2(cos(startRad + sweepRad), sin(startRad + sweepRad));

        float dRadial = abs(distToCenter - midRadius) - halfThickness;
        float dStart = length(localPos - capStart) - halfThickness;
        float dEnd = length(localPos - capEnd) - halfThickness;
        float dCap = min(dStart, dEnd);

        if (dAngle <= sweepRad) {
            sdf = dRadial;
        } else {
            sdf = max(dRadial, dCap);
        }
    }

    float afwidth = max(fwidth(sdf) * 0.85, 0.85);
    float alpha = 1.0 - smoothstep(-afwidth, afwidth, sdf);
    if (alpha <= 0.001) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
