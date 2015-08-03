import com.sun.tools.doclint.HtmlTag;

import javax.smartcardio.ATR;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Attribute {
	public String displayName;
	public boolean isAggregatable;
	public boolean shouldUpdateRenderer;

	private Object value;

	public AttributeDisplayer listeningAttributeDisplayer;

	public Consumer<Object>callback = (obj)->{};

	private Object savedState;
	private Attribute(String displayName, boolean isAggregatable) {
		this.displayName = displayName;
		this.isAggregatable = isAggregatable;
		this.shouldUpdateRenderer = true;
	}
	
	public void notifyWithValue(Object obj) {
		callback.accept(obj);
		if (shouldUpdateRenderer) {
			FrameMaster.setNeedsDisplay();
		}
	}

	public void notifyWithValue(Object obj, boolean doNotUpdateRenderer) {
		boolean defaultValue = this.shouldUpdateRenderer;
		shouldUpdateRenderer = doNotUpdateRenderer;
		notifyWithValue(obj);
		shouldUpdateRenderer = defaultValue;
	}

	public Object getValue() {
		return null;
	}

	public void saveState() {
		this.savedState = getValue();
	}

	public void restoreState() {
		if (this.savedState != null) {
			this.notifyWithValue(this.savedState);
			this.savedState = null;
		}
	}

	public void setListener(AttributeDisplayer listener) {
		this.listeningAttributeDisplayer = listener;
	}

	public void updateAttributeDisplayer() {
		this.listeningAttributeDisplayer.beNotifiedWithValue(this.getValue());
	}

	public static class RangedAttribute extends Attribute {
		private float value;
		private float min;
		private float max;
		
		public RangedAttribute(String displayName, float min, float max, float initialValue, boolean shouldAggregate) {
			super(displayName, shouldAggregate);
			this.min = min;
			this.max = max;
			this.value = initialValue;
		}
		
		@Override
		public void notifyWithValue(Object obj) {
			super.notifyWithValue(obj);
			Float float1 = (Float)obj;
			this.value = float1.floatValue();
		}

		@Override
		public Float getValue() {
			return this.value;
		}

		public Float getMin() {
			return this.min;
		}

		public Float getMax() {
			return this.max;
		}

	}
	
	public static class SteppedRangeAttribute extends Attribute{
		private float value;
		public float min;
		public float max;
		public int steps;

		public SteppedRangeAttribute(String displayName, float min, float max, float initialValue, int steps, boolean shouldAggregate) {
			super(displayName, shouldAggregate);
			this.min = min;
			this.max = max;
			this.value = initialValue;
			this.steps = steps;
		}
		
		@Override
		public void notifyWithValue(Object obj) {
			super.notifyWithValue(obj);
			Float float1 = (Float)obj;
			this.value = float1.floatValue();
		}

		@Override
		public Float getValue() {
			return value;
		}
	}
	
	public static class BinaryAttribute extends Attribute {
		private boolean value;
		
		public BinaryAttribute(String displayName, boolean initialValue, boolean shouldAggregate) {
			super(displayName, shouldAggregate);
			this.value = initialValue;
		}
		
		@Override
		public void notifyWithValue(Object obj) {
			super.notifyWithValue(obj);
			Boolean boolean1 = (Boolean)obj;
			this.value = boolean1.booleanValue();
		}

		@Override
		public Boolean getValue(){
			return this.value;
		}
	}
	
	public static class TextAttribute extends Attribute {
		private String value;
		public boolean isTitle;
		public TextAttribute(String displayName, String value, boolean shouldAggregate) {
			super(displayName, shouldAggregate);
			this.isTitle = false;
			this.value = value;
		}

		@Override
		public void notifyWithValue(Object obj) {
			super.notifyWithValue(obj);
			String str = (String)obj;
			this.value = str;
		}

		@Override
		public String getValue() {
			return this.value;
		}
	}
	
	public static class PathName extends TextAttribute {
		public PathName(String displayName, String value, boolean shouldAggregate) {
			super(displayName, value, shouldAggregate);
		}
	}
	
	public static class FilterSelectionAttribute extends Attribute {
		private Christogram.Filter filter;
		
		public FilterSelectionAttribute(String displayName, boolean shouldAggregate, Christogram.Filter filter) {
			super(displayName, shouldAggregate);
			this.filter = filter;
		}

		@Override
		public void notifyWithValue(Object obj) {
			super.notifyWithValue(obj);
			Christogram.Filter filter = (Christogram.Filter)obj;
			this.filter = filter;
		}

		@Override
		public Christogram.Filter getValue() {
			return this.filter;
		}
	}

	public static class MultiChoiceAttribute extends Attribute {
		public List<Object> choices;
		public Object choice;

		public MultiChoiceAttribute(String displayName, List<Object>possibleChoices, Object initialChoice) {
			super(displayName, false);
			this.choices = possibleChoices;
			this.choice = initialChoice;
		}

		@Override
		public void notifyWithValue(Object obj) {
			super.notifyWithValue(obj);
			this.choice = obj;
		}

		public void updateChoices(List<Object>newChoices) {
			this.choices = newChoices;
		}

		@Override
		public Object getValue() {
			return this.choice;
		}

	}
}
