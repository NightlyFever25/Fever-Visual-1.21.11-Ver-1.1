#version 150

uniform sampler2D AfterSampler;
uniform sampler2D BeforeSampler;
uniform sampler2D PrevTrailSampler;
uniform sampler2D DepthSampler;
uniform sampler2D BeforeDepthSampler;

layout(std140) uniform HandShaderData {
    vec4 resolution;
    vec4 tintColor;
    vec4 settings;
    vec4 flameData;
};

in vec2 texCoord;
out vec4 fragColor;

const vec2 dirs[4] = vec2[](
    vec2(1.0, 0.0), vec2(0.0, 1.0),
    vec2(-1.0, 0.0), vec2(0.0, -1.0)
);

float hash12(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
               mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm3(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += noise(p) * amplitude;
        p = p * 2.03 + vec2(17.13, 9.27);
        amplitude *= 0.5;
    }
    return value;
}

float fbm2(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 2; i++) {
        value += noise(p) * amplitude;
        p = p * 2.03 + vec2(17.13, 9.27);
        amplitude *= 0.5;
    }
    return value;
}

float rawHandMaskColor(vec2 uv, out vec3 outColor) {
    vec4 beforeColor = texture(BeforeSampler, uv);
    vec4 afterColor = texture(AfterSampler, uv);
    outColor = afterColor.rgb;
    vec3 delta = abs(afterColor.rgb - beforeColor.rgb);
    float peak = max(max(delta.r, delta.g), delta.b);
    float luma = dot(delta, vec3(0.299, 0.587, 0.114));
    float value = peak * 0.78 + luma * 0.88 + abs(afterColor.a - beforeColor.a);
    float colorMask = smoothstep(0.004, 0.060, value);
    float depth = texture(DepthSampler, uv).r;
    float beforeDepth = texture(BeforeDepthSampler, uv).r;
    float depthMask = step(depth, beforeDepth - 0.00002) * (1.0 - step(0.9999, depth));
    return max(colorMask, depthMask);
}

float frameDt() {
    return clamp(flameData.w, 0.0005, 0.05);
}

float hintFade() {
    return exp(-28.68 * frameDt());
}

float previousTrailHint(vec2 uv, vec2 historyUv, vec2 px, float wobble) {
    float probe = px.x * (6.0 + wobble * 6.0);
    float alpha = texture(PrevTrailSampler, uv).a;
    alpha = max(alpha, texture(PrevTrailSampler, historyUv).a);
    alpha = max(alpha, texture(PrevTrailSampler, clamp(historyUv + vec2(probe, 0.0), 0.0, 1.0)).a);
    alpha = max(alpha, texture(PrevTrailSampler, clamp(historyUv - vec2(probe, 0.0), 0.0, 1.0)).a);
    return alpha;
}

vec4 previousTrailColor(vec2 historyUv, vec2 softPx) {
    vec4 previous = texture(PrevTrailSampler, historyUv) * 0.34;
    previous += texture(PrevTrailSampler, clamp(historyUv + vec2(softPx.x, 0.0), 0.0, 1.0)) * 0.105;
    previous += texture(PrevTrailSampler, clamp(historyUv - vec2(softPx.x, 0.0), 0.0, 1.0)) * 0.105;
    previous += texture(PrevTrailSampler, clamp(historyUv + vec2(0.0, softPx.y), 0.0, 1.0)) * 0.105;
    previous += texture(PrevTrailSampler, clamp(historyUv - vec2(0.0, softPx.y), 0.0, 1.0)) * 0.105;
    previous += texture(PrevTrailSampler, clamp(historyUv + softPx, 0.0, 1.0)) * 0.045;
    previous += texture(PrevTrailSampler, clamp(historyUv - softPx, 0.0, 1.0)) * 0.045;
    previous += texture(PrevTrailSampler, clamp(historyUv + vec2(softPx.x, -softPx.y), 0.0, 1.0)) * 0.045;
    previous += texture(PrevTrailSampler, clamp(historyUv + vec2(-softPx.x, softPx.y), 0.0, 1.0)) * 0.045;
    return previous;
}

void computeFlameField(vec2 uv, vec2 px, float radiusPx, float previousAlpha, vec3 previousColor,
                       float centerMask, vec3 centerColor,
                       out float envelope, out float currentMask, out vec3 itemColor) {
    float maskSum = centerMask * 1.70;
    float maskWeight = 1.70;
    float saturation = max(max(centerColor.r, centerColor.g), centerColor.b) - min(min(centerColor.r, centerColor.g), centerColor.b);
    float luminance = dot(centerColor, vec3(0.299, 0.587, 0.114));
    float centerWeight = centerMask * (0.3 + saturation * 1.5 + luminance * 0.3) * 1.70;
    vec3 colorSum = centerColor * centerWeight;
    float colorWeight = centerWeight;

    for (int i = 0; i < 4; i++) {
        vec2 nearUv = clamp(uv + dirs[i] * radiusPx * 0.45 * px, 0.0, 1.0);
        vec3 nearColor;
        float nearMask = rawHandMaskColor(nearUv, nearColor);
        maskSum += nearMask * 0.70;
        maskWeight += 0.70;
        float nearSat = max(max(nearColor.r, nearColor.g), nearColor.b) - min(min(nearColor.r, nearColor.g), nearColor.b);
        float nearLum = dot(nearColor, vec3(0.299, 0.587, 0.114));
        float nearWeight = nearMask * 0.70 * (0.3 + nearSat * 1.5 + nearLum * 0.3);
        colorSum += nearColor * nearWeight;
        colorWeight += nearWeight;

        vec2 farUv = clamp(uv + dirs[i] * radiusPx * px, 0.0, 1.0);
        vec3 farColor;
        float farMask = rawHandMaskColor(farUv, farColor);
        maskSum += farMask * 0.30;
        maskWeight += 0.30;
        float farSat = max(max(farColor.r, farColor.g), farColor.b) - min(min(farColor.r, farColor.g), farColor.b);
        float farLum = dot(farColor, vec3(0.299, 0.587, 0.114));
        float farWeight = farMask * 0.30 * (0.3 + farSat * 1.5 + farLum * 0.3);
        colorSum += farColor * farWeight;
        colorWeight += farWeight;
    }

    currentMask = smoothstep(0.020, 0.68, maskSum / max(maskWeight, 0.001));
    envelope = max(currentMask, previousAlpha * hintFade());
    itemColor = colorWeight > 0.001 ? colorSum / colorWeight : (previousAlpha > 0.01 ? previousColor : tintColor.rgb);
}

