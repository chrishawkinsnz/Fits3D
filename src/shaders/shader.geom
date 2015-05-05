#version 330 core

out float val;

layout (points) in;
layout (points, max_vertices = 2) out;

void main() {    

    gl_Position = gl_in[0].gl_Position;
    float cutoff = 0.9f;
    //if (gl_Position.x <cutoff && gl_Position.x >-cutoff && gl_Position.y <cutoff && gl_Position.y>-cutoff ) {
    	EmitVertex();	
    //} 
	
    EndPrimitive();
	
} 