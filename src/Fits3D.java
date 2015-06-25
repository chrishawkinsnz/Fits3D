import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
	        	//Application.getApplication().setDockIconBadge("Alpha");
	        	BufferedImage img = null;
	        	try {
	        	    img = ImageIO.read(new File("stupidSpaceIcon.png"));
	        	    
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
}
