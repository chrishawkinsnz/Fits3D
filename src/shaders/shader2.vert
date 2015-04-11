#version 330 core

out float alpha;

layout(location = 0) in vec3 vertexPosition_modelspace;
layout(location = 1) in float value;

uniform float alphaFudge;
uniform mat4 mvp;

void main() {
	vec4 v = vec4(vertexPosition_modelspace, 1);
    gl_Position = mvp * v;
    alpha =  value * alphaFudge;
}