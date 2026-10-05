#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 FragCoord;
in vec4 FragColor;
flat in vec4 FragShapeData0;
flat in vec4 FragShapeData1;

out vec4 OutColor;

float shapeDistance(vec2 p, vec2 halfSize, vec4 radii, float exponent) {
    radii.xy = p.x > 0.0 ? radii.xy : radii.zw;
    radii.x = p.y > 0.0 ? radii.x : radii.y;
    vec2 q = abs(p) - halfSize + radii.x;
    vec2 outside = max(q, 0.0);
    float lengthValue = abs(exponent - 2.0) < 0.001
        ? length(outside)
        : pow(pow(outside.x, exponent) + pow(outside.y, exponent), 1.0 / exponent);
    return min(max(q.x, q.y), 0.0) + lengthValue - radii.x;
}

void main() {
    vec2 size = FragShapeData0.xy;
    float smoothness = FragShapeData0.z;
    float exponent = FragShapeData0.w;
    vec4 radii = FragShapeData1;
    vec4 finalColor = FragColor;
    if (any(greaterThan(radii, vec4(0.0)))) {
        vec2 center = size * 0.5;
        float distance = shapeDistance(center - FragCoord * size, center - 1.0, radii, exponent);
        finalColor.a *= 1.0 - smoothstep(1.0 - smoothness, 1.0, distance);
    }
    if (finalColor.a <= 0.0) {
        discard;
    }
    OutColor = finalColor * ColorModulator;
}
