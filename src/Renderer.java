import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL2.*;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3bc;
import com.jogamp.opengl.GL3.*;
import com.jogamp.opengl.math.Matrix4;

public class Renderer {
	
	//--CONSTANTS
	public final float orthoHeight = 1.0f;
	public final float orthoWidth = 1.0f;
	public final float orthoOrigX = 0.0f;
	public final float orthoOrigY = 0.0f;
	
	
	
	
	//--SETTINGS
	public boolean isOrthographic = true;
	public float alphaFudge = 0.02f;
	private int width;
	private int height;
	
	//--OPEN GL
	private GL3 gl;
	
	//--OPEN GL HANDLES
	private int shaderProgram;
	
	private int vertexBuffer;
	private int valueBuffer;
	
	private int uniformIdAlphaFudge;
	private int uniformIdMvp;
	private int uniformIdPointArea;
	
	
	
	
	//--MODEL STUFF
	private Viewer viewer;
	private PointCloud pointCloud;

	
	
	
	public Renderer(PointCloud pointCloud, Viewer viewer, GL3 gl){
		this.pointCloud = pointCloud;
		this.gl = gl;
		this.viewer = viewer;
		
		int[] ptr = new int[2];
		
	 	gl.glGenBuffers(2, ptr, 0);
	 	
    	this.vertexBuffer = ptr[0];
    	gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBuffer);
    	gl.glBufferData(GL_ARRAY_BUFFER, this.pointCloud.vertexBuffer.capacity() * 4, this.pointCloud.vertexBuffer, GL_STATIC_DRAW);
		
    	this.valueBuffer = ptr[1];
    	gl.glBindBuffer(GL_ARRAY_BUFFER, this.valueBuffer);
    	gl.glBufferData(GL_ARRAY_BUFFER, this.pointCloud.valueBuffer.capacity() * 4, this.pointCloud.valueBuffer, GL_STATIC_DRAW);
    	
		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "src/shaders/shader2.vert", "src/shaders/shader2.frag");
    	this.uniformIdMvp = gl.glGetUniformLocation(this.shaderProgram, "mvp");
		this.uniformIdAlphaFudge = gl.glGetUniformLocation(this.shaderProgram, "alphaFudge");
		this.uniformIdPointArea = gl.glGetUniformLocation(this.shaderProgram, "pointArea");
		
    	gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		gl.glBlendEquation(GL_FUNC_ADD);
		gl.glDisable(GL_CULL_FACE);
	}	
	

	
	
	public void display() {

		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(this.shaderProgram);
		gl.glUniform1f(this.uniformIdAlphaFudge, this.alphaFudge);
		
    	Matrix4 m = new Matrix4();
    	
    	if (this.isOrthographic) {
    		m.makeOrtho(orthoOrigX - orthoWidth, orthoOrigX + orthoWidth, orthoOrigY - orthoHeight,orthoOrigY + orthoHeight, -6f, 6f);
    		float scale = 1.0f / this.viewer.getRadius();
    		m.scale(scale, scale, scale);
    		
    		float pointRadius = this.calculatePointRadiusInPixels() * scale;
    		float ptArea = 0.5f * pointRadius * pointRadius * (float)Math.PI;
    		gl.glPointSize(Math.max(pointRadius,1f));
			gl.glUniform1f(this.uniformIdPointArea, ptArea);
    	}	
    	else {
    	  	m.makePerspective(3.14159f/2f, 4f/3f, 0.1f, 100f);
    	  	m.translate(0f, 0f, -this.viewer.getRadius());
			gl.glPointSize(1.0f);
    	}
    	
    	m.rotate(this.viewer.getySpin(), 1f, 0f, 0f);
    	m.rotate(this.viewer.getxSpin(), 0f, 1f, 0f);
    	
    	//--pass that matrix to the shader
    	gl.glUniformMatrix4fv(this.uniformIdMvp, 1, false, m.getMatrix(), 0);

    	gl.glEnableVertexAttribArray(0);
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
    	gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    	
    	gl.glEnableVertexAttribArray(1);
    	gl.glBindBuffer(GL_ARRAY_BUFFER, valueBuffer);
    	gl.glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0);
    	
    	gl.glDrawArrays(GL_POINTS, 0, this.pointCloud.validPts);
    	
    	gl.glEnableVertexAttribArray(0);
    	gl.glDisableVertexAttribArray(1);
    	
		gl.glFlush();
	}

	
	public void informOfResolution(int width, int height) {
		this.width = width;
		this.height = height;
	}

	
	private float calculatePointRadiusInPixels() {
		float pointWidth = (float)this.width* this.orthoWidth / (float)this.pointCloud.width; 
		float pointHeight = (float)this.height* this.orthoHeight / (float)this.pointCloud.height;
		return pointWidth < pointHeight ? pointWidth : pointHeight;
	}
}
