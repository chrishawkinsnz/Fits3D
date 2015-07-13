import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.omg.CORBA.PRIVATE_MEMBER;

public class Christogram extends JComponent implements MouseMotionListener, MouseListener{
	private static final int ticks = 5;
//	private float[] values;
	
	private float[] buckets;
	private float maxBucket;
	
	private int nBuckets;

	private String xAxisTitle;
	
	private float min = 0.0f;
	private float max = 0.0f;

	private int leftInset = 0;
	private int rightInset = 0;
	private int topInset = 0;
	private int botInset = 70;

	private float selectionBegin = 0f;
	private float selectionCurrent = 0f;
	private Filter currentFilter;
	
	public Christogram(float[] values, float min, float max, int buckets) {
		this.min = min;
		this.max = max;
		this.nBuckets = buckets;
		this.bucketise(values);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		
		this.currentFilter = Filter.distributionWithLinearIncrease(0f, 1f);
		this.currentFilter.minX = this.min;
		this.currentFilter.maxX = this.max;
	}
	
	public Christogram(int[]counts, float min, float max) {
		this.min = min;
		this.max = max;
		this.nBuckets = counts.length;
		this.turnCountsIntoRelFreqsLol(counts);

		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.currentFilter = Filter.distributionWithLinearIncrease(0f, 1f);

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
		float minSelection = this.currentFilter.minX;//selectionBegin < selectionCurrent ? selectionBegin : selectionCurrent;
		float maxSelection = this.currentFilter.maxX;//selectionBegin < selectionCurrent ? selectionCurrent : selectionBegin;
		
		//--find the proportion along the chart each is
		float startProportion = (minSelection - min)/(max - min);
		float endProportion = (maxSelection - min)/(max - min);
		
		int x1 = chartLeft() + (int)(chartWidth() * startProportion);
		int y1 = chartTop();
		int width = (int)(chartWidth() * (endProportion - startProportion));
		int height = chartHeight();
		g.setColor(new Color(1.0f, 0.5f, 0.5f, 0.5f));
		g.fillRect(x1, y1, width, height);
		
		g.setColor(new Color(1.0f, 0f, 0f, 1f));
		
		g.drawLine(x1, chartBot(), x1, chartTop());
		g.drawLine(x1 + width, chartBot(), x1 + width, chartTop());
		
		//--draw distribution line within selection
		g.setColor(new Color(1.0f, 0f, 0f, 1f));
		
		if (this.currentFilter.isExponential) {
			height = (int) (this.currentFilter.maxY * chartHeight()) - (int)(this.currentFilter.minY * chartHeight());
			x1 = x1 - width;
			y1 = (int) (chartBot() - this.currentFilter.minY * chartHeight()) - height * 2;
			g.drawArc(x1, y1, width * 2, height * 2, 270, 90);
		}
		else {
			int x2 = x1 + width;
			y1 = (int) (chartBot() - this.currentFilter.minY * chartHeight());
			int y2 = (int) (chartBot() - this.currentFilter.maxY * chartHeight());
			g.drawLine(x1, y1, x2, y2);	
		}
		
		//--draw the ticks
		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(Color.black);
		float stepSize = (float)chartWidth() / (float)(ticks - 1);
		for (int tick = 0; tick < ticks; tick++) {
			int tickX = chartLeft() - (int)(stepSize * tick);

			//--fudge the tick position so the start and end labels aren't offscreen
			if (tick > 0 && tick < (ticks - 1)) {
				tickX += 10/2;
			}
			else if (tick == (ticks - 1)) {
				tickX += 10;
			}
			
			int tickY = chartTop() + chartHeight() + 5;
			
			//-rotate the view so our text is written sideways
			AffineTransform orig = g2d.getTransform();
			g2d.drawLine(chartLeft() + (int)(stepSize * tick), chartBot(), chartLeft() + (int)(stepSize * tick), chartBot() + 4);
			g2d.rotate(Math.PI/2);
			
			float value = tick * (max - min)/((float)(ticks-1)) + min;
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
	
	private void bucketise(float[]values) {
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
		return topInset;
	}
	
	private int chartBot() {
		return getSize().height - botInset;
	}
	
	private int chartLeft() {
		return leftInset;
	}
	
	private int chartRight() {
		return getSize().width - rightInset;
	}
	
	private int chartWidth() {
		return chartRight() - chartLeft();
	}
	
	private int chartHeight() {
		return chartBot() - chartTop();
	}
	
	
	public int getLeftInset() {
		return leftInset;
	}

	public void setLeftInset(int leftInset) {
		this.leftInset = leftInset;
	}

	public int getRightInset() {
		return rightInset;
	}

	public void setRightInset(int rightInset) {
		this.rightInset = rightInset;
	}

	public int getTopInset() {
		return topInset;
	}

	public void setTopInset(int topInset) {
		this.topInset = topInset;
	}

	public int getBotInset() {
		return botInset;
	}

	public void setBotInset(int botInset) {
		this.botInset = botInset;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.tentativeSelectionBegin!=Float.MIN_VALUE) 
			this.selectionBegin = tentativeSelectionBegin;
		this.selectionCurrent =  proportionAtPixelX(e.getX());
		this.currentFilter.updateWithXBounds(this.selectionBegin, this.selectionCurrent);
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
			this.currentFilter.nextDefaultDistribution();
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

	
	public static class Filter{
		public float minX;
		public float maxX;
		public float minY = 0f;
		public float maxY = 1f;
		public boolean isExponential;
		
		public Filter(float minX, float maxX, float minY, float maxY, boolean isExponential) {
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
			this.isExponential = isExponential;
		}
		
		public void updateWithXBounds(float selectionBegin,
				float selectionCurrent) {
			this.minX = selectionBegin < selectionCurrent ? selectionBegin : selectionCurrent;
			this.maxX = selectionBegin > selectionCurrent ? selectionBegin : selectionCurrent;
		}

		public void nextDefaultDistribution() {
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

		private Filter(float start, float end, boolean isExponential) {
			this.minX = start;
			this.maxX = end;
			this.isExponential = isExponential;
		}
		
		public static Filter distributionWithLinearIncrease(float startValue, float endValue) {
			return new Filter(startValue, endValue, false);
		}
		
		public static Filter distributionWithStaticValue(float value) {
			return new Filter(value, value, false);
		}
		
		public static Filter distributionWithExponentialIncrease(float startValue, float endValue) {
			return new Filter(startValue, endValue, true);
		}
		
		private static int currentDefault = 0;
//		public static Filter nextDefault() {
//			currentDefault = ++currentDefault % 3;
//			switch (currentDefault) {
//			case 0:
//				return Filter.distributionWithLinearIncrease(0f, 1f);
//			case 1:
//				return Filter.distributionWithStaticValue(1f);
//			case 2:
//				return Filter.distributionWithExponentialIncrease(0f, 1f);
//			default:
//				return null;
//			}
//		}
		
	}
	
	public Filter getCurrentFilter() {
		return this.currentFilter;
	}
	
	public void addChangeListener(ChangeListener changeListener) {
		this.changeListener = changeListener;
	}
}
