

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.omg.CORBA.PRIVATE_MEMBER;


public class Christogram extends JComponent {
	
	private float[] values;
	
	private float[] buckets;
	private float maxBucket;
	
	private int nBuckets;
	private String title;
	private String xAxisTitle;
	private String yAxisTitle;
	
	private float min = 0.0f;
	private float max = 0.0f;

	private int leftInset = 20;
	private int rightInset = 20;
	private int topInset = 20;
	private int botInset = 20;
	private int x = 20;
	private int y = 20;
	private int wd = 50;
	private int ht = 10;

	
	public Christogram() {
		float []dubs = new float[50];
        for (int i = 0; i < dubs.length ; i++) {
        	dubs [i] = (float)i*(i*2);
        }    
	    this.values = dubs;	        
	}
	
	public Christogram(float[] values, float min, float max, int buckets) {
		this.min = min;
		this.max = max;
		this.values = values;
		this.nBuckets = buckets;
		this.bucketise();
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public void setXAxisTitle(String title){
		this.xAxisTitle = title;
	}
	
	
	
	@Override
	protected void paintComponent(Graphics g) {
		// TODO Auto-generated method stub
		super.paintComponent(g);

		g.drawLine(x, y, x + wd, y + ht);
		
		//--draw box
		g.setColor(Color.lightGray);
		g.fillRect(chartTop(), chartLeft(), chartWidth(), chartHeight());
		
		
		//--get min and max
		for(int bindex = 0; bindex < nBuckets; bindex++) {
			float relFreq = buckets[bindex];
			float relRelFreq = (relFreq - 0f)/ maxBucket;
			
			float xProportion = (float)bindex / (float)nBuckets;
			
			
			int width = chartWidth() / nBuckets;
			int height = (int) (relRelFreq * (float)chartHeight());
			
			int x1 = chartLeft() + (int)(xProportion * chartWidth());
			int y1 = chartBot() - height;
			
			Color[]cols = {Color.red,Color.green,Color.blue}; 
			g.setColor(Color.gray);
			g.fillRect(x1, y1, width, height);
			g.setColor(Color.black);
			g.drawRect(x1, y1, width, height);
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
	
	private Point chartTopLeft() {
		return new Point(chartLeft(), chartTop());
	}
	
	private Point chartBotLeft() {
		return new Point(chartLeft(), chartBot());
	}
	
	private Point chartTopRight() {
		return new Point(chartRight(), chartTop());
	}
	
	private Point chartBotRight() {
		return new Point(chartRight(), chartBot());
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
}
