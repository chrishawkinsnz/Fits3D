import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;


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
		
		public PointCloud pointCloud;
		
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
	
	public static class Name extends Attribute {
		public String value;
		public Name(String displayName, String value, boolean shouldAggregate) {
			super(displayName, shouldAggregate);
			this.value = value;
		}
	}
	
	public static class PathName extends Name {
		public PathName(String displayName, String value, boolean shouldAggregate) {
			super(displayName, value, shouldAggregate);
		}
	}
	
	public static class FilterSelectionAttribute extends Attribute {
		public float minX;
		public float maxX;
		public float minY;
		public float maxY;
		public boolean isExponential;
		
		public FilterSelectionAttribute(String displayName, boolean shouldAggregate, Christogram.Filter data) {
			super(displayName, shouldAggregate);
			this.minX = data.minX;
			this.maxX = data.maxX;
			this.minY = data.minY;
			this.maxY = data.maxY;
			this.isExponential = data.isExponential;
		}

		@Override
		public void notifyWithValue(Object obj) {
			super.notifyWithValue(obj);
			Christogram.Filter data = (Christogram.Filter)obj;
			this.minX = data.minX;
			this.maxX = data.maxX;
			this.minY = data.minY;
			this.maxY = data.maxY;
			this.isExponential = data.isExponential;
		}
	}
}
