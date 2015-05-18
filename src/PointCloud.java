import java.nio.FloatBuffer;

import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.BufferedDataOutputStream;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class PointCloud {
	public final static boolean shouldFudge = false;
	
	public final String fileName;
	
	private Fits fits;
	
	
	private final float boxWidth = 2.0f;
	private final float boxHeight = boxWidth;
	private final float boxDepth = boxWidth;

	private final float boxOrigZ = -0.5f * boxDepth;
	private final float boxOrigX = -0.5f * boxWidth;
	private final float boxOrigY = -0.5f * boxHeight;
	
	
	
	final Volume volume = new Volume(boxOrigX, boxOrigY, boxOrigZ, boxWidth, boxHeight, boxDepth);
	
	List<CloudRegion>regions;
 
	
	public PointCloud(String pathName) {
		this.fileName = pathName;
		this.regions = new ArrayList<CloudRegion>();
	}
	

	public void readFitsAtQualityLevel(float proportionOfPerfect) {
		try{
			
			this.fits = new Fits(this.fileName);
			Volume v = new Volume(0f, 0f, 0f, 1f, 1f, 1f);
			CloudRegion cr = new CloudRegion(fits, v, 0.3f);
			this.addRegion(cr);
			
			v = new Volume(0f, 0f, 0.5f, 1f, 1f, 1f);
			cr = new CloudRegion(fits, v, 0.3f);
			this.addRegion(cr);
			
//			int slices = 10;
//			float sliceWidth = 1f / ((float)slices);
//			for (int i = 0; i <slices; i++) {
//				float slicePos = ((float)i) * sliceWidth;
//				Volume v = new Volume(slicePos, 0f, 0f, sliceWidth, 1f, 1f);
//				CloudRegion cr = new CloudRegion(fits, v, 0.3f);
//				this.addRegion(cr);
//			}
			
		} catch (FitsException e) {
			e.printStackTrace();
		}
	}
	
	public List<CloudRegion> getRegions() {
		return regions;
	}
	
	public void addRegion (CloudRegion cr) {
		this.regions.add(cr);
		class RegionOrderer implements Comparator<CloudRegion> {
			public int compare(CloudRegion a, CloudRegion b) {
				return a.depth < b.depth ? -1 : 1;
			}
		}
		Collections.sort(this.getRegions(), new RegionOrderer());
	}

	public String toString() {
		return this.fileName;
	}
}
