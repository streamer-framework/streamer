package cea.streamer.core;

public class NumericalExtractor implements Extractor{

	@Override
	public Object getValue(String value) {		
		return Double.parseDouble(value);
	}

}
