#version 330

uniform sampler2D OriginalSampler;
uniform sampler2D GaussianSampler;
uniform sampler2D AcrylicSampler;

const int MAX_GLASS_BOXES = 128;

layout(std140) uniform LiquidGlassInfo {
    vec4 OutputSize;
    vec4 Glass;
    vec4 Fresnel;
    vec4 BoxInfo;
    vec4 Boxes[MAX_GLASS_BOXES];
    vec4 BoxData[MAX_GLASS_BOXES];
    vec4 BoxOptics[MAX_GLASS_BOXES];
};

in vec2 texCoord;

out vec4 fragColor;

float lnorm(vec2 v, float p) {
    v = abs(v);
    if (v.x <= 0.0 && v.y <= 0.0) {
        return 0.0;
    }
    return pow(pow(v.x, p) + pow(v.y, p), 1.0 / p);
}

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

float sdfHexagon(vec2 p, float r) {
    const vec3 k = vec3(-0.866025404, 0.5, 0.577350269);
    p = abs(p);
    p -= 2.0 * min(dot(k.xy, p), 0.0) * k.xy;
    p -= vec2(clamp(p.x, -k.z * r, k.z * r), r);
    return length(p) * sign(p.y);
}

float roundedBoxDistance(vec2 point, vec2 halfSize, float radius) {
    radius = clamp(radius, 0.0, min(halfSize.x, halfSize.y));
    vec2 q = abs(point) - halfSize + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

// Continuous curvature squircle functions matching squircle.fsh and double_kawase_composite_squircle.fsh
float squircleDist(vec2 pos, vec2 size, float radius, float smoothing) {
    radius = min(radius, min(size.x, size.y));
    vec2 v = abs(pos) - size + radius;
    float p = (smoothing <= 0.6)
        ? mix(2.0, 3.2, clamp(smoothing / 0.6, 0.0, 1.0))
        : mix(3.2, 4.0, clamp((smoothing - 0.6) / 0.4, 0.0, 1.0));
    return min(max(v.x, v.y), 0.0) + lnorm(max(v, vec2(0.0)), p) - radius;
}

float squircleBoxDistance(vec2 point, vec2 halfSize, float radius, float smoothing) {
    vec2 d = abs(point) - halfSize;
    if (d.x > 1.5 || d.y > 1.5) {
        return max(d.x, d.y);
    }
    vec2 targetHalfSize = max(halfSize - vec2(1.0), vec2(0.0));
    return squircleDist(point, targetHalfSize, radius, smoothing);
}

float roundedRectMask(vec2 pixel, vec4 rect, float radius, bool isSquircle, float smoothing) {
    if (rect.z <= 0.5 || rect.w <= 0.5) {
        return 0.0;
    }
    vec2 halfSize = rect.zw * 0.5;
    vec2 center = rect.xy + halfSize;
    vec2 d = abs(pixel - center) - halfSize;
    if (d.x > 1.5 || d.y > 1.5) {
        return 0.0;
    }
    if (isSquircle) {
        float distance = squircleBoxDistance(pixel - center, halfSize, radius, smoothing);
        return 1.0 - smoothstep(-0.5, 0.5, distance);
    } else {
        vec2 targetHalfSize = max(halfSize - vec2(1.0), vec2(0.0));
        float distance = roundedBoxDistance(pixel - center, targetHalfSize, radius);
        return 1.0 - smoothstep(-0.5, 0.5, distance);
    }
}

float sectorDist(vec2 pixel, vec2 center, float innerRadius, float outerRadius, float midAngle, float halfAngle) {
    if (outerRadius <= innerRadius || outerRadius <= 0.5) {
        return 1000.0;
    }
    vec2 p = pixel - center;
    float radial = length(p);
    float ringMid = (outerRadius + innerRadius) * 0.5;
    float ringHalf = max((outerRadius - innerRadius) * 0.5, 0.001);
    float ring = abs(radial - ringMid) - ringHalf;

    if (halfAngle >= 3.10) {
        return ring;
    }

    // In screen coordinates where pixel.y goes down, midAngle 0 is 12 o'clock (-Y)
    vec2 local = vec2(p.x, -p.y);
    float ca = cos(midAngle);
    float sa = sin(midAngle);
    vec2 q = vec2(local.x * ca - local.y * sa, local.x * sa + local.y * ca);

    float wedge = abs(q.x) * cos(halfAngle) - q.y * sin(halfAngle);

    float corner = clamp((outerRadius - innerRadius) * 0.12, 3.0, 6.0);
    float rr = min(corner, ringHalf - 0.05);
    rr = max(rr, 0.0);

    vec2 dd = vec2(ring + rr, wedge + rr);
    return min(max(dd.x, dd.y), 0.0) + length(max(dd, vec2(0.0))) - rr;
}

void liquidShape(float field, out float surface, out float shadowGradient) {
    surface = clamp((1.0 - field) * 8.0, 0.0, 1.0);
    shadowGradient = clamp((1.5 - field) * 2.0, 0.0, 1.0)
        - clamp((1.0 - field) * 2.0, 0.0, 1.0);
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

vec4 sampleGlassBlur(float blurType, vec2 uv, vec2 texelSize, float strength) {
    if (blurType > 1.5 || blurType < -0.5 || strength <= 0.001) {
        return texture(OriginalSampler, uv);
    }
    if (blurType > 0.5) {
        return sampleBoxBlur(AcrylicSampler, uv, texelSize, strength);
    } else {
        return sampleBoxBlur(GaussianSampler, uv, texelSize, strength);
    }
}

void main() {
    vec4 original = texture(OriginalSampler, texCoord);
    
    vec2 pixel = vec2(texCoord.x * OutputSize.x, (1.0 - texCoord.y) * OutputSize.y);
    int boxCount = min(int(BoxInfo.x + 0.5), MAX_GLASS_BOXES);
    int roundingRule = int(BoxInfo.y + 0.5);
    
    int selectedBox = -1;
    float minArea = 1e12;
    float maxCoverage = 0.0;

    for (int i = 0; i < MAX_GLASS_BOXES; i++) {
        if (i >= boxCount) {
            break;
        }
        if (Boxes[i].z <= 0.5 || Boxes[i].w <= 0.5) {
            continue;
        }

        float shapeMode = BoxData[i].z;
        bool isSector = (shapeMode > 5.5 && shapeMode < 6.5) || (shapeMode > 2.5 && shapeMode < 3.5 && Boxes[i].z != Boxes[i].w);
        bool isHexagon = (shapeMode > 3.5 && shapeMode < 4.5);
        bool isCircle = (shapeMode > 2.5 && shapeMode < 3.5 && abs(Boxes[i].z - Boxes[i].w) < 1.0);
        bool isPill = (shapeMode > 1.5 && shapeMode < 2.5);
        bool isSquircle = (shapeMode > 0.5 && shapeMode < 1.5);

        float candidateMask;
        float area = Boxes[i].z * Boxes[i].w;

        if (isSector) {
            vec2 center = Boxes[i].xy;
            float inR = Boxes[i].z;
            float outR = Boxes[i].w;
            float midAng = BoxData[i].x;
            float halfAng = BoxData[i].y;
            float dist = sectorDist(pixel, center, inR, outR, midAng, halfAng);
            candidateMask = 1.0 - smoothstep(-0.75, 0.75, dist);
            area = (outR * outR - inR * inR) * halfAng;
        } else if (isHexagon) {
            vec2 center = Boxes[i].xy + Boxes[i].zw * 0.5;
            float r = min(Boxes[i].z, Boxes[i].w) * 0.5;
            float dist = sdfHexagon(pixel - center, r * 0.866025);
            candidateMask = 1.0 - smoothstep(-0.5, 0.5, dist);
        } else if (isCircle) {
            vec2 center = Boxes[i].xy + Boxes[i].zw * 0.5;
            float r = min(Boxes[i].z, Boxes[i].w) * 0.5;
            float dist = length(pixel - center) - r;
            candidateMask = 1.0 - smoothstep(-0.5, 0.5, dist);
        } else if (isPill) {
            vec2 halfSize = Boxes[i].zw * 0.5;
            vec2 center = Boxes[i].xy + halfSize;
            float r = min(halfSize.x, halfSize.y);
            vec2 d = abs(pixel - center) - max(halfSize - vec2(r), vec2(0.0));
            float dist = min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - r;
            candidateMask = 1.0 - smoothstep(-0.5, 0.5, dist);
        } else {
            float candidateRadius = applyRounding(BoxData[i].x, roundingRule);
            float candidateSmoothing = clamp(BoxData[i].y, 0.0, 1.0);
            candidateMask = roundedRectMask(pixel, Boxes[i], candidateRadius, isSquircle, candidateSmoothing);
        }

        if (candidateMask > 0.001) {
            maxCoverage = max(maxCoverage, candidateMask);
            if (candidateMask > 0.5 && area <= minArea) {
                minArea = area;
                selectedBox = i;
            } else if (selectedBox < 0) {
                selectedBox = i;
            }
        }
    }

    if (selectedBox < 0 || maxCoverage <= 0.001) {
        discard;
    }

    float selShapeMode = BoxData[selectedBox].z;
    bool selIsSector = (selShapeMode > 5.5 && selShapeMode < 6.5) || (selShapeMode > 2.5 && selShapeMode < 3.5 && Boxes[selectedBox].z != Boxes[selectedBox].w);
    bool selIsHexagon = (selShapeMode > 3.5 && selShapeMode < 4.5);
    bool selIsCircle = (selShapeMode > 2.5 && selShapeMode < 3.5 && abs(Boxes[selectedBox].z - Boxes[selectedBox].w) < 1.0);
    bool selIsPill = (selShapeMode > 1.5 && selShapeMode < 2.5);

    float boxBlurStrength = BoxOptics[selectedBox].x >= 0.0 ? BoxOptics[selectedBox].x : 1.0;
    float boxBlurType = BoxOptics[selectedBox].y;
    float boxRefraction = BoxOptics[selectedBox].z >= 0.0 ? BoxOptics[selectedBox].z : Glass.z;
    float boxSampleEscape = BoxOptics[selectedBox].w >= 0.0 ? BoxOptics[selectedBox].w : BoxInfo.z;

    vec2 samplePixel;
    vec2 normalizedLocal;
    vec2 localUv = vec2(0.5);
    float surface = 0.0;
    float shadowGradient = 0.0;

    if (selIsSector) {
        vec2 center = Boxes[selectedBox].xy;
        float inR = Boxes[selectedBox].z;
        float outR = Boxes[selectedBox].w;
        float midAng = BoxData[selectedBox].x;
        float halfAng = BoxData[selectedBox].y;
        float dist = sectorDist(pixel, center, inR, outR, midAng, halfAng);

        float secThickness = max(outR - inR, 1.0);
        float ringHalf = secThickness * 0.5;
        float ringMid = (inR + outR) * 0.5;

        float depthNorm = clamp(-dist / max(ringHalf, 1.0), 0.0, 1.0);
        float roundedBox = clamp(pow(1.0 - depthNorm, 1.5), 0.0, 1.0);

        liquidShape(roundedBox, surface, shadowGradient);

        float lensStrength = clamp(boxRefraction, 0.0, 2.5);
        float sampleEscape = max(boxSampleEscape, 0.0);
        float escapeAmount = sampleEscape > 0.0 ? sampleEscape : secThickness * 0.75;

        if (halfAng >= 3.10) {
            vec2 toCenter = center - pixel;
            float dCenter = length(toCenter);
            vec2 inDir = dCenter > 0.001 ? (toCenter / dCenter) : vec2(0.0);
            normalizedLocal = -inDir;
            localUv = clamp((pixel - (center - vec2(outR))) / max(vec2(outR * 2.0), vec2(0.001)), vec2(0.0), vec2(1.0));
            samplePixel = pixel + inDir * (escapeAmount * (1.0 - roundedBox) * (lensStrength * 0.85 + 0.15));
        } else {
            vec2 sectorCenter = center + vec2(sin(midAng), -cos(midAng)) * ringMid;
            vec2 sectorDelta = pixel - sectorCenter;
            normalizedLocal = clamp(sectorDelta / max(ringHalf, 1.0), vec2(-1.5), vec2(1.5));
            localUv = clamp(normalizedLocal * 0.5 + 0.5, vec2(0.0), vec2(1.0));
            samplePixel = pixel - sectorDelta * (roundedBox * (lensStrength * 0.85 + 0.15));
        }
    } else if (selIsHexagon) {
        vec4 rect = Boxes[selectedBox];
        vec2 halfSize = rect.zw * 0.5;
        vec2 center = rect.xy + halfSize;
        float r = min(halfSize.x, halfSize.y);
        localUv = clamp((pixel - rect.xy) / max(rect.zw, vec2(0.001)), vec2(0.0), vec2(1.0));
        normalizedLocal = clamp((pixel - center) / max(halfSize, vec2(0.001)), vec2(-1.0), vec2(1.0));

        float sampleEscape = max(boxSampleEscape, 0.0);
        float dist = sdfHexagon(pixel - center, r * 0.866025);
        float bevel = max(r * 0.45, 0.001);
        float normDist = clamp(1.0 + dist / bevel, 0.0, 1.0);
        float roundedBox = clamp(pow(normDist, 1.5), 0.0, 1.0);
        liquidShape(roundedBox, surface, shadowGradient);

        float lensStrength = clamp(boxRefraction, 0.0, 2.5);
        vec2 escapeUv = sampleEscape / max(rect.zw, vec2(0.001));
        vec2 lensLocal = sampleEscape > 0.0
            ? localUv - escapeUv * roundedBox * (lensStrength * 0.85 + 0.15) * normalizedLocal
            : (localUv - 0.5) * (1.0 - roundedBox * min(lensStrength, 0.95)) + 0.5;

        samplePixel = rect.xy + lensLocal * rect.zw;
    } else if (selIsCircle) {
        vec4 rect = Boxes[selectedBox];
        vec2 halfSize = rect.zw * 0.5;
        vec2 center = rect.xy + halfSize;
        localUv = clamp((pixel - rect.xy) / max(rect.zw, vec2(0.001)), vec2(0.0), vec2(1.0));
        vec2 toCenter = center - pixel;
        float dCenter = length(toCenter);
        float rMax = halfSize.x;
        vec2 inDir = dCenter > 0.001 ? (toCenter / dCenter) : vec2(0.0);
        float normDist = clamp(dCenter / max(rMax, 0.001), 0.0, 1.0);
        float roundedBox = clamp(pow(normDist, 3.0), 0.0, 1.0);
        liquidShape(roundedBox, surface, shadowGradient);
        float lensStrength = clamp(boxRefraction, 0.0, 2.5);
        float sampleEscape = max(boxSampleEscape, 0.0);
        float escapeAmount = sampleEscape > 0.0 ? sampleEscape : rMax * 0.6;
        samplePixel = pixel + inDir * (escapeAmount * roundedBox * (lensStrength * 0.85 + 0.15));
        normalizedLocal = -inDir;
    } else {
        vec4 rect = Boxes[selectedBox];
        vec2 halfSize = rect.zw * 0.5;
        vec2 center = rect.xy + halfSize;
        float radius = selIsPill ? min(halfSize.x, halfSize.y) : applyRounding(BoxData[selectedBox].x, roundingRule);
        float boxSmoothing = clamp(BoxData[selectedBox].y, 0.0, 1.0);
        bool boxSquircle = !selIsPill && selShapeMode > 0.5 && selShapeMode < 1.5;
        localUv = clamp((pixel - rect.xy) / max(rect.zw, vec2(0.001)), vec2(0.0), vec2(1.0));
        normalizedLocal = clamp((pixel - center) / max(halfSize, vec2(0.001)), vec2(-1.0), vec2(1.0));

        float sampleEscape = max(boxSampleEscape, 0.0);
        float roundedBox;

        if (sampleEscape > 0.0) {
            float dist = boxSquircle
                ? squircleBoxDistance(pixel - center, halfSize, radius, boxSmoothing)
                : roundedBoxDistance(pixel - center, halfSize, radius);
            float bevel = max(min(radius, min(halfSize.x, halfSize.y)), 0.001);
            float normDist = clamp(1.0 + dist / bevel, 0.0, 1.0);
            roundedBox = clamp(pow(normDist, 1.5), 0.0, 1.0);
        } else {
            vec2 normUv = localUv * 2.0 - 1.0;
            roundedBox = clamp(pow(abs(normUv.x), 8.0) + pow(abs(normUv.y), 8.0), 0.0, 1.0);
        }

        liquidShape(roundedBox, surface, shadowGradient);

        float lensStrength = clamp(boxRefraction, 0.0, 2.5);
        vec2 escapeUv = sampleEscape / max(rect.zw, vec2(0.001));
        vec2 lensLocal = sampleEscape > 0.0
            ? localUv - escapeUv * roundedBox * (lensStrength * 0.85 + 0.15) * normalizedLocal
            : (localUv - 0.5) * (1.0 - roundedBox * min(lensStrength, 0.95)) + 0.5;

        samplePixel = rect.xy + lensLocal * rect.zw;
    }

    vec2 screenMin = vec2(0.5);
    vec2 screenMax = OutputSize.xy - vec2(0.5);
    samplePixel = clamp(samplePixel, screenMin, screenMax);

    float refSpan = selIsSector ? (Boxes[selectedBox].w - Boxes[selectedBox].z) : min(Boxes[selectedBox].z, Boxes[selectedBox].w);
    vec2 chromaticOffset = normalizedLocal * refSpan * clamp(Fresnel.w, 0.0, 1.0) * 0.035;

    vec2 redPixel = clamp(samplePixel + chromaticOffset, screenMin, screenMax);
    vec2 bluePixel = clamp(samplePixel - chromaticOffset, screenMin, screenMax);
    vec2 redUv = vec2(redPixel.x * OutputSize.z, 1.0 - redPixel.y * OutputSize.w);
    vec2 greenUv = vec2(samplePixel.x * OutputSize.z, 1.0 - samplePixel.y * OutputSize.w);
    vec2 blueUv = vec2(bluePixel.x * OutputSize.z, 1.0 - bluePixel.y * OutputSize.w);
    vec2 oneTexel = OutputSize.zw;

    vec4 redSample = sampleGlassBlur(boxBlurType, redUv, oneTexel, boxBlurStrength);
    vec4 greenSample = sampleGlassBlur(boxBlurType, greenUv, oneTexel, boxBlurStrength);
    vec4 blueSample = sampleGlassBlur(boxBlurType, blueUv, oneTexel, boxBlurStrength);

    vec4 blurred = vec4(
        redSample.r,
        greenSample.g,
        blueSample.b,
        greenSample.a
    );

    vec2 lightingLocal = vec2(localUv.x - 0.5, 0.5 - localUv.y);
    float gradient = clamp((clamp(lightingLocal.y, 0.0, 0.2) + 0.1) * 0.5, 0.0, 1.0)
        + clamp((clamp(-lightingLocal.y, -1000.0, 0.2) * shadowGradient + 0.1) * 0.5, 0.0, 1.0);
    float luminance = dot(blurred.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 balancedColor = mix(vec3(luminance), blurred.rgb, clamp(Glass.x, 0.0, 1.5));

    // Apply dark tint to liquid glass based on BoxData.w
    float boxTint = clamp(BoxData[selectedBox].w, 0.0, 1.0);
    if (boxTint > 0.001) {
        balancedColor = mix(balancedColor, vec3(0.0), min(boxTint, 0.95));
    }

    // Diffuse ambient lighting across the 3D surface
    float glassDiffuse = selIsSector ? (0.92 + gradient * 0.35) : 1.0;
    vec3 litColor = balancedColor * glassDiffuse;

    // Specular highlight: 3D control thumbs and sectors get natural surface highlight from top-down light
    float highlight = clamp(surface * gradient, 0.0, 1.0) * clamp(Glass.w, 0.0, 1.0) * (selIsSector ? 0.50 : 1.0);
    vec3 lighting = mix(litColor, vec3(1.0), highlight);
    float glassAlpha = maxCoverage * clamp(Glass.y, 0.0, 1.0);
    fragColor = mix(original, vec4(lighting, original.a), glassAlpha);
}
