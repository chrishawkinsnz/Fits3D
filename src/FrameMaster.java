import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;













import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class FrameMaster extends JFrame implements GLEventListener {
    private static final long serialVersionUID = 1L;
    private FPSAnimator animator;
	private List<PointCloud> pointClouds = new ArrayList<PointCloud>();
	private List<PointCloud> currentPointClouds  = new ArrayList<PointCloud>();;

	private Renderer renderer;
	private WorldViewer viewer;
	private GL3 gl;
	
	private boolean debug = false;
	
	private boolean rendererNeedsNewPointCloud = false;
	private MouseController mouseController;
	
	private int drawableWidth = 0;
	private int drawableHeight = 0;
	
	private DefaultListModel<PointCloud> listModel;
	private JList<PointCloud> list; 
	
    public FrameMaster() {
    	super("Very Good Honours Project");
    	    	
        GLProfile profile = GLProfile.get(GLProfile.GL3);
        GLCapabilities capabilities = new GLCapabilities(profile);

        this.setName("Very Good Honours Project");
        this.setSize(new Dimension(700, 800));
        this.setMinimumSize(new Dimension(300,400));
    	this.setLocationRelativeTo(null);
    	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	this.setVisible(true);
    	this.setResizable(true);
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

        this.viewer = new WorldViewer();
        this.mouseController = new MouseController(this.viewer);
        canvas.addMouseMotionListener(this.mouseController);
        canvas.addMouseListener(this.mouseController);
        canvas.addMouseWheelListener(this.mouseController);
        
        makeFileFrame();
    
        
	}
    
    private void makeFileFrame() {
    	listModel = new DefaultListModel<PointCloud>();
    	
    	
        list = new JList<PointCloud>(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				List<PointCloud> selectedPointClouds = list.getSelectedValuesList();
				if (selectedPointClouds != null && e.getValueIsAdjusting() == false) {
					FrameMaster.this.currentPointClouds = selectedPointClouds;
				}
				else {
					FrameMaster.this.currentPointClouds = new ArrayList<PointCloud>();
				}
				rendererNeedsNewPointCloud = true;
			}
		});
        list.setVisibleRowCount(5);
        
        list.setMinimumSize(new Dimension(200, 600));
        list.setFixedCellWidth(200);
    	JFrame fileFrame = new JFrame("Files");
        
    	
        //--add file view
        JPanel filePanel = new JPanel();
        
        
        filePanel.setMinimumSize(new Dimension(200,600));
        
        
        filePanel.add(list);
        fileFrame.add(filePanel);
        
        fileFrame.pack();
        fileFrame.setMinimumSize(filePanel.getMinimumSize());
        fileFrame.setVisible(true);	
    }
    
    private JPanel buttonBar(){
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.blue);
        buttonPanel.setMinimumSize(new Dimension(0, 128));
        
        JButton loadExampleButton = new JButton("Load Example");       
        loadExampleButton.addActionListener(e -> this.loadFile("12CO_MEAN.fits"));
        buttonPanel.add(loadExampleButton);
        
        JButton tripOutButton = new JButton("Trip Out");
        tripOutButton.addActionListener(e -> this.renderer.isTrippy = !this.renderer.isTrippy);
        buttonPanel.add(tripOutButton);
        
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        slider.addChangeListener((ChangeEvent ce) -> {
			float proportion = (float)slider.getValue()/ (float)slider.getMaximum();

			this.renderer.alphaFudge = proportion * 0.1f;
		});
        buttonPanel.add(slider);
        
        JSlider sliderQuality = new JSlider(JSlider.HORIZONTAL,0,10,1);
        sliderQuality.addChangeListener((ChangeEvent ce) -> {
        	boolean isSliding = sliderQuality.getValueIsAdjusting();
        	if (isSliding) {
        		return;
        	}
        	float proportion = (float)sliderQuality.getValue()/ (float)sliderQuality.getMaximum();
        	System.out.println("Proportion:" + proportion);
        	this.pointClouds.get(0).readFitsAtQualityLevel(proportion);
        	this.rendererNeedsNewPointCloud = true;        	
        });
        sliderQuality.setPaintTicks(true);
        sliderQuality.setSnapToTicks(true);
        
        buttonPanel.add(sliderQuality);
        
        
        
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
    	PointCloud pc = new PointCloud(fileName);
    	this.pointClouds.add(pc);
    	pc.readFitsAtQualityLevel(0.1f);
    	this.listModel.addElement(pc);
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
    		this.renderer = new Renderer(this.currentPointClouds, this.viewer, this.gl);
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
		System.out.println("reshape:"+this.drawableWidth+"x"+this.drawableHeight);

    	if (renderer!= null) {
    		renderer.informOfResolution(width, height);
    	}
    }

	public void play() {
		// TODO Auto-generated method stub
	}


	
}