#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 FragColor;

layout(std140) uniform FeverUniforms {
    float Time;
    vec2 ScreenSize;
    vec4 BaseColor;
    vec4 Params;
};

out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

void main() {
    float speed = Params.x;
    float scale = Params.y;
    float intensity = Params.z;
    float alpha = Params.w;
    vec2 uv = gl_FragCoord.xy / max(ScreenSize, vec2(1.0));
    vec2 p = uv * scale;

    float t = Time * speed;
    float wave = sin((p.x + noise(p + t * 0.12)) * 8.0 + t) * 0.5 + 0.5;
    float caustic = smoothstep(0.54 - intensity * 4.0, 0.82, wave * noise(p * 1.7 - t * 0.08));
    vec3 color = mix(BaseColor.rgb * 0.28, BaseColor.rgb, caustic);
    OutColor = vec4(color, alpha * (0.20 + caustic * 0.55)) * ColorModulator;
}
