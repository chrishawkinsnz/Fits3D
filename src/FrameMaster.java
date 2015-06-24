import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
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

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

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
	private JFrame attrbutesFrame; 
	
    public FrameMaster() {
    	super("Very Good Honours Project");
    	    	
        GLProfile profile = GLProfile.get(GLProfile.GL3);
        GLCapabilities capabilities = new GLCapabilities(profile);

        this.setName("Very Good Honours Project");
        this.setSize(new Dimension(700, 800));
        this.setMinimumSize(new Dimension(500,600));
    	this.setLocationRelativeTo(null);
    	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	this.setVisible(true);
    	this.setResizable(true);
    	this.setLayout(new BorderLayout());

        GLCanvas canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this);
        this.getContentPane().add(canvas);
        
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
        
        System.out.println("loading example");
        
        makeAttributesFrame();
	}
    
    private void makeAttributesFrame() {
    	Dimension lilDimension = new Dimension(300, 600);
    	this.attrbutesFrame = new JFrame("Attributely Everybody");
    	reloadAttributePanel();
    	attrbutesFrame.setMinimumSize(lilDimension);
    	attrbutesFrame.setSize(lilDimension);
    	attrbutesFrame.pack();
    	attrbutesFrame.setVisible(true);
    }
    
    private AttributeDisplayer tweakableForAttribute(Attribute attribute) {
    	//--listen okay we are just going to assume it is foo for the moment
    	AttributeDisplayer tweakable;
    	if (attribute instanceof Attribute.RangedAttribute) {
    		Attribute.RangedAttribute rAttribute = (Attribute.RangedAttribute) attribute;
    		tweakable = new Tweakable.Slidable(rAttribute, rAttribute.min, rAttribute.max, rAttribute.value);
    	}
    	else if (attribute instanceof Attribute.BinaryAttribute) {
    		Attribute.BinaryAttribute bAttribute = (Attribute.BinaryAttribute)attribute;
    		tweakable = new Tweakable.Toggleable(bAttribute, bAttribute.value);
    	}
    	else if (attribute instanceof Attribute.Name) {
    		Attribute.Name nAttribute = (Attribute.Name)attribute;
    		tweakable = new Tweakable.ChrisLabel(nAttribute.value);
    	}
    	else if (attribute instanceof Attribute.SteppedRangeAttribute) {
    		Attribute.SteppedRangeAttribute srAttribute = (Attribute.SteppedRangeAttribute)attribute;
    		tweakable = new Tweakable.ClickySlider(srAttribute, srAttribute.min, srAttribute.max, srAttribute.value, srAttribute.steps);
    	}
    	else {
    		tweakable = null;
    	}
    	return tweakable;
    }

    int count;
    
    public static boolean pointCloudNeedsUpdatedPointCloud;
	public static PointCloud pointCloudToUpdate;
	public static float desiredPointCloudFidelity;
    
    private void reloadAttributePanel() {
    	count++;
    	this.attrbutesFrame.getContentPane().removeAll();

    	Dimension lilDimension = new Dimension(300, 700);
    	int maxRows = 16;
    	MigLayout mlLayout = new MigLayout("wrap 2");
    	JPanel panel = new JPanel(mlLayout);
    	panel.setBorder(new EmptyBorder(0, 8, 8, 8));
    	
    	for (PointCloud pc: this.currentPointClouds) {
    		int cloudIndex = this.pointClouds.indexOf(pc);
    		JLabel title = new JLabel("Coud "+cloudIndex);
        	title.setFont(new Font("Dialog", Font.BOLD, 24));
        	panel.add(title, "span 2");	
    		for (Attribute attribute : pc.attributes) {
    			JLabel label = new JLabel(attribute.displayName);
    			label.setFont(new Font("Dialog", Font.BOLD, 12));
    			panel.add(label);
    			
    			AttributeDisplayer tweakable = tweakableForAttribute(attribute);
    			panel.add(tweakable.getComponent(), "gapleft 16");
    		}
    	}

    	panel.setMinimumSize(lilDimension);
    	attrbutesFrame.add(panel);
    	SwingUtilities.updateComponentTreeUI(attrbutesFrame);
    	attrbutesFrame.getContentPane().repaint();
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
					FrameMaster.this.reloadAttributePanel();
				}
				else if (e.getValueIsAdjusting() == false){
					FrameMaster.this.currentPointClouds = new ArrayList<PointCloud>();
					FrameMaster.this.reloadAttributePanel();
				}
				
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
        
        JMenuItem loadExample = new JMenuItem("Load Example");
        loadExample.addActionListener(e -> this.loadFile("12CO_MEAN.fits"));
        
        fileMenu.add(loadItem);
        fileMenu.add(loadExample);
        
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
    		this.renderer = new Renderer(this.pointClouds, this.viewer, this.gl);
    		this.renderer.informOfResolution(this.drawableWidth, this.drawableHeight);
    		this.rendererNeedsNewPointCloud = false;
    	}
    	
    	if (this.pointCloudNeedsUpdatedPointCloud) {
    		this.pointCloudToUpdate.readFitsAtQualityLevel(this.desiredPointCloudFidelity);
    		this.pointCloudNeedsUpdatedPointCloud = false;
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

}