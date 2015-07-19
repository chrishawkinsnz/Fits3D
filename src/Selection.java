/**
 * Created by chrishawkins on 20/07/15.
 */
public class Selection {

    private Volume volume;
    public Selection() {
        volume = Volume.unitVolume();
        volume = new Volume(0.25f, 0.25f, 0f, 0.25f, 0.25f, 0.25f);
    }

    public Volume getVolume() {
        return this.volume;
    }

    public void moveX(float xDelta) {
        Vector3 vecDelta = new Vector3(xDelta, 0f, 0f);
        move(vecDelta);
    }

    public void moveY(float yDelta) {
        Vector3 vecDelta = new Vector3(0f, yDelta, 0f);
        move(vecDelta);
    }

    public void moveZ(float zDelta) {
        Vector3 vecDelta = new Vector3(0f, 0f, zDelta);
        move(vecDelta);
    }

    public void move(Vector3 delta) {
        this.volume = new Volume(volume.origin.add(delta), volume.size);
    }

    public void scaleAddX(float scaleDeltaX){
        Vector3 scaleDelta = new Vector3(scaleDeltaX, 0f, 0f);
        scale(scaleDelta);
    }

    public void scaleAddY(float scaleDeltaY){
        Vector3 scaleDelta = new Vector3(0f, scaleDeltaY, 0f);
        scale(scaleDelta);
    }

    public void scaleAddZ(float scaleDeltaz){
        Vector3 scaleDelta = new Vector3(0f, 0f, scaleDeltaz);
        scale(scaleDelta);
    }

    public void scale(Vector3 scaleDelta) {
        this.volume = new Volume(volume.origin, volume.size.add(scaleDelta));
    }
}
