package cea.streamer.core;

public class GenericRecord extends TimeRecord {
	
	static String[] head;

	public GenericRecord() {
		super();
	}
	
	@Override
	public void fill(String key, String data) {		
		data = data.replace(" ","");
		if (data != "") {
			String[] fields = data.toLowerCase().split(getSeparatorFieldsRawData());			
			if (fields.length > 2) {
				setSourceFromKafkaKey(key);				
				fillTimeStamp(fields[0],"dd-MMM-yyHH:mm:ss.SSS");
				for(int i=1; i< fields.length; i++) {		
					values.put("value"+i,fields[i] );
					extractors.put("value"+i, new NumericalExtractor());
				}
			}			
		}		
	}	

}
