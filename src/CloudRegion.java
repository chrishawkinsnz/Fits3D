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
	
	public final Volume volume;
	private final Fits fits;
	
	
	public CloudRegion (Fits fits, Vector3 origin, Volume volume, float initialFidelity) {
		this.volume = volume;
		this.fits = fits;
		RegionRepresentation initialRepresentation = new RegionRepresentation(fits, initialFidelity);
		this.bestRepresentation = initialRepresentation;
		this.currentRepresentation = initialRepresentation;
	}
	
}
