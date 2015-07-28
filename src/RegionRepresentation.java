import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;


import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayDataInput;

/**
 * A data representation of a region at some fidelity
 * @author chrishawkins
 *
 */
public class RegionRepresentation {
	private int[]buckets;
	private float estMin;
	private float estMax;

	private int numPtsX;
	private int numPtsY;
	private int numPtsZ;
	private int validPts;
	private float fidelity;

	private List<VertexBufferSlice>slices;



	public enum DataType {
		FLOAT, DOUBLE, SHORT, INT, LONG,
	}

	private RegionRepresentation() {

	}

	static int count = 0;

	/**
	 * Removes all values found within the supplied volume
	 * @param volume  The volume to erase normalised to the size of the overall point cloud
	 */
	public void eraseRegion(Volume volume) {
		//--shortify the volume limits
		short minX = (short) (volume.origin.x * Short.MAX_VALUE);
		short maxX = (short) ((volume.origin.x + volume.wd) * Short.MAX_VALUE);

		short minY = (short) (volume.origin.y * Short.MAX_VALUE);
		short maxY = (short) ((volume.origin.y + volume.ht) * Short.MAX_VALUE);

		short minZ = (short) (volume.origin.z * Short.MAX_VALUE);
		short maxZ = (short) ((volume.origin.z + volume.dp) * Short.MAX_VALUE);

		List<VertexBufferSlice>newSlices = new ArrayList<>();
		for (VertexBufferSlice ss : this.slices) {
			//guard against whole slice being outisde bounds

			//--TODO currently assuming nothing about the order of the vertices so not skipping or anything
			ShortBuffer sVerts = ss.vertexBuffer;
			FloatBuffer sValues = ss.valueBuffer;

			ShortBuffer cloneVerts			= ShortBuffer.allocate(ss.numberOfPts * 3);
			FloatBuffer cloneValues 		= FloatBuffer.allocate(ss.numberOfPts * 1);

			int cloneCount = 0;
			for (int i = 0; i < ss.numberOfPts; i++) {
				short x = sVerts.get(i * 3 + 0);
				short y = sVerts.get(i * 3 + 1);
				short z = sVerts.get(i * 3 + 2);
				boolean withinYBounds = y > minY && y < maxY;
				boolean withinZBounds = z > minZ && z < maxZ;
				boolean withinXBounds = x > minX && x < maxX;

				boolean withinSubsection = withinYBounds && withinZBounds && withinXBounds;

				if (withinSubsection) {
					continue;
				}

				float value = sValues.get(i);

				cloneCount++;

				cloneVerts.put(x);
				cloneVerts.put(y);
				cloneVerts.put(z);
				cloneValues.put(value);
			}

			cloneVerts.flip();
			cloneValues.flip();

			VertexBufferSlice newSlice = new VertexBufferSlice();
			newSlice.numberOfPts = cloneCount;
			newSlice.x = ss.x;
			newSlice.vertexBuffer = cloneVerts;
			newSlice.valueBuffer = cloneValues;

			newSlices.add(newSlice);
		}

		this.slices = newSlices;
	}

