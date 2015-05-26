import java.util.function.Function;

public abstract class Attribute {
	public String displayName;
	public boolean isAggregatable;
	
	public abstract void notifyWithValue(Object obj);
	
	public static class RangedAttribute extends Attribute {
		public float value;
		public float min;
		public float max;
		
		public RangedAttribute(String displayName, float min, float max, float initialValue, boolean shouldAggregate) {
			this.displayName = displayName;
			this.min = min;
			this.max = max;
			this.value = initialValue;
			this.isAggregatable = shouldAggregate;
		}
		
		@Override
		public void notifyWithValue(Object obj) {
			Float float1 = (Float)obj;
			this.value = float1.floatValue();
			System.out.println("This here " + displayName + " attribute just updated with the value " + obj.toString());
		}
	}
	
	public static class SteppedRangeAttribute extends Attribute{
		public float value;
		public float min;
		public float max;
		public int steps;
		
		public PointCloud pointCloud;
		
		public SteppedRangeAttribute(String displayName, float min, float max, float initialValue, int steps, boolean shouldAggregate) {
			this.displayName = displayName;
			this.min = min;
			this.max = max;
			this.value = initialValue;
			this.isAggregatable = shouldAggregate;
			this.steps = steps;
		}
		
		@Override
		public void notifyWithValue(Object obj) {
			Float float1 = (Float)obj;
			this.value = float1.floatValue();
			if (this.pointCloud != null) {
				FrameMaster.desiredPointCloudFidelity = value;
				FrameMaster.pointCloudToUpdate = this.pointCloud;
				FrameMaster.pointCloudNeedsUpdatedPointCloud = true;
			}
			System.out.println("This here " + displayName + " attribute just updated with the value " + obj.toString());
		}
	}
	
	public static class BinaryAttribute extends Attribute {
		public boolean value; 
		
		public BinaryAttribute(String displayName, boolean initialValue, boolean shouldAggregate) {
			this.displayName = displayName;
			this.value = initialValue;
			this.isAggregatable = shouldAggregate;
		}
		
		@Override
		public void notifyWithValue(Object obj) {
			Boolean boolean1 = (Boolean)obj;
			this.value = boolean1.booleanValue();
			System.out.println("This here " + displayName + " attribute just updated with the value " + obj.toString());
		}
	}
	
	public static class Name extends Attribute {
		public String value;
		public Name(String displayName, String value, boolean shouldAggregate) {
			this.displayName = displayName;
			this.value = value;
			this.isAggregatable = shouldAggregate;
		}
		
		@Override
		public void notifyWithValue(Object obj) {
			System.out.println("This here " + displayName + " attribute just updated with the value " + obj.toString());
		}
	}
}
