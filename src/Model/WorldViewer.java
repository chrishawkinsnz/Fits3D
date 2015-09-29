package Model;

import Rendering.*;
import Rendering.Renderer;
import UserInterface.FrameMaster;
import com.jogamp.opengl.math.Matrix4;
import com.jogamp.opengl.math.VectorUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WorldViewer {

	public static boolean retinaFix = true;

	//--CONSTANTS
	public final static float orthoHeight = 1.0f;
	public final static float orthoWidth = 1.0f;
	public final static float orthoOrigX = 0.0f;
	public final static float orthoOrigY = 0.0f;

	private int widthOpenGL;
	private int heightOpenGL;

	private float ySpin = 0f;		//spin around the central column
	private float xSpin = 0f;		//spin around the rod through the middle of the screeen
	private float radius = 3f;
	private float yMax = 3.141459f/4f;

	public final float minRadius = 0.5f;
	public final float maxRadius = 10f;

	private Timer animationTimer = null;

	public void addySpin(float addition) {
		if (addition > 0 && this.ySpin > yMax)
			return;
		if (addition < 0 && this.ySpin < -yMax)
			return;
		
		this.ySpin += addition;
		makeSureRendererIsReadyForThis();
		FrameMaster.setNeedsDisplay();
	}

	public float getxSpin() {
		return xSpin;
	}
	
	
	public float getySpin() {
		return ySpin;
	}
	
	
	public void addxSpin(float addition) {
		this.xSpin += addition;
		makeSureRendererIsReadyForThis();
	}
	
	
	public float getRadius() {
		return this.radius;
	}


	float targetRadius = 0.0f;
	boolean canGoAgain = true;
	private Timer refreshTimer = null;
	public void addRadiusAmount(float dist) {
		if (dist > 0 && this.radius > this.maxRadius) 
			return;
		if (dist < 0 && this.radius < this.minRadius)
			return;

		this.radius+=dist;
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
		}

	}

	private Region lastRegion = null;
	private PointCloud lastCloud = null;
	private float animationFraction = 1f;
	private float animationTime = 0f;
	private final float targetAnimationLength = 0.25f;
	private long animationStart;
	/**
	 * Gets a copy of the rotation matrix for the current viewer.
	 * @return The Rotation Matrix for the view
	 */
	public Matrix4 getBaseMatrix() {
		PointCloud cloud = FrameMaster.getActivePointCloud();
		Region region = FrameMaster.getActiveRegion();

		if (lastCloud == null) {
			lastCloud = cloud;
			lastRegion = region;
		}
		if ((lastRegion != region || lastCloud != cloud) && (this.animationTimer == null || !this.animationTimer.isRunning())) {

			this.animationStart = System.currentTimeMillis();
			if (this.animationTimer != null) {
				this.animationTimer.stop();
			}
			this.animationFraction = 0.0f;
			this.animationTimer = new Timer(16, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					long animationNow = System.currentTimeMillis();
					float currentAnimLength = (float)(animationNow - animationStart)/1000f;


					animationFraction = ((-((float)Math.cos((currentAnimLength/targetAnimationLength) * Math.PI))) + 1.0f)/2.0f;

					animationFraction = currentAnimLength / targetAnimationLength;
					System.out.println(animationFraction);
					FrameMaster.setNeedsDisplay();
					if (currentAnimLength > targetAnimationLength) {
						animationFraction = 1.0f;
						lastCloud = cloud;
						lastRegion = region;
						((Timer)e.getSource()).stop();
					}
				}
			});
			this.animationTimer.start();
		}

		Matrix4 baseMatrix = getBaseMatrixRelativeTo(cloud, region);

		return baseMatrix;
