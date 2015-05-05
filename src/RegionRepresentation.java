import java.io.IOException;
import java.nio.FloatBuffer;

import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayDataInput;

/**
 * A data representation of a region at some fidelity
 * @author chrishawkins
 *
 */
public class RegionRepresentation {
	private float[][][] data;
	public int numPtsX;
	public int numPtsY;
	public int numPtsZ;
	public int validPts;
	public boolean isMaximumFidelity;
	public float fidelity;
	
	public FloatBuffer vertexBuffer;
	public FloatBuffer valueBuffer;
	
	/**
	 * 
	 * @param fileName Filename of the original fits file
	 * @param fidelity Fidelity of the represenation (1 being perfect, 0 being emtpy)
	 */
	public RegionRepresentation(Fits fits, float fidelity) {
		this.fidelity = fidelity;
		if (fidelity >=1.0) {
			isMaximumFidelity = true;
		}
		
		long t0 = System.currentTimeMillis();
		try{
			ImageHDU hdu = (ImageHDU) fits.getHDU(0);
			
			int maxWidth = hdu.getAxes()[0];
			int maxHeight = hdu.getAxes()[1];
			int maxDepth = hdu.getAxes()[2];
			
			int sourceMaxWidth = hdu.getAxes()[0];
			int sourceMaxHeight = hdu.getAxes()[1];
			int sourceMaxDepth = hdu.getAxes()[2];
			
			//stirde of 1 = full fidelity , stride of 2 = half fidelity
			int stride = (int)(1.0f/fidelity);
			System.out.println("stride:"+stride);
			
			
			int yRemainder = maxHeight - stride*(maxHeight/stride);
			System.out.println("Remainder:"+yRemainder);
			
			maxWidth = maxWidth/stride;
			maxHeight = maxHeight/stride;
			maxDepth = maxDepth/stride;
			
			this.numPtsX = maxWidth;
			this.numPtsY = maxHeight;
			this.numPtsZ = maxDepth;
			this.data = new float[maxWidth][maxHeight][maxDepth];
			
			float[] storage = new float[sourceMaxDepth];
			if (hdu.getData().reset()) {
				ArrayDataInput adi = fits.getStream();
				for (int x = 0; x < maxWidth; x ++) {
					for (int y = 0; y < maxHeight; y ++) {
						
						adi.read(storage, 0, storage.length);
						for (int z = 0; z < maxDepth; z++) {
							data[x][y][z] = storage[z * stride];
						}
						if (y == maxHeight-1 && yRemainder!=0) {
							//is remainder zone
							int linesToSkip = yRemainder + stride - 1;
							adi.skipBytes(sourceMaxDepth * linesToSkip * 4 );
						} else {
							int linesToSkip = stride - 1;
							adi.skipBytes(sourceMaxDepth * linesToSkip * 4);	
						}
					}
					adi.skipBytes(sourceMaxDepth * sourceMaxHeight * (stride - 1) * 4);
				}
			}

			System.out.println("fits file loaded " + maxWidth + " x " + maxHeight + " x " + maxDepth);
		} catch (FitsException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long t1 = System.currentTimeMillis();
		System.out.println("took "+(t1-t0) + " ms to read in data");
		
		
		loadNormalizedBuffer();
	}
	
	private void loadNormalizedBuffer() {
			long t0 = System.currentTimeMillis();
			float[] vertexData = new float[this.numPtsX * this.numPtsY * this.numPtsZ * 3];
			float[] valueData = new float[this.numPtsX * this.numPtsY * this.numPtsZ];
			
			float xStride = 1.0f/(float)this.numPtsX;
			float yStride = 1.0f/(float)this.numPtsY;
			float zStride = 1.0f/(float)this.numPtsZ;
			
			int pts = 0;
			for (float zProportion = 0.0f; zProportion < 1.0f; zProportion += zStride) {
				float z =  zProportion;
				
				for (float yProportion = 0.0f; yProportion < 1.0f; yProportion += yStride) {
					float y =  yProportion;
					
					for (float xProportion = 0.0f; xProportion < 1.0f; xProportion += xStride) {
						
						float x = xProportion;
						
						float value = data[(int)(xProportion * this.numPtsX)][(int)(yProportion * this.numPtsY)][(int)(zProportion * this.numPtsZ)];
						if (!Float.isNaN(value) && value > 0.0f) {
							
							vertexData[pts * 3 + 0] = x;
							vertexData[pts * 3 + 1] = y;
							vertexData[pts * 3 + 2] = z;
						
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
	}
	
}
