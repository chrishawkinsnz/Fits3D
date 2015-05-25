import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

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
	
//	public FloatBuffer vertexBuffer;
//	public FloatBuffer valueBuffer;
	
	private List<VertexBufferSlice>slices;
	private int seed;
	
	/**
	 * 
	 * @param fileName Filename of the original fits file
	 * @param fidelity Fidelity of the represenation (1 being perfect, 0 being emtpy)
	 * @param volume a volume cube indicating the area of the data to sample (full sample is == new volume(0,0,0,1,1,1));
	 */
	public RegionRepresentation(Fits fits, float fidelity, Volume volume) {
		this.fidelity = fidelity;
		if (fidelity >=1.0) {
			isMaximumFidelity = true;
		}
		this.seed = new Random().nextInt();
		long t0 = System.currentTimeMillis();
		try{
			ImageHDU hdu = (ImageHDU) fits.getHDU(0);
			

			int sourceMaxWidth = hdu.getAxes()[0];
			int sourceMaxHeight = hdu.getAxes()[1];
			int sourceMaxDepth = hdu.getAxes()[2];
			
			//stirde of 1 = full fidelity , stride of 2 = half fidelity
			int stride = (int)(1.0f/fidelity);
			System.out.println("stride:"+stride);
			
			
			int sourceStartX = (int)(volume.x * sourceMaxWidth);
			int sourceStartY = (int)(volume.y * sourceMaxHeight);
			int sourceStartZ = (int)(volume.z * sourceMaxDepth);
			
			int sourceEndX = (int)((volume.x + volume.wd) * sourceMaxWidth);
			int sourceEndY = (int)((volume.y + volume.ht) * sourceMaxHeight);
			int sourceEndZ = (int)((volume.z + volume.dp) * sourceMaxDepth);
			
			int maxWidth = (sourceEndX - sourceStartX)/stride;
			int maxHeight = (sourceEndY - sourceStartY)/stride;
			int maxDepth = (sourceEndZ - sourceStartZ)/stride;
			
			this.numPtsX = maxWidth;
			this.numPtsY = maxHeight;
			this.numPtsZ = maxDepth;
			
			int yRemainder = sourceMaxHeight - stride*(sourceMaxHeight/stride);

			this.data = new float[maxWidth][maxHeight][maxDepth];
			
			float[] storage = new float[sourceMaxDepth];
			if (hdu.getData().reset()) {
				ArrayDataInput adi = fits.getStream();
				int planesToSkip = sourceStartX;
				adi.skipBytes(sourceMaxDepth * sourceMaxHeight * planesToSkip * 4);

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
	}
	
	
	private VertexBufferSlice vertexAndValueBufferForSlice(float zProportion) {
		Random r = new Random(this.seed);
		long t0 = System.currentTimeMillis();
		float[] vertexData = new float[this.numPtsX * this.numPtsY * 3 * 1];
		float[] valueData = new float[this.numPtsX * this.numPtsY * 1 * 1];
		
		float xStride = 1.0f/(float)this.numPtsX;
		float yStride = 1.0f/(float)this.numPtsY;
		float zStride = 1.0f/(float)this.numPtsZ;
		
		int pts = 0;
		float z =  zProportion;
		
		for (float y = 0.0f; y < 1.0f; y += yStride) {
			for (float x = 0.0f; x < 1.0f; x += xStride) {
				float value = data[(int)(x * this.numPtsX)][(int)(y * this.numPtsY)][(int)(z * this.numPtsZ)];
				if (!Float.isNaN(value) && value > 0.0f) {
					float fudge = r.nextFloat();
					fudge = fudge - 0.5f;
					vertexData[pts * 3 + 0] = x;// + fudge * xStride;
					vertexData[pts * 3 + 1] = y;// + fudge * yStride;;
					vertexData[pts * 3 + 2] = z;// + fudge * zStride;;
				
					valueData[pts] = value;
					pts++;
				}
			}	
		}
		
		System.out.println("Number of Points: "+pts);
		FloatBuffer vertexBuffer = FloatBuffer.allocate(pts * 3);
		vertexBuffer.put(vertexData, 0, pts * 3);
		vertexBuffer.flip();
		
		FloatBuffer valueBuffer = FloatBuffer.allocate(pts);
		valueBuffer.put(valueData, 0, pts);
		valueBuffer.flip();
		long t1 = System.currentTimeMillis();
		System.out.println("took "+(t1-t0) + " ms to load into buffers");
		
		VertexBufferSlice vbs = new VertexBufferSlice();
		vbs.vertexBuffer = vertexBuffer;
		vbs.valueBuffer = valueBuffer;
		vbs.numberOfPts = pts;
		vbs.depthValue = zProportion;
		return vbs;
	}
	
	public List<VertexBufferSlice>getSlices() {
		if (this.slices == null) {
			this.slices = new ArrayList<VertexBufferSlice>();
			float zStride = 1.0f/(float)this.numPtsZ;
			for (float z = 0.0f; z < 1.0f; z += zStride) {
				VertexBufferSlice vbs = vertexAndValueBufferForSlice(z);
				this.slices.add(vbs);
			}
		}
		return this.slices;
	}
}
