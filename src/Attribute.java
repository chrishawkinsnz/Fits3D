import com.sun.tools.doclint.HtmlTag;

import javax.smartcardio.ATR;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class Attribute {
	public String displayName;
	public boolean isAggregatable;
	
	public Consumer<Object>callback = (obj)->{};
	
	private Attribute(String displayName, boolean isAggregatable) {
		this.displayName = displayName;
		this.isAggregatable = isAggregatable;
	}
	
	public void notifyWithValue(Object obj) {
		callback.accept(obj);
		FrameMaster.setNeedsDisplay();
	}
	
	public static class RangedAttribute extends Attribute {
		public float value;
		public float min;
		public float max;
		
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
	}
	
	public static class SteppedRangeAttribute extends Attribute{
		public float value;
		public float min;
		public float max;
		public int steps;
		
		private PointCloud pointCloud;
		
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
//			if (this.pointCloud != null) {
//				FrameMaster.desiredPointCloudFidelity = value;
//				FrameMaster.pointCloudToUpdate = this.pointCloud;
//				FrameMaster.pointCloudNeedsUpdatedPointCloud = true;
//			}
		}
	}
	
	public static class BinaryAttribute extends Attribute {
		public boolean value; 
		
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
	}
	
	public static class TextAttribute extends Attribute {
		public String value;
		public TextAttribute(String displayName, String value, boolean shouldAggregate) {
			super(displayName, shouldAggregate);
			this.value = value;
		}
	}
	
	public static class PathName extends TextAttribute {
		public PathName(String displayName, String value, boolean shouldAggregate) {
			super(displayName, value, shouldAggregate);
		}
	}
	
	public static class FilterSelectionAttribute extends Attribute {
		public Christogram.Filter filter;
		
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

	}
}
