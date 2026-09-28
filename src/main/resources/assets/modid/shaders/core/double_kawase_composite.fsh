#version 330

uniform sampler2D OriginalSampler;
uniform sampler2D GaussianSampler;
uniform sampler2D AcrylicSampler;

const int MAX_BLUR_BOXES = 128;

layout(std140) uniform CompositeInfo {
    vec4 OutputSize;
    vec4 Rect;
    vec4 Tint;
    vec4 Shape;
    vec4 Shadow;
    vec4 BoxInfo;
    vec4 Boxes[MAX_BLUR_BOXES];
    vec4 BoxData[MAX_BLUR_BOXES];
    vec4 BoxCorners[MAX_BLUR_BOXES];
};

in vec2 texCoord;
out vec4 fragColor;

float applyRounding(float value, int rule) {
    if (rule == 0) {
        return value >= 0.0 ? floor(value + 0.5) : ceil(value - 0.5);
    } else if (rule == 1) {
        return value >= 0.0 ? floor(value) : ceil(value);
    } else if (rule == 2) {
        return ceil(value);
    } else if (rule == 3) {
        return floor(value);
    }
    return value;
}

float lnorm(vec2 v, float p) {
    vec2 av = abs(v);
    if (av.x <= 0.0001 && av.y <= 0.0001) {
        return 0.0;
    }
    float vx = max(av.x, 0.0001);
    float vy = max(av.y, 0.0001);
    return pow(pow(vx, p) + pow(vy, p), 1.0 / p);
}

float squircleDist4(vec2 pos, vec2 size, vec4 radii, float smoothing) {
    float radius = (pos.x > 0.0) ? ((pos.y > 0.0) ? radii.z : radii.y) : ((pos.y > 0.0) ? radii.w : radii.x);
    radius = min(radius, min(size.x, size.y));
    vec2 v = abs(pos) - size + radius;
    float p = (smoothing <= 0.6)
        ? mix(2.0, 3.2, clamp(smoothing / 0.6, 0.0, 1.0))
        : mix(3.2, 4.0, clamp((smoothing - 0.6) / 0.4, 0.0, 1.0));
    return min(max(v.x, v.y), 0.0) + lnorm(max(v, vec2(0.0)), p) - radius;
}

float squircleRectMask4(vec2 pixel, vec4 rect, vec4 radii, float smoothing) {
    if (rect.z <= 0.5 || rect.w <= 0.5) {
        return 0.0;
    }
    vec2 center = rect.xy + rect.zw * 0.5;
    vec2 halfSize = rect.zw * 0.5;
    vec2 targetHalfSize = max(halfSize - vec2(1.0), vec2(0.0));
    float dist = squircleDist4(pixel - center, targetHalfSize, radii, smoothing);
    return 1.0 - smoothstep(-0.5, 0.5, dist);
}