//		Matrix4 baseMatrix = new Matrix4();
//		baseMatrix.makeOrtho(orthoOrigX - orthoWidth, orthoOrigX + orthoWidth, orthoOrigY - orthoHeight, orthoOrigY + orthoHeight, -6f, 6f);
//
//		baseMatrix.rotate(this.getySpin(), 1f, 0f, 0f);
//		baseMatrix.rotate(this.getxSpin(), 0f, 1f, 0f);
//		float baseScale = 1.0f / this.getRadius();
//		baseMatrix.scale(baseScale, baseScale, baseScale);
//		baseMatrix.translate(2.0f, 0.0f, 0.0f);
//		return baseMatrix;
	}

	public Matrix4 getBaseMatrixRelativeTo(PointCloud cloud, Region region) {
		Vector3 midPoint;
		if (region == null) {
			midPoint = cloud.volume.origin.add(cloud.volume.size.scale(0.5f));
		}
		else {
			Vector3 regionOrigin = cloud.getVolume().origin.add(region.getVolume().origin.scale(cloud.getVolume().size));
			Vector3 regionCenter = regionOrigin.add(region.getVolume().size.scale(0.5f).scale(cloud.getVolume().size));
			midPoint = regionCenter;
		}



		Vector3 fudgePoint = new Vector3(1f,1f,1f);
		Matrix4 baseMatrix = new Matrix4();
		baseMatrix.makeOrtho(orthoOrigX - orthoWidth, orthoOrigX + orthoWidth, orthoOrigY - orthoHeight, orthoOrigY + orthoHeight, -6f, 6f);
		baseMatrix.rotate(this.getySpin(), 1f, 0f, 0f);
		baseMatrix.rotate(this.getxSpin(), 0f, 1f, 0f);
		float baseScale = 1.0f / this.getRadius();
		baseMatrix.scale(baseScale, baseScale, baseScale);

		midPoint = midPoint.scale(this.animationFraction);
		Vector3 animPoint;
		if (lastCloud != null) {
			Vector3 lastMidPoint = getMidPointFor(lastCloud, lastRegion);

		 	animPoint = (lastMidPoint.scale(1.0f - animationFraction).add(midPoint.scale(animationFraction))).scale(0.5f);
		}
		else {
			animPoint = midPoint;
		}

//		baseMatrix.translate(-midPoint.x, -midPoint.y, -midPoint.z);
		baseMatrix.translate(-animPoint.x, -animPoint.y, -animPoint.z);
		return baseMatrix;
	}

	public Vector3 getMidPointFor(PointCloud cloud, Region region) {
		Vector3 midPoint = null;
		if (region == null) {
			midPoint = cloud.volume.origin.add(cloud.volume.size.scale(0.5f));
		}
		else {
			Vector3 regionOrigin = cloud.getVolume().origin.add(region.getVolume().origin.scale(cloud.getVolume().size));
			Vector3 regionCenter = regionOrigin.add(region.getVolume().size.scale(0.5f).scale(cloud.getVolume().size));
			midPoint = regionCenter;
		}
		return midPoint;
	}

	public Vector3 getWorldPositionOfPixelOnPlane(Vector3 pixelPosition, Volume plane, boolean clamp) {
		//find far left of slice
		Vector3 origin = new Vector3(0f, 0f, 0f);

		float[] mat = getBaseMatrix().getMatrix();

		float[]bl = {plane.origin.x					, plane.origin.y				 , plane.origin.z			  	};
		float[]br = {plane.origin.x + plane.size.x	, plane.origin.y				 , plane.origin.z				};
		float[]tl = {plane.origin.x					, plane.origin.y + plane.size.y	 , plane.origin.z			  	};
		float[]tr = {plane.origin.x + plane.size.x	, plane.origin.y + plane.size.y	 , plane.origin.z				};

		float[]bls = new float[3];
		float[]brs = new float[3];
		float[]tls = new float[3];
		float[]trs = new float[3];
		VectorUtil.mulColMat4Vec3(bls, mat, bl);
		VectorUtil.mulColMat4Vec3(brs, mat, br);
		VectorUtil.mulColMat4Vec3(tls, mat, tl);
		VectorUtil.mulColMat4Vec3(trs, mat, tr);

		//--factor in the z position of the slice to work out the


		float factor = (float)this.heightOpenGL / (float)FrameMaster.singleton.canvas.getHeight();
		factor *= 2f;
		float orthoMouseX = WorldViewer.orthoOrigX -(WorldViewer.orthoWidth)+ factor * WorldViewer.orthoWidth * ((pixelPosition.x)/(float)this.widthOpenGL);
		float orthoMouseY = WorldViewer.orthoOrigY +(WorldViewer.orthoHeight)- factor * WorldViewer.orthoHeight * (pixelPosition.y/(float)this.heightOpenGL);


		float proportionX = (orthoMouseX - bls[0]) /(brs[0] - bls[0]);

		if (clamp)
			proportionX = clamp(proportionX, 0f, 1f);
		if (proportionX < 1f && proportionX > 0f || clamp) {
			float[] bm = {bl[0], bl[1], bl[2]};
			bm[0] += proportionX * plane.size.x;

			float[] tm = {tl[0], tl[1], tl[2]};
			tm[0] += proportionX * plane.size.x;

			float[] mm = {bm[0], bm[1], bm[2]};

			//--figure out the proportion between these two middle  points
			float[] bms = {bls[0], bls[1], bls[2]};
			bms[1] += proportionX * (brs[1] - bls[1]);

			float[] tms = {tls[0], tls[1], tls[2]};
			tms[1] += proportionX * (trs[1] - tls[1]);

			float proportionY = (orthoMouseY - bms[1]) / (tms[1] - bms[1]);

			if (clamp)
				proportionY = clamp(proportionY, 0f, 1f);

			if (proportionY > 0f && proportionY < 1f || clamp) {
				//--mm is the world position of the mous cursor on the selection plane
				mm[1] += proportionY * plane.size.y;
				System.out.print("new world position of cursor: " + (new Vector3(mm)));
				return new Vector3(mm);
			}
		}

		return null;
	}

	private float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}


	/**
	 * This is the resolution of the OpenGL scene
	 * @param width
	 * @param height
	 */
	public void informOfResolution(int width, int height) {
		//--if exactly doubled then flick the retina switch
		if (width == this.widthOpenGL *2) {
			retinaFix = true;
			System.out.println("moved to retina screen enabling fix");
			FrameMaster.setMouseFixOn(retinaFix);
		}
		else if (width * 2 == this.widthOpenGL) {
			retinaFix = false;
			System.out.println("moved to non retina screen enabling fix");
			FrameMaster.setMouseFixOn(retinaFix);
		}
		this.widthOpenGL = width;
		this.heightOpenGL = height;
		System.out.println("new graphics window  size:" + width+", "+height);
	}

	/**
	 * If the camera moves then we needa
	 */
	private void makeSureRendererIsReadyForThis() {
		if (Rendering.Renderer.isFat) {
			Renderer.cutTheFat = true;
		}
	}



}
