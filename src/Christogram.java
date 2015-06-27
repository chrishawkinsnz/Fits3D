

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

import org.omg.CORBA.PRIVATE_MEMBER;



public class Christogram extends JComponent implements MouseMotionListener, MouseListener{
	private static final int ticks = 5;

	public static class SelectionDistribution{
		public float start;
		public float end;
		public final boolean isExponential;
		
		private SelectionDistribution(float start, float end, boolean isExponential) {
			this.start = start;
			this.end = end;
			this.isExponential = isExponential;
		}
		
		public static SelectionDistribution distributionWithLinearIncrease(float startValue, float endValue) {
			return new SelectionDistribution(startValue, endValue, false);
		}
		
		public static SelectionDistribution distributionWithStaticValue(float value) {
			return new SelectionDistribution(value, value, false);
		}
		
		public static SelectionDistribution distributionWithExponentialIncrease(float startValue, float endValue) {
			return new SelectionDistribution(startValue, endValue, true);
		}
		
		private static int currentDefault = -1;
		public static SelectionDistribution nextDefault() {
			currentDefault = ++currentDefault % 3;
			switch (currentDefault) {
			case 0:
				return SelectionDistribution.distributionWithLinearIncrease(0f, 1f);
			case 1:
				return SelectionDistribution.distributionWithStaticValue(1f);
			case 2:
				return SelectionDistribution.distributionWithExponentialIncrease(0f, 1f);
			default:
				return null;
			}
		}
	}
		
	
	private float[] values;
	
	private float[] buckets;
	private float maxBucket;
	
	private int nBuckets;
	private String title;
	private String xAxisTitle;
	
	private float min = 0.0f;
	private float max = 0.0f;

	private int leftInset = 20;
	private int rightInset = 20;
	private int topInset = 20;
	private int botInset = 70;
	
	private int x = 20;
	private int y = 20;
	private int wd = 50;
	private int ht = 10;

	private float selectionBegin = 0f;

	private float selectionCurrent = 0f;
	
	
	private SelectionDistribution currentDistribution;
	
	public Christogram(float[] values, float min, float max, int buckets) {
		this.min = min;
		this.max = max;
		this.values = values;
		this.nBuckets = buckets;
		this.bucketise();
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		
		this.currentDistribution = SelectionDistribution.nextDefault();
	}
	
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public void setXAxisTitle(String title){
		this.xAxisTitle = title;
	}
	
	
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.drawLine(x, y, x + wd, y + ht);
		
		//--draw box
		g.setColor(Color.lightGray);
		g.fillRect(chartLeft(), chartTop(), chartWidth(), chartHeight());
		
		
		//--draw bars
		for(int bindex = 0; bindex < nBuckets; bindex++) {
			float relFreq = buckets[bindex];
			float relRelFreq = (relFreq - 0f)/ maxBucket;
			
			float xProportion = (float)bindex / (float)nBuckets;
			
			
			int width = chartWidth() / nBuckets;
			int height = (int) (relRelFreq * (float)chartHeight());
			
			int x1 = chartLeft() + (int)(xProportion * chartWidth());
			int y1 = chartBot() - height;
			
			g.setColor(Color.gray);
			g.fillRect(x1, y1, width, height);
			g.setColor(Color.black);
			g.drawRect(x1, y1, width, height);
		}
	
		//--draw selection
		float minSelection = selectionBegin < selectionCurrent ? selectionBegin : selectionCurrent;
		float maxSelection = selectionBegin < selectionCurrent ?  selectionCurrent : selectionBegin;
		
		float startProportion = (minSelection - min)/(max - min);
		float endProportion = (maxSelection - min)/(max - min);
		
