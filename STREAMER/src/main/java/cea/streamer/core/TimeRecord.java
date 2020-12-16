package cea.streamer.core;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class that implements a time record
 *
 */
public abstract class TimeRecord implements Serializable{

	/**
	 * Timestamp of the time record
	 */
	protected String timestamp = null;
	
	/**
	 * Source of the data
	 */
	protected String source = null;
	
	/**
	 * Values of the time record
	 */
	protected Map<String,String> values = null;
	
	/**
	 * Extractor value
	 */
	protected Map<String,Extractor> extractors = null;

	/**
	 * Expected output (target) for the record (only applied to supervised learning)
	 */
	protected String target = null;
	/**
	 * Expected output (target) label (only applied to supervised learning)
	 */
	protected String targetLabel = null;
	
	/**
	 * Output to the record (after applying model)
	 */
	protected String output = null;
	/**
	 * Output label
	 */
	protected String outputLabel = null;
	
	/**
	 * Separator between fields in the string record
	 */
	protected String separator = ";";


	public TimeRecord(){
		timestamp = Calendar.getInstance().toString();
		values = new LinkedHashMap<String, String>();//TreeMap<String, String>();
		extractors = new TreeMap<String, Extractor>();
	}
	
	
	/**
	 * @return the timestamp of the time record
	 */
	public String getTimeStamp() {
		return timestamp;	
	}
	
	/**
	 * @return the name of the captor 
	 */
	public String getName() {
		return source;	
	}
	
	/**
	 * @return the source of the time record
	 */
	public String getSource() {
		return source;	
	}
	
	/**
	 * 
	 * @return
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * 
	 * @param timestamp
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * 
	 * @param source
	 */
	public void setSource(String source) {
		this.source = source;
	}


	/**
	 * 
	 * @return
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * 
	 * @param separator
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	
	/**
	 * @return the values of time record
	 */
	public Map<String,String> getValues() {
		return values;	
	}
	
	/**
	 * 
	 * @param key
	 * @return the values to the key
	 */
	public String getValue(String key) {
		return values.get(key);	
	}
	
	/**
	 * 
	 * @param key value to insert
	 * @return the values to the key
	 */
	public String setValue(String key, String value) {
		return values.put(key,value);	
	}

	/**
	 * @return the class of the time record
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Setter for target (expected output) parameter
	 * @param classification
	 */
	public void setTarget(String output) {
		setTargetOutput(output, "target");
		
	}
	
	public void setTargetOutput(String output, String label) {
		this.targetLabel=label;
		this.target = output;		
	}

	/**
	 * 
	 * @return
	 */
	public String getExpectedOutputLabel() {
		return targetLabel;
	}

	/**
	 * 
	 * @param targetLabel
	 */
	public void setTargetLabel(String targetLabel) {
		this.targetLabel = targetLabel;
	}

	/**
	 * 
	 * @return
	 */
	public String getOutputLabel() {
		return outputLabel;
	}

	/**
	 * 
	 * @param outputLabel
	 */
	public void setOutputLabel(String outputLabel) {
		this.outputLabel = outputLabel;
	}
	
	/**
	 * @return the string of the timestamp and the values of the time record
	 */
	public String toString(){
		String dev = "TimeStamp = "+timestamp;		
		for(String k:values.keySet() ){
			dev += (" "+k+" = "+values.get(k));
		}

		if(getTarget() != null) {
			dev+=" Target: "+getTarget();
		}
		if(getOutput() != null) {
			dev+=" Output: "+getOutput();
		}
		return dev;
	}
	
	/**
	 * Export the TimeRecord in a string (ready to store in file)
	 * @param separator character for the fields
	 * @return the string of the timestamp and the values of the time record in csv format 
	 */
	public String exportToStringFormat(String separator){	
		String dev = ""+timestamp;		
		for(String k:values.keySet() ){
			dev += (separator+values.get(k));
		}

		if(getTarget() != null) {
			dev+=separator+getTarget();
		}
		if(getOutput() != null) {
			dev+=separator+getOutput();
		}
		return dev;
	}
	
	
	
	/**
	 * Export the TimeRecord HEADER in a string (ready to store in file)
	 * @param separator character for the fields
	 * @return the string of the timestamp and the names of head of the time record in csv format 
	 */
	public String exportHeaderToStringFormat(String separator) {
		String dev = "timestamp";
		for(String k:values.keySet() ){
			dev += (separator+k);
		}
		if(getTarget() != null) {
			dev+=separator+"target";
		}
		if(getOutput() != null) {
			dev+=separator+"output";
		}
		return dev;
	}
	
	/**
	 * @return the name, the timestamp and the values of the time record
	 */
	public String exportValues(){
		String dev = "";
		boolean sep=false;
		for(String k:values.keySet() ){
			if(sep) {
				dev += separator;
			}
			sep=true;
			dev += (values.get(k));
		}
		return dev;
	}
	
	/**
	 * @return the name, the timestamp and the values of the time record
	 */
	public void importValues(String line){
		String[] vals = line.split(separator);
		for(int i=0; i< vals.length; i++){
			values.put("value"+i,vals[i]);			
		}
	}
	
	/**
	 * @return the name, the timestamp and the values of the time record
	 */
	public String fullRecordToString(){		
		return (source+getSeparator()+exportToStringFormat(separator));
	}
	
	/**
	 * Retreives the output to the record (after applying model)
	 * @return output
	 */
	public String getOutput() {
		return output;
	}

	/**
	 * Sets the output to the record (after applying model)
	 * @param output
	 */
	public void setOutput(String output) {
		setOutput(output, "output");
	}
	
	public void setOutput(String output, String label) {
		this.output = output;
		this.outputLabel = label;
	}

	/**
	 * Default time set format
	 * @return the timestamp of the time record in milliseconds
	 */
	public long getTimeStampMillis() {
		Calendar cal = Calendar.getInstance();	
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy HH:mm:ss", Locale.ENGLISH);
		Date dateTime;
		try {
			dateTime = format.parse(timestamp);
			cal.setTime(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}			 
				
		return cal.getTimeInMillis();
	}
	
	
	/**
	 * Method that extracts the timestamp, the name of the captor and the values of the time record
	 * This method is called by the constructor
	 * @param data the string which contains the informations of the time record
	 */
	public abstract void fill(String data);

}
