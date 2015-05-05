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
	
	private List<CloudRegion>regions;
	
	private ImageHDU hdu;
 
	
	public PointCloud(String pathName) {
		this.fileName = pathName;
		this.regions = new ArrayList<CloudRegion>();
		
	}
	

	public void readFitsAtQualityLevel(float proportionOfPerfect) {
		try{
			
			this.fits = new Fits(this.fileName);
			Volume v = this.volume;
			if (regions.size() > 0) {
				v = new Volume(boxOrigX+ (2f * regions.size()), boxOrigY, boxOrigZ, boxWidth, boxHeight, boxDepth);
			}
			
			CloudRegion cr = new CloudRegion(fits, v, proportionOfPerfect);
//			if (this.regions.isEmpty() == false) {
//				this.regions.remove(0);
//			}
			this.regions.add(cr);
			
		} catch (FitsException e) {
			e.printStackTrace();
		}
	}
	
	public List<CloudRegion> regions() {
		return regions;
	}
	

}
