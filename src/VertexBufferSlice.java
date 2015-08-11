import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


/**
 * This represents a slice of data that lies along the X-Y plane.
 */
public class VertexBufferSlice {
	public FloatBuffer valueBuffer;				//The buffer containing the values of each points value
	public ShortBuffer vertexBuffer;			//The buffer containing the xyz coordinates of each point
	public float z;								//The z position of the slice, useful for sorting
	public int index;							//The index of the veretx buffer object this slice corresponds to
	public int numberOfPts;						//The number of points in the slice
	public float w;								//the w dimension (4th dimension) the slice exists in
	public Region region;						//The parent region
	public PointCloud cloud;					//The parent cloud
	public boolean isLive = false;				//--a variable indicating the slice is live and loaded in the renderer

	public float getOverallZ() {
		return (region.getVolume().z + region.getVolume().dp * z) * cloud.volume.dp + cloud.volume.z;
	}
}
