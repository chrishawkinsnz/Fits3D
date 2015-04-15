import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;



public class MouseController implements MouseMotionListener, MouseListener, MouseWheelListener {

	private Viewer viewer;
	
	int lastX = 0;
	int lastY = 0;
	
	public MouseController(Viewer viewer) {
		this.viewer = viewer;
	}
	
	@Override
	public void mouseDragged(java.awt.event.MouseEvent e) {
		System.out.println("drag");
		int diffX = e.getX() - this.lastX;
		int diffY = e.getY() - this.lastY;
		
		float fudge = 0.01f;
		
		this.viewer.addxSpin(diffX * fudge);
		this.viewer.addySpin(diffY * fudge);
		
		this.lastX = e.getX();
		this.lastY = e.getY();
	}

	@Override
	public void mouseMoved(java.awt.event.MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		System.out.println("pressed");
		this.lastX = e.getX();
		this.lastY = e.getY();		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		this.viewer.addRadiusAmount((float)e.getWheelRotation() *0.5f);
	}

}