float roundedBoxDistance4(vec2 point, vec2 halfSize, vec4 radii) {
    float radius = (point.x > 0.0) ? ((point.y > 0.0) ? radii.z : radii.y) : ((point.y > 0.0) ? radii.w : radii.x);
    radius = min(radius, min(halfSize.x, halfSize.y));
    vec2 q = abs(point) - halfSize + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

float roundedRectMask4(vec2 pixel, vec4 rect, vec4 radii) {
    if (rect.z <= 0.5 || rect.w <= 0.5) {
        return 0.0;
    }
    vec2 center = rect.xy + rect.zw * 0.5;
    vec2 halfSize = rect.zw * 0.5;
    vec2 targetHalfSize = max(halfSize - vec2(1.0), vec2(0.0));
    float distance = roundedBoxDistance4(pixel - center, targetHalfSize, radii);
    return 1.0 - smoothstep(-0.5, 0.5, distance);
}

float evalGaussianShadow(float dist, float radius) {
    if (dist <= 0.0) return 1.0;
    if (dist >= radius || radius <= 0.001) return 0.0;
    return clamp(1.0 - (dist / radius), 0.0, 1.0);
}

float sdfHexagon(vec2 p, float r) {
    const vec3 k = vec3(-0.866025404, 0.5, 0.577350269);
    p = abs(p);
    p -= 2.0 * min(dot(k.xy, p), 0.0) * k.xy;
    p -= vec2(clamp(p.x, -k.z * r, k.z * r), r);
    return length(p) * sign(p.y);
}

float sectorDist(vec2 pixel, vec2 center, float innerRadius, float outerRadius, float midAngle, float halfAngle) {
    if (outerRadius <= innerRadius || outerRadius <= 0.5) return 1000.0;
    vec2 p = pixel - center;
    vec2 local = vec2(p.x, -p.y);
    float ca = cos(midAngle);
    float sa = sin(midAngle);
    vec2 q = vec2(local.x * ca - local.y * sa, local.x * sa + local.y * ca);

    float radial = length(q);
    float ringMid = (outerRadius + innerRadius) * 0.5;
    float ringHalf = max((outerRadius - innerRadius) * 0.5, 0.001);
    float ring = abs(radial - ringMid) - ringHalf;
    float wedge = abs(q.x) * cos(halfAngle) - q.y * sin(halfAngle);

    float corner = clamp((outerRadius - innerRadius) * 0.12, 3.0, 6.0);
    float rr = min(corner, ringHalf - 0.05);
    rr = max(rr, 0.0);

    vec2 dd = vec2(ring + rr, wedge + rr);
    return min(max(dd.x, dd.y), 0.0) + length(max(dd, vec2(0.0))) - rr;
}

vec4 sampleBoxBlur(sampler2D blurSampler, vec2 uv, vec2 texelSize, float strength) {
    if (strength <= 0.001) return texture(OriginalSampler, uv);
    if (abs(strength - 1.0) < 0.02) {
        return texture(blurSampler, uv);
    }

    vec2 spread = texelSize * (strength * 3.5);
    vec4 t0 = texture(blurSampler, uv);
    vec4 t1 = texture(blurSampler, uv + vec2(spread.x, 0.0));
    vec4 t2 = texture(blurSampler, uv - vec2(spread.x, 0.0));
    vec4 t3 = texture(blurSampler, uv + vec2(0.0, spread.y));
    vec4 t4 = texture(blurSampler, uv - vec2(0.0, spread.y));
    vec4 t5 = texture(blurSampler, uv + vec2(spread.x, spread.y) * 0.7071);
    vec4 t6 = texture(blurSampler, uv - vec2(spread.x, spread.y) * 0.7071);
    vec4 t7 = texture(blurSampler, uv + vec2(spread.x, -spread.y) * 0.7071);
    vec4 t8 = texture(blurSampler, uv - vec2(spread.x, -spread.y) * 0.7071);

    vec4 multiSample = t0 * 0.36 + (t1 + t2 + t3 + t4) * 0.11 + (t5 + t6 + t7 + t8) * 0.05;

    if (strength < 1.0) {
        float curve = clamp(pow(strength, 0.75), 0.0, 1.0);
        return mix(texture(OriginalSampler, uv), multiSample, curve);
    } else {
        return multiSample;
    }
}

void main() {
    vec4 original = texture(OriginalSampler, texCoord);
    vec2 pixel = vec2(texCoord.x * OutputSize.x, (1.0 - texCoord.y) * OutputSize.y);
    vec2 center = Rect.xy + Rect.zw * 0.5;
    vec2 halfSize = Rect.zw * 0.5;
    vec2 local = pixel - center;
    int roundingRule = int(Shape.y + 0.5);
    float roundedRadius = applyRounding(Shape.x, roundingRule);
    float mask = 0.0;
    if (Rect.z > 0.5 && Rect.w > 0.5 && roundedRadius >= 0.0) {
        vec2 targetHalfSize = max(halfSize - vec2(1.0), vec2(0.0));
        float distance = roundedBoxDistance4(local, targetHalfSize, vec4(roundedRadius));
        mask = 1.0 - smoothstep(-0.5, 0.5, distance);
    }

    int boxCount = min(int(BoxInfo.x + 0.5), MAX_BLUR_BOXES);
    float boxMask = 0.0;
    float boxShadowMask = 0.0;
    vec4 activeBoxBlur = vec4(0.0);
    float activeWeight = 0.0;
    float shadowRadius = max(Shadow.z, 0.001);
    vec2 shadowOffset = Shadow.xy;
    vec2 oneTexel = 1.0 / OutputSize.xy;

    for (int i = 0; i < MAX_BLUR_BOXES; i++) {
        if (i >= boxCount) break;
        vec4 box = Boxes[i];
        if (box.z <= 0.5 || box.w <= 0.5) continue;

        vec4 boxRadii = vec4(
            applyRounding(BoxCorners[i].x, roundingRule),
            applyRounding(BoxCorners[i].y, roundingRule),
            applyRounding(BoxCorners[i].z, roundingRule),
            applyRounding(BoxCorners[i].w, roundingRule)
        );
        float boxSmoothing = clamp(BoxData[i].x, 0.0, 1.0);
        float shapeMode = BoxData[i].y;
        float boxStrength = BoxData[i].z >= 0.0 ? BoxData[i].z : 1.0;
        float boxTypeRaw = BoxData[i].w;
        bool hasBloom = boxTypeRaw >= 9.5;
        float boxType = hasBloom ? (boxTypeRaw - 10.0) : boxTypeRaw; // 0.0 = Gaussian, 1.0 = Acrylic

        bool isSector = (shapeMode > 5.5 && shapeMode < 6.5) || (shapeMode > 2.5 && shapeMode < 3.5 && box.z != box.w);
        bool isHexagon = (shapeMode > 3.5 && shapeMode < 4.5);
        bool isCircle = (shapeMode > 2.5 && shapeMode < 3.5 && abs(box.z - box.w) < 1.0);
        bool isPill = (shapeMode > 1.5 && shapeMode < 2.5);
        bool isSquircle = (shapeMode > 0.5 && shapeMode < 1.5);

        float candidateMask;
        if (isSector) {
            candidateMask = 1.0 - smoothstep(-0.75, 0.75, sectorDist(pixel, box.xy, box.z, box.w, BoxCorners[i].x, BoxCorners[i].y));
        } else if (isHexagon) {
            vec2 boxCenter = box.xy + box.zw * 0.5;
            float r = min(box.z, box.w) * 0.5;
            float dist = sdfHexagon(pixel - boxCenter, r * 0.866025);
            candidateMask = 1.0 - smoothstep(-0.5, 0.5, dist);
        } else if (isCircle) {
            vec2 boxCenter = box.xy + box.zw * 0.5;
            float r = min(box.z, box.w) * 0.5;
            float dist = length(pixel - boxCenter) - r;
            candidateMask = 1.0 - smoothstep(-0.5, 0.5, dist);
        } else if (isPill) {
            vec2 halfBox = box.zw * 0.5;
            vec2 boxCenter = box.xy + halfBox;
            float r = min(halfBox.x, halfBox.y);
            vec2 d = abs(pixel - boxCenter) - max(halfBox - vec2(r), vec2(0.0));
            float dist = min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - r;
            candidateMask = 1.0 - smoothstep(-0.5, 0.5, dist);
        } else if (isSquircle) {
            candidateMask = squircleRectMask4(pixel, box, boxRadii, boxSmoothing);
        } else {
            candidateMask = roundedRectMask4(pixel, box, boxRadii);
        }

        if (candidateMask > 0.001) {
            if (boxType >= -0.5 && boxStrength > 0.001) {
                vec4 chosenBlur = (boxType > 0.5)
                    ? sampleBoxBlur(AcrylicSampler, texCoord, oneTexel, boxStrength)
                    : sampleBoxBlur(GaussianSampler, texCoord, oneTexel, boxStrength);
                chosenBlur.rgb = mix(chosenBlur.rgb, vec3(0.0), Tint.x * 0.15);
                chosenBlur.a = 1.0;

                activeBoxBlur += chosenBlur * candidateMask;
                activeWeight += candidateMask;
                boxMask = max(boxMask, candidateMask);
            }
        }

        if (hasBloom) {
            float shadowDist = 1000.0;
            if (isSector) {
                shadowDist = sectorDist(pixel - shadowOffset, box.xy, box.z, box.w, BoxCorners[i].x, BoxCorners[i].y);
            } else if (isHexagon) {
                vec2 boxCenter = box.xy + box.zw * 0.5;
                float r = min(box.z, box.w) * 0.5;
                shadowDist = sdfHexagon(pixel - boxCenter - shadowOffset, r * 0.866025);
            } else if (isCircle) {
                vec2 boxCenter = box.xy + box.zw * 0.5;
                float r = min(box.z, box.w) * 0.5;
                shadowDist = length(pixel - boxCenter - shadowOffset) - r;
            } else if (isPill) {
                vec2 halfBox = box.zw * 0.5;
                vec2 boxCenter = box.xy + halfBox;
                float r = min(halfBox.x, halfBox.y);
                vec2 d = abs(pixel - boxCenter - shadowOffset) - max(halfBox - vec2(r), vec2(0.0));
                shadowDist = min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - r;
            } else if (isSquircle) {
                shadowDist = squircleDist4(pixel - (box.xy + box.zw * 0.5) - shadowOffset, box.zw * 0.5, boxRadii, boxSmoothing);
            } else {
                shadowDist = roundedBoxDistance4(pixel - (box.xy + box.zw * 0.5) - shadowOffset, box.zw * 0.5, boxRadii);
            }
            float candidateShadow = evalGaussianShadow(shadowDist, shadowRadius) * clamp(boxStrength, 0.0, 1.0);
            boxShadowMask = max(boxShadowMask, candidateShadow);
        }
    }

    vec4 panelBlur = (Tint.z > 0.5)
        ? texture(AcrylicSampler, texCoord)
        : texture(GaussianSampler, texCoord);
    panelBlur.rgb = mix(panelBlur.rgb, vec3(0.0), Tint.x * 0.15);
    panelBlur.a = 1.0;

    float panelShadowMask = 0.0;
    if (Rect.z > 0.5 && Rect.w > 0.5) {
        float shadowDistance = roundedBoxDistance4(local - shadowOffset, halfSize, vec4(roundedRadius));
        panelShadowMask = evalGaussianShadow(shadowDistance, shadowRadius) * Shadow.w * (1.0 - mask);
    }
    float shadowMask = max(panelShadowMask, boxShadowMask * Shadow.w * (1.0 - boxMask));

    vec4 composed = mix(original, vec4(0.0, 0.0, 0.0, 1.0), clamp(shadowMask, 0.0, 1.0));
    if (mask > 0.001) {
        composed = mix(composed, panelBlur, mask * Tint.y);
    }
    if (boxMask > 0.001 && activeWeight > 0.0001) {
        vec4 normalizedBlur = activeBoxBlur / activeWeight;
        composed = mix(composed, normalizedBlur, boxMask * Tint.y * Tint.w);
    }
    fragColor = composed;
}
