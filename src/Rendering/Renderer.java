package Rendering;

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

import Model.*;
import UserInterface.Christogram;
import UserInterface.FrameMaster;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.Matrix4;

public class Renderer {
	private static long lastTime = 0;
	public static long lastFrameDelta;
	//--SETTINGS
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
//	public Model.Selection selection;

	public List<PointCloud> pointClouds;
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

	private int uniformLowLight;


	private int legendMvpUniformHandle;


	//--Primitives
	private List<Line>backLines = new ArrayList<Line>();
	private List<Line>frontLines = new ArrayList<Line>();
	private List<Line>bottomLines = new ArrayList<Line>();
	private List<Line>topLines = new ArrayList<Line>();
	private List<Line>rightLines = new ArrayList<Line>();
	private List<Line>leftLines = new ArrayList<Line>();



	//--DEBUG FLAGS
	public	boolean gay = false;
	private int uniformIsSelecting;
	private int uniformWatchOutForOverflow;

	public Vector3 mouseWorldPosition;

	public static boolean getFat = false;
	public static boolean cutTheFat = false;

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

		rebindAllSlices(true);

		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "shaderCloud.vert", "shaderCloud.frag");

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
		this.uniformWatchOutForOverflow = gl.glGetUniformLocation(this.shaderProgram, "watchOutForOverflow");

		this.uniformLowLight 		= gl.glGetUniformLocation(this.shaderProgram, "lowLight");
		gl.glEnable(GL_BLEND);
		gl.glDisable(GL_POINT_SMOOTH);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL_FUNC_ADD);
		gl.glDisable(GL_CULL_FACE);

		setupLegend();

		this.shaderProgramLegend = ShaderHelper.programWithShaders2(gl, "shaderFlat.vert", "shaderFlat.frag");
		this.legendMvpUniformHandle = gl.glGetUniformLocation(this.shaderProgramLegend, "mvp");
	}

	private void printFps() {
		long delta = System.nanoTime() - lastTime;
		double fps = 1000_000_000 / (double)delta;
		System.out.println("FPS: " + (int) fps);
		lastTime = System.nanoTime();
	}



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
		this.backLines.add(legendLines[0]);	//--add red z axis line
		this.backLines.add(legendLines[1]);	//--add green y axis line
		this.leftLines.add(legendLines[2]);	//--add blue z axis line

	}
	private void renderBackgroundLines(Matrix4 mvp, float spin, boolean firstPass) {

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
			front.addAll(leftLines);
			back.addAll(rightLines);
		}
		if (spin > halfPi && spin < piAndAHalf) {
			front.addAll(backLines);
			back.addAll(frontLines);
		}
		if (spin > pi) {
			front.addAll(rightLines);
			back.addAll(leftLines);
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

	private Line makeLine(Vector3 posa, Vector3 posb) {
		float[]white = {1f,1f,1f,1f};

		return makeLine(posa, posb, white, white);
	}
	private Line makeLine(Vector3 posa, Vector3 posb, float[] cola, float[] colb) {
		return makeLine(posa.toArray(), posb.toArray(), cola, colb);
	}

	private Line makeLine(float[]posa, float[]posb, float []cola, float[]colb) {
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

	public void display() {
		long delta = System.nanoTime() - lastTime;
		lastFrameDelta = delta / 1000_000;
//		printFps();

		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(this.shaderProgram);

		if (FrameMaster.getActivePointCloud() == null) {
			return;
		}
		//--figure out if looking back to front
		float pi = (float)Math.PI;

		float moddedSpin = this.viewer.getxSpin() % (2f * pi);
		float spin = moddedSpin < 0f ? (2f * pi) + moddedSpin : moddedSpin;

		boolean flippityFlop = spin > pi/2f && spin < (3f/2f) * pi  ;


		List<VertexBufferSlice> allSlicesLikeEver = new ArrayList<VertexBufferSlice>();

		if (getFat) {
			rebindAllSlices(false);
			getFat = false;
		}
		else if (cutTheFat){
			rebindAllSlices(true);
			cutTheFat = false;
		}



		for (PointCloud cloud : this.pointClouds){
			if (cloud.isVisible.getValue() == false) {continue;}
			boolean isTheCurrentPointCloud = FrameMaster.getActivePointCloud() == cloud;
			boolean isPositionedRelativeToTheCurrentPointCloud = FrameMaster.getActivePointCloud().pointCloudsPositionedRelativeToThisone.contains(cloud);
			boolean theCurrentCloudIsPositionedRelativeToTheCloud = FrameMaster.getActivePointCloud().pointCloudPositionedRelativeTo == cloud;
			if (!isPositionedRelativeToTheCurrentPointCloud && !isTheCurrentPointCloud && !theCurrentCloudIsPositionedRelativeToTheCloud) {continue;}

			for (Region cr: cloud.getRegions()) {
				if (cr.isVisible.getValue() == false) {continue;}
				for (VertexBufferSlice slice: cr.getSlices()) {
					if (!slice.isLive) {continue;}
					if (!cloud.shouldDisplayFrameWithW(slice.w)) {continue;}

					slice.region = cr;
					slice.cloud = cloud;

					allSlicesLikeEver.add(slice);
				}
			}
		}
		Collections.sort(allSlicesLikeEver, new RegionOrderer());

		Matrix4 baseMatrix = viewer.getBaseMatrix();

		renderBackgroundLines(baseMatrix, spin, true);

		PointCloud lastCloud = null;
		Region lastRegion = null;
		float ptArea = -1.0f;
		for (int i = 0; i < allSlicesLikeEver.size(); i++){
			
			//-if Z is now pointing out of the screen take slices from the back of the list forward
			int sliceIndex = i;
			if (flippityFlop) {
				sliceIndex = allSlicesLikeEver.size() - 1 - i;
			}

			VertexBufferSlice slice = allSlicesLikeEver.get(sliceIndex);
			PointCloud cloud = slice.cloud;
			Region region = slice.region;
			if (region == null || cloud == null) {
				return;
			}
			if (ptArea == -1.0f) {
				float pointRadius = this.calculatePointRadiusInPixelsForSlice(slice) * 1.0f / this.viewer.getRadius();
				ptArea = 0.5f * pointRadius * pointRadius * (float) Math.PI;
			}
			gl.glUniform1f(this.uniformAlphaFudgeHandle, region.intensity.getValue() * cloud.intensity.getValue() * Math.min(ptArea, 1.0f));

			if (cloud.shouldDisplaySlitherenated() && !cloud.getSelection().isActive()) {
				gl.glUniform1i(uniformIsSelecting, GL_TRUE);

				int axis = cloud.getSlitherAxis().ordinal();
				//--figure out a good alpha for the rest of the stuff
				float base = 10f;
				float adjusted = region.getVolume().size.get(axis) * base /(float) region.getDimensionInPts(axis);

				gl.glUniform1f(uniformLowLight, adjusted);
				Volume slither = cloud.getSlither(true);
				int[] uniformsMin = {uniformSelectionMinX, uniformSelectionMinY, uniformSelectionMinZ};
				int[] uniformsMax = {uniformSelectionMaxX, uniformSelectionMaxY, uniformSelectionMaxZ};
				for (int j = 0; j < 3; j++) {
					gl.glUniform1f(uniformsMin[j], slither.origin.get(j));
					gl.glUniform1f(uniformsMax[j], slither.origin.get(j) + slither.size.get(j));
				}
			}

			if (lastRegion == null || lastRegion.isLiableToOverflow() != region.isLiableToOverflow()) {
				if (region.isLiableToOverflow()) {
					gl.glUniform1i(uniformWatchOutForOverflow, GL_TRUE);
				}
				else {
					gl.glUniform1i(uniformWatchOutForOverflow, GL_FALSE);
				}
			}
			//--if there's a new cloud then update the filtering uniforms
			if(lastCloud != cloud) {

				Christogram.ChristogramSelection christogramSelection = cloud.getFilter();
				gl.glUniform1f(this.uniformFilterMinX, christogramSelection.minX);
				gl.glUniform1f(this.uniformFilterMaxX, christogramSelection.maxX);

				float gradient = (christogramSelection.maxY - christogramSelection.minY) / (christogramSelection.maxX - christogramSelection.minX);
				float constant = christogramSelection.minY - gradient * christogramSelection.minX;

				gradient *= cloud.intensity.getValue();
				constant *= cloud.intensity.getValue();
				gl.glUniform1f(this.uniformFilterGradient, gradient);
				gl.glUniform1f(this.uniformFilterConstant, constant);

				Color[] cols = {Color.red, Color.orange, Color.yellow, Color.green, Color.blue, Color.cyan, Color.magenta};
				Color col = cloud.color;
				if (gay) {
					col = cols[i % cols.length];
				}

				float[] colArray = new float[4];
				col.getComponents(colArray);
				gl.glUniform4f(this.uniformColorHandle, colArray[0], colArray[1], colArray[2], colArray[3]);


				if (cloud!=null && cloud.getSelection().isActive()) {
					gl.glUniform1i(uniformIsSelecting, GL_TRUE);

					//--normalise the origin
					Vector3 originRelativeToCloud = cloud.getSelection().getVolume().origin.minus(cloud.getVolume().origin);
					Vector3 normalisedPosition = originRelativeToCloud.divideBy(cloud.getVolume().size);
					Vector3 normalisedSize = cloud.getSelection().getVolume().size.divideBy(cloud.getVolume().size);

					//--figure out an appropriate brightness given the density of points TODO implement this weverywhere.
					float base = 10f;
					int axis = cloud.getSlitherAxis().ordinal();
					float adjusted = cloud.getVolume().size.get(axis) * base /(float) cloud.getRegions().get(0).getDimensionInPts(axis);
					gl.glUniform1f(uniformLowLight, adjusted);

					int dimensions = 3;
					int[] uniformsMin = {uniformSelectionMinX, uniformSelectionMinY, uniformSelectionMinZ};
					int[] uniformsMax = {uniformSelectionMaxX, uniformSelectionMaxY, uniformSelectionMaxZ};
					for (int j = 0; j < dimensions; j++) {
						float a = normalisedPosition.get(j);
						float b = normalisedPosition.get(j) + normalisedSize.get(j);
						gl.glUniform1f(uniformsMin[j], Math.min(a,b));
						gl.glUniform1f(uniformsMax[j], Math.max(a, b));
					}
				}
				else {
					gl.glUniform1i(uniformIsSelecting, GL_FALSE);
				}

			}
			//--if there's a new region then we'd better update the point size
			if (lastRegion != region) {

				//--calculate the point radius
				float pointRadius = this.calculatePointRadiusInPixelsForSlice(slice) * 1.0f / this.viewer.getRadius();
				ptArea = 0.5f * pointRadius * pointRadius * (float) Math.PI;
				gl.glPointSize(Math.max(pointRadius, 1f));
//				gl.glUniform1f(this.uniformPointAreaHandle, ptArea);
			}

	    	Matrix4 m = new Matrix4();
			m.multMatrix(baseMatrix);

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
	    	
	    	gl.glDrawArrays(GL_POINTS, 0, slice.scratchPts);

			lastCloud = cloud;
			lastRegion = region;
		}

		renderBackgroundLines(baseMatrix, spin, false);

		//--draw outlines
		for (PointCloud pc : this.pointClouds) {
			if (pc.shouldDisplaySlitherenated()) {
				if (pc == FrameMaster.getActivePointCloud()) {

					Volume vol = pc.getSlither(false);
					Vector3 worldOrigin = vol.origin;
					Vector3 worldSize = vol.size;

					renderOutline(baseMatrix, vol, pc.color);

					//--draw axes for mouse highlighting
					if (this.mouseWorldPosition != null) {
						//--ensure on correct plane
						this.mouseWorldPosition = 	new Vector3(this.mouseWorldPosition.x		, this.mouseWorldPosition.y		, worldOrigin.z);

						Vector3 lm = 				new Vector3(worldOrigin.x					, this.mouseWorldPosition.y		, worldOrigin.z);
						Vector3 rm = 				new Vector3(worldOrigin.x + worldSize.x 	, this.mouseWorldPosition.y		, worldOrigin.z);
						Vector3 bm = 				new Vector3(this.mouseWorldPosition.x		, worldOrigin.y					, worldOrigin.z);
						Vector3 tm = 				new Vector3(this.mouseWorldPosition.x		, worldOrigin.y + worldSize.y	, worldOrigin.z);
						float[]grey = {0.5f, 0.5f, 0.5f, 1f};
						Line l2r = makeLine(lm, rm, grey, grey);
						Line t2b = makeLine(bm, tm, grey, grey);
						renderLine(l2r, baseMatrix);
						renderLine(t2b, baseMatrix);
					}

					if (pc.getSelection().isActive() ) {
						renderOutline(viewer.getBaseMatrix(), pc.getSelection().getVolume().clampedToVolume(pc.getVolume()), Color.white);
					}
				}
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

		float pointWidth = (float)this.width* WorldViewer.orthoWidth* cr.getVolume().wd/ (float)cr.getWidthInPoints();

		float pointHeight = (float)this.height* WorldViewer.orthoHeight* cr.getVolume().ht / (float)cr.getHeightInPoints();
		float pointsDepth = (float)this.width* WorldViewer.orthoWidth* cr.getVolume().dp / (float)cr.getDepthInPoints();

		float sz =  pointWidth < pointHeight ? pointWidth : pointHeight;
//		sz = sz < pointsDepth ? sz : pointsDepth;

		return sz ;//* 0.5f;
	}



	public static class RegionOrderer implements Comparator<VertexBufferSlice> {
		public int compare(VertexBufferSlice a, VertexBufferSlice b) {
			if (a.getOverallZ() == b.getOverallZ()) {
				return 0;
			}
			else {
				return a.getOverallZ() > b.getOverallZ() ? 1 : -1;
			}

		}
	}

	public void rebindAllSlices(boolean applyFilter) {
		int nSlices = 0;
		for (PointCloud cloud : this.pointClouds)
			for (Region cr : cloud.getRegions())
				nSlices += cr.getSlices().size();

		this.vertexBufferHandles = new int[nSlices];
		this.valueBufferHandles = new int[nSlices];
		int index = 0;

		for (PointCloud cloud : this.pointClouds){
			index = rebindCloud(cloud, index, applyFilter);

		}
		System.out.println("rebinding slices");
	}

	public int rebindCloud(PointCloud cloud,int index, boolean applyFilter) {
		int steps = 0;
		for (Region cr : cloud.getRegions()) {
			for (VertexBufferSlice vbs : cr.getSlices()) {
				vbs.cloud = cloud;
				rebindSliceBuffers(vbs, steps + index, applyFilter);
				steps++;
			}
		}
		return steps+index;
	}
	public void rebindSliceBuffers(VertexBufferSlice vbs, int index, boolean applyFilter) {
		int[] ptr = new int[2];
		gl.glGenBuffers(2, ptr, 0);
		this.vertexBufferHandles[index] = ptr[0];
		vbs.index = index;
		vbs.isLive = true;





		boolean newStyle = true;
		if (applyFilter) {

			Christogram.ChristogramSelection christogramSelection = vbs.cloud.getFilter();
			float 	minValX = christogramSelection.minX;
			float maxValX = christogramSelection.maxX;

			FloatBuffer newValueBuffer = FloatBuffer.allocate(vbs.numberOfPts);
			ShortBuffer newVertBuffer = ShortBuffer.allocate(vbs.numberOfPts * 3);
			int numPts = 0;
			for (int i = 0; i < vbs.numberOfPts; i++) {
				float val = vbs.valueBuffer.get(i);
				if (val >= minValX && val < maxValX) {
					numPts ++;
					newValueBuffer.put(val);
					newVertBuffer.put(vbs.vertexBuffer.get(i * 3));
					newVertBuffer.put(vbs.vertexBuffer.get(i * 3 + 1));
					newVertBuffer.put(vbs.vertexBuffer.get(i * 3 + 2));
				}

			}
			newValueBuffer.flip();
			newVertBuffer.flip();

			FloatBuffer valueBuffer = vbs.valueBuffer;
			this.valueBufferHandles[index] = ptr[1];

			gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBufferHandles[index]);
			gl.glBufferData(GL_ARRAY_BUFFER, numPts * 2 * 3, newVertBuffer, GL_STATIC_DRAW);

			gl.glBindBuffer(GL_ARRAY_BUFFER, this.valueBufferHandles[index]);
			gl.glBufferData(GL_ARRAY_BUFFER, numPts * 4, newValueBuffer, GL_STATIC_DRAW);
			vbs.scratchPts = numPts;
		}
		else {
			FloatBuffer valueBuffer = vbs.valueBuffer;
			this.valueBufferHandles[index] = ptr[1];

			ShortBuffer vertBuffer = vbs.vertexBuffer;
			gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBufferHandles[index]);
			gl.glBufferData(GL_ARRAY_BUFFER, vertBuffer.capacity() * 2, vertBuffer, GL_STATIC_DRAW);

			gl.glBindBuffer(GL_ARRAY_BUFFER, this.valueBufferHandles[index]);
			gl.glBufferData(GL_ARRAY_BUFFER, valueBuffer.capacity() * 4, valueBuffer, GL_STATIC_DRAW);
			vbs.scratchPts = vbs.numberOfPts;
		}
	}
}
