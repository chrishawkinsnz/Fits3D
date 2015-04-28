import java.nio.FloatBuffer;

import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedDataOutputStream;

import java.io.IOException;
import java.util.Random;

public class PointCloud {
	public final static boolean shouldFudge = false;
	
	public FloatBuffer vertexBuffer;
	public FloatBuffer valueBuffer;
	
	public int bufferWidth;
	public int bufferHeight;
	public int bufferDepth;
	
	private int maxWidth;
	private  int maxHeight;
	private int maxDepth;
	
	public int validPts;
	
	private String fileName;
	
	private float[][][] data;
	private Fits fits;
	
	float boxWidth = 1.0f;
	float boxHeight = boxWidth;
	float boxDepth = boxWidth;

	float boxOrigZ = -.5f;
	float boxOrigX = -0.5f * boxWidth;
	float boxOrigY = -0.5f * boxHeight;

	private ImageHDU hdu;
 
	
	public PointCloud(String pathName) {
		this.fileName = pathName;
	}
	public void readFitsAtQualityLevel(float proportionOfPerfect) {
		long t0 = System.currentTimeMillis();
		try{
			this.fits = new Fits(this.fileName);
			this.hdu = (ImageHDU) this.fits.getHDU(0);
//			this.data = (float [][][]) hdu.getKernel();
			
			this.maxWidth = this.hdu.getAxes()[0];
			this.maxHeight = this.hdu.getAxes()[1];
			this.maxDepth = this.hdu.getAxes()[2];
			
			int realMaxWidth = this.hdu.getAxes()[0];
			int realMaxHeight = this.hdu.getAxes()[1];
			System.out.println(realMaxHeight);
			int realMaxDepth = this.hdu.getAxes()[2];
			
			int stride = (int)(1.0f/proportionOfPerfect);
			System.out.println("stride:"+stride);
			
			
			int yRemainder = this.maxHeight - stride*(this.maxHeight/stride);
			System.out.println("Remainder:"+yRemainder);
			
			this.maxWidth = this.maxWidth/stride;
			this.maxHeight = this.maxHeight/stride;
			this.maxDepth = this.maxDepth/stride;
			
			this.data = new float[maxWidth][maxHeight][maxDepth];
			
			float[] storage = new float[realMaxDepth];
			if (hdu.getData().reset()) {
				ArrayDataInput adi = fits.getStream();
				for (int x = 0; x < this.maxWidth; x ++) {
					for (int y = 0; y < this.maxHeight; y ++) {
						
						adi.read(storage, 0, storage.length);
						for (int z = 0; z < this.maxDepth; z++) {
							data[x][y][z] = storage[z * stride];
						}
						if (y==this.maxHeight-1 && yRemainder!=0) {
							//is remainder zone
							int linesToSkip = yRemainder + stride - 1;
							adi.skipBytes(realMaxDepth * linesToSkip * 4 );
						} else {
							int linesToSkip = stride - 1;
							adi.skipBytes(realMaxDepth * linesToSkip * 4);	
						}
					}
					adi.skipBytes(realMaxDepth * realMaxHeight * (stride - 1) * 4);
				}
			}

			System.out.println("fits file loaded " + this.maxWidth + " x " + this.maxHeight + " x " + this.maxDepth);
		} catch (FitsException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long t1 = System.currentTimeMillis();
		System.out.println("took "+(t1-t0) + " ms to read in data");
	}
	
	
	
	public void loadFloatBuffersWithDimensions(int width, int height, int depth) {
		long t0 = System.currentTimeMillis();
		float[] vertexData = new float[width * height * depth * 3];
		float[] valueData = new float[width * height * depth];
		
//		Random random = new Random(1);
		float xStride = 1.0f/(float)width;
		float yStride = 1.0f/(float)height;
		float zStride = 1.0f/(float)depth;
		
		int pts = 0;
		for (float zProportion = 0.0f; zProportion < 1.0f; zProportion += zStride) {
			float z = boxOrigZ + zProportion * boxDepth;
			
			for (float yProportion = 0.0f; yProportion < 1.0f; yProportion += yStride) {
				float y = boxOrigY + yProportion * boxHeight;
				
				for (float xProportion = 0.0f; xProportion < 1.0f; xProportion += xStride) {
					
					float x = boxOrigX + xProportion * boxWidth;
					
					float value = data[(int)(xProportion * this.maxWidth)][(int)(yProportion * this.maxHeight)][(int)(zProportion * this.maxDepth)];
					if (!Float.isNaN(value) && value > 0.0f) {
						
                        //float fluff = random.nextFloat() *0.02f - 0.01f; //this is to stop awkward aligning of poitns
						vertexData[pts * 3 + 0] = x;// + fluff;
						vertexData[pts * 3 + 1] = y;// + fluff;
						vertexData[pts * 3 + 2] = z;// + fluff;
					
						valueData[pts] = value;
						pts++;
					}
				}	
			}
		}

		System.out.println("Number of Points: "+pts);
		this.validPts = pts;
		this.vertexBuffer = FloatBuffer.allocate(pts * 3);
		this.vertexBuffer.put(vertexData, 0, pts * 3);
		this.vertexBuffer.flip();
		
		this.valueBuffer = FloatBuffer.allocate(pts);
		this.valueBuffer.put(valueData, 0, pts);
		this.valueBuffer.flip();
		long t1 = System.currentTimeMillis();
		System.out.println("took "+(t1-t0) + " ms to load into buffers");
		
		this.bufferDepth = depth;
		this.bufferHeight = height;
		this.bufferWidth = width;
	}
	
	public void loadFloatBuffers() {
//		loadFloatBuffersWithDimensions(20, 20, 20);
		loadFloatBuffersWithDimensions(this.maxWidth, this.maxHeight, this.maxDepth);
		
		
		
//		long t0 = System.currentTimeMillis();
//		float[] vertexData = new float[this.maxWidth * this.maxHeight * this.maxDepth * 3];
//		float[] valueData = new float[this.maxWidth * this.maxHeight * this.maxDepth];
//		
//		Random random = new Random(1);
//		int pts = 0;
//		for (int zindex = 0; zindex < this.maxWidth; zindex++) {
//			
//			float zProportion = (float)zindex /(float)this.maxWidth;
//			float z = boxOrigZ + zProportion * boxDepth;
//			
//			for (int yindex = 0; yindex < this.maxDepth; yindex++) {
//			
//				float yProportion = (float)yindex /(float)this.maxHeight;
//				float y = boxOrigY + yProportion * boxHeight;
//				
//				for (int xindex = 0; xindex < this.maxDepth; xindex++) {						
//					float value = data[xindex][yindex][zindex];
//					if (!Float.isNaN(value) && value > 0.0f) {
//						float xProportion = (float)xindex /(float)this.maxDepth;
//						float x = boxOrigX + xProportion * boxWidth;
//
////						float fluff = random.nextFloat() *0.02f - 0.01f; //this is to stop awkward aligning of poitns
////						if (shouldFudge == false) { fluff = 0.0f;}
//						
//						vertexData[pts * 3 + 0] = x;// + fluff;
//						vertexData[pts * 3 + 1] = y;// + fluff;
//						vertexData[pts * 3 + 2] = z;// + fluff;
//						
//						valueData[pts] = value;
//						pts++;						
//					}
//
//				}
//			}
//		}
//		
//		System.out.println("Number of Points: "+pts);
//		this.validPts = pts;
//		this.vertexBuffer = FloatBuffer.allocate(pts * 3);
//		this.vertexBuffer.put(vertexData, 0, pts * 3);
//		this.vertexBuffer.flip();
//		
//		this.valueBuffer = FloatBuffer.allocate(pts);
//		this.valueBuffer.put(valueData, 0, pts);
//		this.valueBuffer.flip();
//		long t1 = System.currentTimeMillis();
//		System.out.println("took "+(t1-t0) + " ms to load into buffers");
	}
}
