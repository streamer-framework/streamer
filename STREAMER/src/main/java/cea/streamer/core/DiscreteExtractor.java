package cea.streamer.core;

public class DiscreteExtractor implements Extractor{

	String[] categories;
	
	public DiscreteExtractor(String[] values) {
		categories = values;
	} 
	
	@Override
	public Object getValue(String value) {
		for(int i=0; i< categories.length; i++){
			if(categories[i].equals(value)){
				return i;
			}
		}
		return -1;
	}

}
