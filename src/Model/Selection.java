package Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chrishawkins on 20/07/15.
 */
public class Selection implements AttributeProvider {
    private static Volume DEFAULT_VOLUME = new Volume(-0.5f, -0.5f, -0.5f, 1f, 1f, 1f);
    private boolean active = false;
    private Volume volume;

    private List<Attribute>attributes;
    public PointCloud observingPointCloud;

    private Selection() {
        attributes = new ArrayList<>();

        Attribute.TextAttribute test  = new Attribute.TextAttribute("it's a test"," it's going well", false);
        attributes.add(test);
    }

    public static Selection defaultSelection() {
        Selection selection = new Selection();
        selection.resetToDefault();
        return  selection;
    }

    public void resetToDefault() {
        setVolume(DEFAULT_VOLUME);
    }
    public Volume getVolume() {
        return this.volume;
    }

    public void setVolume(Volume volume) {

        this.volume = volume;
        if (this.observingPointCloud!=null) {
            this.observingPointCloud.notifyOfNewSelectionVolume(this.volume);
        }
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
        setVolume(new Volume(volume.origin.add(delta), volume.size));
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
        Vector3 newSize = volume.size.add(scaleDelta);
        for (int axis = 0; axis < 3; axis++) {
            if (newSize.get(axis) < 0f) {
                return;
            }
        }
        setVolume(new Volume(volume.origin, volume.size.add(scaleDelta)));
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public List<AttributeProvider> getChildProviders() {
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "Selection";
    }
}
