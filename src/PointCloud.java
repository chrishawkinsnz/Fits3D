import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PointCloud {

	private final static Color[] colors = {Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE, Color.PINK};
	public final static float startingFidelity = 0.1f;

	private static int clouds = 0;

	public CloudRegion pendingRegion;

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
	private Attribute.FilterSelectionAttribute filterSelection;

	//--static attributes
	public Attribute.TextAttribute fileName;


	
	public PointCloud(String pathName) {
		this.regions = new ArrayList<CloudRegion>();
		this.volume = new Volume(boxOrigX, boxOrigY, boxOrigZ, boxWidth, boxHeight, boxDepth);

		fileName = new Attribute.PathName("File TextAttribute", pathName, false);
		attributes.add(fileName);
		
		intensity = new Attribute.RangedAttribute("Visibility", 0.001f, 1f, 0.5f, true);
		attributes.add(intensity);
		
		quality = new Attribute.SteppedRangeAttribute("Quality", 0.1f, 1.0f, startingFidelity, 10, true);
		quality.pointCloud = this;
		quality.callback = (obj) -> {
			float newQuality = ((Float)obj).floatValue();
			System.out.println("quality is now :" + newQuality);
			loadRegionAtFidelity(newQuality);
		};
		attributes.add(quality);
		
		
		isVisible = new Attribute.BinaryAttribute("Visible", true, true);
		attributes.add(isVisible);
		
		Christogram.Filter data = new Christogram.Filter(0f,1f,0f,1f,false);
		filterSelection = new Attribute.FilterSelectionAttribute("Filter", false, data);
		attributes.add(filterSelection);
		
		this.color = colors[clouds++ % colors.length];
	}
	
	public void readFits() {
		readFitsAtQualityLevel(this.quality.value);
	}

	private void readFitsAtQualityLevel(float proportionOfPerfect) {
		try{
			this.fits = new Fits(this.fileName.value);


			ImageHDU hdu = (ImageHDU)this.fits.getHDU(0);
			attributes.add(0,new Attribute.TextAttribute("Data Type", BitPix.dataTypeForBitPix(hdu.getBitPix()).name(), false));
			attributes.add(1,new Attribute.TextAttribute("Observer", hdu.getObserver(), false));
			attributes.add(2,new Attribute.TextAttribute("Observed", "" + hdu.getObservationDate(), false));
			String[] axesNames = {"X", "Y", "Z"};
			for (int i = 0; i < hdu.getAxes().length; i++) {
				attributes.add(3,new Attribute.TextAttribute(axesNames[i] + " Resolution", "" + hdu.getAxes()[i], false));
			}
			attributes.add(6,new Attribute.TextAttribute("Instrument", hdu.getInstrument(), false));
			for (Attribute attr : attributes) {
				if (attr instanceof Attribute.TextAttribute) {
					Attribute.TextAttribute namedAttr = (Attribute.TextAttribute)attr;
					if (namedAttr.value == null || namedAttr.value.equals("") || namedAttr.value.equals("null"))
						namedAttr.value = "?";
				}
			}

			loadRegionAtFidelity(proportionOfPerfect);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void loadRegionAtFidelity(float fidelity) {
		Volume v = new Volume(0f, 0f, 0f, 1f, 1f, 1f);
		CloudRegion cr = new CloudRegion(fits, v, fidelity);

		this.pendingRegion = cr;
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
	
	public void addRegion (CloudRegion cr, List<CloudRegion>existingRegions) {
		existingRegions.add(cr);
		class RegionOrderer implements Comparator<CloudRegion> {
			public int compare(CloudRegion a, CloudRegion b) {
				return a.depth < b.depth ? -1 : 1;
			}
		}
		Collections.sort(existingRegions, new RegionOrderer());
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