	/**
	 * Goes through a region representation finding values that could be in subvolume.  Those that are are either moved
	 * or copied from this region representation.
	 * @param volume The volume of the subvolume normalised to the size of the overall poitn cloud
	 * @param replaceValues Whether or not to replace those values eligible for the subregion (true = cut, false = copy)
	 * @return The newly created region representation for the subregion
	 */
	public RegionRepresentation generateSubrepresentation(Volume volume, boolean replaceValues) {
		RegionRepresentation rr = new RegionRepresentation();
		List<VertexBufferSlice>newSlices = new ArrayList<>();

		//--shortify the volume limits
		short minY = (short) (volume.origin.y * Short.MAX_VALUE);
		short maxY = (short) ((volume.origin.y + volume.ht) * Short.MAX_VALUE);

		short minZ = (short) (volume.origin.z * Short.MAX_VALUE);
		short maxZ = (short) ((volume.origin.z + volume.dp) * Short.MAX_VALUE);

		for (VertexBufferSlice ss : this.slices) {

			//guard against whole slice being outisde bounds
			if(ss.x < volume.origin.x) { continue;}
			if(ss.x >= volume.origin.x + volume.wd) { continue;}

			//--TODO currently assuming nothing about the order of the vertices so not skipping or anything
			ShortBuffer sVerts = ss.vertexBuffer;
			FloatBuffer sValues = ss.valueBuffer;

			ShortBuffer subsectionVerts 	= ShortBuffer.allocate(ss.numberOfPts * 3);
			ShortBuffer cloneVerts			= ShortBuffer.allocate(ss.numberOfPts * 3);
			FloatBuffer subsectionValues 	= FloatBuffer.allocate(ss.numberOfPts * 1);
			FloatBuffer cloneValues 		= FloatBuffer.allocate(ss.numberOfPts * 1);

			int subsectionCount = 0;
			int cloneCount = 0;
			for (int i = 0; i < ss.numberOfPts; i++) {
				short y = sVerts.get(i * 3 + 1);
				short z = sVerts.get(i * 3 + 2);
				boolean withinYBounds = y > minY && y < maxY;
				boolean withinZBounds = z > minZ && z < maxZ;
				boolean withinSubsection = withinYBounds && withinZBounds;

				if (!withinSubsection && !replaceValues) {
					continue;
				}

				short x = sVerts.get(i * 3);
				float value = sValues.get(i);


				ShortBuffer destinationVertBuffer = withinSubsection ? subsectionVerts : cloneVerts;
				FloatBuffer destinationValueBuffer = withinSubsection ? subsectionValues : cloneValues;

				if (withinSubsection)
					subsectionCount++;
				else
					cloneCount++;

				destinationVertBuffer.put(x);
				destinationVertBuffer.put(y);
				destinationVertBuffer.put(z);
				destinationValueBuffer.put(value);
			}

			subsectionVerts.flip();
			subsectionValues.flip();
			cloneVerts.flip();
			cloneValues.flip();

			VertexBufferSlice newSlice = new VertexBufferSlice();
			newSlice.numberOfPts = subsectionCount;
			newSlice.x = ss.x;
			newSlice.vertexBuffer = subsectionVerts;
			newSlice.valueBuffer = subsectionValues;

			if (replaceValues) {
				ss.numberOfPts = cloneCount;
				ss.vertexBuffer = cloneVerts;
				ss.valueBuffer = cloneValues;
			}
			newSlices.add(newSlice);
		}
		rr.setFidelity(getFidelity());
		rr.slices = newSlices;
		rr.setNumPtsX((((int)(volume.wd * this.getNumPtsX()))));
		rr.setNumPtsY((((int)(volume.ht * this.getNumPtsY()))));
		rr.setNumPtsZ((((int)(volume.dp * this.getNumPtsZ()))));
		return rr;
	}


