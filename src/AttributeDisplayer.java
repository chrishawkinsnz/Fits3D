import javax.swing.JCheckBox;
import javax.swing.JComponent;


public interface AttributeDisplayer {
	public void setValue(Object value);
	public JComponent getComponent();
	public boolean isDoubleLiner();

	default public void beNotifiedWithValue(Object obj) {
		this.setValue(obj);
	}
}
