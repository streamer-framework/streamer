package cea.util.prepostprocessors;

import java.util.Arrays;
import java.util.List;


/**
 * Class thta splits the source line fields
 * Ideally data source line = timestamp;value
 * @author sgarcia
 *
 */
public class ProcessRawLines {
	
	String fieldsSeparator = ";";
	
	public ProcessRawLines(String separation) {
		fieldsSeparator = separation;
	}
	
	public ProcessRawLines() {}
   
	/**
	 * Method to process the line from the original file. Used in producer.
	 * fieldsSeparation is specific to each domain
	 * @param line: name;ts;value
	 * @param path of the file from we are reading
	 * @return a pair of key-value for the KStreamProducer object
	 */
	public String[] processRecords(String path, String line) {
		
        String[] key_value = new String[2];
		
        line = line.replaceAll("\"", "").replaceAll("\t", "");
        
		List<String> vals = Arrays.asList(line.split(fieldsSeparator));		
		
		String sensorName = path.trim().substring(path.lastIndexOf("/")+1,path.lastIndexOf("."));		
	
		key_value[0] = (sensorName+vals.get(0)).trim();		//id = name sensor + timestamp + value
		key_value[1] = sensorName+";"+line;		//record content
		
		return key_value;
	}
}
	