import java.nio.FloatBuffer;

import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

public class PointCloud {
	public FloatBuffer vertexBuffer;
	public FloatBuffer valueBuffer;
	
	public int width;
	public int height;
	public int depth;
	
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
 
	
	public PointCloud(String pathName) {
		this.fileName = pathName;
	}
	
	public void readFits() {
		try{
			this.fits = new Fits(this.fileName);
			ImageHDU hdu = (ImageHDU) this.fits.getHDU(0);
			this.data = (float [][][]) hdu.getKernel();
			this.width = data.length;
			this.height = data[0].length;
			this.depth = data[0][0].length;
			
			System.out.println("fits file loaded " + this.width + " x " + this.height + " x " + this.depth);
		} catch (FitsException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadFloatBuffers() {
		float[] vertexData = new float[this.width * this.height * this.depth * 3];
		float[] valueData = new float[this.width * this.height * this.depth];
		
		Random random = new Random(1);
		int pts = 0;
		int nanPts = 0;
		for (int zindex = 0; zindex < this.width; zindex++) {
			for (int yindex = 0; yindex < this.height; yindex++) {
				for (int xindex = 0; xindex < this.depth; xindex++) {						
					float value = data[zindex][yindex][xindex];
					if (!Float.isNaN(value) && value > 0.0f) {
						float xProportion = (float)xindex /(float)this.depth;
						float yProportion = (float)yindex /(float)this.height;
						float zProportion = (float)zindex /(float)this.width;
						
						float x = boxOrigX + xProportion * boxWidth;
						float y = boxOrigY + yProportion * boxHeight;
						float z = boxOrigZ + zProportion * boxDepth;

						float fluff = random.nextFloat() *0.02f - 0.01f; //this is to stop awkward aligning of poitns

						vertexData[pts * 3 + 0] = x + fluff;
						vertexData[pts * 3 + 1] = y + fluff;
						vertexData[pts * 3 + 2] = z + fluff;
						
						valueData[pts] = value;
						pts++;						
					}
					else {
						nanPts++;
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
	}
}
