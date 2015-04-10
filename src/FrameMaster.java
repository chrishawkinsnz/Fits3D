import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL2.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

public class FrameMaster extends JFrame implements GLEventListener {
    private static final long serialVersionUID = 1L;
    private FPSAnimator animator;
	private PointCloud pointCloud;
	private Renderer renderer;
	private GL2 gl;
	
	private boolean debug = true;
	
	private boolean stupidBoolean = false;
	
    public FrameMaster() {
    	super("Very Good Honours Project");
    	    	
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);

        this.setName("Very Good Honours Project");
        this.setMinimumSize(new Dimension(800, 600));
    	this.setLocationRelativeTo(null);
    	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	this.setVisible(true);
    	this.setResizable(false);
    	this.setLayout(new BorderLayout());

        GLCanvas canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this);
        this.getContentPane().add(canvas);

        JPanel buttonBar = buttonBar();
        this.add(buttonBar, BorderLayout.SOUTH);
        
        
        JMenuBar menuBar = menuBar();
        this.setJMenuBar(menuBar);
                
        this.pack();

        canvas.requestFocusInWindow();

        this.animator = new FPSAnimator(canvas, 60);
        this.animator.start();

	}
    
    private JPanel buttonBar(){
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.orange);
        buttonPanel.setMinimumSize(new Dimension(0, 128));
        
        JButton spinButton = new JButton("Spin");
        spinButton.addActionListener(e -> this.renderer.toggleSpinning());
        buttonPanel.add(spinButton);
        
        
        JButton loadExampleButton = new JButton("Load Example");       
        loadExampleButton.addActionListener(e -> this.loadFile("12CO_MEAN.fits"));
        buttonPanel.add(loadExampleButton);
        
        return buttonPanel;
    }
    
    private void loadFile(String fileName) {
    	this.pointCloud = new PointCloud(fileName);
    	this.pointCloud.readFits();
    	this.pointCloud.loadFloatBuffers();
//    	this.pointCloud.createDummyFloatBuffers();
    	this.stupidBoolean = true;
    	
    }
    
    private JMenuBar menuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem loadItem = new JMenuItem("Load");
        
        fileMenu.add(loadItem);
        menuBar.add(fileMenu);
        return menuBar;
    }
    
    @Override
    public void display(GLAutoDrawable drawable) {
    	
    	if (this.stupidBoolean == true) {
    		this.renderer = new Renderer(this.pointCloud, this.gl);
    		this.stupidBoolean = false;
    	}
    	if (this.renderer != null) {
    		this.renderer.display();
    	}
    	else {
    		gl.glLoadIdentity();
        	gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        	gl.glColor3f(1.0f, 1.0f, 0.0f);
        	gl.glBegin(GL_LINES);
        		gl.glVertex3f(0f, 0f, 0.5f);
        		gl.glVertex3f(1f, 1f,0.5f);
        	gl.glEnd();
        	gl.glFlush();
    	}
    	
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void init(GLAutoDrawable drawable) {
    	if (debug)
    		this.gl = new DebugGL2(drawable.getGL().getGL2());
    	else 
			this.gl = drawable.getGL().getGL2();
		
    	
    	gl.glMatrixMode(GL_PROJECTION);
    	gl.glLoadIdentity();
    	gl.glOrtho(-3.2, 3.2, -2.4, 2.4, -6, 6);
    	gl.glMatrixMode(GL_MODELVIEW);
    	
    	gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL_FUNC_ADD);
		
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
            int height) {
    }

	public void play() {
		// TODO Auto-generated method stub
		
	}
}