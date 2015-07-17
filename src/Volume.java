
public class Volume {
	public static final Volume unit = new Volume(Vector3.zeros, Vector3.ones);
	
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
	
	public static Volume unitVolume() {
		return new Volume(Vector3.zeros, Vector3.ones);
	}

	public Vector3 a() {
		return origin;
	}

	public Vector3 b() {
		return origin.add(Vector3.right.scale(wd));
	}

	public Vector3 c() {
		return origin.add(Vector3.up.scale(ht));
	}

	public Vector3 d() {
		return c().add(Vector3.right.scale(wd));
	}

	public Vector3 e() {
		return a().add(Vector3.in.scale(dp));
	}

	public Vector3 f() {
		return b().add(Vector3.in.scale(dp));
	}

	public Vector3 g() {
		return c().add(Vector3.in.scale(dp));
	}

	public Vector3 h() {
		return d().add(Vector3.in.scale(dp));
	}

}
