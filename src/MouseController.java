import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;


public class MouseController implements MouseMotionListener, MouseListener, MouseWheelListener {

	public static float spinSpeed = 0.01f;

	private WorldViewer viewer;
	private Renderer renderer;
	private List<PointCloud>pointClouds;
	private  Selection selection;

	private MouseActionType lastMouseMotionType;

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


	public MouseController(WorldViewer viewer, Renderer renderer, List<PointCloud>pointClouds) {
		this.viewer = viewer;
		this.renderer = renderer;
		this.pointClouds = pointClouds;
		this.selection = this.renderer.selection;
	}
	
	@Override
	public void mouseDragged(java.awt.event.MouseEvent e) {

		if (this.currentDragType == MouseActionType.Select) {

			this.registerMousePosition(e.getX(), e.getY(), e.getButton());

			FrameMaster.setNeedsDisplay();
		}
		else if (this.currentDragType == MouseActionType.Camera){
			int diffX = e.getX() - this.lastX;
			int diffY = e.getY() - this.lastY;

			this.viewer.addxSpin(diffX * spinSpeed);
			this.viewer.addySpin(diffY * spinSpeed);

			this.lastX = e.getX();
			this.lastY = e.getY();
		}
	}


	@Override
	public void mouseMoved(java.awt.event.MouseEvent e) {
		this.registerMousePosition(e.getX(), e.getY(), 0);
		if (isCurrentlySelectingPlaneInPointClouds(e.getX(), e.getY())) {
			FrameMaster.setNeedsDisplay();
			this.lastMouseMotionType = MouseActionType.Select;
		}
		else {
			if (this.lastMouseMotionType == MouseActionType.Select) {
				this.renderer.mouseWorldPosition = null;
				FrameMaster.setNeedsDisplay();
			}
			this.lastMouseMotionType = MouseActionType.Camera;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		this.selection.setActive(false);
		this.renderer.mouseWorldPosition = null;
		FrameMaster.setNeedsDisplay();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		this.lastX = e.getX();
		this.lastY = e.getY();

		if (isCurrentlySelectingPlaneInPointClouds(e.getX(), e.getY())) {
			this.registerStartDrag(e.getX(), e.getY(), e.getButton());
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

			Vector3 oldOrigin = this.selection.getVolume().origin;
			Vector3 oldSize = this.selection.getVolume().size;
			Vector3 newSize = oldSize.add(Vector3.in.scale(selectionDepthDelta));
			Volume newVolume = new Volume(oldOrigin, newSize);

			this.selection.setVolume(newVolume);
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
			if (pc.shouldDisplaySlitherenated() == false) {continue;}

			Volume slither = pc.getSlither(false);
			Vector3 worldPos = this.viewer.getWorldPositionOfPixelOnPlane(screenPos, slither, false);

			if (worldPos != null) {
				found = true;
			}
		}

		return found;
	}

	public void registerMousePosition(int x, int y, int button) {
		for (PointCloud pc : this.pointClouds) {
			if (pc.shouldDisplaySlitherenated() == false) {continue;}

			this.renderer.mouseWorldPosition = this.viewer.getWorldPositionOfPixelOnPlane(new Vector3(x, y, 3f), pc.getSlither(false), true);


			//--if this is some continuation of a drag
			if (button == selectButton && this.selection.isActive()) {
				Vector3 oldOrigin = this.selection.getVolume().origin;

				Vector3 newSize = this.renderer.mouseWorldPosition.minus(oldOrigin);
				float[] sizeArr = newSize.toArray();
				sizeArr[pc.getSlitherAxis().ordinal()] = this.selection.getVolume().size.get(pc.getSlitherAxis().ordinal());
				newSize = new Vector3(sizeArr);
				Volume newVolume = new Volume(oldOrigin, newSize);
				this.selection.setVolume(newVolume.clampedToVolume(pc.getVolume()));
			}

		}
	}

	public void registerStartDrag(int x, int y, int button) {
		for (PointCloud pc : this.pointClouds) {
			if (pc.shouldDisplaySlitherenated() == false) {continue;}
			this.renderer.mouseWorldPosition = this.viewer.getWorldPositionOfPixelOnPlane(new Vector3(x, y, 3f), pc.getSlither(false), true);
			float depth = this.selection!=null ? this.selection.getVolume().size.get(pc.getSlitherAxis().ordinal()) : 0f;
			float[] sizeArr = new float[3];
			sizeArr[pc.getSlitherAxis().ordinal()] = depth;
			Vector3 size = new Vector3(sizeArr);
			Volume newSelection = new Volume(this.renderer.mouseWorldPosition, size);
			this.selection.setVolume(newSelection);
			this.selection.setActive(true);
		}
	}

}
