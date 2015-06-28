import java.nio.FloatBuffer;

import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.header.IFitsHeader.HDU;
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
import java.util.concurrent.Callable;

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
	//--interactive attributes
	public Attribute.RangedAttribute intensity;
	public Attribute.BinaryAttribute isVisible;
	public Attribute.SteppedRangeAttribute quality;
	
	public Attribute.Name fileName;
	private Attribute.FilterSelectionAttribute filterSelection;
	 
	private static final Color[] colors = {Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE, Color.PINK};
	
	//--histogram estimation data
	
	
	public PointCloud(String pathName) {
		this.regions = new ArrayList<CloudRegion>();
		this.volume = new Volume(boxOrigX, boxOrigY, boxOrigZ, boxWidth, boxHeight, boxDepth);

		fileName = new Attribute.PathName("File Name", pathName, false);
		attributes.add(fileName);
		
		intensity = new Attribute.RangedAttribute("Visibility", 0.001f, 1f, 0.5f, true);
		attributes.add(intensity);
		
		quality = new Attribute.SteppedRangeAttribute("Quality", 0.1f, 1.0f, 0.25f, 10, true);
		quality.pointCloud = this;
		quality.callback = (obj) -> {
			float newQuality = ((Float)obj).floatValue();
			System.out.println("quality is now :" + newQuality);
			readFitsAtQualityLevel(newQuality);
		};
		attributes.add(quality);
		
		
		isVisible = new Attribute.BinaryAttribute("Visible", true, true);
		attributes.add(isVisible);
		
		Christogram.Filter data = new Christogram.Filter(0f,0f,0f,1f,false);
		filterSelection = new Attribute.FilterSelectionAttribute("Filter", false, data);
		attributes.add(filterSelection);
		
		this.color = colors[clouds++ % colors.length];
	}
	

	public void readFitsAtQualityLevel(float proportionOfPerfect) {
		try{
			
			this.fits = new Fits(this.fileName.value);
			
			ImageHDU hdu;
			try {
				hdu = (ImageHDU)this.fits.getHDU(0);
				attributes.add(0,new Attribute.Name("Data Type", BitPix.dataTypeForBitPix(hdu.getBitPix()).name(), false));
				attributes.add(1,new Attribute.Name("Observer", hdu.getObserver(), false));
				attributes.add(2,new Attribute.Name("Observed", "" + hdu.getObservationDate(), false));
				String[] axesNames = {"X", "Y", "Z"};
				for (int i = 0; i < hdu.getAxes().length; i++) {
					attributes.add(3,new Attribute.Name(axesNames[i] + " Resolution", "" + hdu.getAxes()[i], false));	
				}
				attributes.add(6,new Attribute.Name("Instrument", hdu.getInstrument(), false));
				for (Attribute attr : attributes) {
					if (attr instanceof Attribute.Name) {
						Attribute.Name namedAttr = (Attribute.Name)attr;
						if (namedAttr.value == null || namedAttr.value.equals("") || namedAttr.value.equals("null")) 
							namedAttr.value = "?";
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
			loadRegionAtFidelity(0.4f);
			
		} catch (FitsException e) {
			e.printStackTrace();
		}
	}
	
	public void loadRegionAtFidelity(float fidelity) {
		this.regions = new ArrayList<CloudRegion>();
		Volume v = new Volume(0f, 0f, 0f, 1f, 1f, 1f);
		CloudRegion cr = new CloudRegion(fits, v, fidelity);
		this.addRegion(cr);
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
	
	public int[] getHistBuckets() {
		return this.regions.get(0).currentRepresentation.buckets;
	}


	public float getHistMin() {
		return this.regions.get(0).currentRepresentation.estMin;
	}

	public float getHistMax() {
		return this.regions.get(0).currentRepresentation.estMax;
	}

	public Christogram.Filter getFilter() {
		return this.filterSelection.filter;
	}
}
