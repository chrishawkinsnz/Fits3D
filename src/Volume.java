
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

	/**
	 * Returns a volume normalised to a {0,0,0,1,1,1} cube that represeents the proportional volume of the supplied volume compard with this
	 * @param selection The volume to find the proportional size of
	 * @return The proportional size of the supplied selection volume ({0,0,0,1,1,1} is a volume at the same position of the same size)
	 */
	public Volume normalisedProportionVolume(Volume selection) {
		float[] newOrigin = new float[3];
		for (int i = 0; i < 3; i++) {
			newOrigin[i] = (selection.origin.get(i) - this.origin.get(i))/ this.size.get(i);
		}
		Vector3 newOriginVec = new Vector3(newOrigin);

		float[] newSize = new float[3];
		for (int i = 0; i < 3; i++) {
			newSize[i] = selection.size.get(i) / this.size.get(i);
		}
		Vector3 newSizeVec = new Vector3(newSize);

		Volume newVolume = new Volume(newOriginVec, newSizeVec);
		return newVolume;
	}

	/**
	 * Checks to see if a point is contained within this volume
	 * @param point The point to check
	 * @return true if the point is contained within the volume false otherwise.
	 */
	public boolean containsPoint(Vector3 point) {
		for (int axis = 0; axis < 3; axis++) {
			if (point.get(axis) < this.origin.get(axis)) return false;
			if (point.get(axis) > this.origin.add(this.size).get(axis)) return false;
		}
		return true;
	}

	public String toString() {
		return "Volume {"+this.origin.x + "," +this.origin.y + "," +this.origin.z + "," +this.size.x + "," +this.size.y + "," +this.size.z + "," +"}";
	}
}
