package Model;

public class Vector3 {
	public static final Vector3 zeros = 	new Vector3( 0f, 0f, 0f);
	public static final Vector3 ones = 		new Vector3( 1f, 1f, 1f);
	public static final Vector3 right = 	new Vector3( 1f, 0f, 0f);
	public static final Vector3 left = 		new Vector3(-1f, 0f, 0f);
	public static final Vector3 up = 		new Vector3( 0f, 1f, 0f);
	public static final Vector3 down = 		new Vector3( 0f,-1f, 0f);
	public static final Vector3 in = 		new Vector3( 0f, 0f, 1f);
	public static final Vector3 out = 		new Vector3( 0f, 0f,-1f);
	
	public final float x;
	public final float y;
	public final float z;

	public Vector3(float[] arr) {
		this.x = arr[0];
		this.y = arr[1];
		this.z = arr[2];
	}

	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public float[] toArray() {
		float[] result = {x, y ,z};
		return result;
	}

	public Vector3 add(Vector3 rhs) {
		Vector3 result = new Vector3(x + rhs.x, y + rhs.y, z + rhs.z);
		return result;
	}

	public Vector3 minus(Vector3 rhs) {
		Vector3 result = new Vector3(x - rhs.x, y - rhs.y, z - rhs.z);
		return result;
	}

	public Vector3 scale(float scale) {
		Vector3 result = new Vector3(x * scale, y * scale, z *  scale);
		return result;
	}

	public Vector3 scale(Vector3 scale) {
		Vector3 result = new Vector3( x * scale.x, y * scale.y, z * scale.z);
		return result;
	}

	public Vector3 divideBy(Vector3 dividend) {
		Vector3 result = new Vector3( x /dividend.x, y/dividend.y, z/dividend.z);
		return result;
	}

	public float get(int idx) {
		if (idx == 0) {return x;}
		if (idx == 1) {return y;}
		if (idx == 2) {return z;}
		return 0f;
	}

	@Override
	public String toString(){
		return "Vec{"+x+", "+y+", "+z+"}";
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof  Vector3)) {
			return false;
		}

		Vector3 otherv = (Vector3)other;
		for (int i = 0; i < 3; i++) {
			if (otherv.get(i) != this.get(i)) {
				return false;
			}
		}
		return true;
	}
}
