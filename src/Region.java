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

	public RegionRepresentation regionRepresentation;

	public float depth;
	
	public Volume volume;
	private Fits fits;

	public Attribute.BinaryAttribute isVisible;
	public Attribute.SteppedRangeAttribute quality;
	public Attribute.TextAttribute nameAttribute;
	public List<Attribute>attributes = new ArrayList<Attribute>();

	public final static Color[] cols = {Color.blue, Color.green, Color.pink, Color.orange};

	private Region() {
		Attribute.BinaryAttribute visibleAttr = new Attribute.BinaryAttribute("Visble", true, false);
		this.isVisible = visibleAttr;
		attributes.add(visibleAttr);

		quality = new Attribute.SteppedRangeAttribute("Quality", 0.1f, 1.0f, startingFidelity, 10, true);
		quality.callback = (obj) -> {
			float newQuality = ((Float)obj).floatValue();
			System.out.println("quality is now :" + newQuality);

			Runnable r = new Runnable() {
				public void run() {


				}
			};
			new Thread(r).start();
		};


		this.nameAttribute = new Attribute.TextAttribute("Name", "Region0", false);
		this.attributes.add(this.nameAttribute);
		attributes.add(quality);
	}
	private Region(Volume volume) {
		this();
		this.volume = volume;
		this.depth = this.volume.origin.z + 0.5f * this.volume.dp;
	}

	public Region(Fits fits, Volume volume, float initialFidelity) {
		this(volume);
		this.fits = fits;

		RegionRepresentation initialRepresentation = RegionRepresentation.justTheSlicesPlease(fits, initialFidelity, this.volume);
		this.regionRepresentation = initialRepresentation;

		this.regionRepresentation = initialRepresentation;
	}

	public Region(Fits fits, Volume volume, float initialFidelity, List<Region> minusRegions) {
		this(fits, volume, initialFidelity);
		for (Region region : minusRegions) {
			this.regionRepresentation.eraseRegion(region.volume);
		}


	}
	public List<VertexBufferSlice>getSlices() {
		return this.regionRepresentation.getSlices();
	}


	/**
	 *
	 * @param subVolume volume is unit volume that is relative to the overall fits file (not the existing region)
	 * @return
	 */
	public Region subRegion(Volume subVolume, float fidelity, boolean replaceValues) {
		Region cr = new Region(subVolume);
		assert (subVolume.origin.x >= this.volume.origin.x);
		assert (subVolume.origin.y >= this.volume.origin.y);
		assert (subVolume.origin.z >= this.volume.origin.z);

		if (fidelity == this.regionRepresentation.fidelity) {
			RegionRepresentation subRepresentation = regionRepresentation.generateSubrepresentation(subVolume, replaceValues);
			cr.regionRepresentation = subRepresentation;
		}
		else {
			cr.regionRepresentation = RegionRepresentation.justTheSlicesPlease(this.fits, fidelity, subVolume);
			if (replaceValues) {
				this.regionRepresentation.eraseRegion(subVolume);
			}
		}

		return cr;
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
		return this.nameAttribute.value;
	}

	@Override
	public List<Attribute> getAttributes() {
		return this.attributes;
	}
}
