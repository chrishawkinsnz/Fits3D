import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;



public class MouseController implements MouseMotionListener, MouseListener, MouseWheelListener {

	private WorldViewer viewer;

	private int lastX = 0;
	private int lastY = 0;
	public int camButton = 1;
	public MouseController(WorldViewer viewer) {
		this.viewer = viewer;
	}
	
	@Override
	public void mouseDragged(java.awt.event.MouseEvent e) {
		if (e.getButton() == camButton) {
			int diffX = e.getX() - this.lastX;
			int diffY = e.getY() - this.lastY;

			float fudge = 0.01f;

			this.viewer.addxSpin(diffX * fudge);
			this.viewer.addySpin(diffY * fudge);

			this.lastX = e.getX();
			this.lastY = e.getY();
		}
	}

	@Override
	public void mouseMoved(java.awt.event.MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == camButton) {
			this.lastX = e.getX();
			this.lastY = e.getY();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		this.viewer.addRadiusAmount((float)e.getWheelRotation() *0.1f);
	}

}
