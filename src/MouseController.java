import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;


public class MouseController implements MouseMotionListener, MouseListener, MouseWheelListener {

	private WorldViewer viewer;
	private Canvas canvas;
	private Renderer renderer;
	private List<PointCloud>pointClouds;

	private int lastX = 0;
	private int lastY = 0;

	public int selectButton = 1;
	public int camButton = 1;

	public enum MouseActionType{
		Camera,
		Select,
		None
	}

	private MouseActionType currentDragType;


	public MouseController(WorldViewer viewer, Canvas canvas, Renderer renderer, List<PointCloud>pointClouds) {
		this.viewer = viewer;
		this.canvas = canvas;
		this.renderer = renderer;
		this.pointClouds = pointClouds;
	}
	
	@Override
	public void mouseDragged(java.awt.event.MouseEvent e) {

		if (this.currentDragType == MouseActionType.Select) {
			this.renderer.registerMousePosition(e.getX(), e.getY(), e.getButton());

			FrameMaster.setNeedsDisplay();
		}
		else if (this.currentDragType == MouseActionType.Camera){
			int diffX = e.getX() - this.lastX;
			int diffY = e.getY() - this.lastY;

			float fudge = 0.01f;

			this.viewer.addxSpin(diffX * fudge);
			this.viewer.addySpin(diffY * fudge);

			this.lastX = e.getX();
			this.lastY = e.getY();
		}
	}

	private MouseActionType lastMouseMotionType;
	@Override
	public void mouseMoved(java.awt.event.MouseEvent e) {

		this.renderer.registerMousePosition(e.getX(), e.getY(), 0);
		if (isCurrentlySelectingPlaneInPointClouds(e.getX(), e.getY())) {
			FrameMaster.setNeedsDisplay();
			this.lastMouseMotionType = MouseActionType.Select;
		}
		else {
			if (this.lastMouseMotionType == MouseActionType.Select) {
				this.renderer.registerLeaveSelectionZone();
				FrameMaster.setNeedsDisplay();
			}
			this.lastMouseMotionType = MouseActionType.Camera;
		}
		//TODO don't redraw unless is actually over square
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		this.renderer.registerClick(e.getX(), e.getY(), MouseActionType.Select);
		FrameMaster.setNeedsDisplay();
	}

	@Override
	public void mousePressed(MouseEvent e) {

		this.lastX = e.getX();
		this.lastY = e.getY();

		if (isCurrentlySelectingPlaneInPointClouds(e.getX(), e.getY())) {
			this.renderer.registerStartDrag(e.getX(), e.getY(), e.getButton());
			FrameMaster.setNeedsDisplay();
			this.currentDragType = MouseActionType.Select;
		}
		else {
			this.currentDragType = MouseActionType.Camera;
		}

	}


	@Override
	public void mouseReleased(MouseEvent e) {
		this.currentDragType = MouseActionType.None;
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (isCurrentlySelectingPlaneInPointClouds(e.getX(), e.getY())) {
			float selectionDepthDelta = (float)e.getWheelRotation() * 0.1f;
			this.renderer.setSelectionDepth(this.renderer.getSelectionDepth() + selectionDepthDelta);
			FrameMaster.setNeedsDisplay();
		}
		else {
			this.viewer.addRadiusAmount((float)e.getWheelRotation() *0.1f);
		}
	}

	public boolean isCurrentlySelectingPlaneInPointClouds(int x, int y) {

		Vector3 screenPos  = new Vector3(x, y, 3f);
		boolean found = false;
		for (PointCloud pc : this.pointClouds) {
			if (pc.shouldDisplaySlitherenated()) {
				Volume slither = pc.getSlither(false);
				Vector3 worldPos = this.viewer.getWorldPositionOfPixelOnPlane(screenPos, slither, false);
				if (worldPos != null) {
					found = true;
				}
			}

		}

		return found;
	}

}
