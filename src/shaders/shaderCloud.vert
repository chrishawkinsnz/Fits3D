#version 330 core

out float val;
out float shade;
layout(location = 0) in vec3 vertexPosition_modelspace;
layout(location = 1) in float value;

uniform mat4 mvp;

uniform float selectionMinX;
uniform float selectionMaxX;

uniform float selectionMinY;
uniform float selectionMaxY;

uniform float selectionMinZ;
uniform float selectionMaxZ;

uniform int isSelecting;

void main() {
	vec4 v = vec4(vertexPosition_modelspace, 1);
    gl_Position = mvp * v;
    val =  value;

    float lowLight = 0.25f;
    shade = 1.0f;
    if (isSelecting == 1) {
        if (v.x < selectionMinX) {
            shade = lowLight;
        }
        else if (v.x > selectionMaxX) {
            shade = lowLight;
        }
        else if (v.y < selectionMinY) {
            shade = lowLight;
        }
        else if (v.y > selectionMaxY) {
            shade = lowLight;
        }
        else if (v.z < selectionMinZ) {
            shade = lowLight;
        }
        else if (v.z > selectionMaxZ) {
            shade = lowLight;
        }
    }

}