#version 150

uniform sampler2D Sampler0;
layout(std140) uniform KawaseData {
    vec4 Params;
};

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    // Linear filtering folds a nine-tap Gaussian into five texture samples.
    // The first pass downsamples to half resolution, hence the 2x X radius.
    vec2 d = vec2((1.0 / Params.x) * max(1.0, Params.z) * 2.0, 0.0);
    vec3 sum = texture(Sampler0, TexCoord).rgb * 0.22702703;
    sum += texture(Sampler0, TexCoord + d * 1.38461538).rgb * 0.31621622;
    sum += texture(Sampler0, TexCoord - d * 1.38461538).rgb * 0.31621622;
    sum += texture(Sampler0, TexCoord + d * 3.23076923).rgb * 0.07027027;
    sum += texture(Sampler0, TexCoord - d * 3.23076923).rgb * 0.07027027;
    OutColor = vec4(sum, 1.0);
}