void main() {
    float strength = settings.z;
    float riseSpeed = settings.w;
    float wobble = flameData.x;
    float flameLength = flameData.y;
    float brightness = flameData.z;
    float time = settings.y;
    vec2 px = 1.0 / max(resolution.xy, vec2(1.0));
    float lengthCurve = clamp(flameLength / 2.5, 0.0, 1.0);
    float radiusPx = mix(22.0, 62.0, lengthCurve);
    float rise = 0.05 + smoothstep(0.0, 2.0, riseSpeed) * 0.80;
    float stepScale = clamp(frameDt() * 60.0, 0.25, 3.0);

    vec3 quickColor;
    float quickMask = rawHandMaskColor(texCoord, quickColor);
    if (quickMask < 0.01) {
        vec2 quickHistory = clamp(texCoord + vec2(0.0, -px.y * rise * stepScale), 0.0, 1.0);
        if (previousTrailHint(texCoord, quickHistory, px, wobble) < 0.0015) {
            fragColor = vec4(0.0);
            return;
        }
    }

    float curl = noise(texCoord * vec2(8.0, 6.0) + vec2(time * 0.20, time * 0.11)) - 0.5;
    vec2 drift = vec2(
        sin(texCoord.y * 18.0 + time * 2.35) * px.x * wobble * 2.6 + curl * px.x * (2.0 + wobble * 3.2),
        -px.y * rise
    ) * stepScale;
    vec2 historyUv = clamp(texCoord + drift, 0.0, 1.0);
    vec2 softPx = px * (2.1 + wobble * 0.65);
    vec4 previous = previousTrailColor(historyUv, softPx);

    float envelope;
    float currentMask;
    vec3 itemColor;
    if (quickMask < 0.01) {
        currentMask = 0.0;
        envelope = previous.a * hintFade();
        itemColor = previous.a > 0.01 ? previous.rgb : tintColor.rgb;
    } else {
        computeFlameField(texCoord, px, radiusPx, previous.a, previous.rgb, quickMask, quickColor,
                          envelope, currentMask, itemColor);
    }

    float flow = time * (0.08 + riseSpeed * 0.50);
    float lateralWobble = sin(texCoord.y * 28.0 + time * (1.5 + wobble * 2.5)) * wobble * 0.22;
    vec2 flameUv = vec2(texCoord.x + lateralWobble, texCoord.y - flow);
    float n1 = fbm3(vec2(flameUv.x * 4.4, flameUv.y * (5.2 + flameLength * 2.7)));
    float n2 = fbm2(vec2(flameUv.x * 8.2 + 3.7, flameUv.y * 9.5 - time * 0.35));
    float fireNoise = n1 * 0.72 + n2 * 0.28;
    float solidZone = smoothstep(0.15, 0.55, envelope);
    float flameShape = smoothstep(0.18, 0.78, mix(fireNoise, 1.0, solidZone * 0.78));
    float source = clamp(currentMask * mix(0.64, 1.35, flameShape), 0.0, 1.0);
    float strengthCurve = 1.0 - exp(-clamp(strength, 0.0, 2.0) * 0.95);
    float brightnessCurve = 0.45 + (1.0 - exp(-clamp(brightness, 0.0, 2.0) * 0.85)) * 0.70;
    source = clamp(source * strengthCurve * brightnessCurve * 1.22 * tintColor.a, 0.0, 0.92);

    vec3 baseColor = tintColor.rgb;
    float baseLuma = dot(baseColor, vec3(0.299, 0.587, 0.114));
    if (baseLuma > 0.45) baseColor *= 0.45 / baseLuma;
    float distanceFromItem = 1.0 - clamp(envelope * 1.5, 0.0, 1.0);
    vec3 coreColor = min(baseColor * 1.4 + vec3(0.06), vec3(1.0));
    vec3 fireColor = mix(coreColor, baseColor * 0.85, smoothstep(0.0, 0.35, distanceFromItem));
    fireColor = mix(fireColor, baseColor * 0.35, smoothstep(0.25, 0.9, distanceFromItem));
    fireColor *= 0.92 + 0.08 * sin(time * 7.3 + texCoord.x * 20.0 + texCoord.y * 15.0);

    float dt = frameDt();
    float historyFade = exp(dt * mix(-4.353, -3.078, lengthCurve));
    float blendBase = clamp(source * 0.56 + 0.10, 0.08, 0.66);
    float blendFactor = 1.0 - exp(60.0 * log(1.0 - blendBase) * dt);
    float alpha = clamp(mix(previous.a * historyFade, source, blendFactor), 0.0, 0.95);
    alpha = max(alpha, previous.a * historyFade);
    float colorBlend = blendFactor * smoothstep(0.015, 0.22, currentMask);
    vec3 color = alpha > 0.001 ? mix(previous.rgb, fireColor, colorBlend) : vec3(0.0);
    fragColor = vec4(color, alpha);
}
