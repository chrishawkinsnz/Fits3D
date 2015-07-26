import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import com.jogamp.opengl.util.FPSAnimator;
import net.miginfocom.swing.MigLayout;

public class FrameMaster extends JFrame implements GLEventListener {

    private static final long serialVersionUID = 1L;
    
    private static FrameMaster singleFrameMaster;
    
	private List<PointCloud> pointClouds = new ArrayList<PointCloud>();
	private List<PointCloud> currentPointClouds  = new ArrayList<PointCloud>();;

	private AttributeProvider selectedAttributeProvider = null;

	private Renderer renderer;
	private WorldViewer viewer;
	private Selection selection;
	private GL3 gl;

	private PointCloud pleaseSelectThisNextChanceYouGet;
	private boolean debug = false;
	
	private MouseController mouseController;

	private KeyboardSelectionController selectionController;
	
	private int drawableWidth = 0;
	private int drawableHeight = 0;
	
//	private DefaultListModel<PointCloud> listModel;
//	private JList<PointCloud> list;


	private JTree tree;
	private DefaultMutableTreeNode treeRoot;
	private DefaultTreeModel treeModel;
	private JPanel attributPanel;
	private GLCanvas canvas;


	public static boolean vain = false;

	private FPSAnimator animator;
	private static Color colorBackground = new Color(238, 238, 238, 255);
    public FrameMaster() {
    	super("Very Good Honours Project");
    	singleFrameMaster = this;


//		UIManager.put("Tree.rendererFillBackground", false);


        this.setName("Very Good Honours Project");
        
        this.setMinimumSize(new Dimension(800, 600));
        this.setPreferredSize(new Dimension(1600, 1000));
    	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	this.setVisible(true);
    	this.setResizable(true);
		BorderLayout bl = new BorderLayout();
    	this.getContentPane().setLayout(bl);
       
        this.setJMenuBar(makeMenuBar());

        canvas = makeCanvas();
        attachControlsToCanvas(canvas);
        
        this.getContentPane().add(canvas, BorderLayout.CENTER);
        this.getContentPane().add(filePanel(), BorderLayout.WEST);

		setDefaultLookAndFeelDecorated(true);

		Component leftComponent = bl.getLayoutComponent(BorderLayout.WEST);
		leftComponent.setBackground(Color.WHITE);

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
		if (vain) {
			this.animator = new FPSAnimator(canvas, 120);
			this.animator.setUpdateFPSFrames(100, System.out);
			this.animator.start();
		}


		return canvas;
    }
    
    private void attachControlsToCanvas(Canvas canvas) {

		if (vain) {
			this.viewer = new VanitySpinnerViewer();
		} else {
			this.viewer = new WorldViewer();
		}

        this.mouseController = new MouseController(this.viewer);

		this.selection = new Selection();
		this.selectionController = new KeyboardSelectionController(this.selection);

		canvas.addMouseMotionListener(this.mouseController);
		canvas.addMouseListener(this.mouseController);
		canvas.addMouseWheelListener(this.mouseController);
		canvas.addKeyListener(this.selectionController);
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

		JLabel title = new JLabel("Attributes");
		title.setFont(new Font("Dialog", Font.BOLD, 24));
		attributPanel.add(title, "span 2");
		if (selectedAttributeProvider != null) {
			for (Attribute attribute : selectedAttributeProvider.getAttributes()) {
				AttributeDisplayer attributeDisplayer = this.attributeDisplayManager.tweakableForAttribute(attribute, selectedAttributeProvider);
				addTweakableToAttributePanel(attributeDisplayer, attribute);
			}

			//--now look for child providers
			List<AttributeProvider> childProviders = selectedAttributeProvider.getChildProviders();
			int minimumChildrenToDisplay = 2;
			if (childProviders.size() >= minimumChildrenToDisplay) {
				JLabel subsectionsTitle = new JLabel("Subsections");
				subsectionsTitle.setFont(new Font("Dialog", Font.BOLD, 24));
				attributPanel.add(subsectionsTitle, "span 2");
				for (AttributeProvider childProvider : childProviders) {

					for (Attribute attribute : childProvider.getAttributes()) {
						AttributeDisplayer attributeDisplayer = this.attributeDisplayManager.tweakableForAttribute(attribute, childProvider);
						addTweakableToAttributePanel(attributeDisplayer, attribute);
					}
					JLabel ssssh = new JLabel(" ");
					ssssh.setFont(new Font("Dialog", Font.BOLD, 24));
					attributPanel.add(ssssh, "span 2");
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

	void addTweakableToAttributePanel(AttributeDisplayer attributeDisplayer, Attribute attribute) {
		if (attributeDisplayer == null) {
			return;
		}
		String formatString = attributeDisplayer.isDoubleLiner() ? "span 2" : "";

		JLabel label = new JLabel(attribute.displayName);
		label.setFont(new Font("Dialog", Font.BOLD, 12));
		attributPanel.add(label, formatString);


		formatString = attributeDisplayer.isDoubleLiner() ? "width ::250, span 2" : "gapleft 16, width ::150";
		attributPanel.add(attributeDisplayer.getComponent(), formatString);
	}



	private JPanel filePanel() {
		this.treeRoot = new DefaultMutableTreeNode("Point Clouds");
		this.treeModel = new DefaultTreeModel(treeRoot);


		tree = new JTree(treeModel);


		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		tree.setShowsRootHandles(true);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TreePath path = e.getPath();
				Object obj = e.getPath().getLastPathComponent();
				FrameMaster.this.currentPointClouds.clear();
				if (obj instanceof DefaultMutableTreeNode) {
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) obj;
					Object contents = treeNode.getUserObject();
					if (contents instanceof AttributeProvider) {
						FrameMaster.this.selectedAttributeProvider = (AttributeProvider) contents;
					} else {
						FrameMaster.this.selectedAttributeProvider = null;
					}
				}
				FrameMaster.this.reloadAttributePanel();
			}
		});


		tree.setMinimumSize(new Dimension(240, 200));
		tree.setPreferredSize(new Dimension(240, 2000));
		tree.setBorder(BorderFactory.createTitledBorder("Fits Files"));
//		tree.setBackground(colorBackground);

        JPanel filePanel = new JPanel(new MigLayout("flowy"));


		Dimension lilDimension = new Dimension(240, 600);
        filePanel.setMaximumSize(lilDimension);
        filePanel.setPreferredSize(lilDimension);

		filePanel.add(tree);

		JButton buttonAddFitsFile = new JButton("Open New Fits File");
		buttonAddFitsFile.addActionListener(e -> this.showOpenDialog());
		filePanel.add(buttonAddFitsFile, "grow");

//        filePanel.setBackground(colorBackground);
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

		for (PointCloud p: this.pointClouds) {
			List<Object>choicesForRelativeToDropDown = new ArrayList<>();
			choicesForRelativeToDropDown.add("-");
			for (PointCloud pp: this.pointClouds) {
				if (pp != p) {
					choicesForRelativeToDropDown.add(pp);
				}
			}
			p.relativeTo.choices = choicesForRelativeToDropDown;
			p.relativeTo.notifyWithValue(p.relativeTo.choice);
		}

    	pc.readFits();
		MutableTreeNode newNode = new DefaultMutableTreeNode(pc);
		this.treeModel.insertNodeInto(newNode, this.treeRoot, 0);
//		for (Region region : pc.regions) {
//			addRegionToTree(pc, region);
//		}
//    	this.listModel.addElement(pc);
		this.pleaseSelectThisNextChanceYouGet = pc;
		setNeedsDisplay();
    }

