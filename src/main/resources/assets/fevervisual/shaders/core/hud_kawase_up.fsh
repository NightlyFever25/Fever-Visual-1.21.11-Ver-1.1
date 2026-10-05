#version 150

uniform sampler2D Sampler0;
layout(std140) uniform KawaseData {
    vec4 Params;
};

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    vec2 d = vec2(0.0, (1.0 / Params.y) * max(1.0, Params.z));
    vec3 sum = texture(Sampler0, TexCoord).rgb * 0.22702703;
    sum += texture(Sampler0, TexCoord + d * 1.38461538).rgb * 0.31621622;
    sum += texture(Sampler0, TexCoord - d * 1.38461538).rgb * 0.31621622;
    sum += texture(Sampler0, TexCoord + d * 3.23076923).rgb * 0.07027027;
    sum += texture(Sampler0, TexCoord - d * 3.23076923).rgb * 0.07027027;
    OutColor = vec4(sum, 1.0);
}
