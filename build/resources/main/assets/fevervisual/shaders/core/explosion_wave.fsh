#version 150

uniform sampler2D Scene;
uniform sampler2D DepthSampler;

layout(std140) uniform Waves {
    vec4 header;
    vec4 header2;
    mat4 invViewProj;
    vec4 data[24];
};

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;

vec3 worldFromDepth(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 world = invViewProj * clip;
    return world.xyz / world.w;
}

void main() {
    vec2 uv = texCoord;
    int count = int(header.x + 0.5);
    float aspect = header.y;
    float chroma = header.z;
    float globalFlash = header.w;
    float rimGlowStrength = header2.x;

    float sceneDepth = texture(DepthSampler, uv).r;
    bool hasScene = sceneDepth < 1.0;
    vec3 scenePos = worldFromDepth(uv, hasScene ? sceneDepth : 1.0);
    float sceneDist = hasScene ? length(scenePos) : 1e9;
    vec3 rayDir = normalize(scenePos);

    vec2 offset = vec2(0.0);
    float influence = 0.0;
    float rimFlash = 0.0;

    for (int i = 0; i < count; i++) {
        vec3 center = data[i * 3].xyz;
        float radius = data[i * 3].w;
        float thickness = max(data[i * 3 + 1].x, 1e-3);
        float amp = data[i * 3 + 1].y;
        float env = data[i * 3 + 1].z;
        float flash = data[i * 3 + 1].w;
        vec2 cuv = data[i * 3 + 2].xy;
        float valid = data[i * 3 + 2].z;
        float distCam = data[i * 3 + 2].w;

        float tc = dot(center, rayDir);
        float w = 2.0;
        vec2 dirScreen = vec2(0.0);
        if (tc > 0.0 && valid > 0.5) {
            float b = length(center - rayDir * tc);
            float chord = sqrt(max(radius * radius - b * b, 0.0));
            float tHit = tc - chord;
            if (hasScene && sceneDist + 0.5 < tHit) {
                continue;
            }
            w = (b - radius) / thickness;
            vec2 d = (uv - cuv) * vec2(aspect, 1.0);
            float dl = length(d);
            dirScreen = dl > 1e-5 ? d / dl : vec2(0.0);
        } else {
            w = (distCam - radius) / thickness;
            vec2 d = (uv - vec2(0.5)) * vec2(aspect, 1.0);
            float dl = length(d);
            dirScreen = dl > 1e-5 ? d / dl : vec2(0.0);
        }

        if (abs(w) >= 1.0) {
            continue;
        }

        float band = smoothstep(0.0, 1.0, 1.0 - abs(w)) * env;
        if (band <= 0.0) {
            continue;
        }

        float wave = sin(w * PI) * (1.0 - abs(w));
        vec2 sd = dirScreen;
        sd.x /= aspect;
        offset += sd * wave * amp;

        influence = influence + band - influence * band;
        rimFlash += max(flash, env * 0.75) * band;
    }

    vec3 col;
    if (influence > 0.001 && chroma > 0.0) {
        float spread = chroma * clamp(influence, 0.0, 1.0);
        col.r = texture(Scene, uv + offset * (1.0 + spread)).r;
        col.g = texture(Scene, uv + offset).g;
        col.b = texture(Scene, uv + offset * (1.0 - spread)).b;
    } else {
        col = texture(Scene, uv + offset).rgb;
    }

    if (rimFlash > 0.001) {
        col += vec3(1.0, 0.96, 0.88) * (rimFlash * rimGlowStrength);
        col = mix(col, vec3(0.72, 0.86, 1.0), clamp(rimFlash * 0.14, 0.0, 0.32));
    }

    if (globalFlash > 0.001) {
        col = mix(col, vec3(1.0, 0.98, 0.92), clamp(globalFlash, 0.0, 1.0));
    }

    fragColor = vec4(min(col, vec3(1.0)), 1.0);
}
