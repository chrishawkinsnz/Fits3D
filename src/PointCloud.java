import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class PointCloud implements  AttributeProvider {

	private final Attribute.BinaryAttribute displaySlitherenated;
	private Attribute.BinaryAttribute cyclingSlitherAttribute;
	private Attribute.RangedAttribute slitherPositionAttribute;

	public enum Axis {
		x,y,z
	}

	private final static String[]   AXES_NAMES          = {"X", "Y", "Z", "W", "Q", "YOU", "DO", "NOT", "NEED", "THIS", "MANY", "AXES"};
	private final static Color[] 	DEFAULT_COLORS 		= {Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE, Color.PINK};
	private final static float 		STARTING_FIDELITY 	= 0.075f;
	private final static int 		STARTING_TARGET_PIX	= 1_000_000;

	private final static float 		BOX_WIDTH			= 2.0f;
	private final static float 		BOX_HEIGHT 			= BOX_WIDTH;
	private final static float 		BOX_DEPTH 			= BOX_WIDTH;

	private final static float		BOX_ORIGIN_X 		= -0.5f * BOX_WIDTH;
	private final static float 		BOX_ORIGIN_Y 		= -0.5f * BOX_HEIGHT;
	private final static float 		BOX_ORIGIN_Z 		= -0.5f * BOX_DEPTH;
	private final Timer timerCycle;


	private Timer slitherCycleTimer;


	private Fits fits;

	private static int clouds = 0;

	public Volume volume;
	private Volume backupVolume;
	private Volume galacticVolume;

	private Axis slitherAxis = Axis.z;

	public final Color color;
	
	private List<Region>regions;
	
	private List<Attribute>attributes = new ArrayList<Attribute>();

	private Selection selection;

	//--interactive attributes
	public Attribute.RangedAttribute intensity;
	public Attribute.BinaryAttribute isVisible;
	public Attribute.SteppedRangeAttribute quality;
	public Attribute.FilterSelectionAttribute filterSelection;
	public Attribute.MultiChoiceAttribute relativeTo;
	public Attribute.RangedAttribute frame;
	private final Attribute.BinaryAttribute cycling;

	public Attribute.TextAttribute[] unitTypes;
	//--static attributes
	public Attribute.TextAttribute fileName;


	public PointCloud(String pathName) {


		this.regions = new ArrayList<Region>();
		this.volume = new Volume(BOX_ORIGIN_X, BOX_ORIGIN_Y, BOX_ORIGIN_Z, BOX_WIDTH, BOX_HEIGHT, BOX_DEPTH);
		this.backupVolume = this.volume;
		fileName = new Attribute.PathName("Filename", pathName, false);
		attributes.add(fileName);

		this.selection = Selection.defaultSelection();
		this.selection.setVolume(this.volume);
		this.selection.setActive(false);

		Attribute.TextAttribute optionsTitleAttribute = new Attribute.TextAttribute(" ", "Options", false);
		optionsTitleAttribute.isTitle = true;
		attributes.add(optionsTitleAttribute);

		isVisible = new Attribute.BinaryAttribute("Visible", true, true);
		attributes.add(isVisible);


		intensity = new Attribute.RangedAttribute("Brightness", 0.001f, 1f, 0.5f, false);
		attributes.add(intensity);

		float fidelity = STARTING_FIDELITY;
		try {
			this.fits = new Fits(this.fileName.getValue());
			fidelity = fidelityToGetTargetPixels(fits, STARTING_TARGET_PIX);
		}catch (Exception e) {
			e.printStackTrace();
		}

		quality = new Attribute.SteppedRangeAttribute("Quality", 0.1f, 1.0f, fidelity, 10, true);
		quality.callback = (obj) -> {
			float newQuality = ((Float) obj).floatValue();
			System.out.println("quality is now :" + newQuality);

			Runnable r = new Runnable() {
				public void run() {
					//--TODO don't hack this maybe
					Region primaryRegion = PointCloud.this.regions.get(0);
					List<Region> children = primaryRegion.getMinusRegions();
					primaryRegion.setMinusRegions(new ArrayList<>());
					primaryRegion.loadRepresentationAtFidelity(newQuality);
					for (Region child : children) {
						child.populateAsSubregion(primaryRegion, newQuality, true);
					}

					for (Region region : PointCloud.this.regions) {
						region.quality.notifyWithValue(newQuality, false);
					}

					primaryRegion.setMinusRegions(children);
					FrameMaster.setNeedsNewRenderer();
					FrameMaster.setNeedsDisplay();
				}
			};
			new Thread(r).start();
		};
		attributes.add(quality);




		Attribute.TextAttribute filteringTitleAttribute = new Attribute.TextAttribute(" ", "Filters", false);
		filteringTitleAttribute.isTitle = true;
		attributes.add(filteringTitleAttribute);







		Christogram.Filter data = new Christogram.Filter(0f, 1f, 0f, 1f, false);
		filterSelection = new Attribute.FilterSelectionAttribute("Value Filter", false, data);

		attributes.add(filterSelection);

		this.frame = new Attribute.RangedAttribute("Waxis", 0f, 1f, 0f, false);
		attributes.add(this.frame);


		this.timerCycle = new Timer(16, new ActionListener() {
			private boolean forward = true;
			@Override
			public void actionPerformed(ActionEvent e) {
				float delta = forward ? 0.01f : -0.01f;
				float newValue = PointCloud.this.frame.getValue() + delta;
				if (newValue >= PointCloud.this.frame.getMax()) {
					forward = false;
					PointCloud.this.frame.notifyWithValue(PointCloud.this.frame.getMax());
				} else if (newValue <= PointCloud.this.frame.getMin()){
					forward = true;
					PointCloud.this.frame.notifyWithValue(PointCloud.this.frame.getMin());
				} else {
					PointCloud.this.frame.notifyWithValue(newValue);
				}
				PointCloud.this.frame.updateAttributeDisplayer();


			}
		});

		this.cycling = new Attribute.BinaryAttribute("Cyle", false, false);
		this.cycling.callback = (obj) -> {
			boolean on = ((Boolean)obj).booleanValue();
			if (on) {
				PointCloud.this.timerCycle.start();
			}
			else {
				PointCloud.this.timerCycle.stop();
			}
		};
		attributes.add(this.cycling);

		this.slitherPositionAttribute = new Attribute.RangedAttribute("Slither Pos", 0f, 1f, 0f, false);
		this.slitherPositionAttribute.callback = (obj) -> {
			Vector3 oldOrigin = this.selection.getVolume().origin;
			Vector3 oldSize = this.selection.getVolume().size;

			float newZ = this.getSlither(false).origin.get(this.slitherAxis.ordinal());
			float[]pos = oldOrigin.toArray();
			pos[slitherAxis.ordinal()] = newZ;

			Vector3 newOrigin = new Vector3(pos);

			Volume volume = new Volume(newOrigin, oldSize);
			this.selection.setVolume(volume);
		};

		this.attributes.add(this.slitherPositionAttribute);

		this.displaySlitherenated = new Attribute.BinaryAttribute("Slitherise", false, false);
		this.attributes.add(displaySlitherenated);

		this.slitherCycleTimer = new Timer(16, new ActionListener() {
			private boolean forward = true;
			@Override
			public void actionPerformed(ActionEvent e) {
				float delta = forward ? 0.01f : -0.01f;
				float newValue = PointCloud.this.slitherPositionAttribute.getValue() + delta;
				if (newValue >= PointCloud.this.slitherPositionAttribute.getMax()) {
					forward = false;
					PointCloud.this.slitherPositionAttribute.notifyWithValue(PointCloud.this.slitherPositionAttribute.getMax());
				} else if (newValue <= PointCloud.this.frame.getMin()){
					forward = true;
					PointCloud.this.slitherPositionAttribute.notifyWithValue(PointCloud.this.slitherPositionAttribute.getMin());
				} else {
					PointCloud.this.slitherPositionAttribute.notifyWithValue(newValue);
				}
				PointCloud.this.slitherPositionAttribute.updateAttributeDisplayer();
			}
		});

		this.cyclingSlitherAttribute = new Attribute.BinaryAttribute("Cycle", false, false);
		this.attributes.add(this.cyclingSlitherAttribute);

		this.cyclingSlitherAttribute.callback = (obj) -> {
			boolean on = ((Boolean)obj).booleanValue();
			if (on) {
				PointCloud.this.slitherCycleTimer.start();
			}
			else {
				PointCloud.this.slitherCycleTimer.stop();
			}
		};

		List<Object> possiblePairings = new ArrayList<>();
		possiblePairings.add("-");
		relativeTo = new Attribute.MultiChoiceAttribute("Relative to", possiblePairings, possiblePairings.get(0));
		relativeTo.callback = (obj) -> {
			if (obj instanceof PointCloud) {
				PointCloud parent = (PointCloud) obj;
				this.setVolume(this.volumeNormalisedToParent(parent));
			} else {
				this.setVolume(this.backupVolume);
			}
		};

		attributes.add(relativeTo);
		this.color = DEFAULT_COLORS[clouds++ % DEFAULT_COLORS.length];
	}

	public void readFits() {
		readFitsAtQualityLevel();
	}

	private void readFitsAtQualityLevel() {


		try{
			this.fits = new Fits(this.fileName.getValue());

			ImageHDU hdu = (ImageHDU)this.fits.getHDU(0);


			Attribute.TextAttribute unitAttribute = new Attribute.TextAttribute("Value Unit", "" + hdu.getHeader().getStringValue("BUNIT"), false);
			attributes.add(1, unitAttribute);

			this.filterSelection.setAxisName(unitAttribute.getValue());

			this.unitTypes = new Attribute.TextAttribute[hdu.getAxes().length];
			for (int i = hdu.getAxes().length-1; i >= 0 ; i--) {
				unitAttribute = new Attribute.TextAttribute(AXES_NAMES[i] + " Unit", "" + hdu.getHeader().getStringValue("CTYPE"+(i+1)), false);
				this.unitTypes[i] = unitAttribute;
				attributes.add(1, unitAttribute);
			}


			for (int i = hdu.getAxes().length-1; i >= 0 ; i--) {
				attributes.add(1,new Attribute.TextAttribute(AXES_NAMES[i] + " pts", "" + hdu.getAxes()[i], false));
			}


			for (Attribute attr : attributes) {
				if (attr instanceof Attribute.TextAttribute) {
					Attribute.TextAttribute namedAttr = (Attribute.TextAttribute)attr;
					if (namedAttr.getValue() == null || namedAttr.getValue().equals("") || namedAttr.getValue().equals("null"))
						namedAttr.notifyWithValue("?");
				}
			}

			//--figure out the reference position

			Header header = hdu.getHeader();
			int naxis = hdu.getAxes().length;
			float[] pixels = new float[naxis];
			float[] values = new float[naxis];
			float[] sizes = new float[naxis];
			for (int i = 1; naxis <= hdu.getAxes().length; naxis++) {
				pixels[i - 1] = header.getFloatValue("CRPIX"+i)/ (float) hdu.getAxes()[i - 1];
				sizes[i - 1] = header.getFloatValue("CDELT"+i) * (float) hdu.getAxes()[i - 1];
				values[i - 1] = header.getFloatValue("CRVAL"+i);
			}

			Vector3 refPixel = new Vector3(pixels);
			Vector3 refCoordinate = new Vector3(values);
			Vector3 size = new Vector3(sizes);

			Vector3 realOrigin = refCoordinate.minus(refPixel.scale(size));

			this.galacticVolume = new Volume(realOrigin, size);

			Volume v = new Volume(0f,0f,0f,1f,1f,1f);


			Region region = new Region(fits, v, this.quality.getValue());
			this.addRegion(region);

			FrameMaster.setNeedsNewRenderer();
			FrameMaster.setNeedsDisplay();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * All the slices that compose this point cloud.  This includes from all the contained regions.  There are no guarancees concerning order.
	 * @return The list of all slices composing the point cloud
	 */
	public List<VertexBufferSlice>getSlices() {
		List<VertexBufferSlice>slices = new ArrayList<VertexBufferSlice>();
		for (Region region:this.regions) {
			slices.addAll(region.getSlices());
		}
		return slices;
	}


	private void addRegion (Region cr) {
		this.regions.add(cr);

		for (Region region : this.regions) {
			List<Region>chrilden = new ArrayList<>();
			for (Region potentialChild : this.regions) {
				Vector3 origin = potentialChild.getVolume().origin;
				Vector3 extremety = origin.add(potentialChild.getVolume().size);
				boolean containsOrigin = region.getVolume().containsPoint(origin);
				boolean containsExtremety = region.getVolume().containsPoint(extremety);
				boolean isNotParadox = region != potentialChild;
				if (containsOrigin && containsExtremety && isNotParadox) {
					chrilden.add(potentialChild);
				}
			}
			region.setMinusRegions(chrilden);
		}
	}


	/**
	 * Cuts out a volume from the primary region.  The cut out region is kept in the point cloud as a new region.
	 * @param volume The volume to cut out (in world space)
	 */
	public void cutOutSubvolume(Volume volume) {
		Volume subRegion = this.volume.normalisedProportionVolume(volume);
		Region newRegion = this.regions.get(0).subRegion(subRegion, this.regions.get(0).getRegionRepresentation().getFidelity(), true);
		this.addRegion(newRegion);
		FrameMaster.notifyFileBrowserOfNewRegion(this, newRegion);
		FrameMaster.setNeedsNewRenderer();
		FrameMaster.setNeedsDisplay();
	}




	/**
	 * Get all the children of this point cloud that could provide their own attribuets (eg. Regions)
	 * @return The list of all children that provide attributes
	 */
	public List<AttributeProvider> getChildProviders() {
		List<AttributeProvider>attributeProviders = new ArrayList<>(this.regions);
		return attributeProviders;
	}


	/**
	 * Returns a volume that represents this point cloud scaled down and position relative to the supplied parent.
	 * @param parent The parent cloud to position and scale the volume relative to
	 * @return The scaled and positioned volume that is relative to the parent
	 */
	public Volume volumeNormalisedToParent(PointCloud parent) {
		Volume parGalVol = parent.galacticVolume;
		Volume parVol = parent.volume;
		Volume galVol = this.galacticVolume;

		Vector3 realDist = galVol.origin.minus(parGalVol.origin);

		float[] realToDisplayFactorArray = new float[3];
		for (int i = 0; i < 3; i++) {
			realToDisplayFactorArray[i] = parVol.size.get(i)/ parGalVol.size.get(i);
		}
		Vector3 realToDisplayFactor = new Vector3(realToDisplayFactorArray);

		Vector3 displayOrigin = parVol.origin.add(realDist.scale(realToDisplayFactor));
		Vector3 displaySize = galVol.size.scale(realToDisplayFactor);

		//--ok so heres what's going to happen if the units don't match that of the real cube then it will just match the parent cube in that dimension.  So if none of the dimensions match both cubes should perfectly sit on top of each other.
		for (int i = 0; i < 3; i++) {
			if (this.unitTypes[i].getValue().equals(parent.unitTypes[i].getValue()) == false) {
				System.err.println("Cannot position clouds relative in "+ AXES_NAMES[i]+" axis as there is a unit mismatch ("+this.unitTypes[i].getValue() +" != " + parent.unitTypes[i].getValue()+")");
				float []newOriginValues = displayOrigin.toArray();
				newOriginValues[i] = parVol.origin.get(i);
				displayOrigin = new Vector3(newOriginValues);
			}
		}

		Volume newDisplayVolume = new Volume(displayOrigin, displaySize);

		return newDisplayVolume;
	}


	public String toString() {
		String[] components = this.fileName.getValue().split(File.separator);
		return components[components.length - 1];
	}


	public static float fidelityToGetTargetPixels(Fits fits, int targetPixels) {
		try {
			ImageHDU hdu = (ImageHDU) fits.getHDU(0);
			int totalPixels = hdu.getAxes()[0] * hdu.getAxes()[1] * hdu.getAxes()[2];
			float proportionShouldLoad = (float) targetPixels / (float) totalPixels;
			float proportionShouldLoadPerAxis = (float) Math.pow((double) proportionShouldLoad, 1.0 / 3.0);

			float proportionOfPerfect = proportionShouldLoadPerAxis;
			if (proportionOfPerfect > 1.0f) {
				proportionOfPerfect = 1.0f;
			}
			return proportionOfPerfect;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 1.0f;
	}




	//==================================================================================================================
	//  HISTOGRAM
	//==================================================================================================================

	public int[] getHistBuckets() {
		return this.regions.get(0).getRegionRepresentation().getBuckets();
	}


	public float getHistMin() {
		return this.regions.get(0).getRegionRepresentation().getEstMin();
	}


	public float getHistMax() {
		return this.regions.get(0).getRegionRepresentation().getEstMax();
	}


	public Christogram.Filter getFilter() {
		return this.filterSelection.getValue();
	}






	//==================================================================================================================
	//  GETTERS + SETTERS
	//==================================================================================================================

	public Volume getVolume() {
		return volume;
	}


	public void setVolume(Volume volume) {
		this.volume = volume;
	}


	@Override
	public List<Attribute> getAttributes() {
		List<Attribute>visibleAttributes = new ArrayList<>();
		visibleAttributes.addAll(this.attributes);
		return visibleAttributes;
	}


	public List<Region> getRegions() {
		return regions;
	}

	public int getNumberOfFrames() {
		return this.getRegions().get(0).getFramesInPoints();
	}

	public boolean shouldDisplayFrameWithW(float w) {
		float step = 1.0f/(float)this.getNumberOfFrames();

		int stepsCurrent = (int)(this.frame.getValue() / step);
		int stepsNext = stepsCurrent + 1;

		float min = stepsCurrent * step;
		float max = stepsNext * step;

		return w >= min && w <= max;
	}

	/**
	 * Gets the slither volume to display to the user.
	 * @return
	 */
	public Volume getSlither(boolean normalised) {
		float []originArray = new float[3];
		originArray[this.slitherAxis.ordinal()] = this.slitherPositionAttribute.getValue();
		Vector3 origin = new Vector3(originArray);

		float sliceWidth = 1.0f/(float)this.regions.get(0).getDimensionInPts(this.slitherAxis.ordinal());
		float []sizeArray = {1.0f, 1.0f, 1.0f};
		sizeArray[this.slitherAxis.ordinal()] = sliceWidth;
		Vector3 size = new Vector3(sizeArray);


		if (!normalised) {
			origin = origin.scale(this.volume.size).add(this.volume.origin);
			size = size.scale(this.volume.size);

		}

//		originArray[slitherAxis.ordinal()] += sizeArray[slitherAxis.ordinal()]/2f;

		Volume vol = new Volume(origin, size);

		return vol;
	}

	/**
	 * Gets whether the point cloud should be displayed slitherenated
	 */

	public boolean shouldDisplaySlitherenated() {
		return this.displaySlitherenated.getValue();
	}

	public void setShouldDisplaySlitherenated(boolean shouldDisplay) {
		this.displaySlitherenated.notifyWithValue(shouldDisplay);
	}

	public Axis getSlitherAxis() {
		return this.slitherAxis;
	}

	public Selection getSelection() {
		return selection;
	}

	public Fits getFits() {
		return fits;
	}
}
