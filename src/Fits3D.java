import java.io.*;

import javax.swing.*;

import IO.ImageLoader;
import UserInterface.FrameMaster;
import com.apple.eawt.Application;


public class Fits3D {

	public static void main(String[] args){
		try {
			String os = System.getProperty("os.name").toLowerCase();


			if (os.indexOf("mac") != -1) {
	        	System.setProperty("apple.laf.useScreenMenuBar", "true");
	        	System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Fits3D");	        	
	        	System.setProperty("com.apple.mrj.application.apple.menu.about.version", "0.1");
				Application.getApplication().setDockIconImage(ImageLoader.loadImageNamed("stupidSpaceIcon.png"));
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
}
