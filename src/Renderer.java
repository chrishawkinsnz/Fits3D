import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Comparator;

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
	
	private int uniformAlphaFudgeHandle;
	private int uniformMvpHandle;
	private int uniformPointAreaHandle;
	private int uniformColorHandle;
	
	private int[] vertexBufferHandles;
	private int[] valueBufferHandles;
	
	
	//--MODEL STUFF
	private WorldViewer viewer;
	private PointCloud pointCloud;
	public boolean isTrippy;


	public Renderer(PointCloud pointCloud, WorldViewer viewer, GL3 gl){
		int nRegions = pointCloud.regions().size();
		this.pointCloud = pointCloud;
		this.gl = gl;
		this.viewer = viewer;
		
		this.vertexBufferHandles = new int[nRegions];
		this.valueBufferHandles = new int[nRegions];
		
		int[] ptr = new int[2 * pointCloud.regions().size()];
		
		for (int i = 0; i < nRegions; i++) {
		 	gl.glGenBuffers(2, ptr, 0);
		 	
//	    	this.vertexBufferHandle = ptr[0];
		 	this.vertexBufferHandles[i] = ptr[0];
	    	CloudRegion region = this.pointCloud.regions().get(0);
	    	FloatBuffer vertBuffer = region.vertexBuffer();
	    	gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBufferHandles[i]);
	    	gl.glBufferData(GL_ARRAY_BUFFER, vertBuffer.capacity() * 4, vertBuffer, GL_STATIC_DRAW);
	    	gl.glBufferData(GL_ARRAY_BUFFER, vertBuffer.capacity() * 4, vertBuffer, GL_STATIC_DRAW);
			
	    	FloatBuffer valueBuffer = region.valueBuffer();
	    	this.valueBufferHandles[i] = ptr[1];
	    	gl.glBindBuffer(GL_ARRAY_BUFFER, this.valueBufferHandles[i]);
	    	gl.glBufferData(GL_ARRAY_BUFFER, valueBuffer.capacity() * 4, valueBuffer, GL_STATIC_DRAW);
		}
		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "src/shaders/shader2.vert", "src/shaders/shader2.frag");
    	this.uniformMvpHandle = gl.glGetUniformLocation(this.shaderProgram, "mvp");
		this.uniformAlphaFudgeHandle = gl.glGetUniformLocation(this.shaderProgram, "alphaFudge");
		this.uniformPointAreaHandle = gl.glGetUniformLocation(this.shaderProgram, "pointArea");
		this.uniformColorHandle = gl.glGetUniformLocation(this.shaderProgram, "pointColor");
		
//    	gl.glEnable(GL_BLEND);
//		gl.glBlendFunc(GL_ONE, GL_ONE);
////		gl.glBlendEquation(GL_FUNC_ADD);
//		gl.glDisable(GL_CULL_FACE);
	}	
	
	public void display() {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(this.shaderProgram);
		gl.glUniform1f(this.uniformAlphaFudgeHandle, this.alphaFudge);
		
		//--figure out if looking back to front
		float pi = (float)Math.PI;
		float spin = Math.abs(this.viewer.getxSpin() % (2f * pi));
//		boolean flippityFlop = spin > pi/2f && spin < 3f*pi/2f ;
		boolean flippityFlop = spin > 0f && spin < pi ;
		if (this.isTrippy == true) {
			flippityFlop = true;
		}
		for (int i = 0; i < this.pointCloud.regions().size(); i++){
			int sliceIndex = i;
			if (flippityFlop) {
				sliceIndex = this.pointCloud.regions().size() - 1 - i;
			}
			CloudRegion cr = this.pointCloud.regions().get(sliceIndex);
			Color col = CloudRegion.cols [1 % CloudRegion.cols.length]; 
			gl.glUniform4f(this.uniformColorHandle, col.getRed()/255, col.getGreen()/255, col.getBlue()/255, col.getAlpha()/255);
	    	Matrix4 m = new Matrix4();
	    	

    		m.makeOrtho(orthoOrigX - orthoWidth, orthoOrigX + orthoWidth, orthoOrigY - orthoHeight,orthoOrigY + orthoHeight, -6f, 6f);
    		
    		Volume v = cr.volume;
    		Volume vpc = this.pointCloud.volume;
    		float baseScale = 1.0f / this.viewer.getRadius();
    		
    		float pointRadius = this.calculatePointRadiusInPixelsForRegionIndex(sliceIndex) * baseScale;
    		float ptArea = 0.5f * pointRadius * pointRadius * (float)Math.PI;
    		gl.glPointSize(Math.max(pointRadius,1f));
			gl.glUniform1f(this.uniformPointAreaHandle, ptArea);

	    	m.rotate(this.viewer.getySpin(), 1f, 0f, 0f);
	    	m.rotate(this.viewer.getxSpin(), 0f, 1f, 0f);
	    	m.scale(baseScale, baseScale, baseScale);
	    	
	    	m.translate(v.x, v.y, v.z);
	    	m.scale(v.wd,v.ht,v.dp);
	    	
	    	m.translate(vpc.x, vpc.y, vpc.z);
	    	m.scale(vpc.wd, vpc.ht, vpc.dp);
	    	
	    	//--pass that matrix to the shader
	    	gl.glUniformMatrix4fv(this.uniformMvpHandle, 1, false, m.getMatrix(), 0);
	
	    	gl.glEnableVertexAttribArray(0);
	    	gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferHandles[sliceIndex]);
	    	gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
	    	
	    	gl.glEnableVertexAttribArray(1);
	    	gl.glBindBuffer(GL_ARRAY_BUFFER, valueBufferHandles[sliceIndex]);
	    	gl.glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0);
	    	
	    	gl.glDrawArrays(GL_POINTS, 0, cr.numberOfPoints());
		}
    	gl.glEnableVertexAttribArray(0);
    	gl.glDisableVertexAttribArray(1);
    	
		gl.glFlush();
	}

	
	public void informOfResolution(int width, int height) {
		this.width = width;
		this.height = height;
	}

	
	private float calculatePointRadiusInPixelsForRegionIndex(int i) {
		CloudRegion cr = this.pointCloud.regions().get(i);
//		float pointWidth = (float)this.width* this.orthoWidth*cr.volume.wd/ (float)cr.ptWidth(); 
		float pointHeight = (float)this.height* this.orthoHeight*cr.volume.ht / (float)cr.ptHeight();
//		float sz =  pointWidth < pointHeight ? pointWidth : pointHeight;
		float sz =  pointHeight;
		return sz;
	}
}
