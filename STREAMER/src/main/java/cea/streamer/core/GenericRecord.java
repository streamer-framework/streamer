package cea.streamer.core;

public class GenericRecord extends TimeRecord {
	
	static String[] head;

	public GenericRecord() {
		super();
	}
	
	public GenericRecord(String data) {
		this();
		fill(data);
	}
	
	@Override
	public void fill(String data) {		
		if (data.trim() != "") {
			String[] fields = data.trim().toLowerCase().split(";");			
			if (fields.length > 3) {
				source = fields[0];
				timestamp = fields[1];
				for(int i=2; i< fields.length; i++) {		
					values.put("value"+(i-1),fields[i] );
					extractors.put("value"+(i-1), new NumericalExtractor());
				}
			}			
		}		
	}	

}
