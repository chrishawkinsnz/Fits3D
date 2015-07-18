import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class VertexBufferSlice {
	public FloatBuffer valueBuffer;
	public ShortBuffer vertexBuffer;
	public float x;
	public float scratchX;
	public int index;
	public int numberOfPts;
	public CloudRegion region;
	public PointCloud cloud;
}
