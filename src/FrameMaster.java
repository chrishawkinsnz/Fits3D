import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.FloatBuffer;

import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3bc;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLOffscreenAutoDrawable.FBO;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.math.Matrix4;
import com.jogamp.opengl.util.FPSAnimator;

import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL2.*;

import javax.rmi.CORBA.Util;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import jogamp.opengl.awt.Java2D;

public class FrameMaster extends JFrame implements GLEventListener {
    private static final long serialVersionUID = 1L;
    private FPSAnimator animator;
	private PointCloud pointCloud;
	private Renderer renderer;
	private GL3 gl;
	
	private boolean debug = false;
	
	private boolean rendererNeedsNewPointCloud = false;
	
    public FrameMaster() {
    	super("Very Good Honours Project");
    	    	
        GLProfile profile = GLProfile.get(GLProfile.GL3);
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

        this.animator = new FPSAnimator(canvas, 120);
        this.animator.setUpdateFPSFrames(100,System.out);
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
        
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        slider.addChangeListener((ChangeEvent ce) -> {
			float proportion = (float)slider.getValue()/ (float)slider.getMaximum();
//			float adjustedProportion = 0.5f + (-0.5f + proportion)*(-0.5f + proportion)*(-0.5f + proportion);
//			System.out.println(adjustedProportion);

			this.renderer.alphaFudge = proportion * 0.1f;
		});
        buttonPanel.add(slider);
        return buttonPanel;
    }
    
    private void openDialog() {
    	JFileChooser jfc = new JFileChooser();
    	jfc.setAcceptAllFileFilterUsed(false);
    	jfc.setFileFilter(new FileFilterFits());
    	int returnVal = jfc.showOpenDialog(this);
    	if (returnVal == JFileChooser.APPROVE_OPTION) {
    		File file = jfc.getSelectedFile();
    		loadFile(file.getAbsolutePath());
    	}
    }
    

    private void loadFile(String fileName) {
    	this.pointCloud = new PointCloud(fileName);
    	this.pointCloud.readFits();
    	this.pointCloud.loadFloatBuffers();
    	this.rendererNeedsNewPointCloud = true;
    	
    }
    
    private JMenuBar menuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem loadItem = new JMenuItem("Load");
        loadItem.addActionListener(e -> this.openDialog());
        fileMenu.add(loadItem);
        menuBar.add(fileMenu);
        return menuBar;
    }
    

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }
    
    private int vertexBuffer = 99;

    static float[] g_vertex_buffer_data = {
    	-2f, -1f, 0f,
    	 0f, -1f, 0f,
    	 -1f,  1f, 0f,
    	 
     	 0f,  -1f, 0f,
     	 2f,  -1f, 0f,
   	 	 1f,  1f, 0f,
    	 
   	 	 1f,  1f, 0f,
   	 	 -1f,  1f, 0f,
   	 	 0f,  3f, 0f,
    };
    
    FloatBuffer floatBuffer;
	private int programID;
	private int uniformId;
	private int uniformIdMvp;
    
    @Override
    public void init(GLAutoDrawable drawable) {
//    	exampleInit();

    	if (debug)
    		this.gl = new DebugGL3(drawable.getGL().getGL3());
    	else 
    		this.gl = drawable.getGL().getGL3();
    	
    	
    	
//    	gl.glEnable(GL_BLEND);
//		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//		gl.glBlendEquation(GL_FUNC_ADD);
		
    }

    float theta = 0f;
    
	
	
    @Override
    public void display(GLAutoDrawable drawable) {
//    	exampleDisplay();
    	
    	if (this.rendererNeedsNewPointCloud) {
    		this.renderer = new Renderer(this.pointCloud, this.gl);
    		this.rendererNeedsNewPointCloud = false;
    	}
    	
    	if (this.renderer != null) {
    		this.renderer.display();
    	}
    	
    }

    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
            int height) {
    }

	public void play() {
		// TODO Auto-generated method stub
		
	}
	
	private void exampleInit() {
    	int [] ptr = new int [1];
    	
    	this.floatBuffer = FloatBuffer.allocate(g_vertex_buffer_data.length);
    	this.floatBuffer.put(g_vertex_buffer_data);
    	this.floatBuffer.flip();
    	
    	gl.glGenBuffers(1, ptr, 0);
    	this.vertexBuffer = ptr[0];
    	gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexBuffer);
    	gl.glBufferData(GL_ARRAY_BUFFER, g_vertex_buffer_data.length * 4, this.floatBuffer, GL_STATIC_DRAW);
    	
    	this.programID = ShaderHelper.programWithShaders2(gl, "src/shaders/shader2.vert", "src/shaders/shader2.frag");
    	this.uniformId = gl.glGetUniformLocation(this.programID, "f");
    	this.uniformIdMvp = gl.glGetUniformLocation(this.programID, "mvp");
    }
    
    private void exampleDisplay() {
    	theta += 0.1f;
    	System.out.println(theta);
    	gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    	gl.glUseProgram(this.programID);
    	
    	gl.glUniform1f(this.uniformId, 0.0f);
    	
    	Matrix4 m = new Matrix4();
    	m.makePerspective(3.14159f/2f, 4f/3f, 0.1f, 100f);
    	m.translate(0f, 0f, -5f);
    	m.rotate(theta, 0f, 1f, 0f);
    	
    	gl.glUniformMatrix4fv(this.uniformIdMvp, 1, false, m.getMatrix(), 0);
    	
    	gl.glEnableVertexAttribArray(0);
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
    	gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    	gl.glDrawArrays(GL_TRIANGLES, 0, 9);
    	
    	gl.glDisableVertexAttribArray(0);
    }
}