#version 150

in vec2 texCoord;

out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FogBlurData {
    vec4 projectionParams;
    vec4 fogParams;
};

float linearizeDepth(float depth) {
    float nearPlane = projectionParams.z;
    float farPlane = projectionParams.w;
    float z = depth * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

float luminance(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    vec4 blurred = texture(BlurSampler, texCoord);

    float depth = texture(DepthSampler, texCoord).r;
    float viewDistance = depth >= 0.999999 ? projectionParams.w : linearizeDepth(depth);
    float fogStart = fogParams.x;
    float fogEnd = max(fogStart + 1.0, fogParams.y);
    float strength = clamp(fogParams.z, 0.0, 1.0);
    float fogRange = max(1.0, fogEnd - fogStart);
    float farBlurStart = fogStart + fogRange * 0.60;

    float fogMask = smoothstep(farBlurStart, fogEnd, viewDistance);
    float blendValue = clamp(fogMask * strength, 0.0, 0.85);
    float sceneLuma = max(luminance(scene.rgb), 0.001);
    float blurLuma = max(luminance(blurred.rgb), 0.001);
    vec3 brightnessSafeBlur = blurred.rgb * clamp(sceneLuma / blurLuma, 0.55, 1.8);
    fragColor = vec4(mix(scene.rgb, brightnessSafeBlur, blendValue), scene.a);
}
