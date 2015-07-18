import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.List;

import nom.tam.fits.Fits;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * A region of the data
 * @author chrishawkins
 *
 */
public class CloudRegion {
	public RegionRepresentation bestRepresentation;
	public RegionRepresentation currentRepresentation;
	
	public final float depth;
	
	public final Volume volume;
	private Fits fits;
	
	public final static Color[] cols = {Color.blue, Color.green, Color.pink, Color.orange};

	private CloudRegion (Volume volume ) {
		this.volume = volume;
		this.depth = this.volume.origin.z + 0.5f * this.volume.dp;
	}

	public CloudRegion (Fits fits, Volume volume, float initialFidelity) {
		this.volume = volume;
		this.depth = this.volume.origin.x + 0.5f * this.volume.dp;
		this.fits = fits;

		RegionRepresentation initialRepresentation = RegionRepresentation.justTheSlicesPlease(fits, initialFidelity, this.volume);
		this.bestRepresentation = initialRepresentation;

		this.currentRepresentation = initialRepresentation;
	}

	public List<VertexBufferSlice>getSlices() {
		return this.currentRepresentation.getSlices();
	}


	/**
	 *
	 * @param subVolume volume is unit volume that is relative to the overall fits file (not the existing region)
	 * @return
	 */
	public CloudRegion subRegion(Volume subVolume, boolean replaceValues) {
		CloudRegion cr = new CloudRegion(subVolume);
		assert (subVolume.origin.x >= this.volume.origin.x);
		assert (subVolume.origin.y >= this.volume.origin.y);
		assert (subVolume.origin.z >= this.volume.origin.z);


		RegionRepresentation subRepresentation = currentRepresentation.generateSubrepresentation(subVolume, replaceValues);
		cr.bestRepresentation = subRepresentation;
		cr.currentRepresentation = subRepresentation;

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