	private void test() {
		this.pointClouds.get(0).makeSomeStupidSubregion();
		reloadAttributePanel();
	}
	private void test2() {
		this.pointClouds.get(0).makeSomeStupidOtherSubregion(this.selection.getVolume());
		reloadAttributePanel();
	}

	private void test3() {
		this.pointClouds.get(0).blastVolumeWithQuality(this.selection.getVolume());
		reloadAttributePanel();
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

		JMenuItem test2 = new JMenuItem("cut it out");
		test2.addActionListener(e -> this.test2());
		fileMenu.add(test2);

		JMenuItem test3 = new JMenuItem("enhance!");
		test3.addActionListener(e -> this.test3());
		fileMenu.add(test3);

		JMenuItem gay = new JMenuItem("proud?");

		gay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FrameMaster.this.renderer.gay = !FrameMaster.this.renderer.gay;
				setNeedsDisplay();
			}
		});
		fileMenu.add(gay);

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

	static boolean needsFreshRenderer = false;
    @Override
    public void display(GLAutoDrawable drawable) {
		//--check all the point clouds and if they have a pending region create a new renderer.
		for (PointCloud pc : this.pointClouds) {
			if (pc.pendingRegion != null) {
//				pc.clearRegions();
				//TODO
				pc.addRegion(pc.pendingRegion, pc.regions);
				if (pc.regions.size() == 2) {
					addRegionToTree(pc, pc.regions.get(0));
					//--TODO how do you sleep at night?
				}
				addRegionToTree(pc, pc.pendingRegion);
				pc.pendingRegion = null;

				needsFreshRenderer = true;
			}
		}
    	if (needsFreshRenderer){

			if (this.renderer == null) {
				this.renderer = new Renderer(this.pointClouds, this.viewer, this.gl);
				this.renderer.selection = this.selection;
			}
			else {
				this.renderer.setupWith(this.pointClouds, this.viewer, this.gl);
				System.gc();
			}
    		this.renderer.informOfResolution(this.drawableWidth, this.drawableHeight);

			FrameMaster.setNeedsAttributesReload();
			this.needsFreshRenderer = false;
    	}
		if (this.pleaseSelectThisNextChanceYouGet != null) {


			TreePath selectionPath = new TreePath(this.pleaseSelectThisNextChanceYouGet);
			this.tree.setSelectionPath(selectionPath);
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

	public static void addRegionToTree(PointCloud pointCloud, Region cr) {
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(cr);
		TreePath pathToParent = find(singleFrameMaster.treeRoot, pointCloud);
		MutableTreeNode parentNode = (MutableTreeNode) pathToParent.getLastPathComponent();
		singleFrameMaster.treeModel.insertNodeInto(newNode, parentNode, pointCloud.regions.indexOf(cr));
	}

	private static TreePath find(DefaultMutableTreeNode root, Object obj) {
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			if (node.getUserObject() == obj) {
				return new TreePath(node.getPath());
			}
		}
		return null;
	}

	public static void setNeedsNewRenderer() {
		singleFrameMaster.needsFreshRenderer = true;
	}

	public static void setNeedsAttributesReload() {
		singleFrameMaster.reloadAttributePanel();
	}

}