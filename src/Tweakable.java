import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.xml.internal.ws.api.Component;
import com.sun.xml.internal.ws.org.objectweb.asm.Label;

import sun.security.x509.DeltaCRLIndicatorExtension;

/**
 * Container for some tweakable UIElement
 * @author chrishawkins
 *
 */
public abstract class Tweakable {
	public List<Attribute>attributes = new ArrayList<Attribute>();
	
	
	
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
	public static class Toggleable extends Tweakable implements ChangeListener, AttributeDisplayer {
		public JCheckBox checkBox;
		
		public Toggleable(Attribute attribute, boolean isOn) {
			this.attributes.add(attribute);
			this.checkBox = new JCheckBox();
			this.checkBox.setSelected(isOn);
			this.checkBox.addChangeListener(this);
		}
		
		@Override
		protected Object getValue() {
			return new Boolean(checkBox.isSelected());
		}

		@Override
		public void setValue(Object value) {
			Boolean b = (Boolean)value;
			this.checkBox.setSelected(b);
		}

		@Override
		public void stateChanged(ChangeEvent e) {		
			notifyAttributes();
		}		
		
		@Override
		public JComponent getComponent() {
			return this.checkBox;
		}
	}
	
	/**
	 * A slider for choosing a single value wihtin a range
	 * @author chrishawkins
	 *
	 */
	public static class Slidable extends Tweakable implements ChangeListener, AttributeDisplayer{
		public JSlider slider;
		
		protected int steps;
		
		protected float min;
		protected float max;
		
		public Slidable(Attribute attrbute, float min, float max, float initialValue) {
			this(attrbute, min, max, initialValue, 100);
		}
		
		public Slidable(Attribute attrbute, float min, float max, float initialValue, int steps) {
			this.steps = steps;
			this.attributes.add(attrbute);
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
		
		
	}
	
	public static class ClickySlider extends Slidable {
		public ClickySlider(Attribute attrbute, float min, float max, float initialValue, int steps) {
			super(attrbute, min, max, steps);
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

	}
}
