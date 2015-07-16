import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class VertexBufferSlice {
	public FloatBuffer valueBuffer;
	public ShortBuffer vertexBuffer;
	public float depthValue;
	public float scratchDepth;
	public int index;
	public int numberOfPts;
	public CloudRegion region;
	public PointCloud cloud;
}
