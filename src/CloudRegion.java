import java.awt.Color;
import java.nio.FloatBuffer;

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
	private final Fits fits;
	
	public final static Color[] cols = {Color.blue, Color.green, Color.pink, Color.orange};
	
	public CloudRegion (Fits fits, Volume volume, float initialFidelity) {
		this.volume = volume;
		this.depth = this.volume.origin.z + 0.5f * this.volume.dp;
		this.fits = fits;

		
		RegionRepresentation initialRepresentation = new RegionRepresentation(fits, initialFidelity, this.volume);
		this.bestRepresentation = initialRepresentation;
		this.currentRepresentation = initialRepresentation;
	}
	
	public FloatBuffer vertexBuffer() {
		return this.currentRepresentation.vertexBuffer;
	}
	
	public FloatBuffer valueBuffer() {
		return this.currentRepresentation.valueBuffer;
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
