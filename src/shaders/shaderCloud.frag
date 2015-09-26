#version 330 core

in float chuckIt;
in float val;
in float alpha;
in float shade;

out vec4 color;


uniform vec4 pointColor;

uniform int isSelecting;

void main(){
	if (chuckIt > 0.0f) {
    	discard;
    }

    color = pointColor;
    color[3] = alpha;

    //TODO replace min(pointArea, 1.0) with code in renderer, no point in doing this for each fragment you dope.
}