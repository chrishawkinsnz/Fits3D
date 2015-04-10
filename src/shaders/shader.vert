varying vec4 color;

uniform float alphaFudge;

void main() {
    color = vec4(1, 1, 1, gl_Color.a * alphaFudge );
    gl_Position = ftransform();
}