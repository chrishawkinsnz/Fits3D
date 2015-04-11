#version 330 core

in float val;

out vec4 color;
  
 uniform float alphaFudge;
  
  
void main(){
    color = vec4(1, val, 0, val * alphaFudge);
}