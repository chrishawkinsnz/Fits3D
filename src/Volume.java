
public class Volume {
	
	public final Vector3 origin;
	public final Vector3 size;
	public final float x;
	public final float y;
	public final float z;
	public final float wd;
	public final float ht;
	public final float dp;
	
	public Volume (Vector3 origin, Vector3 size) {
		this.origin = origin;
		this.size = size;
		this.x = origin.x;
		this.y = origin.y;
		this.z = origin.z;
		this.wd = size.x;
		this.ht = size.y;
		this.dp = size.z;
	}
	
	public Volume (Vector3 origin, float wd, float ht, float dp) {
		this(origin, new Vector3(wd, ht, dp));
	}
	
	public Volume (float x, float y, float z, float wd, float ht, float dp) {
		this(new Vector3(x, y, z), new Vector3(wd, ht, dp));
	}
}
