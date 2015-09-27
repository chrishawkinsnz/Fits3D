package UserInterface;

import Model.Attribute;
import net.miginfocom.swing.MigLayout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Container for some tweakable UIElement
 * @author chrishawkins
 *
 */
public abstract class Tweakable implements  AttributeDisplayer{
	public List<Attribute>attributes = new ArrayList<Attribute>();

	public Tweakable(Attribute attribute) {
		this.attributes.add(attribute);
	}

	
	public void notifyAttributes() { 
		for (Attribute attribute : attributes) {
			attribute.notifyWithValue(this.getValue());
		}
	}


	protected abstract Object getValue();

        
	/**
	 * A toggleable checkbox for selecting a boolean value
	 * @author chrishawkins
	 *
	 */
	public static class Toggleable extends Tweakable implements ActionListener, AttributeDisplayer {
		public JCheckBox checkBox;
		
		public Toggleable(Attribute attribute, boolean isOn) {
			super(attribute);
			this.checkBox = new JCheckBox();
			this.checkBox.setSelected(isOn);
			this.checkBox.addActionListener(this);
		}
		
		@Override
		protected Object getValue() {
			boolean isSelected = checkBox.isSelected();
			return new Boolean(checkBox.isSelected());
		}

		@Override
		public void setValue(Object value) {
			Boolean b = (Boolean)value;
			this.checkBox.setSelected(b);
		}




		@Override
		public JComponent getComponent() {
			return this.checkBox;
		}

		@Override
		public boolean isDoubleLiner() {
			return false;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			notifyAttributes();

			FrameMaster.setNeedsDisplay();
		}


	}
	
	/**
	 * A slider for choosing a single value wihtin a range
	 * @author chrishawkins
	 *
	 */
	public static class Slidable extends Tweakable implements ChangeListener, AttributeDisplayer {
		public JSlider slider;
		
		protected int steps;
		
		protected float min;
		protected float max;
		
		public Slidable(Attribute attrbute, float min, float max, float initialValue) {
			this(attrbute, min, max, initialValue, 100);
		}
		
		public Slidable(Attribute attrbute, float min, float max, float initialValue, int steps) {
			super (attrbute);
			this.steps = steps;
			this.min = min;
			this.max = max;
			this.slider = new JSlider(0, steps);
			setValue(new Float(initialValue));
			this.slider.addChangeListener(this);
		}
		
		protected float getDelta() {
			return (max - min) / (float)steps;
		}
		
		@Override
		protected Object getValue() {
			int stepsIn = (slider.getValue() - this.slider.getMinimum());
			int stepsRange = slider.getMaximum() - this.slider.getMinimum();
			float proportion = (float)stepsIn / (float)stepsRange;
			float value = proportion * (max - min) + min;
			return new Float(value);
		}

		
		@Override
		public void stateChanged(ChangeEvent e) {
			//--don't update till released
//			if (slider.getValueIsAdjusting()) 
//				return;
			
			notifyAttributes();
		}		
		
		@Override
		public JComponent getComponent() {
			return this.slider;
		}
		
		@Override
		public void setValue(Object value) {
			Float fl = (Float)value;
			float proportion = (fl - min) / (max - min);
			int steps = (int)(proportion / getDelta());
			slider.setValue(steps);
		}
		
		@Override
		public boolean isDoubleLiner() {
			return false;
		}
		
	}
	
	public static class ClickySlider extends Slidable {
		public ClickySlider(Attribute attrbute, float min, float max, float initialValue, int steps) {
			super(attrbute, min, max, initialValue);
			slider.setPaintTicks(true);
			slider.setSnapToTicks(true);

		}
		
		@Override
		public void stateChanged(ChangeEvent e) {
			//--don't update till released
			if (slider.getValueIsAdjusting()) 
				return;
			
			notifyAttributes();
		}
		
		@Override
		public boolean isDoubleLiner() {
			return false;
		}
	}

	public static class DropDown extends Tweakable implements ActionListener{
		private JComboBox<Object>comboBox;

		public DropDown(Attribute attribute, List<Object>choices, Object currentChoice) {
			super(attribute);
			comboBox = new JComboBox<Object>(choices.toArray());
			comboBox.setSelectedItem(currentChoice);
			comboBox.addActionListener(this);
		}


		@Override
		public Object getValue() {
			return comboBox.getSelectedItem();
		}

		@Override
		public void setValue(Object value) {
			comboBox.setSelectedItem(value);
		}

		@Override
		public JComponent getComponent() {
			return this.comboBox;
		}

		@Override
		public boolean isDoubleLiner() {
			return false;
		}


