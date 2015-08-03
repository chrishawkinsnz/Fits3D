import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class VertexBufferSlice {
	public FloatBuffer valueBuffer;
	public ShortBuffer vertexBuffer;
	public float x;
	public float scratchX;
	public int index;
	public int numberOfPts;
	public int frame;
	public Region region;
	public PointCloud cloud;
	public boolean isLive = false;
}
