import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.*;


import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayDataInput;

/**
 * A data representation of a region at some fidelity
 * @author chrishawkins
 *
 */
public class RegionRepresentation {
	public static boolean shouldFudge = true;
	public static boolean shouldScaleCells = true;
	public static boolean fakeFourthDimension = false;
	public static boolean exaggerateCellScaling = false;

	private int[]buckets;
	private float estMin;
	private float estMax;

	private int numPtsW;
	private int numPtsX;
	private int numPtsY;
	private int numPtsZ;

	private int validPts;
	private float fidelity;

	private  boolean isLiableToOverflow = false;

	public static JSlider sliderToEnable = null;

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
			newSlice.z = ss.z;
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

		short minX = (short) (volume.origin.x * Short.MAX_VALUE);
		short maxX = (short) ((volume.origin.x + volume.wd) * Short.MAX_VALUE);

		for (VertexBufferSlice ss : this.slices) {

			//guard against whole slice being outisde bounds
			if(ss.z < volume.origin.z) { continue;}/**/
			if(ss.z >= volume.origin.z + volume.dp) { continue;}

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
				short x = sVerts.get(i * 3);
				boolean withinYBounds = y > minY && y < maxY;
				boolean withinXBounds = x > minX && x < maxX;
				boolean withinSubsection = withinYBounds && withinXBounds;

				if (!withinSubsection && !replaceValues) {
					continue;
				}

				short z = sVerts.get(i * 3 + 2);
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
			newSlice.z = ss.z;
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

	public static int numLoading = 0;
	public static boolean currentlyLoading = false;
	/**
	 *
	 *
	 * @param fits	The fits file to read.
	 * @param fidelity The level of fidelity to read the cube at (note fidelity is interpreted cubically so 1 is 8 times larger than 0.5)
	 * @param volume The unit volume of the fits file to read.
	 * @return A representation of the asked for region.
	 */
	public static RegionRepresentation loadFromDisk(Fits fits, float fidelity, Volume volume, boolean dummyRun) {
		if (!dummyRun) {
			currentlyLoading = true;
			numLoading++;
		}
		RegionRepresentation rr = new RegionRepresentation();
		rr.setFidelity(fidelity);
		long t0 = System.currentTimeMillis();
		try {

			ImageHDU hdu = (ImageHDU) fits.getHDU(0);


			MinAndMax minAndMax = new MinAndMax();
			if (!dummyRun) {
			 	minAndMax = minAndMaxBasedOnRoughOnePercentRushThrough(hdu, volume, fits);
			}
			else {
				minAndMax.min = 999f;
				minAndMax.max = -999f;
			}
			rr.setEstMax(minAndMax.max);
			rr.setEstMin(minAndMax.min);

			int naxis = hdu.getAxes().length;
			int stride = (int)(1.0f/fidelity);												//how many to step in each direction

			int[]sourceLengths	 = new int[4];
			int[]sourceStarts	 = new int[4];
			int[]sourceEnds		 = new int[4];
			int[]repLengths		 = new int[4];
			int[]crpix			 = new int[4];
			float[]centerPos 	 = new float[4];
			float[]strides		 = new float[4];
			int shortOnAxes = 4 - hdu.getAxes().length;
			float[]fudges = new float[4];

			//--first load uninitialised dimensions with defaults
			for (int i = 0; i < shortOnAxes; i++) {
				sourceLengths[i] = 1;
				sourceStarts[i]  = 0;
				sourceEnds[i] 	 = 1;
				repLengths[i] 	 = 1;
				strides[i] 		 = 1;
				crpix[i] 		 = 0;
				centerPos[i]	 = 0f;
				rr.setNumPts(3-i, repLengths[i]);
			}
			//--then load the remaining genuine values
			for (int i = shortOnAxes; i < 4; i++) {

				sourceLengths[i] = hdu.getAxes()[i - shortOnAxes];
				float proportionalStart = i == 0 ? 0f : volume.origin.get(3- i);
				float proportionalEnd   = i == 0 ? 1f : volume.origin.get(3- i ) + volume.size.get(3 - i);

				sourceStarts[i]  = (int)(proportionalStart * sourceLengths[i]);
				sourceEnds[i] 	 = (int)(proportionalEnd * sourceLengths[i]);
				repLengths[i]	 = (sourceEnds[i] - sourceStarts[i])/stride;
				strides[i]		 = 1.0f/(float)repLengths[i];
				fudges[i]		 = volume.size.get(3-i)/(float)repLengths[i];

				String crpixKey  = "CRPIX"+ (4-i);
				crpix[i]		 = (int)hdu.getHeader().getFloatValue(crpixKey);
				centerPos[i]	 = volume.origin.get(3-i) + (((float)(crpix[i] - sourceStarts[i]) / (float)sourceLengths[i]) * volume.size.get(3-i));

				rr.setNumPts(3-i , repLengths[i]);
			}




			boolean inverseFrequencyCellScaling = false;
			String cellscal = hdu.getHeader().getStringValue("CELLSCAL");
			float scalingFactor = -0.5f;

			if (exaggerateCellScaling) {
				scalingFactor *= 100f;
			}

			float baseFrequency = volume.origin.z;

			int zDim = shortOnAxes > 0 ? 0 : 1;
			float baseProportion = ((float)crpix[zDim]) / ((float)sourceLengths[zDim]);
			float baseZPosition = baseProportion * volume.dp + volume.z;

			int zPixels = sourceLengths[zDim];
			float zBaseFreq = hdu.getHeader().getFloatValue("CRVAL3");
			float zCDELT = hdu.getHeader().getFloatValue("CDELT3");
			float zBasePix = hdu.getHeader().getFloatValue("CRPIX3");




			//--if the frequency decreases with z then the baseFrequency is actually the other end of the cube
			if (cellscal != null && cellscal.contains("1/F")) {
				inverseFrequencyCellScaling = true;
				rr.isLiableToOverflow = true;
			}

			int pointCount = 0;



			if (fakeFourthDimension) {
				rr.setNumPts(3, repLengths[1]);
			}


			//In this context
			//0 = w
			//1 = z
			//2 = y
			//3 = z


			int yRemainder = sourceLengths[2] - stride*(sourceLengths[2]/stride);

			DataType dataType = null;
			if 		(hdu.getBitPix() == -64) dataType = DataType.DOUBLE;
			else if (hdu.getBitPix() == -32) dataType = DataType.FLOAT;
			else throw new IOException("Whoops, no support forthat file format (BitPix = "+hdu.getBitPix()+") at the moment.  Floats and Doubles only sorry.");

			int typeSize = Math.abs(hdu.getBitPix())/8;
			float[] storagef = new float[sourceLengths[3]];
			double[] storaged = new double[sourceLengths[3]];

			int nBuckets = 100;
			rr.setBuckets(new int[nBuckets]);
			float min = minAndMax.min;
			float max = minAndMax.max;
			float stepSize = (max - min) / (float)nBuckets;


			rr.slices = new ArrayList<>();

			Random r = new Random(1);

			if (hdu.getData().reset()) {
				ArrayDataInput adi = fits.getStream();

				//--skip whole planes at the start if the volume doesn't start at zero z
				adi.skipBytes(sourceLengths[3] * sourceLengths[2] * sourceStarts[1] * typeSize);
				int[] indices = new int[4];
				float[] position = new float[4];

				for (indices[0] = 0; indices[0] < repLengths[0]; indices[0]++) {
					float proportion = (float) indices[0] / (float) repLengths[0];

					for (indices[1] = 0; indices[1] < repLengths[1]; indices[1]++) {
						proportion = (float) indices[1] / (float) repLengths[1];
						position[1] = volume.z + volume.dp * proportion;
						//--figure out the frequency at this position (only appropriate if cell scaling)
						float pixelDiff = (position[1] * (float)zPixels) - zBasePix;
						float currFreq = zBaseFreq + pixelDiff * zCDELT;
						float proportionalCellSizeIncrease = (1/currFreq ) / (1/zBaseFreq);
						proportionalCellSizeIncrease -= 1.0f;
						if (exaggerateCellScaling) {
							proportionalCellSizeIncrease *= 10f;
						}

						int pts = 0;
						int maxPts = repLengths[2] * repLengths[3];

						//--skip whole lines at the start if the volume doesn't start at zero ys
						adi.skipBytes(sourceLengths[3] * sourceStarts[2] * typeSize);

						ShortBuffer vertexBuffer = ShortBuffer.allocate(maxPts * 3);
						FloatBuffer valueBuffer = FloatBuffer.allocate(maxPts);

						for (indices[2] = 0; indices[2] < repLengths[2]; indices[2]++) {
							proportion = (float) indices[2] / (float) repLengths[2];
							position[2] = volume.y + volume.ht * proportion;

							//--cellscall buuuuulllllshit
							if (inverseFrequencyCellScaling) {
								float xyPlaneDiff = position[2] - centerPos[2];
								position[2] += proportionalCellSizeIncrease * xyPlaneDiff;

							}

							if (dataType == DataType.DOUBLE)
								adi.read(storaged, 0, storaged.length);
							else if (dataType == DataType.FLOAT)
								adi.read(storagef, 0, storagef.length);

							for (indices[3] = 0; indices[3] < repLengths[3]; indices[3]++) {
								proportion = (float) indices[3] / (float) repLengths[3];
								position[3] = volume.x + volume.wd * proportion;

								float val;

								//--cellscall buuuuulllllshit
								if (inverseFrequencyCellScaling) {
									float xyPlaneDiff = position[3] - centerPos[3];
									position[3] += proportionalCellSizeIncrease * xyPlaneDiff;
								}

								if (dataType == DataType.DOUBLE) {
									val = (float) storaged[sourceStarts[3] + indices[3] * stride];
								} else {
									val = storagef[sourceStarts[3] + indices[3] * stride];
								}
								int bucketIndex = (int) (val - rr.getEstMin() / stepSize);
								if (bucketIndex >= 0 && bucketIndex < nBuckets && !Double.isNaN(val)) {    //--TODO should the buckets really be stopping points from being added ???
									rr.getBuckets()[bucketIndex]++;

									pointCount ++;
									if (dummyRun) {
										if (val < rr.estMin)
											rr.estMin = val;
										if (val > rr.estMax)
											rr.estMax = val;
									}

									for (int i = 3; i > 0; i--) {
										float fudge =  shouldFudge ? r.nextFloat() - 0.5f : 0.0f;
										vertexBuffer.put((short) ((position[i] + fudge * fudges[i]) * Short.MAX_VALUE));
									}


									valueBuffer.put(val);
									pts++;
								}
							}

							//--stride over to the next line
							int linesToSkip = stride - 1;
							adi.skipBytes(sourceLengths[3] * linesToSkip * typeSize);
						}
						//--skip to end of slice
						int currentLine = stride * repLengths[2] + sourceStarts[2];
						int linesToSkip = sourceLengths[2] - currentLine;
						adi.skip(sourceLengths[3] * linesToSkip * typeSize);

						//--make a vbo slice
						vertexBuffer.flip();
						valueBuffer.flip();

						VertexBufferSlice vbs = new VertexBufferSlice();
						vbs.vertexBuffer = vertexBuffer;
						vbs.valueBuffer = valueBuffer;
						vbs.numberOfPts = pts;

						vbs.w = (float) indices[0] / (float) repLengths[0];
						vbs.z = (float) indices[1] / (float) repLengths[1];

						if (fakeFourthDimension) {
							vbs.w = (float) indices[1] / (float) repLengths[1];
						}

						rr.slices.add(vbs);

						//--skip to the next slice
						adi.skipBytes(sourceLengths[3] * sourceLengths[2] * (stride - 1) * typeSize);
					}

				}
			}
			System.out.println("fits file loaded " + repLengths[0] + " z " + repLengths[1] + " z " + repLengths[2] + " z " + repLengths[3]);
			System.out.println("total non NaN points:" + pointCount);
		}catch (Exception e) {

			JOptionPane.showMessageDialog(null, e.getClass().getName()+": " + e.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		finally {

			if (!dummyRun) {
				currentlyLoading = false;
				numLoading--;
			}
		}

		long t1 = System.currentTimeMillis();
		System.out.println("time taken to load file in " + rr.numPtsW * rr.numPtsX * rr.numPtsY * rr.numPtsZ +"points:" + (t1 - t0));

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
		float fidelity = PointCloud.fidelityToGetTargetPixels(fits, 250_000);
		RegionRepresentation rr = loadFromDisk(fits, fidelity, volume, true);
		float range = rr.estMax - rr.estMin;

		mam.min = rr.estMin - 0.25f * range;
		mam.max = rr.estMax + 0.25f * range;

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

	public int getNumPtsW() {return this.numPtsW;}

	public void setNumPtsW(int number) {
		this.numPtsW = number;
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

	public float getFidelity() {
		return fidelity;
	}

	public void setFidelity(float fidelity) {
		this.fidelity = fidelity;
	}

	public void setNumPts(int axis, int number) {
		if (axis == 0) {
			this.setNumPtsX(number);
		}else if (axis == 1) {
			this.setNumPtsY(number);
		}else if (axis == 2) {
			this.setNumPtsZ(number);
		}else if (axis == 3) {
			this.setNumPtsW(number);
		}
		else {
			System.out.println("hey hey hey whoops we don't really support this many axis eh");
		}
	}

	public int getDimensionInPts(int dimension) {
		if (dimension == 0) {
			return this.getNumPtsX();
		}
		if (dimension == 1) {
			return this.getNumPtsY();
		}
		if (dimension == 2) {
			return this.getNumPtsZ();
		}
		if (dimension == 3) {
			return this.getNumPtsW();
		}
		return 0;
	}



	public boolean isLiableToOverflow() {
		return this.isLiableToOverflow;
	}
}