		int x1 = chartLeft() + (int)(chartWidth() * startProportion);
		int y1 = chartTop();
		int width = (int)(chartWidth() * (endProportion - startProportion));
		int height = chartHeight();
		int y2;
		int x2;
		g.setColor(new Color(1.0f, 0.5f, 0.5f, 0.5f));
		g.fillRect(x1, y1, width, height);
		
		
		//--draw distribution line within selection
		g.setColor(new Color(1.0f, 0f, 0f, 1f));
		if (this.currentDistribution.isExponential) {
			x1 = x1 - width;
			x2 = x1 + width;
			height = (int) (this.currentDistribution.end * chartHeight()) - (int)(this.currentDistribution.start * chartHeight());
			y1 = (int) (chartBot() - this.currentDistribution.start * chartHeight()) - height * 2;
			g.drawArc(x1, y1, width * 2, height * 2, 270, 90);
		}
		else {
			x1 = x1;
			x2 = x1 + width;
			y1 = (int) (chartBot() - this.currentDistribution.start * chartHeight());
			y2 = (int) (chartBot() - this.currentDistribution.end * chartHeight());
			System.out.println("y1:"+y1+" y2:"+y2);
			g.drawLine(x1, y1, x2, y2);	
		}
		
		
		
		//--draw the ticks
		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(Color.black);
		float stepSize = (float)chartWidth() / (float)(ticks - 1);
		for (int tick = 0; tick < ticks; tick++) {

			FontMetrics fm   = g.getFontMetrics(g2d.getFont());
			java.awt.geom.Rectangle2D rect = fm.getStringBounds(xAxisTitle, g2d);

			int textHeight = (int)(rect.getHeight()); 
			int textWidth  = (int)(rect.getWidth());
			
			int tickX = chartLeft() - (int)(stepSize * tick);

			if (tick > 0 && tick < (ticks - 1)) {
				tickX += textHeight/2;
			}
			else if (tick == (ticks - 1)) {
				tickX += textHeight;
			}
			
			int tickY = chartTop() + chartHeight() + 5;//chartBot();
			AffineTransform orig = g2d.getTransform();
			g2d.drawLine(chartLeft() + (int)(stepSize * tick), chartBot(), chartLeft() + (int)(stepSize * tick), chartBot() + 4);
			g2d.rotate(Math.PI/2);
			
			float value = tick * stepSize;
			String valueString = "" + value;
			if (valueString.length() > 4) 
				valueString = valueString.substring(0, 4);
			if (valueString.charAt(valueString.length() - 1) == '.') {
				valueString = valueString.substring(0, valueString.length() - 1);
			}

			g2d.drawString(valueString, tickY, tickX);
			
			g2d.setTransform(orig);
			
			g2d.drawString(xAxisTitle, chartLeft() + chartWidth()/2 - textWidth/2, chartBot() + 50);

			System.out.println("text width" + textWidth);
			
			
		}
	}
	
	
	
	
	
	private void bucketise() {
		int []counts = new int[nBuckets];
		float stepSize = (max - min) / (float)nBuckets; 
		for (float val : values) {
			int bucketIndex = (int)(val/stepSize);
			
			if (bucketIndex > counts.length) 
				counts[counts.length]++;
			else
				counts[bucketIndex]++;
		}
		
		//--draw
		maxBucket = -999f;
		
		buckets = new float[nBuckets];
		for(int bindex = 0; bindex < nBuckets; bindex++) {
			float relFreq = (float)counts[bindex]/(float)values.length;
			buckets[bindex] = relFreq;
			if (relFreq > maxBucket) 
				maxBucket = relFreq;
			
			System.out.println("bucket"+bindex + ": "+relFreq);
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
		this.repaint();
	}


	@Override
	public void mouseClicked(MouseEvent e) {}

	private float tentativeSelectionBegin = Float.MIN_VALUE;
	@Override
	public void mousePressed(MouseEvent e) {
		this.tentativeSelectionBegin = proportionAtPixelX(e.getX());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		float tentativeSelectionEnd = proportionAtPixelX(e.getX());
		if (tentativeSelectionEnd == tentativeSelectionBegin) {
			this.currentDistribution = SelectionDistribution.nextDefault();
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

}
