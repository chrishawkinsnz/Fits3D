
public class WorldViewer {
	private float ySpin = 0f;		//spin around the central column
	private float xSpin = 0f;		//spin around the rod through the middle of the screeen
	private float radius = 3f;
		
	private float yMax = 3.141459f/4f;
	
	public final float minRadius = 0.5f;
	public final float maxRadius = 10f;
	

	public void addySpin(float addition) {
		if (addition > 0 && this.ySpin > yMax)
			return;
		if (addition < 0 && this.ySpin < -yMax)
			return;
		
		this.ySpin += addition;
	}
	
	
	public float getxSpin() {
		return xSpin;
	}
	
	
	public float getySpin() {
		return ySpin;
	}
	
	
	public void addxSpin(float addition) {
		this.xSpin += addition;
	}
	
	
	public float getRadius() {
		return this.radius;
	}

	
	public void addRadiusAmount(float dist) {
		if (dist > 0 && this.radius > this.maxRadius) 
			return;
		if (dist < 0 && this.radius < this.minRadius)
			return;
		
		this.radius += dist;
	}
}
