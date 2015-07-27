import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.*;

import static com.jogamp.opengl.GL2.*;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.Matrix4;

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
	private int shaderProgramLegend;

	private int uniformAlphaFudgeHandle;
	private int uniformMvpHandle;
	private int uniformPointAreaHandle;
	private int uniformColorHandle;
	
	private int[] vertexBufferHandles;
	private int[] valueBufferHandles;
	
	
	//--MODEL STUFF
	private WorldViewer viewer;
	public Selection selection;

	private List<PointCloud> pointClouds;
	public boolean isTrippy;



	private int uniformFilterMinX;
	private int uniformFilterMaxX;
	private int uniformFilterGradient;
	private int uniformFilterConstant;

	private int uniformSelectionMinX;
	private int uniformSelectionMaxX;

	private int uniformSelectionMinY;
	private int uniformSelectionMaxY;

	private int uniformSelectionMinZ;
	private int uniformSelectionMaxZ;

	//--highlighting shader uniforms

	private int legendMvpUniformHandle;




	//--Primitives
	private List<Line>backLines = new ArrayList<Line>();
	private List<Line>frontLines = new ArrayList<Line>();
	private List<Line>bottomLines = new ArrayList<Line>();
	private List<Line>topLines = new ArrayList<Line>();
	private List<Line>rightLines = new ArrayList<Line>();
	private List<Line>leftLines = new ArrayList<Line>();


	//--SELECTIOn
	private Volume selectionVolume = Volume.unitVolume();



	//--DEBUG FLAGS
	private boolean legendary = true;
	public	boolean gay = false;
	private int uniformIsSelecting;


	public Renderer(List<PointCloud> pointClouds, WorldViewer viewer, GL3 gl){
		setupWith(pointClouds, viewer, gl);
	}

	public void setupWith(List<PointCloud> pointClouds, WorldViewer viewer, GL3 gl){
		this.pointClouds = pointClouds;
		this.gl = gl;
		this.viewer = viewer;

		int nSlices = 0;
		for (PointCloud cloud : this.pointClouds)
			for (Region cr : cloud.getRegions())
				nSlices += cr.getSlices().size();

		this.vertexBufferHandles = new int[nSlices];
		this.valueBufferHandles = new int[nSlices];
		int index = 0;
		int[] ptr = new int[2];
		for (PointCloud cloud : this.pointClouds){
			for (Region cr : cloud.getRegions()) {
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

		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "shader2.vert", "shader2.frag");


		this.uniformMvpHandle 		= gl.glGetUniformLocation(this.shaderProgram, "mvp");
		this.uniformAlphaFudgeHandle= gl.glGetUniformLocation(this.shaderProgram, "alphaFudge");
		this.uniformPointAreaHandle = gl.glGetUniformLocation(this.shaderProgram, "pointArea");
		this.uniformColorHandle 	= gl.glGetUniformLocation(this.shaderProgram, "pointColor");

		this.uniformFilterMinX 		= gl.glGetUniformLocation(this.shaderProgram, "filterMinX");
		this.uniformFilterMaxX 		= gl.glGetUniformLocation(this.shaderProgram, "filterMaxX");
		this.uniformFilterGradient 	= gl.glGetUniformLocation(this.shaderProgram, "filterGradient");
		this.uniformFilterConstant 	= gl.glGetUniformLocation(this.shaderProgram, "filterConstant");

		this.uniformSelectionMinX 	= gl.glGetUniformLocation(this.shaderProgram, "selectionMinX");
		this.uniformSelectionMaxX 	= gl.glGetUniformLocation(this.shaderProgram, "selectionMaxX");
		this.uniformSelectionMinY 	= gl.glGetUniformLocation(this.shaderProgram, "selectionMinY");
		this.uniformSelectionMaxY 	= gl.glGetUniformLocation(this.shaderProgram, "selectionMaxY");
		this.uniformSelectionMinZ 	= gl.glGetUniformLocation(this.shaderProgram, "selectionMinZ");
		this.uniformSelectionMaxZ 	= gl.glGetUniformLocation(this.shaderProgram, "selectionMaxZ");

		this.uniformIsSelecting 	= gl.glGetUniformLocation(this.shaderProgram, "isSelecting");

		gl.glEnable(GL_BLEND);
gl.glDisable(GL_POINT_SMOOTH);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL_FUNC_ADD);
		gl.glDisable(GL_CULL_FACE);

		setupLegend();
	}

	private void printFps() {
		long delta = System.nanoTime() - lastTime;
		double fps = 1000_000_000 / (double)delta;
		System.out.println("FPS: " + (int)fps);
		lastTime = System.nanoTime();
	}


	boolean first = true;




	private void setupLegend() {
		float length = 10f;
		Line[] legendLines = new Line[3];
		for (int axisNumber = 0; axisNumber < 3; axisNumber++) {
			float[] origin = {-1f, -1f, -1f};
			float[] dest = origin.clone();
			dest[axisNumber] += length;

			FloatBuffer colorBuffer = FloatBuffer.allocate(8);
			float[] col = {0.3f, 0.3f, 0.3f, 1f};
			col[axisNumber] = 1f;
			float[] black = {0f, 0f, 0f, 0f};
			legendLines[axisNumber] = makeLine(origin, dest, col, black);
		}
		this.backLines.add(legendLines[0]);	//--add red x axis line
		this.backLines.add(legendLines[1]);	//--add green y axis line
		this.leftLines.add(legendLines[2]);	//--add blue z axis line

	}
	private void renderPrimitives(Matrix4 mvp, float spin, boolean firstPass) {

		float zero = 0f;
		float halfPi = 3.141592f/2f;
		float pi = 3.141592f;
		float piAndAHalf = 3.141592f * 1.5f;

		Set<Line> front= new HashSet<>();
		Set<Line> back= new HashSet<>();

		//--back
		if (spin < halfPi || spin >piAndAHalf) {
			front.addAll(frontLines);
			back.addAll(backLines);
		}
		if (spin < pi) {
			front.addAll(rightLines);
			back.addAll(leftLines);
		}
		if (spin > halfPi && spin < piAndAHalf) {
			front.addAll(backLines);
			back.addAll(frontLines);
		}
		if (spin > pi) {
			front.addAll(leftLines);
			back.addAll(rightLines);
		}

		if (firstPass) {
			renderLines(back, mvp);
		}
		else {
			renderLines(front, mvp);
		}
	}

	private void renderOutline(Matrix4 mvp, Volume v, Color color) {
		float[] col = new float[4];
		col[0] = (float) color.getRed() / 255;
		col[1] = (float) color.getGreen() / 255;
		col[2] = (float) color.getBlue() / 255;
		col[3] = (float) color.getAlpha() / 255;

		Line one 	= makeLine(v.a(), v.b(), col, col);
		Line two 	= makeLine(v.a(), v.c(), col, col);
		Line three 	= makeLine(v.c(), v.d(), col, col);
		Line four 	= makeLine(v.b(), v.d(), col, col);
		Line five	= makeLine(v.e(), v.f(), col, col);
		Line six	= makeLine(v.e(), v.g(), col, col);
		Line seven 	= makeLine(v.g(), v.h(), col, col);
		Line eight	= makeLine(v.f(), v.h(), col, col);
		Line nine	= makeLine(v.a(), v.e(), col, col);
		Line ten 	= makeLine(v.b(), v.f(), col, col);
		Line eleven = makeLine(v.c(), v.g(), col, col);
		Line twelve = makeLine(v.d(), v.h(), col, col);

		Line[] lines = {one, two, three, four, five, six, seven, eight, nine, ten, eleven, twelve};
		renderLines(lines, mvp);
	}

	public static class Line{
		public Line(int vertHandle_, int colHandle_) {vertHandle = vertHandle_; colHandle = colHandle_;}
		int vertHandle;
		int colHandle;
	}

	private Line makeLine(Vector3 posa, Vector3 posb, float[] cola, float[] colb) {
		return makeLine(posa.toArray(), posb.toArray(), cola, colb);
	}

	private Line makeLine(float[]posa, float[]posb, float []cola, float[]colb) {
		if (first) {
			//--load the shader files in
			this.shaderProgramLegend = ShaderHelper.programWithShaders2(gl, "shaderFlat.vert", "shaderFlat.frag");
			this.legendMvpUniformHandle = gl.glGetUniformLocation(this.shaderProgramLegend, "mvp");
			first = false;
		}

		int[] vertHandle = new int[1];
		gl.glGenBuffers(1, vertHandle, 0);

		FloatBuffer vertexBuffer = FloatBuffer.allocate(6);
		vertexBuffer.put(posa);
		vertexBuffer.put(posb);
		vertexBuffer.flip();

		gl.glBindBuffer(GL_ARRAY_BUFFER, vertHandle[0]);
		gl.glBufferData(GL_ARRAY_BUFFER, 2 * 3 * 4, vertexBuffer, GL_STATIC_DRAW);

		int[] colHandle = new int[1];
		gl.glGenBuffers(1, colHandle, 0);

		FloatBuffer colorBuffer = FloatBuffer.allocate(8);
		colorBuffer.put(cola);
		colorBuffer.put(colb);
		colorBuffer.flip();

		gl.glBindBuffer(GL_ARRAY_BUFFER, colHandle[0]);
		gl.glBufferData(GL_ARRAY_BUFFER, 2 * 4 * 4, colorBuffer, GL_STATIC_DRAW);

		return new Line(vertHandle[0], colHandle[0]);
	}

	private void renderLine(Line line, Matrix4 mvp) {
		Line[] temp = {line};
		renderLines(temp, mvp);
	}

	private void renderLines(Collection<Line>lines, Matrix4 mvp) {
		Line[]larray = new Line[lines.size()];
		int idx = 0;
		for (Line line : lines) {
			larray[idx++] = line;
		}
		renderLines(larray, mvp);
	}

	private void renderLines(Line[] lines, Matrix4 mvp) {

		gl.glUseProgram(this.shaderProgramLegend);
		gl.glUniformMatrix4fv(this.legendMvpUniformHandle, 1, false, mvp.getMatrix(), 0);

		for (Line line : lines) {
			//--select the lines vertex buffer
			gl.glEnableVertexAttribArray(0);
			gl.glBindBuffer(GL_ARRAY_BUFFER, line.vertHandle);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, true, 0, 0);

			//--select the lines color buffer
			gl.glEnableVertexAttribArray(1);
			gl.glBindBuffer(GL_ARRAY_BUFFER, line.colHandle);
			gl.glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);

			gl.glDrawArrays(GL_LINES, 0, 2);
		}

			gl.glUseProgram(this.shaderProgram);
	}

	private boolean lastFlippity = true;
	public void display() {

		printFps();

		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(this.shaderProgram);
		if (!FrameMaster.vain && this.selection != null) {


			gl.glUniform1i(uniformIsSelecting, GL_TRUE);

			PointCloud cloudOfInterest = this.pointClouds.get(0);
			//--normalise the origin
			Vector3 originRelativeToCloud = this.selection.getVolume().origin.minus(cloudOfInterest.getVolume().origin);
			Vector3 normalisedPosition = originRelativeToCloud.divideBy(cloudOfInterest.getVolume().size);
			Vector3 normalisedSize = this.selection.getVolume().size.divideBy(cloudOfInterest.getVolume().size);

			int dimensions = 3;
			int[] uniformsMin = {uniformSelectionMinX, uniformSelectionMinY, uniformSelectionMinZ};
			int[] uniformsMax = {uniformSelectionMaxX, uniformSelectionMaxY, uniformSelectionMaxZ};
			for (int i = 0; i < dimensions; i++) {
				gl.glUniform1f(uniformsMin[i], normalisedPosition.get(i));
				gl.glUniform1f(uniformsMax[i], normalisedPosition.get(i) + normalisedSize.get(i));
			}
		}
		else {
			gl.glUniform1i(uniformIsSelecting, GL_FALSE);
		}
		//--figure out if looking back to front
		float pi = (float)Math.PI;

		float moddedSpin = this.viewer.getxSpin() % (2f * pi);
		float spin = moddedSpin < 0f ? (2f * pi) + moddedSpin : moddedSpin;

		boolean flippityFlop = spin > pi;


		lastFlippity = flippityFlop;
		
		List<VertexBufferSlice> allSlicesLikeEver = new ArrayList<VertexBufferSlice>();

		float minScratchX =  Float.MAX_VALUE;
		float maxScractchX = Float.MIN_VALUE;
		for (PointCloud cloud : this.pointClouds){
			if (cloud.isVisible.getValue() == false) {continue;}
			for (Region cr: cloud.getRegions()) {
				if (cr.isVisible.getValue() == false) {continue;}

				for (VertexBufferSlice slice: cr.getSlices()) {

					slice.scratchX = (cr.volume.x + cr.volume.wd * slice.x) * cloud.volume.wd + cloud.volume.x;
					if (slice.scratchX > maxScractchX) {
						maxScractchX = slice.scratchX;
					}
					if (slice.scratchX < minScratchX) {
						minScratchX = slice.scratchX;
					}
					slice.region = cr;
					slice.cloud = cloud;
				}
				allSlicesLikeEver.addAll(cr.getSlices());
			}
		}

		
		class RegionOrderer implements Comparator<VertexBufferSlice> {
			public int compare(VertexBufferSlice a, VertexBufferSlice b) {
				return a.scratchX < b.scratchX ? 1 : -1;
			}
		}
		Collections.sort(allSlicesLikeEver, new RegionOrderer());

		Matrix4 baseMatrix = new Matrix4();
		baseMatrix.makeOrtho(orthoOrigX - orthoWidth, orthoOrigX + orthoWidth, orthoOrigY - orthoHeight, orthoOrigY + orthoHeight, -6f, 6f);
		baseMatrix.rotate(this.viewer.getySpin(), 1f, 0f, 0f);
		baseMatrix.rotate(this.viewer.getxSpin(), 0f, 1f, 0f);
		float baseScale = 1.0f / this.viewer.getRadius();

		baseMatrix.scale(baseScale, baseScale, baseScale);

		if (FrameMaster.vain == false) {
			renderPrimitives(baseMatrix, spin, true);
		}


		for (int i = 0; i < allSlicesLikeEver.size(); i++){
			
			//-if Z is now pointing out of the screen take slices from the back of the list forward
			int sliceIndex = i;
			if (flippityFlop) {
				sliceIndex = allSlicesLikeEver.size() - 1 - i;
			}

			VertexBufferSlice slice = allSlicesLikeEver.get(sliceIndex);
			PointCloud cloud = slice.cloud;
			Region cr = slice.region;
			
			gl.glUniform1f(this.uniformAlphaFudgeHandle, cr.intensity.getValue() * cloud.intensity.getValue());
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
				float proportion = (slice.scratchX - minScratchX) / (maxScractchX - minScratchX);
				col = new Color(0.1f + 0.8f * proportion, 0.9f - 0.8f * proportion, 0.0f, 1.0f);
				System.out.println(col);

				col = cols[i%cols.length];
			}

			float[] colArray = new float[4];
			col.getComponents(colArray);
			gl.glUniform4f(this.uniformColorHandle, colArray[0], colArray[1], colArray[2], colArray[3]);

    		

    		float pointRadius = this.calculatePointRadiusInPixelsForSlice(slice) * baseScale;
			float ptArea = 0.5f * pointRadius * pointRadius * (float)Math.PI;
    		gl.glPointSize(Math.max(pointRadius, 1f));
			gl.glUniform1f(this.uniformPointAreaHandle, ptArea);



	    	Matrix4 m = new Matrix4();
			m.loadIdentity();
			m.multMatrix(baseMatrix);


	    	m.translate(cloud.volume.x, cloud.volume.y, cloud.volume.z);
	    	m.scale(cloud.volume.wd, cloud.volume.ht, cloud.volume.dp);

			//m.translate(cr.volume.x, cr.volume.y, cr.volume.z);

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



		if (FrameMaster.vain == false) {
			renderPrimitives(baseMatrix, spin, false);
		}

		//--draw outlines
		for (PointCloud pc : this.pointClouds) {
			if (pc.isSelected.getValue()) {
				renderOutline(baseMatrix, pc.volume, pc.color);
			}
		}
		if (!FrameMaster.vain) {
			if (this.selection != null) {
				renderOutline(baseMatrix, this.selection.getVolume(), Color.white);
			}
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
		Region cr = slice.region;
		//TODO actually consider the z pixel size yo
		float pointWidth = (float)this.width* this.orthoWidth*cr.volume.wd/ (float)cr.ptWidth(); 
		float pointHeight = (float)this.height* this.orthoHeight*cr.volume.ht / (float)cr.ptHeight();
		float sz =  pointWidth < pointHeight ? pointWidth : pointHeight;
		return sz;
	}
}
