import java.io.IOException;

import javax.management.RuntimeErrorException;


public class BitPix {
	public enum DataType {
		FLOAT, DOUBLE, SHORT, INT, LONG,
	}
	
	public static DataType dataTypeForBitPix(int bitPix) {
		switch (bitPix) {
			case -64:
				return DataType.DOUBLE;
			case -32:
				return DataType.FLOAT;
			case 16:
				return DataType.SHORT;
			case 32:
				return DataType.INT;
			case 64:
				return DataType.LONG;
			default:
				throw new RuntimeErrorException(null, "Cannot decode bitpix value of " + bitPix);
		}
	}
	
	
}
