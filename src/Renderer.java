import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
		this.pointCloud = pointCloud;
		this.gl = gl;
		this.viewer = viewer;
		
		int nSlices = 0;
		for (CloudRegion cr : this.pointCloud.getRegions())
			nSlices += cr.getSlices().size();
		
		this.vertexBufferHandles = new int[nSlices];
		this.valueBufferHandles = new int[nSlices];
		int index = 0;
		int[] ptr = new int[2];
		
		for (CloudRegion cr : this.pointCloud.getRegions()) {
			for (VertexBufferSlice vbs : cr.getSlices()) {
			 	gl.glGenBuffers(2, ptr, 0);
			 	this.vertexBufferHandles[index] = ptr[0];
			 	vbs.index = index;
			 	
		    	FloatBuffer vertBuffer = vbs.vertexBuffer;
		    	gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBufferHandles[index]);
		    	gl.glBufferData(GL_ARRAY_BUFFER, vertBuffer.capacity() * 4, vertBuffer, GL_STATIC_DRAW);
				
		    	FloatBuffer valueBuffer = vbs.valueBuffer;
		    	this.valueBufferHandles[index] = ptr[1];
		    	gl.glBindBuffer(GL_ARRAY_BUFFER, this.valueBufferHandles[index]);
		    	gl.glBufferData(GL_ARRAY_BUFFER, valueBuffer.capacity() * 4, valueBuffer, GL_STATIC_DRAW);
		    	index++;
			}
		}
		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "src/shaders/shader2.vert", "src/shaders/shader2.frag");
    	this.uniformMvpHandle = gl.glGetUniformLocation(this.shaderProgram, "mvp");
		this.uniformAlphaFudgeHandle = gl.glGetUniformLocation(this.shaderProgram, "alphaFudge");
		this.uniformPointAreaHandle = gl.glGetUniformLocation(this.shaderProgram, "pointArea");
		this.uniformColorHandle = gl.glGetUniformLocation(this.shaderProgram, "pointColor");
		
    	gl.glEnable(GL_BLEND);
    	
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		gl.glBlendEquation(GL_FUNC_ADD);
		gl.glDisable(GL_CULL_FACE);
	}	
	
	public void display() {
		
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(this.shaderProgram);
		gl.glUniform1f(this.uniformAlphaFudgeHandle, this.alphaFudge);
		
		//--figure out if looking back to front
		float pi = (float)Math.PI;
		float spin = Math.abs(this.viewer.getxSpin() % (2f * pi));
		boolean flippityFlop = spin > pi/2f && spin < 3f*pi/2f ;
//		boolean flippityFlop = spin > 0f && spin < pi ;
		if (this.isTrippy == true) {
			flippityFlop = true;
		}
		System.out.println("num slices ever:" + this.pointCloud.getRegions().size());

		List<VertexBufferSlice> allSlicesLikeEver = new ArrayList<VertexBufferSlice>();
		for (CloudRegion cr: this.pointCloud.getRegions()) {
			for (VertexBufferSlice slice: cr.getSlices()) {
				slice.scratchDepth = cr.volume.z + cr.volume.dp * slice.depthValue;
				slice.region = cr;
			}
			allSlicesLikeEver.addAll(cr.getSlices());

		}

		
		class RegionOrderer implements Comparator<VertexBufferSlice> {
			public int compare(VertexBufferSlice a, VertexBufferSlice b) {
				return a.scratchDepth < b.scratchDepth ? -1 : 1;
			}
		}
		Collections.sort(allSlicesLikeEver, new RegionOrderer());
		for (VertexBufferSlice vbs : allSlicesLikeEver) {
			System.out.println(vbs.scratchDepth);
		}

			
		for (int i = 0; i < allSlicesLikeEver.size(); i++){
			
			//-if Z is now pointing out of the screen take slices from the back of the list forward
			int sliceIndex = i;
			if (flippityFlop) {
				sliceIndex = allSlicesLikeEver.size() - 1 - i;
			}
			
			Color col = Color.orange;
			if (i > 100) {
				col = Color.green;
			}
			
			VertexBufferSlice slice = allSlicesLikeEver.get(sliceIndex);
			CloudRegion cr = slice.region;
			
			gl.glUniform4f(this.uniformColorHandle, col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
	    	Matrix4 m = new Matrix4();
	    	

    		m.makeOrtho(orthoOrigX - orthoWidth, orthoOrigX + orthoWidth, orthoOrigY - orthoHeight,orthoOrigY + orthoHeight, -6f, 6f);
    		
    		Volume v = cr.volume;
    		Volume vpc = this.pointCloud.volume;
    		float baseScale = 1.0f / this.viewer.getRadius();
    		
    		float pointRadius = this.calculatePointRadiusInPixelsForRegionIndex(0) * baseScale;
    		float ptArea = 0.5f * pointRadius * pointRadius * (float)Math.PI;
    		gl.glPointSize(Math.max(pointRadius,1f));
			gl.glUniform1f(this.uniformPointAreaHandle, ptArea);

	    	m.rotate(this.viewer.getySpin(), 1f, 0f, 0f);
	    	m.rotate(this.viewer.getxSpin(), 0f, 1f, 0f);
	    	m.scale(baseScale, baseScale, baseScale);
	    	
	    	m.translate(v.x, v.y, v.z + slice.depthValue);
	    	m.scale(v.wd,v.ht,v.dp);
	    	
	    	m.translate(vpc.x, vpc.y, vpc.z);
	    	m.scale(vpc.wd, vpc.ht, vpc.dp);
	    	
	    	//--pass that matrix to the shader
	    	gl.glUniformMatrix4fv(this.uniformMvpHandle, 1, false, m.getMatrix(), 0);
	
	    	gl.glEnableVertexAttribArray(0);
	    	gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferHandles[slice.index]);
	    	gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
	    	
	    	gl.glEnableVertexAttribArray(1);
	    	gl.glBindBuffer(GL_ARRAY_BUFFER, valueBufferHandles[slice.index]);
	    	gl.glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0);
	    	
	    	gl.glDrawArrays(GL_POINTS, 0, slice.numberOfPts);
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
		CloudRegion cr = this.pointCloud.getRegions().get(i);
//		float pointWidth = (float)this.width* this.orthoWidth*cr.volume.wd/ (float)cr.ptWidth(); 
		float pointHeight = (float)this.height* this.orthoHeight*cr.volume.ht / (float)cr.ptHeight();
//		float sz =  pointWidth < pointHeight ? pointWidth : pointHeight;
		float sz =  pointHeight;
//		return sz;
		return 5f;
	}
}
