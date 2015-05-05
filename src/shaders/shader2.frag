#version 330 core

in float val;

out vec4 color;
  
uniform float alphaFudge;
uniform float pointArea;  
uniform vec4 pointColor;
  
void main(){
    color = vec4(pointColor[0], pointColor[1], pointColor[2], val * alphaFudge * min(pointArea, 1.0));
}