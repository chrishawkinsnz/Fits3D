import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
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
import com.sun.java.swing.plaf.motif.MotifBorders.BevelBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jreliability.function.ReliabilityFunction;
import org.jreliability.function.common.ExponentialReliabilityFunction;
import org.jreliability.gui.SamplerHistogramPanel;

import javax.media.jai.Histogram;
import javax.swing.BorderFactory;
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
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jreliability.gui.SamplerHistogramPanel;
import org.jreliability.gui.sampler.AbstractSampler;
import org.jreliability.gui.sampler.Sampler;
import org.jreliability.gui.sampler.TTFFrequencyDistributionSampler;

import net.miginfocom.swing.MigLayout;

public class FrameMaster extends JFrame implements GLEventListener {
    private static final long serialVersionUID = 1L;
    
    private static FrameMaster singleFrameMaster;
    
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
	private JPanel attributPanel;
	private GLCanvas canvas;
	
	private static Color colorBackground = new Color(238, 238, 238, 255);
    public FrameMaster() {
    	super("Very Good Honours Project");
    	singleFrameMaster = this;
        

        this.setName("Very Good Honours Project");
        
        this.setMinimumSize(new Dimension(800,600));
        this.setPreferredSize(new Dimension(1600,1000));
    	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	this.setVisible(true);
    	this.setResizable(true);
    	this.setLayout(new BorderLayout());
    	
            
       
        this.setJMenuBar(makeMenuBar());
                

    
        canvas = makeCanvas();
        attachControlsToCanvas(canvas);
        
        this.add(canvas,BorderLayout.CENTER);
        this.add(filePanel(), BorderLayout.WEST);
        
        reloadAttributePanel();
        
        this.pack();
    }

    private GLCanvas makeCanvas() {
    	GLProfile profile = GLProfile.get(GLProfile.GL3);
        GLCapabilities capabilities = new GLCapabilities(profile);
        this.canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this);
        canvas.setMinimumSize(new Dimension());
        canvas.requestFocusInWindow();