	/**
	 *
	 *
	 * @param fits	The fits file to read.
	 * @param fidelity The level of fidelity to read the cube at (note fidelity is interpreted cubically so 1 is 8 times larger than 0.5)
	 * @param volume The unit volume of the fits file to read.
	 * @return A representation of the asked for region.
	 */
	public static RegionRepresentation loadFromDisk(Fits fits, float fidelity, Volume volume) {
		RegionRepresentation rr = new RegionRepresentation();
		rr.setFidelity(fidelity);
		long t0 = System.currentTimeMillis();
		try {
			ImageHDU hdu = (ImageHDU) fits.getHDU(0);
			MinAndMax minAndMax = minAndMaxBasedOnRoughOnePercentRushThrough(hdu, volume, fits);
			rr.setEstMax(minAndMax.max);
			rr.setEstMin(minAndMax.min);

			int sourceWidth = hdu.getAxes()[0];											//the length of the cube in points
			int sourceHeight = hdu.getAxes()[1];											//the height of the cube in points
			int sourceDepth = hdu.getAxes().length > 2? hdu.getAxes()[2] : 1;			//the depth of the cube in points

			int stride = (int)(1.0f/fidelity);												//how many to step in each direction

			int sourceStartX = (int)(volume.x * sourceWidth);							//the first point number x to read
			int sourceStartY = (int)(volume.y * sourceHeight);							//the first point number y to read
			int sourceStartZ = (int)(volume.z * sourceDepth);							//the first point number z to read

			int sourceEndX = (int)((volume.x + volume.wd) * sourceWidth);				//the last point number x to read
			int sourceEndY = (int)((volume.y + volume.ht) * sourceHeight);				//the last point number y to read
			int sourceEndZ = (int)((volume.z + volume.dp) * sourceDepth);				//the last point number z to read

			int repWidth = (sourceEndX - sourceStartX)/stride;
			int repHeight = (sourceEndY - sourceStartY)/stride;
			int repDepth = (sourceEndZ - sourceStartZ)/stride;

			rr.setNumPtsX(repWidth);
			rr.setNumPtsY(repHeight);
			rr.setNumPtsZ(repDepth);

			int yRemainder = sourceHeight - stride*(sourceHeight/stride);

			DataType dataType;
			int bitPix = hdu.getBitPix();
			int typeSize = Math.abs(bitPix)/8;
			float[] storagef = null;
			double[] storaged = null;
			switch (bitPix) {
				case -64:
					dataType = DataType.DOUBLE;
					storaged = new double[sourceDepth];
					break;
				case -32:
					dataType = DataType.FLOAT;
					storagef = new float[sourceDepth];
					break;
				default:
					throw new IOException("Whoops, no support forthat file format (BitPix = "+bitPix+") at the moment.  Floats and Doubles only sorry.");
			}

			int nBuckets = 100;
			rr.setBuckets(new int[nBuckets]);
			float min = minAndMax.min;
			float max = minAndMax.max;
			float stepSize = (max - min) / (float)nBuckets;

			rr.slices = new ArrayList<>();

			Random r = new Random(1);
			float xStride = 1.0f/(float)repWidth;
			float yStride = 1.0f/(float)repHeight;
			float zStride = 1.0f/(float)repDepth;
			if (hdu.getData().reset()) {
				ArrayDataInput adi = fits.getStream();

				//--skip whole planes at the start if the volume doesn't start at zero x
				adi.skipBytes(sourceDepth * sourceHeight * sourceStartX * typeSize);

				for (int x = 0; x < repWidth; x ++) {
					float xProportion = (float)x/(float)repWidth;
					float xPosition = volume.x + volume.wd * xProportion;
					int pts = 0;
					int maxPts = repHeight * repDepth;

					//--skip whole lines at the start if the volume doesn't start at zero ys
					adi.skipBytes(sourceDepth * sourceStartY * typeSize);

					ShortBuffer vertexBuffer = ShortBuffer.allocate(maxPts * 3);
					FloatBuffer valueBuffer = FloatBuffer.allocate(maxPts);

					for (int y = 0; y < repHeight; y ++) {
						float yProportion = (float)y/(float)repHeight;
						float yPosition = volume.y + volume.ht * yProportion;

						if (dataType == DataType.DOUBLE)
							adi.read(storaged, 0, storaged.length);
						else if (dataType == DataType.FLOAT)
							adi.read(storagef, 0, storagef.length);

						for (int z = 0; z < repDepth; z++) {
							float zProportion = (float)z/(float)repDepth;
							float zPosition = volume.z + volume.dp * zProportion;

							float val;
							if (dataType == DataType.DOUBLE) {
								val = (float)storaged[sourceStartZ + z * stride];
							} else {
								val = storagef[sourceStartZ + z * stride];
							}
							int bucketIndex = (int)(val - rr.getEstMin() /stepSize);
							if (bucketIndex >= 0 && bucketIndex < nBuckets && !Double.isNaN(val)){	//--TODO should the buckets really be stopping points from being added ???
								rr.getBuckets()[bucketIndex]++;

								float fudge = r.nextFloat();
								fudge = fudge - 0.5f;

								vertexBuffer.put((short) ((xPosition + fudge * xStride) * Short.MAX_VALUE));
								vertexBuffer.put((short) ((yPosition + fudge * yStride) * Short.MAX_VALUE));
								vertexBuffer.put((short) ((zPosition + fudge * zStride) * Short.MAX_VALUE));
								valueBuffer.put(val);
								pts++;
							}
						}

						//--stride over to the next line
						int linesToSkip = stride - 1;
						adi.skipBytes(sourceDepth * linesToSkip * typeSize);
					}
					//--skip to end of slice
					int currentLine = stride * repHeight + sourceStartY;
					int linesToSkip = sourceHeight - currentLine;
					adi.skip(sourceDepth * linesToSkip * typeSize);

					//--make a vbo slice
					vertexBuffer.flip();
					valueBuffer.flip();

					VertexBufferSlice vbs = new VertexBufferSlice();
					vbs.vertexBuffer = vertexBuffer;
					vbs.valueBuffer = valueBuffer;
					vbs.numberOfPts = pts;
					vbs.x = xProportion;
					rr.slices.add(vbs);

					//--skip to the next slice
					adi.skipBytes(sourceDepth * sourceHeight * (stride - 1) * typeSize);
				}
			}

			System.out.println("fits file loaded " + repWidth + " x " + repHeight + " x " + repDepth);

		}catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getClass().getName()+": " + e.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

		long t1 = System.currentTimeMillis();
		System.out.println("time taken to load file in new fancy way:" + (t1 - t0));
		return rr;
	}






