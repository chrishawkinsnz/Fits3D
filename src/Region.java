import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.Fits;


/**
 * A region of the data
 * @author chrishawkins
 *
 */
public class Region implements  AttributeProvider{
//	public final static float 			STARTING_FIDELITY 	= 0.15f;

	private static int regionCount = 0;
	private List<Region> minusRegions;
	private RegionRepresentation regionRepresentation;

	/**
	 * Volume is a region of space within the overall point cloud.  that is the largest possible volume for a region is
	 * {0, 0, 0, 1, 1, 1}
	 */
	private final Volume volume;
	private Fits fits;


	public Attribute.BinaryAttribute isVisible;
	public Attribute.SteppedRangeAttribute quality;
	public Attribute.RangedAttribute intensity;
	public Attribute.TextAttribute nameAttribute;
	private List<Attribute>attributes = new ArrayList<Attribute>();

	public final static Color[] cols = {Color.blue, Color.green, Color.pink, Color.orange};


	private Region(Volume volume, float initialFidelity) {
		this.volume = volume;

		this.nameAttribute = new Attribute.TextAttribute("Name", "Region "+regionCount++, false);
		this.attributes.add(this.nameAttribute);

		Attribute.BinaryAttribute visibleAttr = new Attribute.BinaryAttribute("Visble", true, false);
		this.isVisible = visibleAttr;
		attributes.add(visibleAttr);

		intensity = new Attribute.RangedAttribute("Visibility", 0.001f, 1f, 1f, false);
		attributes.add(intensity);

		quality = new Attribute.SteppedRangeAttribute("Quality", 0.1f, 1.0f, initialFidelity, 10, true);
		quality.callback = (obj) -> {
			float newQuality = ((Float)obj).floatValue();

			Runnable r = new Runnable() {
				public void run() {
					if (newQuality == Region.this.getRegionRepresentation().getFidelity()) {
						return;
					}

					Region.this.loadRepresentationAtFidelity(newQuality);
					FrameMaster.setNeedsNewRenderer();
					FrameMaster.setNeedsDisplay();
					System.gc();
				}
			};
			new Thread(r).start();
		};
		attributes.add(quality);


	}

	public Region(Fits fits, Volume volume, float initialFidelity) {
		this(volume, initialFidelity);
		this.fits = fits;

		RegionRepresentation initialRepresentation = RegionRepresentation.loadFromDisk(fits, initialFidelity, this.getVolume(), false);
		this.setRegionRepresentation(initialRepresentation);
	}


	public void loadRepresentationAtFidelity(float fidelity) {
		RegionRepresentation initialRepresentation = RegionRepresentation.loadFromDisk(fits, fidelity, this.getVolume(), false);
		this.setRegionRepresentation(initialRepresentation);

		if (this.minusRegions != null) {
			for (Region region : minusRegions) {
				this.getRegionRepresentation().eraseRegion(region.getVolume());
			}
		}
	}


	public String toString() {
		return this.nameAttribute.getValue();
	}






	//==================================================================================================================
	//  SUBREGIONS
	//==================================================================================================================

	/**
	 *	Creates a new Region that is a subregion of this one.
	 *
	 * 	Note: To be a subregion it must be able to be fully contained within the parent region.
	 * @param subVolume volume is unit volume that is relative to the overall fits file (not the existing region)
	 * @return The subregion generated
	 */
	public Region subRegion(Volume subVolume, float fidelity, boolean replaceValues) {
		assert (subVolume.origin.x >= this.getVolume().origin.x);
		assert (subVolume.origin.y >= this.getVolume().origin.y);
		assert (subVolume.origin.z >= this.getVolume().origin.z);

		Region cr = new Region(subVolume, fidelity);
		cr.fits = this.fits;
		cr.populateAsSubregion(this, fidelity, replaceValues);

		return cr;
	}


	/**
	 * Populates a region with values that represent a subregion of some parent.
	 * @param parentRegion The region which wraps around this region
	 * @param fidelity The desired fidelity of the region to load
	 * @param replaceValues Whether or not the new region should replace the values in the parent (true means this subregion will 'cut' values while false means it will 'copy')
	 */
	public void populateAsSubregion(Region parentRegion, float fidelity, boolean replaceValues) {

		//-- If the parent is already of the correct fidelity then just generate a subsection from that
		if (fidelity == parentRegion.getRegionRepresentation().getFidelity()) {
			RegionRepresentation subRepresentation = parentRegion.getRegionRepresentation().generateSubrepresentation(this.getVolume(), replaceValues);
			this.setRegionRepresentation(subRepresentation);
		}

		//--Otherwise load in a fresh version of the point cloud
		else {
			this.setRegionRepresentation(RegionRepresentation.loadFromDisk(this.fits, fidelity, this.getVolume(),  false));

			//--make sure to cut out values that belong in the parent
			if (replaceValues) {
				parentRegion.getRegionRepresentation().eraseRegion(this.getVolume());
			}
		}
	}






	//==================================================================================================================
	//  ATTRIBUTE PROVIDER
	//==================================================================================================================

	@Override
	public List<Attribute> getAttributes() {
		return this.attributes;
	}


	@Override
	public List<AttributeProvider> getChildProviders() {
		return new ArrayList<>();
	}






	//==================================================================================================================
	//  GETTERS + SETTERS
	//==================================================================================================================

	public int getWidthInPoints() {
		return this.getRegionRepresentation().getNumPtsX();
	}


	public int getHeightInPoints() {
		return this.getRegionRepresentation().getNumPtsY();
	}


	public int getFramesInPoints(){return this.getRegionRepresentation().getNumPtsW();}

	public int getDepthInPoints() {
		return this.getRegionRepresentation().getNumPtsZ();
	}


	public int getDimensionInPts(int dimension) {
		return this.getRegionRepresentation().getDimensionInPts(dimension);
	}
	public void setMinusRegions(List<Region> minusRegions) {
		this.minusRegions = minusRegions;
	}


	public List<Region> getMinusRegions() {
		return this.minusRegions;
	}


	public RegionRepresentation getRegionRepresentation() {
		return regionRepresentation;
	}


	public void setRegionRepresentation(RegionRepresentation regionRepresentation) {
		this.regionRepresentation = regionRepresentation;
	}

	public Volume getVolume() {
		return volume;
	}


	public List<VertexBufferSlice>getSlices() {
		return this.getRegionRepresentation().getSlices();
	}
}