        return canvas;
    }
    
    private void attachControlsToCanvas(Canvas canvas) {
        this.viewer = new WorldViewer();
        this.mouseController = new MouseController(this.viewer);
        canvas.addMouseMotionListener(this.mouseController);
        canvas.addMouseListener(this.mouseController);
        canvas.addMouseWheelListener(this.mouseController);
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
    	else if (attribute instanceof Attribute.PathName) {
    		Attribute.PathName pnAttribute = (Attribute.PathName)attribute;
    		//--split up url 
    		String[] urlComponentsStrings = pnAttribute.value.split("/");
    		String name = urlComponentsStrings[urlComponentsStrings.length - 1];
    		tweakable = new Tweakable.ChrisLabel(name);
    	}
    	else if (attribute instanceof Attribute.Name) {
    		Attribute.Name nAttribute = (Attribute.Name)attribute;
    		tweakable = new Tweakable.ChrisLabel(nAttribute.value);
    	}
    	else if (attribute instanceof Attribute.SteppedRangeAttribute) {
    		Attribute.SteppedRangeAttribute srAttribute = (Attribute.SteppedRangeAttribute)attribute;
    		tweakable = new Tweakable.ClickySlider(srAttribute, srAttribute.min, srAttribute.max, srAttribute.value, srAttribute.steps);
    	}
    	else if (attribute instanceof Attribute.FilterSelectionAttribute) {
    		Attribute.FilterSelectionAttribute fsAttribute = (Attribute.FilterSelectionAttribute)attribute;
    		tweakable = new Tweakable.ChristogramTweakable();
    	}
    	else {
    		tweakable = null;
    	}
    	return tweakable;
    }

    
    public static boolean pointCloudNeedsUpdatedPointCloud;
	public static PointCloud pointCloudToUpdate;
	public static float desiredPointCloudFidelity;
    
    private void reloadAttributePanel() {
    	if (this.attributPanel != null) {
    		this.getContentPane().remove(this.attributPanel);
    	}

    	Dimension lilDimension = new Dimension(300, 700);

    	MigLayout mlLayout = new MigLayout("wrap 2");
    	this.attributPanel = new JPanel(mlLayout);
    	attributPanel.setBorder(new EmptyBorder(0, 8, 8, 8));
    	
    	for (PointCloud pc: this.currentPointClouds) {
    		int cloudIndex = this.pointClouds.indexOf(pc);
    		JLabel title = new JLabel("Coud "+cloudIndex);
        	title.setFont(new Font("Dialog", Font.BOLD, 24));
        	attributPanel.add(title, "span 2");	
    		for (Attribute attribute : pc.attributes) {
    			
    			AttributeDisplayer tweakable = tweakableForAttribute(attribute);
    			String formatString = tweakable.isDoubleLiner() ? "span 2" : "";
    			
    			JLabel label = new JLabel(attribute.displayName);
    			label.setFont(new Font("Dialog", Font.BOLD, 12));
    			attributPanel.add(label, formatString);
    			
    			
    			formatString = tweakable.isDoubleLiner() ? "width ::250, span 2" : "gapleft 16, width ::150";
    			attributPanel.add(tweakable.getComponent(), formatString);
    		}
    	}

    	attributPanel.setMinimumSize(lilDimension);
    	attributPanel.setMaximumSize(lilDimension);
    	attributPanel.setPreferredSize(lilDimension);
    	this.getContentPane().add(attributPanel, BorderLayout.EAST);
    	SwingUtilities.updateComponentTreeUI(this);
    	this.getContentPane().repaint();
    }
    
    
    private JPanel filePanel() {
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
        list.setMinimumSize(new Dimension(240, 200));
        list.setPreferredSize(new Dimension(240, 2000));
//        list.setFixedCellWidth(240);
        
        
        list.setBorder(BorderFactory.createTitledBorder("Fits Files"));
        
        JPanel filePanel = new JPanel(new MigLayout("flowy"));
        
        
        Dimension lilDimension = new Dimension(240, 600);
        filePanel.setMaximumSize(lilDimension);
        filePanel.setPreferredSize(lilDimension);

        filePanel.add(list);
        
        JButton buttonAddFitsFile = new JButton("Open New Fits File");
        buttonAddFitsFile.addActionListener(e -> this.showOpenDialog());
        filePanel.add(buttonAddFitsFile, "grow");
        
        list.setBackground(colorBackground);
        filePanel.setBackground(colorBackground);
        filePanel.setPreferredSize(new Dimension(260, 500));

        
        //--Graff
        
        float []dubs = new float[50];
        for (int i = 0; i < dubs.length ; i++) {
        	dubs [i] = (float)i * 1.2f;
        }
        
        int bins = 20;
        Christogram christogram = new Christogram(dubs, 0f,50f * 1.2f, bins);
        christogram.setXAxisTitle("Frequency");
        christogram.setLeftInset(0);
        christogram.setRightInset(0);
        
        filePanel.add(christogram);
        christogram.setPreferredSize(new Dimension(200, 150));
        christogram.setMinimumSize(new Dimension(200, 150));
        christogram.setBackground(Color.pink);
        return filePanel;        
    }
    
    
    private void showOpenDialog() {
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
    	setNeedsDisplay();
    }
    
    private JMenuBar makeMenuBar() {


        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem loadItem = new JMenuItem("Open");
        setKeyboardShortcutTo(KeyEvent.VK_O, loadItem);
        loadItem.addActionListener(e -> this.showOpenDialog());
        fileMenu.add(loadItem);
        
        menuBar.add(fileMenu);
        return menuBar;
    }
    
    private static void setKeyboardShortcutTo(int key, JMenuItem menuItem){
    	String os = System.getProperty("os.name").toLowerCase();	
        if (os.indexOf("mac") != -1) {
        	menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.META_MASK));
        } else {
        	menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        }
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
    	
    	if (pointCloudNeedsUpdatedPointCloud) {
    		pointCloudToUpdate.readFitsAtQualityLevel(desiredPointCloudFidelity);
    		pointCloudNeedsUpdatedPointCloud = false;
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
    
    public static void setNeedsDisplay() {
    	singleFrameMaster.canvas.display();
    }
    
}