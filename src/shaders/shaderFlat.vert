#version 330 core

out vec4 col;

layout(location = 0) in vec3 vertexPosition_modelspace;
layout(location = 1) in vec4 color;

uniform mat4 mvp;

void main() {
	vec4 v = vec4(vertexPosition_modelspace, 1);
    gl_Position = mvp * v;
    col =  color;
}