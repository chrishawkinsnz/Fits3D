import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;



import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

public class FrameMaster extends JFrame implements GLEventListener {
    private static final long serialVersionUID = 1L;
    private FPSAnimator animator;
	private PointCloud pointCloud;
	private Renderer renderer;
	private Viewer viewer;
	private GL3 gl;
	
	private boolean debug = false;
	
	private boolean rendererNeedsNewPointCloud = false;
	private MouseController mouseController;
	
	private int drawableWidth = 0;
	private int drawableHeight = 0;
	
    public FrameMaster() {
    	super("Very Good Honours Project");
    	    	
        GLProfile profile = GLProfile.get(GLProfile.GL3);
        GLCapabilities capabilities = new GLCapabilities(profile);

        this.setName("Very Good Honours Project");
        this.setMinimumSize(new Dimension(700, 800));
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
        this.animator.setUpdateFPSFrames(100, null);
        
        this.animator.start();

        this.viewer = new Viewer();
        this.mouseController = new MouseController(this.viewer);
        canvas.addMouseMotionListener(this.mouseController);
        canvas.addMouseListener(this.mouseController);
        canvas.addMouseWheelListener(this.mouseController);
	}
    
    private JPanel buttonBar(){
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.orange);
        buttonPanel.setMinimumSize(new Dimension(0, 128));
        
        JButton loadExampleButton = new JButton("Load Example");       
        loadExampleButton.addActionListener(e -> this.loadFile("12CO_MEAN.fits"));
        buttonPanel.add(loadExampleButton);
        
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        slider.addChangeListener((ChangeEvent ce) -> {
			float proportion = (float)slider.getValue()/ (float)slider.getMaximum();

			this.renderer.alphaFudge = proportion * 0.1f;
		});
        buttonPanel.add(slider);
        
        JLabel projectionLabel = new JLabel("Projection:");
        buttonPanel.add(projectionLabel);
        
        ButtonGroup projectionButtons = new ButtonGroup();
        
        JRadioButton perspectiveButton = new JRadioButton("Perspective");
        perspectiveButton.addActionListener(e -> this.renderer.isOrthographic = false);
        perspectiveButton.setSelected(false);
        projectionButtons.add(perspectiveButton);
        buttonPanel.add(perspectiveButton);
        
        JRadioButton orthographicButton = new JRadioButton("Orthographic");
        orthographicButton.addActionListener(e -> this.renderer.isOrthographic = true);
        orthographicButton.setSelected(true);
        projectionButtons.add(orthographicButton);
        buttonPanel.add(orthographicButton);
        
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
    
    @Override
    public void init(GLAutoDrawable drawable) {
    	if (debug)
    		this.gl = new DebugGL3(drawable.getGL().getGL3());
    	else 
    		this.gl = drawable.getGL().getGL3();	
    }
    
    @Override
    public void display(GLAutoDrawable drawable) {    	
    	if (this.rendererNeedsNewPointCloud) {
    		this.renderer = new Renderer(this.pointCloud, this.viewer, this.gl);
    		this.renderer.informOfResolution(this.drawableWidth, this.drawableHeight);
    		this.rendererNeedsNewPointCloud = false;
    	}
    	
    	if (this.renderer != null) {
    		this.renderer.display();
    	}    	
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
            int height) {
    	this.drawableHeight = height;
    	this.drawableWidth = width;
    	if (renderer!= null) {
    		renderer.informOfResolution(width, height);
    	}
    }

	public void play() {
		// TODO Auto-generated method stub
	}
	
}