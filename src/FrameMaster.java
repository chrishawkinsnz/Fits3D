import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.jogamp.opengl.GLAutoDrawable;/**/
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;

import com.jogamp.opengl.util.FPSAnimator;
import net.miginfocom.swing.MigLayout;

public class FrameMaster extends JFrame implements GLEventListener {

	//CONSTANTS
	private final static int WINDOW_WIDTH_MIN 			= 800;
	private final static int WINDOW_HEIGHT_MIN 			= 600;
	private final static int WINDOW_WIDTH_DEFAULT		= 1400;
	private final static int WINDOW_HEIGHT_DEFAULT		= 1000;

	//FLAGS
	private static final boolean debug = true;
	private static boolean vain = false;
	static boolean rendererNeedsFreshPointClouds = false;

	//ACCESSOR
    private static FrameMaster singleton;

	//MODEL
	private AttributeProvider selectedAttributeProvider = null;
	private List<PointCloud> pointClouds = new ArrayList<PointCloud>();
	private WorldViewer viewer;
	private Selection selection;
	private DefaultTreeModel treeModel;

	//VIEW
	private Renderer renderer;
	private GLCanvas canvas;

	//CONTROLLER
	private KeyboardSelectionController selectionController;


	public FrameMaster() {
    	super("Very Good Honours Project");
		singleton = this;

        this.setName("FITS 3D");

		this.setMinimumSize(new Dimension(WINDOW_WIDTH_MIN, WINDOW_HEIGHT_MIN));
		this.setPreferredSize(new Dimension(WINDOW_WIDTH_DEFAULT, WINDOW_HEIGHT_DEFAULT));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
    	this.setResizable(true);
		BorderLayout bl = new BorderLayout();
    	this.getContentPane().setLayout(bl);
       
        this.setJMenuBar(makeMenuBar());

        this.canvas = makeCanvas();
		this.viewer = createViewer();
        attachControlsToCanvas(canvas, viewer);

		this.getContentPane().add(canvas, BorderLayout.CENTER);
		this.getContentPane().add(makeFilePanel(), BorderLayout.WEST);
		this.getContentPane().add(makeAttributePanel(), BorderLayout.EAST);

		bl.getLayoutComponent(BorderLayout.WEST).setBackground(Color.WHITE);

		this.pack();
    }






	//==================================================================================================================
	//  FILE HANDLING
	//==================================================================================================================

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
		MutableTreeNode root = (MutableTreeNode)this.treeModel.getRoot();
		this.treeModel.insertNodeInto(newNode, root, 0);

