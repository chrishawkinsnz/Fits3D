import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.DebugGL3;
import com.jogamp.opengl.GLAutoDrawable;/**/
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

public class FrameMaster extends JFrame implements GLEventListener {

    private static final long serialVersionUID = 1L;
    
    private static FrameMaster singleFrameMaster;
    
	private List<PointCloud> pointClouds = new ArrayList<PointCloud>();
	private List<PointCloud> currentPointClouds  = new ArrayList<PointCloud>();;

	private Renderer renderer;
	private WorldViewer viewer;
	private GL3 gl;

	private PointCloud pleaseSelectThisNextChanceYouGet;
	private boolean debug = false;
	
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
    
	public AttributeDisplayManager  attributeDisplayManager = new AttributeDisplayManager();
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
    			
    			AttributeDisplayer tweakable = this.attributeDisplayManager.tweakableForAttribute(attribute, pc);
    			if (tweakable == null) {continue;}
    			String formatString = tweakable.isDoubleLiner() ? "span 2" : "";
    			
    			JLabel label = new JLabel(attribute.displayName);
    			label.setFont(new Font("Dialog", Font.BOLD, 12));
    			attributPanel.add(label, formatString);
    			
    			
    			formatString = tweakable.isDoubleLiner() ? "width ::250, span 2" : "gapleft 16, width ::150";
    			attributPanel.add(tweakable.getComponent(), formatString);
    		}

			if (pc.regions.size() > 1) {
				for (int i = 0; i < pc.regions.size(); i++) {
					JSeparator separator = new JSeparator();
					attributPanel.add(separator, "span 2");

					JLabel label = new JLabel("Region " + i);
					label.setFont(new Font("Dialog", Font.BOLD, 14));
					attributPanel.add(label, "span 2");

					CloudRegion cr = pc.regions.get(i);
					for (Attribute attribute : cr.attributes) {
						AttributeDisplayer tweakable = this.attributeDisplayManager.tweakableForAttribute(attribute, pc);
						if (tweakable == null) {continue;}
						String formatString = tweakable.isDoubleLiner() ? "span 2" : "";

						label = new JLabel(attribute.displayName);
						label.setFont(new Font("Dialog", Font.BOLD, 12));
						attributPanel.add(label, formatString);

						formatString = tweakable.isDoubleLiner() ? "width ::250, span 2" : "gapleft 16, width ::150";
						attributPanel.add(tweakable.getComponent(), formatString);
					}
				}
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
    	pc.readFits();
    	this.listModel.addElement(pc);
		this.pleaseSelectThisNextChanceYouGet = pc;
		setNeedsDisplay();
    }
    
    private JMenuBar makeMenuBar() {


        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem loadItem = new JMenuItem("Open");
        setKeyboardShortcutTo(KeyEvent.VK_O, loadItem);
        loadItem.addActionListener(e -> this.showOpenDialog());
        fileMenu.add(loadItem);

		JMenuItem test = new JMenuItem("test");
		test.addActionListener(e -> this.test());
		fileMenu.add(test);

        menuBar.add(fileMenu);
        return menuBar;
    }

	private void test() {
		this.pointClouds.get(0).makeSomeStupidSubregion();
		reloadAttributePanel();
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
		boolean needsFreshRenderer = false;
		//--check all the point clouds and if they have a pending region create a new renderer.
		for (PointCloud pc : this.pointClouds) {
			if (pc.pendingRegion != null) {
//				pc.clearRegions();
				//TODO
				pc.addRegion(pc.pendingRegion, pc.regions);
				pc.pendingRegion = null;
				needsFreshRenderer = true;
			}
		}
    	if (needsFreshRenderer){
			if (this.renderer == null) {
				this.renderer = new Renderer(this.pointClouds, this.viewer, this.gl);
			}
			else {
				this.renderer.setupWith(this.pointClouds, this.viewer, this.gl);
				System.gc();
			}
    		this.renderer.informOfResolution(this.drawableWidth, this.drawableHeight);
    	}
		if (this.pleaseSelectThisNextChanceYouGet != null) {
			this.list.setSelectedValue(pleaseSelectThisNextChanceYouGet, true);
			pleaseSelectThisNextChanceYouGet = null;
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


    public static void setNeedsDisplay() {
    	singleFrameMaster.canvas.display();
    }
}