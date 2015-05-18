import java.nio.FloatBuffer;


public class VertexBufferSlice {
	public FloatBuffer valueBuffer;
	public FloatBuffer vertexBuffer;
	public float depthValue;
	public float scratchDepth;
	public int index;
	public int numberOfPts;
	public CloudRegion region;
	public PointCloud cloud;
}
