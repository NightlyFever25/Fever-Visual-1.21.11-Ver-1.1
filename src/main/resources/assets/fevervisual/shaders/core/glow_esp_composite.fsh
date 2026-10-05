#version 150

layout(std140) uniform GlowEspData {
    vec4 params;
    vec4 params2;
};

uniform sampler2D MaskSampler;
uniform sampler2D GlowSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 mask = texture(MaskSampler, texCoord);
    vec4 glow = texture(GlowSampler, texCoord);
    float maskAlpha = max(mask.a, max(mask.r, max(mask.g, mask.b)));
    float glowAlpha = max(glow.a, max(glow.r, max(glow.g, glow.b)));
    float outer = max(glowAlpha - maskAlpha * 0.82, 0.0);
    float inner = min(glowAlpha, maskAlpha);
    float selected = params.y < 0.5 ? outer : (params.y < 1.5 ? inner : max(outer, inner));
    float blurScale = 1.0 / max(params.z, 1.0);
    vec3 glowColor = glowAlpha > 0.0001 ? glow.rgb / glowAlpha : vec3(0.0);
    vec3 maskColor = maskAlpha > 0.0001 ? mask.rgb / maskAlpha : glowColor;
    vec3 color = glowColor * selected * params.x * (0.75 + blurScale * 2.0);
    float alpha = selected * params.x;
    if (params.w > 0.5) {
        color += maskColor * maskAlpha * params.x * 0.38;
        alpha = max(alpha, maskAlpha * 0.55);
    }
    if (params2.x > 0.5) {
        float edge = min(maskAlpha, max(outer * 2.2, glowAlpha - inner * 0.72));
        color += maskColor * edge * params.x * 0.75;
        alpha = max(alpha, edge * params.x);
    }
    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
