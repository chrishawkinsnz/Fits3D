#version 330 core

layout(location = 0) in vec3 vertexPosition_modelspace;

uniform float f;

void main() {
    gl_Position.xyz = vec3(vertexPosition_modelspace.x + f, vertexPosition_modelspace.y, vertexPosition_modelspace.z) ;;
    
    gl_Position.w = 1.0;
}