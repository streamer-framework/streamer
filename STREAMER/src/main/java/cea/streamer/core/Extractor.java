package cea.streamer.core;

public interface Extractor {

	/**
	 * Get the value in the correct domain
	 * @param value in string format
	 * @return value in domain
	 */
	public Object getValue(String value);	
	
}
