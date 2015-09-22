import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
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
import javax.swing.event.*;
import javax.swing.tree.*;

import com.jogamp.opengl.util.FPSAnimator;
import com.sun.org.apache.xerces.internal.util.ShadowedSymbolTable;
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
    public static FrameMaster singleton;

	//MODEL
	private AttributeProvider selectedAttributeProvider = null;
	public List<PointCloud> pointClouds = new ArrayList<PointCloud>();
	private WorldViewer viewer;
	private DefaultTreeModel treeModel;

	//VIEW
	private Renderer renderer;
	public GLCanvas canvas;

	//CONTROLLER
	private KeyboardSelectionController selectionController;

	private PointCloud activePointCloud;
	private JTree tree;
	public JCheckBoxMenuItem mouseFix;


	public FrameMaster() {
    	super("FITS 3D");
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
		FileDialog fd = new FileDialog(this, "Save File", FileDialog.LOAD);
		fd.setVisible(true);

		fd.setMultipleMode(false);
		if (fd.getFiles().length > 0) {
			String fileName = fd.getFiles()[0].getAbsolutePath();
			loadFile(fileName);
		}

    }


    private void loadFile(String fileName) {
    	PointCloud pc = new PointCloud(fileName);
    	this.pointClouds.add(pc);


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
	private Component makeAttributePanel() {

		MigLayout mlLayout = new MigLayout("wrap 1, insets 10 0 0 10", "[grow,fill]");
//		MigLayout mlLayout = new MigLayout("insets 0", "[grow,fill]");
//		mlLayout.


		JPanel attributPanel = new JPanel(mlLayout);
		attributPanel.setBackground(Color.white);
		attributPanel.setBorder(new EmptyBorder(0, 8, 8, 8));

//		JLabel title = new JLabel("Attributes");
//		title.setFont(new Font("Dialog", Font.BOLD, 24));
//		attributPanel.add(title, "span 2");
		if (selectedAttributeProvider != null) {
			JPanel attrAttrPanel = new JPanel(new MigLayout("wrap 2, insets 4", "[40:40:40][grow,fill]"));
			for (Attribute attribute : selectedAttributeProvider.getAttributes()) {


				AttributeDisplayer attributeDisplayer = AttributeDisplayManager.defaultDisplayManager.tweakableForAttribute(attribute, selectedAttributeProvider);
				addTweakableToAttributePanel(attributeDisplayer, attribute, attrAttrPanel);
			}
			attributPanel.add(attrAttrPanel);
			attrAttrPanel.setBorder(BorderFactory.createTitledBorder("Attributes"));
			attrAttrPanel.setBackground(Color.white);

			//--now look for child providers
			List<AttributeProvider> childProviders = selectedAttributeProvider.getChildProviders();

				for (AttributeProvider childProvider : childProviders) {
					JPanel subAttrPanel = new JPanel(new MigLayout("wrap 2, insets 4", "[grow,fill]"));
//					AttributeDisplayer titleTweakable = new Tweakable.ChrisTitle(childProvider.getName());
//					attributPanel.add(titleTweakable.getComponent(), "span 2");
					for (Attribute attribute : childProvider.getAttributes()) {
						AttributeDisplayer attributeDisplayer = AttributeDisplayManager.defaultDisplayManager.tweakableForAttribute(attribute, childProvider);
						addTweakableToAttributePanel(attributeDisplayer, attribute, subAttrPanel);
					}
					//-space them out
//					attributPanel.add(new JLabel(" "), "span 2");
					attributPanel.add(subAttrPanel);
					subAttrPanel.setBorder(BorderFactory.createTitledBorder(childProvider.getName()));
					subAttrPanel.setBackground(Color.white);
				}
		}

		Dimension lilDimension = new Dimension(310, 700);
		attributPanel.setMinimumSize(lilDimension);

		JScrollPane scrollPane = new JScrollPane(attributPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setMinimumSize(lilDimension);
		scrollPane.setMaximumSize(lilDimension);
		scrollPane.setPreferredSize(lilDimension);

		return scrollPane;
	}


	private JPanel  makeSelectionPanel() {
		MigLayout migLayout = new MigLayout("wrap 2");
		JPanel selectionPanel = new JPanel(migLayout);

		JLabel title = new JLabel("Selection");
		title.setFont(new Font("Dialog", Font.BOLD, 24));
		selectionPanel.add(title, "span 2");

		return selectionPanel;

	}


	void addTweakableToAttributePanel(AttributeDisplayer attributeDisplayer, Attribute attribute, JPanel attributPanel) {
		if (attributeDisplayer == null) {
			return;
		}
		String formatString = attributeDisplayer.isDoubleLiner() ? "span 2" : "";

		JLabel label = new JLabel(attribute.displayName);
		label.setFont(new Font("Dialog", Font.BOLD, 12));
		if (attributeDisplayer.shouldShowDisplayName()) {
			attributPanel.add(label, formatString);
		}


		formatString = attributeDisplayer.isDoubleLiner() ? "width ::250, span 2" : "gapleft 16, width ::150";
		attributPanel.add(attributeDisplayer.getComponent(), formatString);
	}


	/**
	 * Makes and returns the file panel
	 * @return The created file panel
	 */
	private JPanel makeFilePanel() {
		JPanel filePanel = new JPanel(new MigLayout("flowy, insets 10 10 10 10"));

		TreeNode treeRoot = treeRoot = new DefaultMutableTreeNode("Point Clouds");
		this.treeModel = new DefaultTreeModel(treeRoot);

		this.tree = new JTree(treeModel);
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
				TreePath parentPath = e.getTreePath();
				TreePath childPath = parentPath.pathByAddingChild(e.getChildren()[e.getChildren().length - 1]);

				FrameMaster.this.tree.expandPath(e.getTreePath());
				try {
					FrameMaster.this.tree.setSelectionPath(childPath);
				}catch (NullPointerException npe) {
					System.out.println("whoops");
				}
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
		menuBar.add(makeAdvancedMenu());
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

		JMenuItem exportRegionItem = new JMenuItem("Export Region");
		setKeyboardShortcutTo(KeyEvent.VK_E, exportRegionItem);
		exportRegionItem.addActionListener(e -> this.exportRegion());
		fileMenu.add(exportRegionItem);
		return fileMenu;
	}


	private JMenu makeAdvancedMenu() {
		JMenu advancedMenu = new JMenu("Advanced");

		JCheckBoxMenuItem fudge = new JCheckBoxMenuItem("De-jitter images");
		fudge.setSelected(RegionRepresentation.shouldFudge);
		fudge.addActionListener(e -> this.fudge());
		fudge.setToolTipText("This will display the cube with some slight noise to avoid distracting moire patterns");
		advancedMenu.add(fudge);

		this.mouseFix = new JCheckBoxMenuItem("Retina Mouse Fix");
		mouseFix.setSelected(WorldViewer.retinaFix);
		mouseFix.addActionListener(e -> {
			WorldViewer.retinaFix ^= true;
		});
		mouseFix.setToolTipText("This may fix issues with mouse selection being off by a factor of two on some high dpi screens");
		advancedMenu.add(mouseFix);

		JCheckBoxMenuItem cellScale = new JCheckBoxMenuItem("Adjust for Chromatic Abberation");
		cellScale.setSelected(RegionRepresentation.shouldScaleCells);
		cellScale.addActionListener(e -> RegionRepresentation.shouldScaleCells ^= true);
		cellScale.setToolTipText("This will adjust the sizes of the cells/pixels according to the CELLSCAL value in the header.  This accounts for the efffect of chromatic abberation");
		advancedMenu.add(cellScale);

		return advancedMenu;
	}
	/**
	 * Creates and returns the debug menu
	 * @return The created debug menu
	 */
	private JMenu makeDebugMenu() {
		JMenu debugMenu = new JMenu("Debug");

		JCheckBoxMenuItem rainbow = new JCheckBoxMenuItem("rainbow");
		debugMenu.add(rainbow);

		rainbow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FrameMaster.this.renderer.gay = !FrameMaster.this.renderer.gay;
				setNeedsDisplay();
			}
		});

		JCheckBoxMenuItem exaggerateCellScale = new JCheckBoxMenuItem("Exaggerate chromatic abberation");
		exaggerateCellScale.setSelected(RegionRepresentation.exaggerateCellScaling);
		exaggerateCellScale.addActionListener(e -> RegionRepresentation.exaggerateCellScaling ^= true);
		debugMenu.add(exaggerateCellScale);


		return debugMenu;
	}