		@Override
		public void actionPerformed(ActionEvent e) {
			notifyAttributes();
		}
	}
	/**
	 * A simple un-editable label that displays a value
	 * @author chrishawkins
	 *
	 */
	public static class ChrisLabel implements AttributeDisplayer {
		JLabel label = new JLabel(":)");
		
		public ChrisLabel(String text){
			this.label.setText(text);
		}
		
		@Override
		public void setValue(Object value) {
			String string = (String)value;
			label.setText(string);
		}
		
		@Override
		public JComponent getComponent() {
			return label;
		}

		@Override
		public boolean isDoubleLiner() {
			return false;
		}
		
	}

	public static class ChrisTitle extends  ChrisLabel {
		public ChrisTitle(String text) {
			super(text);
			label.setFont(new Font("Dialog", Font.BOLD, 24));
		}

		@Override
		public boolean isDoubleLiner() {
			return true;
		}
	}
	
	public static class ChristogramTweakable extends Tweakable implements AttributeDisplayer, ChangeListener, ActionListener {
		private Christogram christogram;
		private JFormattedTextField textLeft;
		private JFormattedTextField textRight;
		private JComponent holder;
		
		public ChristogramTweakable(int[]buckets, Attribute attribute, float min, float max, String title){
			super(attribute);
			int width = 240;
			Dimension christogramDimension = new Dimension(width, 215);
	        christogram = new Christogram(buckets, min, max);
	        christogram.setXAxisTitle(title != null ? title : " ");
	        christogram.setMinimumSize(christogramDimension);
//	        christogram.setPreferredSize(christogramDimension);
//			christogram.setMaximumSize(christogramDimension);
	        christogram.addChangeListener(this);



			Dimension lilTextFieldDimension = new Dimension(50,20);
			this.textLeft = new JFormattedTextField("0.0");
			this.textLeft.addActionListener(this);
			this.textLeft.setText("" + christogram.getSelection().minX);

			this.textRight = new JFormattedTextField("0.0");
			this.textRight.setHorizontalAlignment(SwingConstants.RIGHT);
			this.textRight.setText("" + christogram.getSelection().maxX);
			this.textRight.addActionListener(this);



			MigLayout mlLayout = new MigLayout("wrap 3, insets 2 2 2 2", "[grow,fill]");
			holder = new JPanel(mlLayout);

//			holder.setMinimumSize(christogramDimension);
//			holder.setMaximumSize(christogramDimension);
//			holder.setPreferredSize(christogramDimension);
			holder.add(christogram, "span 3");
			holder.add(new JLabel("   Left/Right mouse to move bounds"),"span 3");
			holder.add(new JLabel("            or enter bounds below"), "span 3");
			holder.add(new JLabel("  (Shift click to move entire window)"), "span 3");

			holder.add(this.textLeft);
			holder.add(new JLabel("        to"));
			holder.add(this.textRight);
		}

		
		@Override
		public void setValue(Object value) {
			System.err.println("not yet implemented setValue");
			
		}
		
		@Override
		public JComponent getComponent() {
			return holder;
//			return christogram;
		}

		@Override
		public boolean isDoubleLiner() {
			return true;
		}
		
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() instanceof Christogram) {
				String leftText = "" + this.christogram.getSelection().minX;
				if (leftText.length() > 7) {
					leftText = leftText.substring(0,7);
				}

				String rightText = "" + this.christogram.getSelection().maxX;
				if (rightText.length() > 7) {
					rightText = rightText.substring(0,7);
				}
				this.textLeft.setText(leftText);
				this.textRight.setText(rightText);
			}
			//--update the text
			notifyAttributes();
		}
		public void actionPerformed(ActionEvent e) {
			System.out.println("hellelel");
			try {
				this.christogram.getSelection().minX = Float.parseFloat(this.textLeft.getText());
				this.christogram.getSelection().maxX = Float.parseFloat(this.textRight.getText());
				FrameMaster.setNeedsDisplay();
			}catch (NumberFormatException nfe) {
				nfe.printStackTrace();
				java.awt.Toolkit.getDefaultToolkit().beep();
			}
			this.christogram.repaint();
		}

		@Override
		protected Object getValue() {
			return christogram.getSelection();
		}
	}

	public static class ChrisButton extends Tweakable implements AttributeDisplayer {
		private String name;
		private JButton button;
		public ChrisButton(Attribute.Actchin attribute) {
			super(attribute);
			name = attribute.displayName;
			button = new JButton();
			button.setText(name);
			button.addActionListener(e -> notifyAttributes());

		}

		@Override
		protected Object getValue() {
			return null;
		}

		@Override
		public void setValue(Object value) {

		}

		@Override
		public JComponent getComponent() {
			return button;
		}

		@Override
		public boolean isDoubleLiner() {
			return true;
		}

		@Override
		public boolean shouldShowDisplayName() {
			return false;
		}



	}
	
}


