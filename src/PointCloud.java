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
	private static int clouds = 0;
	public final static boolean shouldFudge = false;
	
	
	private Fits fits;
	
	private final float boxWidth = 2.0f;
	private final float boxHeight = boxWidth;
	private final float boxDepth = boxWidth;

	private final float boxOrigZ = -0.5f * boxDepth;
	private final float boxOrigX = -0.5f * boxWidth;
	private final float boxOrigY = -0.5f * boxHeight;
	
	
	final Volume volume;
	
	public final Color color;
	
	List<CloudRegion>regions;
	
	public List<Attribute>attributes = new ArrayList<Attribute>();
	public Attribute.RangedAttribute intensity;
	public Attribute.BinaryAttribute isVisible;
	public Attribute.BinaryAttribute isTrippy;
	public Attribute.RangedAttribute quality;
	public Attribute.Name fileName;
 
	private static final Color[] colors = {Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE, Color.PINK};
	
	public PointCloud(String pathName) {
		this.regions = new ArrayList<CloudRegion>();
		this.volume = new Volume(boxOrigX, boxOrigY, boxOrigZ, boxWidth, boxHeight, boxDepth);

		fileName = new Attribute.Name("File Name", pathName, false);
		attributes.add(fileName);
		
		intensity = new Attribute.RangedAttribute("Visibility", 0.001f, 0.15f, 0.15f, true);
		attributes.add(intensity);
		
		quality = new Attribute.RangedAttribute("Quality", 0.1f, 1.0f, 0.25f, true);
		attributes.add(quality);
		
		isVisible = new Attribute.BinaryAttribute("Visible", true, true);
		attributes.add(isVisible);
		
		isTrippy = new Attribute.BinaryAttribute("Trippy", false, true);
		attributes.add(isTrippy);
		
		this.color = colors[clouds++ % colors.length];
	}
	

	public void readFitsAtQualityLevel(float proportionOfPerfect) {
		try{
			
			this.fits = new Fits(this.fileName.value);
			Volume v = new Volume(0f, 0f, 0f, 1f, 1f, 1f);
			CloudRegion cr = new CloudRegion(fits, v, 0.2f);
			this.addRegion(cr);
			
		} catch (FitsException e) {
			e.printStackTrace();
		}
	}
	
	public List<CloudRegion> getRegions() {
		return regions;
	}
	
	public List<VertexBufferSlice>getSlices() {
		List<VertexBufferSlice>slices = new ArrayList<VertexBufferSlice>();
		for (CloudRegion region:this.regions) {
			slices.addAll(region.getSlices());
		}
		return slices;
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
		return this.fileName.value;
	}
}
