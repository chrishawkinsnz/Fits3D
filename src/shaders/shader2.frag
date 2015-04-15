#version 330 core

in float val;

out vec4 color;
  
uniform float alphaFudge;
uniform float pointArea;  
  
void main(){
    color = vec4(1, val, 1, val * alphaFudge * min(pointArea, 1.0));
}