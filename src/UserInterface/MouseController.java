package UserInterface;

import Model.*;


import java.awt.event.*;


import Rendering.Renderer;
import UserInterface.*;
import Rendering.*;
import Model.*;

import javax.swing.*;

public class MouseController implements MouseMotionListener, MouseListener, MouseWheelListener {

	public static float spinSpeed = 0.01f;
	public static boolean doubleSpeed = false;
	private WorldViewer viewer;
	private Renderer renderer;

	private MouseActionType lastMouseMotionType;

	private boolean mouseFix = false;
	private int lastX = 0;
	private int lastY = 0;


	public enum MouseActionType{
		Camera,
		Select,
		None
	}

	private MouseActionType currentDragType;


	public MouseController(WorldViewer viewer, Renderer renderer) {
		this.viewer = viewer;
		this.renderer = renderer;

	}
	
	@Override
	public void mouseDragged(java.awt.event.MouseEvent e) {

		if (this.currentDragType == MouseActionType.Select) {
			this.registerMousePosition(e.getX(), e.getY(), e.getButton());
			if (this.getSelection().isActive()) {
				Vector3 oldOrigin = this.getSelection().getVolume().origin;
				PointCloud pc = FrameMaster.getActivePointCloud();

				Vector3 newSize = this.renderer.mouseWorldPosition.minus(oldOrigin);
				float[] sizeArr = newSize.toArray();
				sizeArr[pc.getSlitherAxis().ordinal()] = this.getSelection().getVolume().size.get(pc.getSlitherAxis().ordinal());
				newSize = new Vector3(sizeArr);
				Volume newVolume = new Volume(oldOrigin, newSize);
				this.getSelection().setVolume(newVolume.clampedToVolume(pc.getVolume()));
			}
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
		System.out.println("clicked:"+e.getButton());
		Selection selection = this.getSelection();
		if (selection != null) {
			if (selection.isActive()) {
				selection.setActive(false);
				this.renderer.mouseWorldPosition = null;
				FrameMaster.setNeedsDisplay();
				}
			else if (!isCurrentlySelectingPlaneInPointClouds(e.getX(), e.getY())) {
				for (PointCloud pc: this.renderer.pointClouds) {
					pc.setShouldDisplaySlitherenated(false);
					FrameMaster.setNeedsDisplay();
					pc.displaySlitherenated.updateAttributeDisplayer();
				}
			}
		}
		if (e.getButton() == 3) {
			this.doubleSpeed = !this.doubleSpeed;
		}

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


	private Timer refreshTimer = null;
	private boolean canGoAgain = true;

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		if (isCurrentlySelectingPlaneInPointClouds(e.getX(), e.getY())) {
			float selectionDepthDelta = (float)e.getWheelRotation() * 0.1f;


			Vector3 oldOrigin = this.getSelection().getVolume().origin;
			Vector3 oldSize = this.getSelection().getVolume().size;
			Vector3 newSize = oldSize.add(Vector3.in.scale(selectionDepthDelta));

			float zLimit = oldOrigin.z + newSize.z;

			//--don't push out past boundaries
			if (selectionDepthDelta > 0f) {
				float cloudLimit = FrameMaster.getActivePointCloud().volume.origin.z + FrameMaster.getActivePointCloud().volume.size.z;
				if (zLimit > cloudLimit) {
					float maxSizeZ = cloudLimit - oldOrigin.z;
					newSize = new Vector3(newSize.x, newSize.y, maxSizeZ);
				}
			}

			//--don't push out past boundaries
			if (selectionDepthDelta < 0f) {
				float cloudLimit = FrameMaster.getActivePointCloud().volume.origin.z;
				if (zLimit < cloudLimit) {
					float maxSizeZ = -(oldOrigin.z - cloudLimit);
					newSize = new Vector3(newSize.x, newSize.y, maxSizeZ);
				}
			}
			Volume newVolume = new Volume(oldOrigin, newSize);


			if (this.refreshTimer == null) {
				this.refreshTimer = new Timer(32, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						canGoAgain = true;
					}
				});
			}
			if (canGoAgain) {
				FrameMaster.setNeedsDisplay();
				canGoAgain = false;
				this.refreshTimer.start();
				FrameMaster.getActivePointCloud().selectionDepthAttribute.shouldUpdateRenderer = true;
			}
			else {
				FrameMaster.getActivePointCloud().selectionDepthAttribute.shouldUpdateRenderer = false;
			}
			FrameMaster.getActivePointCloud().selectionDepthAttribute.notifyWithValue(newSize.z / FrameMaster.getActivePointCloud().volume.dp);
			FrameMaster.getActivePointCloud().selectionDepthAttribute.updateAttributeDisplayer();
//			UserInterface.FrameMaster.setNeedsDisplay();
		}
		else {
			this.viewer.addRadiusAmount((float)e.getWheelRotation() *0.1f);
		}
	}

	public boolean isCurrentlySelectingPlaneInPointClouds(int x, int y) {
		Vector3 screenPos;
		if (this.doubleSpeed) {
			screenPos = new Vector3(x / 2, y / 2, 3f);
		} else {
			screenPos = new Vector3(x, y, 3f);
		}
		boolean found = false;

		if (FrameMaster.getActivePointCloud() == null)									{return false;}
		if (FrameMaster.getActivePointCloud().shouldDisplaySlitherenated() == false)	{return false;}

		Volume slither = FrameMaster.getActivePointCloud().getSlither(false);
		Vector3 worldPos = this.viewer.getWorldPositionOfPixelOnPlane(screenPos, slither, false);
		FrameMaster.getActivePointCloud().setCursorAtPosition(worldPos);
		return worldPos != null;
	}

	public void registerMousePosition(int x, int y, int button) {
		PointCloud pc = FrameMaster.getActivePointCloud();
		if (pc == null)									{return;}
		if (pc.shouldDisplaySlitherenated() == false) 	{return;}

		Vector3 screenPos;
		if (this.doubleSpeed) {
			screenPos = new Vector3(x / 2, y / 2, 3f);
		} else {
			screenPos = new Vector3(x, y, 3f);
		}

		this.renderer.mouseWorldPosition = this.viewer.getWorldPositionOfPixelOnPlane(screenPos, pc.getSlither(false), true);

//		System.out.println("is the pushed button("+button+")  equal to the select button:"+selectButton+"?");
		//--if this is some continuation of a drag



	}

	public void registerStartDrag(int x, int y, int button) {
		Vector3 screenPos;
		if (this.doubleSpeed) {
			screenPos = new Vector3(x / 2, y / 2, 3f);
		} else {
			screenPos = new Vector3(x, y, 3f);
		}

		PointCloud pc = FrameMaster.getActivePointCloud();
		if (pc == null)									{return;}
		if (pc.shouldDisplaySlitherenated() == false) 	{return;}

		this.renderer.mouseWorldPosition = this.viewer.getWorldPositionOfPixelOnPlane(screenPos, pc.getSlither(false), true);
		float depth = this.getSelection()!=null ? this.getSelection().getVolume().size.get(pc.getSlitherAxis().ordinal()) : 0f;
		float[] sizeArr = new float[3];
		sizeArr[pc.getSlitherAxis().ordinal()] = depth;
		Vector3 size = new Vector3(sizeArr);
		Volume newSelection = new Volume(this.renderer.mouseWorldPosition, size);
		this.getSelection().setVolume(newSelection);
		this.getSelection().setActive(true);
	}


	public Selection getSelection() {
		PointCloud pc = FrameMaster.getActivePointCloud();
		if (pc != null) {
			return pc.getSelection();
		}
		else {
			return null;
		}
	}
}
