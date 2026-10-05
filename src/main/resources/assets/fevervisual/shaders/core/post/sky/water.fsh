#version 150

layout(std140) uniform SkyData {
    vec4 uResolutionTime;
    vec4 uColorAlpha;
    vec4 uParams;
    vec4 uCameraDirData;
};

out vec4 fragColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
}

float wave(vec2 p, float t) {
    float a = sin(p.x * 2.10 + t * 0.55);
    float b = sin(p.y * 2.65 - t * 0.42);
    float c = sin((p.x + p.y) * 1.45 + t * 0.28);
    float d = sin(length(p) * 3.20 - t * 0.35);
    return (a + b + c + d) * 0.25;
}

void main() {
    vec2 uResolution = uResolutionTime.xy;
    float uTime = uResolutionTime.z;
    float uFov = uResolutionTime.w;
    vec3 uColor = uColorAlpha.rgb;
    float uAlpha = uColorAlpha.a;
    float uSpeed = uParams.x;
    float uScale = uParams.y;
    float uIntensity = uParams.z;
    vec2 uCameraDir = uCameraDirData.xy;

    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;

    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    vec3 rayW = rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV;
    vec2 p = rayW.xz / max(0.15, abs(rayW.y) + 0.35);
    p *= uScale * 0.55;

    float t = uTime * uSpeed;
    float w1 = wave(p, t);
    float w2 = wave(p * 1.85 + vec2(2.7, -1.4), t * 0.72);
    float foam = smoothstep(0.55, 0.96, abs(w1 * 0.70 + w2 * 0.45));
    float depth = smoothstep(-0.2, 1.0, rayW.y * 0.5 + 0.5);
    float brightness = 0.18 + (w1 * 0.5 + 0.5) * 0.20 + foam * uIntensity * 16.0;
    vec3 finalColor = uColor * brightness + mix(uColor * 0.04, uColor * 0.18, depth);

    fragColor = vec4(finalColor, uAlpha);
}
