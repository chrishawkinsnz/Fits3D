import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
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

import com.jogamp.opengl.util.gl2.GLUT;

public class Renderer {
	private static long lastTime = 0;

	//--CONSTANTS
	public final float orthoHeight = 1.0f;
	public final float orthoWidth = 1.0f;
	public final float orthoOrigX = 0.0f;
	public final float orthoOrigY = 0.0f;

	//--SETTINGS
	public boolean isOrthographic = true;
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
	private List<PointCloud> pointClouds;
	public boolean isTrippy;
	private int uniformFilterMinX;
	private int uniformFilterMaxX;
	private int uniformFilterGradient;
	private int uniformFilterConstant;





	//--DEBUG FLAGS
	private boolean legendary = true;
	private	boolean gay = false;


	public Renderer(List<PointCloud> pointClouds, WorldViewer viewer, GL3 gl){
		setupWith(pointClouds, viewer, gl);
	}

	public void setupWith(List<PointCloud> pointClouds, WorldViewer viewer, GL3 gl){
		this.pointClouds = pointClouds;
		this.gl = gl;
		this.viewer = viewer;

		int nSlices = 0;
		for (PointCloud cloud : this.pointClouds)
			for (CloudRegion cr : cloud.getRegions())
				nSlices += cr.getSlices().size();

		this.vertexBufferHandles = new int[nSlices];
		this.valueBufferHandles = new int[nSlices];
		int index = 0;
		int[] ptr = new int[2];
		for (PointCloud cloud : this.pointClouds){
			for (CloudRegion cr : cloud.getRegions()) {
				for (VertexBufferSlice vbs : cr.getSlices()) {
					gl.glGenBuffers(2, ptr, 0);
					this.vertexBufferHandles[index] = ptr[0];
					vbs.index = index;

					ShortBuffer vertBuffer = vbs.vertexBuffer;
					gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBufferHandles[index]);
					gl.glBufferData(GL_ARRAY_BUFFER, vertBuffer.capacity() * 2, vertBuffer, GL_STATIC_DRAW);

					FloatBuffer valueBuffer = vbs.valueBuffer;
					this.valueBufferHandles[index] = ptr[1];
					gl.glBindBuffer(GL_ARRAY_BUFFER, this.valueBufferHandles[index]);
					gl.glBufferData(GL_ARRAY_BUFFER, valueBuffer.capacity() * 4, valueBuffer, GL_STATIC_DRAW);
					index++;
				}
			}
		}

//		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "/Users/chrishawkins/shaders/shader2.vert", "/Users/chrishawkins/shaders/shader2.frag");
		//this.shaderProgram = ShaderHelper.programWithShaders2(gl, "bin/shaders/shader2.vert", "bin/shaders/shader2.frag");
		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "shader2.vert", "shader2.frag");

		this.uniformMvpHandle = gl.glGetUniformLocation(this.shaderProgram, "mvp");
		this.uniformAlphaFudgeHandle = gl.glGetUniformLocation(this.shaderProgram, "alphaFudge");
		this.uniformPointAreaHandle = gl.glGetUniformLocation(this.shaderProgram, "pointArea");
		this.uniformColorHandle = gl.glGetUniformLocation(this.shaderProgram, "pointColor");

		this.uniformFilterMinX = gl.glGetUniformLocation(this.shaderProgram, "filterMinX");
		this.uniformFilterMaxX = gl.glGetUniformLocation(this.shaderProgram, "filterMaxX");
		this.uniformFilterGradient = gl.glGetUniformLocation(this.shaderProgram, "filterGradient");
		this.uniformFilterConstant = gl.glGetUniformLocation(this.shaderProgram, "filterConstant");
		gl.glEnable(GL_BLEND);

		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL_FUNC_ADD);
		gl.glDisable(GL_CULL_FACE);

	}

	private void printFps() {
		long delta = System.nanoTime() - lastTime;
		double fps = 1000_000_000 / (double)delta;
		System.out.println("FPS: " + (int)fps);
		lastTime = System.nanoTime();
	}

	int[] legendVertexHandles = new int[3];
	int[] legendColorHandles = new int[3];

	private void drawLegend() {

		float[] origin = {0f, 0f, 0f};
		if (legendVertexHandles.length == 0) {
			gl.glGenBuffers(3, legendVertexHandles, 0);
			gl.glGenBuffers(3, legendColorHandles, 0);


			int axisNumber = 0;
			//--X
			FloatBuffer vertexBuffer = FloatBuffer.allocate(6);
			vertexBuffer.put(origin);
			float[] dest = {2f,0f,0f};
			vertexBuffer.put(dest);
			vertexBuffer.flip();

			gl.glBindBuffer(GL_ARRAY_BUFFER, legendVertexHandles[axisNumber]);
			gl.glBufferData(GL_ARRAY_BUFFER, 2 * 3 * 4, vertexBuffer, GL_STATIC_DRAW);

			FloatBuffer colorBuffer = FloatBuffer.allocate(8);
			float[] red = {1f, 0f, 0f, 0f};
			colorBuffer.put(dest);
			colorBuffer.flip();

			gl.glBindBuffer(GL_ARRAY_BUFFER, legendColorHandles[axisNumber]);
			gl.glBufferData(GL_ARRAY_BUFFER, 4 * 4, vertexBuffer, GL_STATIC_DRAW);
		}


//		int axisNumber = 0;
//		gl.glEnableVertexAttribArray(0);
//		gl.glBindBuffer(GL_ARRAY_BUFFER, legendVertexHandles[axisNumber]);
//		gl.glVertexAttribPointer(0, 3, GL_FLOAT, true, 0, 1);
//
//		gl.glEnableVertexAttribArray(1);
//		gl.glBindBuffer(GL_ARRAY_BUFFER, legendColorHandles[axisNumber]);
//		gl.glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 1);
//
//		gl.glDrawArrays(GL_LINE, 0, 2);

	}
	public void display() {

		printFps();

		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(this.shaderProgram);
		
		
		//--figure out if looking back to front
		float pi = (float)Math.PI;
		float spin = Math.abs(this.viewer.getxSpin() % (2f * pi));
//		boolean flippityFlop = spin > pi/2f && spin < 3f*pi/2f ;
		boolean flippityFlop = spin > 0f && spin < pi ;
		if (this.isTrippy == true) {
			flippityFlop = true;
		}
		
		List<VertexBufferSlice> allSlicesLikeEver = new ArrayList<VertexBufferSlice>();
		for (PointCloud cloud : this.pointClouds){
			if (cloud.isVisible.value == false) 
				continue;
			for (CloudRegion cr: cloud.getRegions()) {
				for (VertexBufferSlice slice: cr.getSlices()) {
					slice.scratchDepth = cr.volume.x + cr.volume.wd * slice.depthValue;
					slice.region = cr;
					slice.cloud = cloud;
				}
				allSlicesLikeEver.addAll(cr.getSlices());
			}
		}

		
		class RegionOrderer implements Comparator<VertexBufferSlice> {
			public int compare(VertexBufferSlice a, VertexBufferSlice b) {
				return a.scratchDepth < b.scratchDepth ? 1 : -1;
			}
		}
		Collections.sort(allSlicesLikeEver, new RegionOrderer());

		Matrix4 baseMatrix = new Matrix4();
		baseMatrix.makeOrtho(orthoOrigX - orthoWidth, orthoOrigX + orthoWidth, orthoOrigY - orthoHeight, orthoOrigY + orthoHeight, -6f, 6f);
		baseMatrix.rotate(this.viewer.getySpin(), 1f, 0f, 0f);
		baseMatrix.rotate(this.viewer.getxSpin(), 0f, 1f, 0f);
		float baseScale = 1.0f / this.viewer.getRadius();

		baseMatrix.scale(baseScale, baseScale, baseScale);


		for (int i = 0; i < allSlicesLikeEver.size(); i++){
			
			//-if Z is now pointing out of the screen take slices from the back of the list forward
			int sliceIndex = i;
			if (flippityFlop) {
				sliceIndex = allSlicesLikeEver.size() - 1 - i;
			}

			VertexBufferSlice slice = allSlicesLikeEver.get(sliceIndex);
			PointCloud cloud = slice.cloud;
			CloudRegion cr = slice.region;
			
			gl.glUniform1f(this.uniformAlphaFudgeHandle, cloud.intensity.value);
			//--filtery doodle TODO probably move this out one level of the loop
			
			Christogram.Filter filter = cloud.getFilter();
			gl.glUniform1f(this.uniformFilterMinX, filter.minX);
			gl.glUniform1f(this.uniformFilterMaxX, filter.maxX);
			
			float gradient = (filter.maxY - filter.minY) / (filter.maxX - filter.minX);
			float constant = filter.minY - gradient * filter.minX;
			
			gl.glUniform1f(this.uniformFilterGradient, gradient);
			gl.glUniform1f(this.uniformFilterConstant, constant);

			Color[] cols = {Color.red, Color.orange, Color.yellow, Color.green, Color.blue, Color.cyan, Color.magenta};
			Color col = cloud.color;
			if (gay) {
				col = cols[i%cols.length];
			}

			gl.glUniform4f(this.uniformColorHandle, col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());

    		

    		float pointRadius = this.calculatePointRadiusInPixelsForSlice(slice) * baseScale;
			float ptArea = 0.5f * pointRadius * pointRadius * (float)Math.PI;
    		gl.glPointSize(Math.max(pointRadius, 1f));
			gl.glUniform1f(this.uniformPointAreaHandle, ptArea);

			if (legendary) {
				drawLegend();
			}

	    	Matrix4 m = new Matrix4();
			m.loadIdentity();
			m.multMatrix(baseMatrix);
	    	m.translate(cr.volume.x + slice.depthValue, cr.volume.y, cr.volume.z);
	    	m.scale(cr.volume.wd, cr.volume.ht, cr.volume.dp);
	    	
	    	m.translate(cloud.volume.x, cloud.volume.y, cloud.volume.z);
	    	m.scale(cloud.volume.wd, cloud.volume.ht, cloud.volume.dp);
	    	
	    	//--pass that matrix to the shader
	    	gl.glUniformMatrix4fv(this.uniformMvpHandle, 1, false, m.getMatrix(), 0);
	
	    	gl.glEnableVertexAttribArray(0);
	    	gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferHandles[slice.index]);
	    	gl.glVertexAttribPointer(0, 3, GL_SHORT, true, 0, 0);
	    	
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

	
	private float calculatePointRadiusInPixelsForSlice(VertexBufferSlice slice) {
		CloudRegion cr = slice.region;
		float pointWidth = (float)this.width* this.orthoWidth*cr.volume.wd/ (float)cr.ptWidth(); 
		float pointHeight = (float)this.height* this.orthoHeight*cr.volume.ht / (float)cr.ptHeight();
		float sz =  pointWidth < pointHeight ? pointWidth : pointHeight;
		return sz;
	}
}