		setNeedsDisplay();
    }






	//==================================================================================================================
	//  UI FACTORIES
	//==================================================================================================================

	/**
	 * Make and return the attributes panel
	 * @return The created attributes panel
	 */
	private JPanel makeAttributePanel() {

		MigLayout mlLayout = new MigLayout("wrap 2");
		JPanel attributPanel = new JPanel(mlLayout);
		attributPanel.setBorder(new EmptyBorder(0, 8, 8, 8));

		JLabel title = new JLabel("Attributes");
		title.setFont(new Font("Dialog", Font.BOLD, 24));
		attributPanel.add(title, "span 2");
		if (selectedAttributeProvider != null) {
			for (Attribute attribute : selectedAttributeProvider.getAttributes()) {
				AttributeDisplayer attributeDisplayer = AttributeDisplayManager.defaultDisplayManager.tweakableForAttribute(attribute, selectedAttributeProvider);
				addTweakableToAttributePanel(attributeDisplayer, attribute, attributPanel);
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
						AttributeDisplayer attributeDisplayer = AttributeDisplayManager.defaultDisplayManager.tweakableForAttribute(attribute, childProvider);
						addTweakableToAttributePanel(attributeDisplayer, attribute, attributPanel);
					}
					//-space them out
					attributPanel.add(new JLabel(" "), "span 2");
				}
			}
		}

		Dimension lilDimension = new Dimension(300, 700);
		attributPanel.setMinimumSize(lilDimension);
		attributPanel.setMaximumSize(lilDimension);
		attributPanel.setPreferredSize(lilDimension);

		return attributPanel;
	}


	void addTweakableToAttributePanel(AttributeDisplayer attributeDisplayer, Attribute attribute, JPanel attributPanel) {
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


	/**
	 * Makes and returns the file panel
	 * @return The created file panel
	 */
	private JPanel makeFilePanel() {
		JPanel filePanel = new JPanel(new MigLayout("flowy"));

		TreeNode treeRoot = treeRoot = new DefaultMutableTreeNode("Point Clouds");
		this.treeModel = new DefaultTreeModel(treeRoot);

		JTree tree = new JTree(treeModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
				Object contents = selectedNode.getUserObject();
				FrameMaster.this.selectedAttributeProvider = contents instanceof AttributeProvider ? (AttributeProvider) contents : null;
				FrameMaster.this.reloadAttributePanel();
			}
		});

		//--If somethign new is added to the model then select it.
		treeModel.addTreeModelListener(new TreeModelListener() {
			public void treeNodesChanged(TreeModelEvent e) {}
			public void treeNodesRemoved(TreeModelEvent e) {}
			public void treeStructureChanged(TreeModelEvent e) {}

			@Override
			public void treeNodesInserted(TreeModelEvent e) {
				System.out.println("tree node added");
				TreePath parentPath = e.getTreePath();
				TreePath childPath = parentPath.pathByAddingChild(e.getChildren()[e.getChildren().length - 1]);
				tree.setSelectionPath(childPath);
			}
		});

		tree.setMinimumSize(new Dimension(240, 200));
		tree.setPreferredSize(new Dimension(240, 2000));
		tree.setBorder(BorderFactory.createTitledBorder("Fits Files"));

		filePanel.add(tree);

		JButton buttonAddFitsFile = new JButton("Open New Fits File");
		buttonAddFitsFile.addActionListener(e -> this.showOpenDialog());
		filePanel.add(buttonAddFitsFile, "grow");

		return filePanel;
	}


	/**
	 * Create and return the OpenGL canvas
	 * @return The created OpenGL canvas
	 */
	private GLCanvas makeCanvas() {
		GLProfile profile = GLProfile.get(GLProfile.GL3);
		GLCapabilities capabilities = new GLCapabilities(profile);
		GLCanvas canvas = new GLCanvas(capabilities);
		canvas.addGLEventListener(this);

		if (vain) {
			FPSAnimator animator = new FPSAnimator(canvas, 120);
			animator.setUpdateFPSFrames(100, System.out);
			animator.start();
		}

		return canvas;
	}


	/**
	 * Create and return the menu bar
	 * @return The created menu bar
	 */
	private JMenuBar makeMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(makFileMenu());
		if (debug) {
			menuBar.add(makeDebugMenu());
		}

		return menuBar;
	}


	/**
	 * Creates and returns the file menu
	 * @return The created file menu
	 */
	private JMenu makFileMenu() {
		JMenu fileMenu = new JMenu("File");

		JMenuItem loadItem = new JMenuItem("Open");
		setKeyboardShortcutTo(KeyEvent.VK_O, loadItem);
		loadItem.addActionListener(e -> this.showOpenDialog());
		fileMenu.add(loadItem);

		JMenuItem cutItem = new JMenuItem("Cut selection");
		setKeyboardShortcutTo(KeyEvent.VK_X, cutItem);
		cutItem.addActionListener(e -> this.cutSelection());
		fileMenu.add(cutItem);

		JMenuItem selectItem = new JMenuItem("Make selection");
		setKeyboardShortcutTo(KeyEvent.VK_H, selectItem);
		selectItem.addActionListener(e -> this.toggleSelectMode());
		fileMenu.add(selectItem);

		return fileMenu;
	}


	/**
	 * Creates and returns the debug menu
	 * @return The created debug menu
	 */
	private JMenu makeDebugMenu() {
		JMenu debugMenu = new JMenu("Debug");

		JMenuItem rainbow = new JMenuItem("rainbow");
		debugMenu.add(rainbow);

		JMenuItem test = new JMenuItem("foo");
		test.addActionListener(e -> this.foo());
		debugMenu.add(test);

		rainbow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FrameMaster.this.renderer.gay = !FrameMaster.this.renderer.gay;
				setNeedsDisplay();
			}
		});
		return debugMenu;
	}






	//==================================================================================================================
	//  USER ACTIONS
	//==================================================================================================================

	/**
	 * User chooses to begin making a selection of a volume
	 */
	private void toggleSelectMode() {
		if (this.selection == null || this.selectionController == null) {
			this.selection = Selection.defaultSelection();
			this.selectionController = new KeyboardSelectionController(this.selection);
		}

		if (this.renderer.selection == null) {
			this.renderer.selection = this.selection;
			canvas.addKeyListener(this.selectionController);
		}
		else {
			this.renderer.selection = null;
			canvas.removeKeyListener(this.selectionController);
		}
		FrameMaster.setNeedsDisplay();
	}


	/**
	 * User chooses to cut out a subsection
	 */
	private void cutSelection() {
		this.pointClouds.get(0).cutOutSubvolume(this.selection.getVolume());
		this.toggleSelectMode();
		reloadAttributePanel();
	}


	/**
	 * All purpose hook for testing features
	 */
	private void foo() {

	}








	//==================================================================================================================
	//  GL LISTENER METHODS
	//==================================================================================================================

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }


    @Override
    public void init(GLAutoDrawable drawable) {
		this.renderer = new Renderer(this.pointClouds, this.viewer, drawable.getGL().getGL3());
    }


    @Override
    public void display(GLAutoDrawable drawable) {
    	if (rendererNeedsFreshPointClouds){
			this.renderer.setupWith(this.pointClouds, this.viewer, drawable.getGL().getGL3());
			this.rendererNeedsFreshPointClouds = false;
    	}

		this.renderer.display();
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		renderer.informOfResolution(width, height);
    }







	//==================================================================================================================
	//  NOTIFICATIONS
	//==================================================================================================================
	public static void setNeedsNewRenderer() {
		singleton.rendererNeedsFreshPointClouds = true;
	}


	public static void setNeedsDisplay() {
		singleton.canvas.display();
	}


	public static void notifyFileBrowserOfNewRegion(PointCloud pc, Region region) {
		DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) singleton.treeModel.getRoot();
		Enumeration<DefaultMutableTreeNode> e = rootNode.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			if (node.getUserObject() == pc) {
				//--if theres exactly two regions then also sneak in the first
				if (pc.getRegions().size() == 2) {
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(pc.getRegions().get(0));
					singleton.treeModel.insertNodeInto(newNode, node, 0);
				}
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(region);
				singleton.treeModel.insertNodeInto(newNode, node, pc.getRegions().indexOf(region));
			}
		}
	}






	//==================================================================================================================
	//  HELPERS
	//==================================================================================================================

	private static void setKeyboardShortcutTo(int key, JMenuItem menuItem){
		String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("mac") != -1) {
			menuItem.setAccelerator(KeyStroke.getKeyStroke(key, Event.META_MASK));
		} else {
			menuItem.setAccelerator(KeyStroke.getKeyStroke(key, Event.CTRL_MASK));
		}
	}


	/**
	 * Creates and returns a new WorldViewer which holds the state of the view in the world
	 * @return The created WorldViewer object
	 */
	private WorldViewer createViewer() {
		if (vain)
			return new VanitySpinnerViewer();
		else
			return new WorldViewer();
	}


	/**
	 * Glues together the canvas to the worldViewer
	 * @param canvas The canvas on which the events will occur
	 * @param worldViewer The world viewer model that controls view state
	 */
	private void attachControlsToCanvas(Canvas canvas, WorldViewer worldViewer) {
		MouseController mouseController = new MouseController(this.viewer);
		canvas.addMouseMotionListener(mouseController);
		canvas.addMouseListener(mouseController);
		canvas.addMouseWheelListener(mouseController);
	}/**/


	/**
	 * Creates an all new attributes panel and replaces the one (if there) in the frame.
	 */
	private void reloadAttributePanel() {
		BorderLayout bl = (BorderLayout)this.getContentPane().getLayout();

		if (bl.getLayoutComponent(BorderLayout.EAST)!= null) {
			this.getContentPane().remove(bl.getLayoutComponent(BorderLayout.EAST));
		}

		JPanel attributPanel = makeAttributePanel();
		this.getContentPane().add(attributPanel, BorderLayout.EAST);
		SwingUtilities.updateComponentTreeUI(this);
		this.getContentPane().repaint();
	}
}