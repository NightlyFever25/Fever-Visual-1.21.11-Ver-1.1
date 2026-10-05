#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FogCompatData {
    vec4 projectionParams;
    vec4 fogParams;
    vec4 fogColor;
};

float linearizeDepth(float depth) {
    float nearPlane = projectionParams.z;
    float farPlane = projectionParams.w;
    float z = depth * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    float viewDistance = depth >= 0.999999 ? projectionParams.w : linearizeDepth(depth);
    float fogStart = fogParams.x;
    float fogEnd = max(fogStart + 1.0, fogParams.y);
    float fogFactor = smoothstep(fogStart, fogEnd, viewDistance) * clamp(fogParams.z, 0.0, 1.0);
    fragColor = vec4(mix(scene.rgb, fogColor.rgb, fogFactor), scene.a);
}