//	//==================================================================================================================
//	//  USER ACTIONS
//	//==================================================================================================================



	/**
	 * User chooses to cut out a subsection
	 */
	public static void cutSelection() {
		PointCloud pc = getActivePointCloud();
		pc.cutOutSubvolume(pc.getSelection().getVolume().rejiggeredForPositiveSize());
		for (int i = 0; i < pc.getRegions().size() - 1; i++) {
			Region region = pc.getRegions().get(i);
			region.isVisible.notifyWithValue(false, true);
		}


		pc.getSelection().setActive(false);

		pc.setShouldDisplaySlitherenated(false);

		singleton.reloadAttributePanel();
		FrameMaster.setNeedsDisplay();
	}

	private void exportRegion() {

		if (this.selectedAttributeProvider instanceof  Region) {
			Region selectedRegion = (Region) this.selectedAttributeProvider;
			exportRegion(selectedRegion);
		} else {
			JOptionPane.showMessageDialog(null,"Error: Must select a region in the Fits files navigator to export a region.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static void exportRegion(Region region) {
		PointCloud parentCloud = null;
		for (PointCloud pc : singleton.pointClouds) {
			if (pc.getRegions().contains(region)) {
				parentCloud = pc;
			}
		}

		if (parentCloud != null) {

			FileDialog fd = new FileDialog(FrameMaster.singleton, "Open File", FileDialog.SAVE);
			fd.setVisible(true);
			fd.setMultipleMode(false);
			if (fd.getFiles().length > 0) {
				FitsWriter.writeFits(parentCloud, region, fd.getFiles()[0]);
			}
		}
	}


	/**
	 * User choses to toggle the fudge factor in the point cloud loading
	 */

	private void fudge() {
		RegionRepresentation.shouldFudge = !RegionRepresentation.shouldFudge;
		for (PointCloud pc: this.pointClouds) {
			pc.refreshSelfWithQuality(pc.quality.getValue());
		}
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
		attachControlsToCanvas();
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
		this.renderer.informOfResolution(width, height);
		this.viewer.informOfResolution(width, height);
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

	public static void setNeedsAttribtueDisplay() {
		singleton.reloadAttributePanel();
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
	 */
	private void attachControlsToCanvas() {
		MouseController mouseController = new MouseController(this.viewer, this.renderer);
		canvas.addMouseMotionListener(mouseController);
		canvas.addMouseListener(mouseController);
		canvas.addMouseWheelListener(mouseController);
	}


	/**
	 * Creates an all new attributes panel and replaces the one (if there) in the frame.
	 */
	private void reloadAttributePanel() {
		BorderLayout bl = (BorderLayout)this.getContentPane().getLayout();

		if (bl.getLayoutComponent(BorderLayout.EAST)!= null) {
			this.getContentPane().remove(bl.getLayoutComponent(BorderLayout.EAST));
		}
		JScrollPane scrollPane = new JScrollPane();
		Component attributPanel = makeAttributePanel();


		this.getContentPane().add(attributPanel, BorderLayout.EAST);
		SwingUtilities.updateComponentTreeUI(this);
		this.getContentPane().repaint();
	}


	public static PointCloud getActivePointCloud() {
		AttributeProvider ap = singleton.selectedAttributeProvider;
//		System.out.println("the selcted atribute provider is :" + ap);
		if (ap instanceof  PointCloud) {
			return (PointCloud)ap;
		}
		else if (ap instanceof Region) {
			Region region = (Region)ap;
			for (PointCloud pc: singleton.pointClouds) {
				if(pc.getRegions().contains(region)){
					return pc;
				}
			}
		}
		return null;
	}

	public static void showOverlayDialogForPointCloud(PointCloud pc) {
		JFrame f = new JFrame("Overlay");

		MigLayout mlLayout = new MigLayout("wrap 2", "[grow,fill]");
		JPanel mainPanel = new JPanel(mlLayout);
		Dimension lilDimension = new Dimension(500,150);

		f.setPreferredSize(lilDimension);
		f.setMinimumSize(lilDimension);
		f.setMaximumSize(lilDimension);

		JLabel title = new JLabel("Which other point cloud would you like to overlay "+pc.toString()+" onto?");
		mainPanel.add(title, "span 2");

		int pos = 0;
		Object[]otherPointClouds = new Object[singleton.pointClouds.size()];
		otherPointClouds[pos++] = "-";
		for (PointCloud pc2 : singleton.pointClouds) {
			if (pc2!=pc) {
				otherPointClouds[pos++] = pc2;
			}
		}
		JComboBox<Object>pointCloudComboBox = new JComboBox<>(otherPointClouds);

		mainPanel.add(pointCloudComboBox, "span 2");

		JButton canelButton = new JButton("Cancel");
		canelButton.addActionListener(e -> f.dispose());
		mainPanel.add(canelButton);


		JButton applyButton = new JButton("OK");
		applyButton.addActionListener(e -> {
			Object obj = pointCloudComboBox.getSelectedItem();
			PointCloud selectedCloud = null;
			if (obj instanceof  PointCloud) {
				selectedCloud = (PointCloud)obj;
			}
			pc.setRelativeTo(selectedCloud);
			f.dispose();
			FrameMaster.setNeedsDisplay();
		});
		mainPanel.add(applyButton);
		applyButton.setFocusPainted(true);
		applyButton.setFocusable(true);

		f.add(mainPanel);
		f.setVisible(true);
	}


	public static void setMouseFixOn(boolean isOn) {
		singleton.mouseFix.setSelected(isOn);
	}
}