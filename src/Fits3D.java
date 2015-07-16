import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.apple.eawt.Application;


public class Fits3D {

	public static void main(String[] args){
		try {
			String os = System.getProperty("os.name").toLowerCase();	
	        if (os.indexOf("mac") != -1) {
	        	System.setProperty("apple.laf.useScreenMenuBar", "true");
	        	System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Fits3D");	        	
	        	System.setProperty("com.apple.mrj.application.apple.menu.about.version", "0.1");
	        	BufferedImage img = null;
	        	try {
	        	    img = ImageIO.read(new File("resources/stupidSpaceIcon.png"));
	        	    
	        	    Application.getApplication().setDockIconImage(img);
	        	} catch (IOException e) {
	        	}
	        }
	            
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	        
	     }
	     catch(ClassNotFoundException e) {
	             System.out.println("ClassNotFoundException: " + e.getMessage());
	     }
	     catch(InstantiationException e) {
	             System.out.println("InstantiationException: " + e.getMessage());
	     }
	     catch(IllegalAccessException e) {
	             System.out.println("IllegalAccessException: " + e.getMessage());
	     }
	     catch(UnsupportedLookAndFeelException e) {
	             System.out.println("UnsupportedLookAndFeelException: " + e.getMessage());
	     }
	 
		 new FrameMaster();
	}
	static class IdealBlockSize {
	    // You could alternatively use BufferedInputStream and System.in .
	    private static class MyBufferedOS extends BufferedOutputStream {
	        public MyBufferedOS() { super(System.out); }
	        public MyBufferedOS(OutputStream out) { super(out); }
	        public int bufferSize() { return buf.length; }
	    }

	    public static int VALUE = new IdealBlockSize.MyBufferedOS().bufferSize();
	}



//	private VertexBufferSlice vertexAndValueBufferForSlice(float zProportion) {
//
//
//		Random r = new Random(this.seed);
//		long t0 = System.currentTimeMillis();
//		int extras = 100;
//		float[] vertexData = new float[this.numPtsX * this.numPtsY * 3 * 1 + extras * 3];
//		float[] valueData = new float[this.numPtsX * this.numPtsY * 1 * 1 + extras * 1];
//
//		float xStride = 1.0f/(float)this.numPtsX;
//		float yStride = 1.0f/(float)this.numPtsY;
//		float zStride = 1.0f/(float)this.numPtsZ;
//
//		int pts = 0;
//		float z =  zProportion;
//
//
//		for (float y = 0.0f; y < 1.0f; y += yStride) {
//			for (float x = 0.0f; x < 1.0f; x += xStride) {
//				float value = data[(int)(x * this.numPtsX)][(int)(y * this.numPtsY)][(int)(z * this.numPtsZ)];
//				if (!Float.isNaN(value) ) {
////				if (!Float.isNaN(value) && value > 0.0f) {
//					float fudge = r.nextFloat();
//					fudge = fudge - 0.5f;
//
//					vertexData[pts * 3 + 0] = x + fudge * xStride;
//					vertexData[pts * 3 + 1] = y + fudge * yStride;;
//					vertexData[pts * 3 + 2] = z + fudge * zStride;;
//
//					valueData[pts] = value;
//					pts++;
//				}
//			}
//		}
//
//		FloatBuffer vertexBuffer = FloatBuffer.allocate(pts * 3);
//		vertexBuffer.put(vertexData, 0, pts * 3);
//		vertexBuffer.flip();
//
//		FloatBuffer valueBuffer = FloatBuffer.allocate(pts);
//		valueBuffer.put(valueData, 0, pts);
//		valueBuffer.flip();
//		long t1 = System.currentTimeMillis();
//		System.out.println("took "+(t1-t0) + " ms to load " + pts + "into buffers (" + (int)(zProportion * 100) + "% read)");
//
//		VertexBufferSlice vbs = new VertexBufferSlice();
//		vbs.vertexBuffer = vertexBuffer;
//		vbs.valueBuffer = valueBuffer;
//		vbs.numberOfPts = pts;
//		vbs.depthValue = zProportion;
//		return vbs;
//	}
}
