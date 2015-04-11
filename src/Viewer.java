
public class Viewer {
	private float ySpin = 0f;		//spin around the central column
	private float xSpin = 0f;		//spin around the rod through the middle of the screeen
	private float radius = 3f;
	
	private boolean isSpinning = false;
	
	private float yMax = 3.141459f/4f;
	
	public final float speed = 0.5f;
	
	public float getySpin() {
		return ySpin;
	}
	public void setySpin(float ySpin) {
		this.ySpin = ySpin;
	}
	
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
	
	public void addxSpin(float addition) {
		this.xSpin += addition;
	}
	
	public void setxSpin(float xSpin) {
		this.xSpin = xSpin;
	}
	
	public boolean isSpinning(){
		return this.isSpinning;
	}
	
	public void setSpining(boolean spinning) {
		this.isSpinning = spinning;
	}
	
	
	public float getRadius() {
		return this.radius;
	}
	
	public void setRadius(float radius) {
		this.radius = radius;
	}
	
	public void update(float delta) {
		if (this.isSpinning) {
			xSpin += delta * speed;
		}
	}
}
