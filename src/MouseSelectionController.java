import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Created by chrishawkins on 8/08/15.
 */
public class MouseSelectionController implements MouseMotionListener, MouseListener {

    private Renderer renderer;

    public MouseSelectionController(Renderer renderer) {
        if (renderer == null) {
            throw new RuntimeException("oh no. no renerer here");
        }
        this.renderer = renderer;
    }

    public static int selectButton = 3;

    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.getButton() == selectButton) {


        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        this.renderer.drawLineToPoint(e.getX(), e.getY());
        FrameMaster.setNeedsDisplay();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
