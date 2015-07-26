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
	public final static float startingFidelity = 0.15f;
	private List<Region> minusRegions;

	public RegionRepresentation regionRepresentation;

	public float depth;
	
	public final Volume volume;
	private Fits fits;
	private static int regionCount = 0;
	public Attribute.BinaryAttribute isVisible;
	public Attribute.SteppedRangeAttribute quality;
	public Attribute.RangedAttribute intensity;
	public Attribute.TextAttribute nameAttribute;
	public List<Attribute>attributes = new ArrayList<Attribute>();

	public final static Color[] cols = {Color.blue, Color.green, Color.pink, Color.orange};


	private Region(Volume volume) {

		this.nameAttribute = new Attribute.TextAttribute("Name", "Region "+regionCount++, false);
		this.attributes.add(this.nameAttribute);

		Attribute.BinaryAttribute visibleAttr = new Attribute.BinaryAttribute("Visble", true, false);
		this.isVisible = visibleAttr;
		attributes.add(visibleAttr);

		intensity = new Attribute.RangedAttribute("Visibility", 0.001f, 1f, 1f, false);
		attributes.add(intensity);

		quality = new Attribute.SteppedRangeAttribute("Quality", 0.1f, 1.0f, startingFidelity, 10, true);
		quality.callback = (obj) -> {
			float newQuality = ((Float)obj).floatValue();
			System.out.println("quality is now :" + newQuality);

			Runnable r = new Runnable() {
				public void run() {
					float newQuality = ((Float)obj).floatValue();
					if (newQuality == Region.this.regionRepresentation.fidelity) {
						return;
					}
					System.out.println("quality is now :" + newQuality);

					Runnable r = new Runnable() {
						public void run() {
							Region.this.getMeMyRepresentation(newQuality);
							FrameMaster.setNeedsNewRenderer();
							FrameMaster.setNeedsDisplay();
							System.gc();
						}
					};
					new Thread(r).start();

				}
			};
			new Thread(r).start();
		};




		attributes.add(quality);

		this.volume = volume;
		this.depth = this.volume.origin.z + 0.5f * this.volume.dp;
	}

	public Region(Fits fits, Volume volume, float initialFidelity) {
		this(volume);
		this.fits = fits;

		RegionRepresentation initialRepresentation = RegionRepresentation.justTheSlicesPlease(fits, initialFidelity, this.volume);
		this.regionRepresentation = initialRepresentation;
	}

	public Region(Fits fits, Volume volume, float initialFidelity, List<Region> minusRegions) {
		this(fits, volume, initialFidelity);
		this.minusRegions = minusRegions;
		for (Region region : minusRegions) {
			this.regionRepresentation.eraseRegion(region.volume);
		}


	}
	public List<VertexBufferSlice>getSlices() {
		return this.regionRepresentation.getSlices();
	}

	public void getMeMyRepresentation(float fidelity) {
		RegionRepresentation initialRepresentation = RegionRepresentation.justTheSlicesPlease(fits, fidelity, this.volume);
		this.regionRepresentation = initialRepresentation;

		if (this.minusRegions != null) {
			for (Region region : minusRegions) {
				this.regionRepresentation.eraseRegion(region.volume);
			}
		}
	}
	/**
	 *
	 * @param subVolume volume is unit volume that is relative to the overall fits file (not the existing region)
	 * @return
	 */
	public Region subRegion(Volume subVolume, float fidelity, boolean replaceValues) {
		Region cr = new Region(subVolume);
		cr.fits = this.fits;

		assert (subVolume.origin.x >= this.volume.origin.x);
		assert (subVolume.origin.y >= this.volume.origin.y);
		assert (subVolume.origin.z >= this.volume.origin.z);

		cr.populateAsSubregion(this, fidelity, replaceValues);

		return cr;
	}

	public void populateAsSubregion(Region parentRegion, float fidelity, boolean replaceValues) {
		if (fidelity == parentRegion.regionRepresentation.fidelity) {
			RegionRepresentation subRepresentation = parentRegion.regionRepresentation.generateSubrepresentation(this.volume, replaceValues);
			this.regionRepresentation = subRepresentation;
		}
		else {
			this.regionRepresentation = RegionRepresentation.justTheSlicesPlease(this.fits, fidelity, this.volume);
			if (replaceValues) {
				parentRegion.regionRepresentation.eraseRegion(this.volume);
			}
		}
	}

	public void clear() {
		this.regionRepresentation.clear();
	}
	public int numberOfPoints() {
		return this.regionRepresentation.validPts;
	}
	
	public int ptWidth() {
		return this.regionRepresentation.numPtsX;
	}
	
	public int ptHeight() {
		return this.regionRepresentation.numPtsY;
	}
	
	public int ptDepth() {
		return this.regionRepresentation.numPtsZ;
	}

	public void setName(String name) {
		this.nameAttribute.notifyWithValue(name);
	}

	public String toString() {
		return this.nameAttribute.getValue();
	}

	@Override
	public List<Attribute> getAttributes() {
		return this.attributes;
	}

	@Override
	public List<AttributeProvider> getChildProviders() { return new ArrayList<>();}

	public void setMinusRegions(List<Region> minusRegions) {
		this.minusRegions = minusRegions;
	}

	public List<Region> getMinusRegions() {
		return this.minusRegions;
	}


}
