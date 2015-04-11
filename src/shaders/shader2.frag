#version 330 core

in float alpha;

out vec4 color;
  
void main(){
    color = vec4(1,alpha,0,alpha);
}