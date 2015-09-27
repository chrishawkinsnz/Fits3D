#version 330 core

out float chuckIt;
out float alpha;

layout(location = 0) in vec3 vertexPosition_modelspace;
layout(location = 1) in float value;

uniform mat4 mvp;

uniform float selectionMinX;
uniform float selectionMaxX;

uniform float selectionMinY;
uniform float selectionMaxY;

uniform float selectionMinZ;
uniform float selectionMaxZ;

uniform float filterMinX;
uniform float filterMaxX;

uniform float lowLight;

uniform int isSelecting;
uniform int watchOutForOverflow;

uniform float filterGradient;
uniform float filterConstant;
uniform float alphaFudge;

void main() {
	vec4 v = vec4(vertexPosition_modelspace, 1);

	//--this is a hack to account fo rthe fact that with chromatic abberation some values may exceed SHORT_MAX and overflow.
	//--the check say "look, if you are below -SHORT_MAX/2 you've clearly overflowed so we simply put you back in your rihgt place by adding the equivelant of SHORT_MAX.
	//--this is not a great solution, a better choice would to swap SHORT_MAX and SHORT_MAX/2 as the "max" value but this is a fairly risky change that touches a lot of the code base so was deemed too risky this late in the project.
	if (watchOutForOverflow == 1) {
	    if (v.x < -0.5) {
    	    v.x += 2.0;
    	}
    	if (v.y < -0.5) {
    	    v.y += 2.0;
    	}
	}

    gl_Position = mvp * v;

    if (value < filterMinX || value > filterMaxX) {
		chuckIt = 1.0f;

	}
		else {
	    chuckIt = 0.0f;
	}

    alpha = (value * filterGradient + filterConstant);
    //--only do the selecting if toggled on (don't waste GPU time here if we don't have to)
    if (isSelecting == 1) {
        if (v.x < selectionMinX) {
            alpha = alpha * lowLight;
        }
        else if (v.x > selectionMaxX) {
            alpha = alpha * lowLight;
        }
        else if (v.y < selectionMinY) {
            alpha = alpha * lowLight;
        }
        else if (v.y > selectionMaxY) {
            alpha = alpha * lowLight;
        }
        else if (v.z < selectionMinZ) {
            alpha = alpha * lowLight;
        }
        else if (v.z > selectionMaxZ) {
            alpha = alpha * lowLight;
        }
        else {
            alpha = alpha / alphaFudge;
        }
    }

}