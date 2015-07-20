/**
 * Created by chrishawkins on 20/07/15.
 */
public class VanitySpinnerViewer extends  WorldViewer {

    double speed = 0.001f;

    @Override
    public float getxSpin() {
        double angle = ((speed * System.currentTimeMillis()) % (2.0 * 3.14159));
        return (float) angle ;
    }





}
