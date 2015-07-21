import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.Fits;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;


/**
 * A region of the data
 * @author chrishawkins
 *
 */
public class CloudRegion {
	public final static float startingFidelity = 0.15f;

	public RegionRepresentation bestRepresentation;
	public RegionRepresentation currentRepresentation;
	
	public float depth;
	
	public Volume volume;
	private Fits fits;

	public Attribute.BinaryAttribute isVisible;
	public Attribute.SteppedRangeAttribute quality;
	public List<Attribute>attributes = new ArrayList<Attribute>();

	public final static Color[] cols = {Color.blue, Color.green, Color.pink, Color.orange};

	private CloudRegion() {
		Attribute.BinaryAttribute visibleAttr = new Attribute.BinaryAttribute("Visble", true, false);
		this.isVisible = visibleAttr;
		attributes.add(visibleAttr);

		quality = new Attribute.SteppedRangeAttribute("Quality", 0.1f, 1.0f, startingFidelity, 10, true);
		quality.callback = (obj) -> {
			float newQuality = ((Float)obj).floatValue();
			System.out.println("quality is now :" + newQuality);

			Runnable r = new Runnable() {
				public void run() {

					//loadRegionAtFidelity(newQuality);
				}
			};
			new Thread(r).start();
		};
		attributes.add(quality);
	}
	private CloudRegion (Volume volume ) {
		this();
		this.volume = volume;
		this.depth = this.volume.origin.z + 0.5f * this.volume.dp;
	}

	public CloudRegion (Fits fits, Volume volume, float initialFidelity) {
		this(volume);
		this.fits = fits;

		RegionRepresentation initialRepresentation = RegionRepresentation.justTheSlicesPlease(fits, initialFidelity, this.volume);
		this.bestRepresentation = initialRepresentation;

		this.currentRepresentation = initialRepresentation;
	}

	public CloudRegion (Fits fits, Volume volume, float initialFidelity, List<CloudRegion> minusRegions) {
		this(fits, volume, initialFidelity);
		for (CloudRegion region : minusRegions) {
			this.currentRepresentation.eraseRegion(region.volume);
		}


	}
	public List<VertexBufferSlice>getSlices() {
		return this.currentRepresentation.getSlices();
	}


	/**
	 *
	 * @param subVolume volume is unit volume that is relative to the overall fits file (not the existing region)
	 * @return
	 */
	public CloudRegion subRegion(Volume subVolume, float fidelity, boolean replaceValues) {
		CloudRegion cr = new CloudRegion(subVolume);
		assert (subVolume.origin.x >= this.volume.origin.x);
		assert (subVolume.origin.y >= this.volume.origin.y);
		assert (subVolume.origin.z >= this.volume.origin.z);

		if (fidelity == this.currentRepresentation.fidelity) {
			RegionRepresentation subRepresentation = currentRepresentation.generateSubrepresentation(subVolume, replaceValues);
			cr.bestRepresentation = subRepresentation;
			cr.currentRepresentation = subRepresentation;
		}
		else {
			cr.bestRepresentation = RegionRepresentation.justTheSlicesPlease(this.fits, fidelity, subVolume);
			cr.currentRepresentation = cr.bestRepresentation;
			if (replaceValues) {
				this.bestRepresentation.eraseRegion(subVolume);
			}
		}

		return cr;
	}
	public void clear() {
		this.bestRepresentation.clear();
		this.currentRepresentation.clear();
	}
	public int numberOfPoints() {
		return this.currentRepresentation.validPts;
	}
	
	public int ptWidth() {
		return this.currentRepresentation.numPtsX;
	}
	
	public int ptHeight() {
		return this.currentRepresentation.numPtsY;
	}
	
	public int ptDepth() {
		return this.currentRepresentation.numPtsZ;
	}
}