	/**
	 * Makes a rough estimate of the point cloud's minimum and maximum values present.  It does this by taking a 1:1000
	 * sampling of points
	 *
	 * @param hdu  The HDU to read the data from
	 * @param volume The volume of the data to read normalised to the volume of the overall point cloud
	 * @param fits The fits file to read the data from
	 * @return The minimum and maximum values found within the point cloud
	 */
	private static MinAndMax minAndMaxBasedOnRoughOnePercentRushThrough(ImageHDU hdu, Volume volume, Fits fits) {
		MinAndMax mam = new MinAndMax();

		long t0 = System.currentTimeMillis();
		float minn = 999f;
		float maxx = -999f;

		List<Float>allOfThemFloats =new ArrayList<Float>();
		try{
			float shittyFidelity = 0.1f;

			int sourceMaxWidth = hdu.getAxes()[0];
			int sourceMaxHeight = hdu.getAxes()[1];
			int sourceMaxDepth = hdu.getAxes().length > 2? hdu.getAxes()[2] : 0;

			//stirde of 1 = full fidelity , stride of 2 = half fidelity
			int stride = (int)(1.0f/shittyFidelity);
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

			int numPtsX = maxWidth;
			int numPtsY = maxHeight;
			int numPtsZ = maxDepth;

			int yRemainder = sourceMaxHeight - stride*(sourceMaxHeight/stride);

			DataType dataType;
			int bitPix = hdu.getBitPix();
			int typeSize = Math.abs(bitPix)/8;
			float[] storagef = null;
			double[] storaged = null;
			switch (bitPix) {
				case -64:
					dataType = DataType.DOUBLE;
					storaged = new double[sourceMaxDepth];
					break;
				case -32:
					dataType = DataType.FLOAT;
					storagef = new float[sourceMaxDepth];
					break;
				default:
					throw new IOException("Whoops, no support forthat file format (BitPix = "+bitPix+") at the moment.  Floats and Doubles only sorry.");
			}


			if (hdu.getData().reset()) {

				ArrayDataInput adi = fits.getStream();
				int planesToSkip = sourceStartX;
				adi.skipBytes(sourceMaxDepth * sourceMaxHeight * planesToSkip * typeSize);

				for (int x = 0; x < maxWidth; x ++) {
					for (int y = 0; y < maxHeight; y ++) {
						if (dataType == DataType.DOUBLE) {
							adi.read(storaged, 0, storaged.length);
							for (int z = 0; z < maxDepth; z++) {
								float val = (float)storaged[z * stride];
								if (Double.isNaN(val))
									continue;
								if (val < minn) minn = val;
								if (val > maxx) maxx = val;

								allOfThemFloats.add(val);
							}
						}
						else if (dataType == DataType.FLOAT) {
							adi.read(storagef, 0, storagef.length);
							for (int z = 0; z < maxDepth; z++) {
								float val = (float)storagef[z * stride];
								if (Float.isNaN(val))
									continue;
								if (val < minn) minn = val;
								if (val > maxx) maxx = val;
								allOfThemFloats.add(val);
							}
						}

						if (y == maxHeight-1 && yRemainder!=0) {
							//is remainder zone
							int linesToSkip = yRemainder + stride - 1;
							adi.skipBytes(sourceMaxDepth * linesToSkip * typeSize );
						} else {
							int linesToSkip = stride - 1;
							adi.skipBytes(sourceMaxDepth * linesToSkip * typeSize);
						}
					}
					adi.skipBytes(sourceMaxDepth * sourceMaxHeight * (stride - 1) * typeSize);
				}
			}

			System.out.println("fits file loaded " + maxWidth + " x " + maxHeight + " x " + maxDepth);
			System.out.println("min" + minn);
			System.out.println("max" + maxx);
			allOfThemFloats.sort(new Comparator<Float>() {
				@Override
				public int compare(Float o1, Float o2) {
					return Float.compare(o1.floatValue(), o2.floatValue());
				}
			});


		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("time taken to estimate min and max :" + (System.currentTimeMillis() - t0) + "ms");
		mam.min = minn;
		mam.max = maxx;
		return mam;
	}


	private static class MinAndMax{
		float min,max;
	}







	//==================================================================================================================
	//  GETTERS + SETTERS
	//==================================================================================================================

	public List<VertexBufferSlice> getSlices() {
		return this.slices;
	}

	public int[] getBuckets() {
		return buckets;
	}

	public void setBuckets(int[] buckets) {
		this.buckets = buckets;
	}

	public float getEstMin() {
		return estMin;
	}

	public void setEstMin(float estMin) {
		this.estMin = estMin;
	}

	public float getEstMax() {
		return estMax;
	}

	public void setEstMax(float estMax) {
		this.estMax = estMax;
	}

	public int getNumPtsX() {
		return numPtsX;
	}

	public void setNumPtsX(int numPtsX) {
		this.numPtsX = numPtsX;
	}

	public int getNumPtsY() {
		return numPtsY;
	}

	public void setNumPtsY(int numPtsY) {
		this.numPtsY = numPtsY;
	}

	public int getNumPtsZ() {
		return numPtsZ;
	}

	public void setNumPtsZ(int numPtsZ) {
		this.numPtsZ = numPtsZ;
	}

	public int getValidPts() {
		return validPts;
	}

	public void setValidPts(int validPts) {
		this.validPts = validPts;
	}

	public float getFidelity() {
		return fidelity;
	}

	public void setFidelity(float fidelity) {
		this.fidelity = fidelity;
	}

}