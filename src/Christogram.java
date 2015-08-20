import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Christogram extends JComponent implements MouseMotionListener, MouseListener{
	private static final Color SELECTION_COLOR_BACKGROUND = new Color(1.0f, 0.5f, 0.5f, 0.5f);
	private static final Color SELECTION_COLOR_OUTLINE = new Color(1.0f, 0.0f, 0.0f, 1.0f);
	private static final int TICKS_TO_DISPLAY = 5;

	private static final int LEFT_INSET = 0;
	private static final int RIGHT_INSET = 0;
	private static final int TOP_INSET = 0;
	private static final int BOT_INSET = 70;

	private float[] buckets;
	private float maxBucket;
	private int nBuckets;
	private String xAxisTitle;
	
	private float min = 0.0f;
	private float max = 0.0f;



	private float mouseSelectionBeginX = 0f;
	private float mouseSelectionEndX = 0f;

	private ChristogramSelection selection;

	/**
	 * Constructs a christogram with the given values.  This will automatically find the min and max and split the values into the supplied buckets.
	 *
	 * WARNING:This method is extremely slow and requires iterating over all of the supplied values twice.
	 *
	 * @param values The values to display
	 * @param nBuckets The number of buckets to divide the values into
	 */
	public Christogram(float[] values, int nBuckets) {
		this.nBuckets = nBuckets;

		//--find the minimum and maximum values;
		this.min = Float.MAX_VALUE;
		this.max = Float.MIN_VALUE;
		for (float f : values) {
			if (f < min) { min = f;}
			if (f > max) { max= f;}
		}

		this.bucketise(values, nBuckets);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);

		this.selection = ChristogramSelection.distributionWithLinearIncrease(0f, 1f);
		this.selection.minX = this.min;
		this.selection.maxX = this.max;
	}

	/**
	 * Constructs a Christogram with the supplied counts.  The counts are the frequency of values within each bucket.
	 * The buckets are assumed to be evenly distributed between min and max with the first element of counts corresponding to the lowest bucket.
	 * @param counts The array of counts for each bucket.
	 * @param min The minimum a value can be.  Represents the lower bound of the first bucket
	 * @param max The maximum a value can be.  Represents the upper bound of the last bucket
	 */
	public Christogram(int[]counts, float min, float max) {
		this.min = min;
		this.max = max;
		this.nBuckets = counts.length;
		this.turnCountsIntoRelFreqsLol(counts);

		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.selection = ChristogramSelection.distributionWithLinearIncrease(0f, 1f);
	}
	
	
	public void setXAxisTitle(String title){
		this.xAxisTitle = title;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		//--draw box
		g.setColor(Color.lightGray);
		g.fillRect(chartLeft(), chartTop(), chartWidth(), chartHeight());
		g.setColor(Color.black);
		g.drawRect(chartLeft(), chartTop(), chartWidth(), chartHeight());
		
		//--draw bars
		for(int bindex = 0; bindex < nBuckets; bindex++) {
			float relFreq = buckets[bindex];
			float relRelFreq = (relFreq - 0f)/ maxBucket;

			int width = chartWidth() / nBuckets;
			int height = (int) (relRelFreq * chartHeight());
			
			int x1 = chartLeft() + (int)(chartWidth() * bindex / nBuckets);
			int y1 = chartBot() - height;
			
			//--draw background of bar
			g.setColor(Color.gray);
			g.fillRect(x1, y1, width, height);
			
			//--draw outline of bar
			g.setColor(Color.black);
			g.drawRect(x1, y1, width, height);
		}
	
		//--draw selection
		
		//--figure out which one is on the left and which is on the right
		float minSelection = this.selection.minX;//mouseSelectionBeginX < mouseSelectionEndX ? mouseSelectionBeginX : mouseSelectionEndX;
		float maxSelection = this.selection.maxX;//mouseSelectionBeginX < mouseSelectionEndX ? mouseSelectionEndX : mouseSelectionBeginX;
		
		//--find the proportion along the chart each is
		float startProportion = (minSelection - min)/(max - min);
		float endProportion = (maxSelection - min)/(max - min);
		
		int x1 = chartLeft() + (int)(chartWidth() * startProportion);
		int y1 = chartTop();
		int width = (int)(chartWidth() * (endProportion - startProportion));
		int height = chartHeight();
		g.setColor(SELECTION_COLOR_BACKGROUND);
		g.fillRect(x1, y1, width, height);
		
		g.setColor(SELECTION_COLOR_OUTLINE);
		
		g.drawLine(x1, chartBot(), x1, chartTop());
		g.drawLine(x1 + width, chartBot(), x1 + width, chartTop());
		
		//--draw distribution line within selection
		g.setColor(SELECTION_COLOR_OUTLINE);
		
		if (this.selection.isExponential) {
			height = (int) (this.selection.maxY * chartHeight()) - (int)(this.selection.minY * chartHeight());
			x1 = x1 - width;
			y1 = (int) (chartBot() - this.selection.minY * chartHeight()) - height * 2;
			g.drawArc(x1, y1, width * 2, height * 2, 270, 90);
		}
		else {
			int x2 = x1 + width;
			y1 = (int) (chartBot() - this.selection.minY * chartHeight());
			int y2 = (int) (chartBot() - this.selection.maxY * chartHeight());
			g.drawLine(x1, y1, x2, y2);	
		}
		
		//--draw the TICKS_TO_DISPLAY
		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(Color.black);
		float stepSize = (float)chartWidth() / (float)(TICKS_TO_DISPLAY - 1);
		for (int tick = 0; tick < TICKS_TO_DISPLAY; tick++) {
			int tickX = chartLeft() - (int)(stepSize * tick);

			//--fudge the tick position so the start and end labels aren't offscreen
			if (tick > 0 && tick < (TICKS_TO_DISPLAY - 1)) {
				tickX += 10/2;
			}
			else if (tick == (TICKS_TO_DISPLAY - 1)) {
				tickX += 10;
			}
			
			int tickY = chartTop() + chartHeight() + 5;
			
			//-rotate the view so our text is written sideways
			AffineTransform orig = g2d.getTransform();
			g2d.drawLine(chartLeft() + (int)(stepSize * tick), chartBot(), chartLeft() + (int)(stepSize * tick), chartBot() + 4);
			g2d.rotate(Math.PI/2);
			
			float value = tick * (max - min)/((float)(TICKS_TO_DISPLAY -1)) + min;
			String valueString = "" + value;
			
			//--cut the string down if necessarry (making sure not to leave a '.' on the end
			if (valueString.length() > 4) 
				valueString = valueString.substring(0, 4);
			if (valueString.charAt(valueString.length() - 1) == '.') {
				valueString = valueString.substring(0, valueString.length() - 1);
			}
			g2d.drawString(valueString, tickY, tickX);
			g2d.setTransform(orig);
			
			//--use the size of the font to figure out where to center the xAxis
			FontMetrics fm   = g.getFontMetrics(g2d.getFont());
			java.awt.geom.Rectangle2D rect = fm.getStringBounds(xAxisTitle, g2d);
			int textWidth  = (int)(rect.getWidth());
			g2d.drawString(xAxisTitle, chartLeft() + chartWidth()/2 - textWidth/2, chartBot() + 50);			
		}
	}
	
	private void bucketise(float[]values, int nBuckets) {
		int []counts = new int[nBuckets];
		float stepSize = (max - min) / (float)nBuckets; 
		for (float val : values) {
			int bucketIndex = (int)(val/stepSize);	
			if (bucketIndex > counts.length) 
				counts[counts.length]++;
			else
				counts[bucketIndex]++;
		}
		
		turnCountsIntoRelFreqsLol(counts);
	}
	
	private void turnCountsIntoRelFreqsLol(int[]counts) {
		
		int totValues = 0;
		for (int i : counts) {totValues+=i;}
		
		maxBucket = -999f;
		buckets = new float[nBuckets];
		 
		for(int bindex = 0; bindex < nBuckets; bindex++) {
			float relFreq = (float)counts[bindex]/(float)totValues;
			buckets[bindex] = relFreq;
			if (relFreq > maxBucket) 
				maxBucket = relFreq;			
		}
	}
	
	private int chartTop() {
		return TOP_INSET;
	}
	
	private int chartBot() {
		return getSize().height - BOT_INSET;
	}
	
	private int chartLeft() {
		return LEFT_INSET;
	}
	
	private int chartRight() {
		return getSize().width - RIGHT_INSET;
	}
	
	private int chartWidth() {
		return chartRight() - chartLeft();
	}
	
	private int chartHeight() {
		return chartBot() - chartTop();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.tentativeSelectionBegin!=Float.MIN_VALUE) 
			this.mouseSelectionBeginX = tentativeSelectionBegin;
		this.mouseSelectionEndX =  proportionAtPixelX(e.getX());
		this.selection.updateWithXBounds(this.mouseSelectionBeginX, this.mouseSelectionEndX);
		this.changeListener.stateChanged(new ChangeEvent(this));
		this.repaint();
	}


	@Override
	public void mouseClicked(MouseEvent e) {}

	private float tentativeSelectionBegin = Float.MIN_VALUE;

	private ChangeListener changeListener;
	@Override
	public void mousePressed(MouseEvent e) {
		this.tentativeSelectionBegin = proportionAtPixelX(e.getX());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		float tentativeSelectionEnd = proportionAtPixelX(e.getX());
		if (tentativeSelectionEnd == tentativeSelectionBegin) {
			this.selection.cycleToNextDistributionType();
			this.changeListener.stateChanged(new ChangeEvent(this));
			this.repaint();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {}
	
	private float proportionAtPixelX(int xPixel) {
		float proportion = ((float)(xPixel - chartLeft())) / ((float)chartWidth());
		float value = min + proportion * (max - min);
		return value;
	}

	
	public static class ChristogramSelection {
		private int currentDefault = 0;

		public float minX;
		public float maxX;
		public float minY = 0f;
		public float maxY = 1f;
		public boolean isExponential;
		
		public ChristogramSelection(float minX, float maxX, float minY, float maxY, boolean isExponential) {
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
			this.isExponential = isExponential;
		}

		public void updateWithXBounds(float selectionBegin, float selectionCurrent) {
			this.minX = selectionBegin < selectionCurrent ? selectionBegin : selectionCurrent;
			this.maxX = selectionBegin > selectionCurrent ? selectionBegin : selectionCurrent;
		}

		/**
		 * Cycles between the linear-flat, linear-increasing and exponential distributions
		 */
		public void cycleToNextDistributionType() {
			currentDefault = ++currentDefault % 3;
			switch (currentDefault) {
			case 0:
				this.isExponential = false;
				this.minY = 0f;
				this.maxY = 1f;
				break;
			case 1:
				this.isExponential = false;
				this.minY = 0.8f;
				this.maxY = 0.8f;
				break;
			case 2:
				this.isExponential = true;
				this.minY = 0f;
				this.maxY = 1f;
				break;
			}
		}

		private ChristogramSelection(float start, float end) {
			this.minX = start;
			this.maxX = end;
			this.isExponential = false;
		}
		
		public static ChristogramSelection distributionWithLinearIncrease(float startValueX, float endValueX) {
			ChristogramSelection selection = new ChristogramSelection(startValueX, endValueX);
			return selection;
		}
		
		public static ChristogramSelection distributionWithStaticValue(float startValueX, float endValueX) {
			ChristogramSelection selection = new ChristogramSelection(startValueX, endValueX);
			selection.cycleToNextDistributionType();
			return selection;
		}
		
		public static ChristogramSelection distributionWithExponentialIncrease(float startValueX, float endValueX) {
			ChristogramSelection selection = new ChristogramSelection(startValueX, endValueX);
			selection.cycleToNextDistributionType();
			selection.cycleToNextDistributionType();
			return selection;
		}

		

	}
	
	public ChristogramSelection getSelection() {
		return this.selection;
	}
	
	public void addChangeListener(ChangeListener changeListener) {
		this.changeListener = changeListener;
	}
}
