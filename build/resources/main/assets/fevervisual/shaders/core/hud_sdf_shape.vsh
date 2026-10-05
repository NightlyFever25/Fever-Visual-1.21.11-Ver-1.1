#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec4 ShapeData0;
in vec4 ShapeData1;

out vec2 FragCoord;
out vec4 FragColor;
flat out vec4 FragShapeData0;
flat out vec4 FragShapeData1;

void main() {
    FragCoord = UV0;
    FragColor = Color;
    FragShapeData0 = ShapeData0;
    FragShapeData1 = ShapeData1;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
