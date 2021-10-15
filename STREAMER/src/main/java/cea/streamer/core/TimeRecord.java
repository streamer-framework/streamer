package cea.streamer.core;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cea.util.GlobalUtils;

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
	 * (It is usually the name of the file or the sensor)
	 */
	protected String source = null;
	
	/**
	 * Headers of the data
	 */
	protected List<String> headers = null;
	
	/**
	 * Values of the time record
	 */
	protected Map<String,String> values = null;
	
	/**
	 * Extractor value
	 */
	protected Map<String,Extractor> extractors = null;

	/**
	 * Expected output (target) values for the record (only applied to supervised learning)
	 */
	protected List<String> target = null;
	/**
	 * Expected output (target) label (only applied to supervised learning)
	 */
	protected String targetLabel = null;
	
	/**
	 * Output values to the record (after applying model)
	 */
	protected List<String> output = null;
	/**
	 * Output label
	 */
	protected String outputLabel = null;
	
	/**
	 * Fields Separator token of raw data records
	 * (retrieved from kafka topics)
	 * User must set the one used in raw data
	 * Default value: ";"
	 */
	protected String separatorFieldsRawData;
	
	/**
	 * Separator of TimeStamp from rest of features for records stored in Redis
	 * Default value "·;"
	 * User can define its own separator token
	 */
	protected String separatorTSRedis; 	

	/**
 	 * Field separator token for data sotred/retreived from Redis
	 * Algorithms using Redis will use such separator to extract the data features
	 * Default value: " "
	 * User can define its own separator token
	 */
	protected String separatorFieldsRedis;
	
	/**
	 * Date format used by STREAMER
	 */
	private SimpleDateFormat systemTimeDateFormat;


	public TimeRecord(){
		timestamp = Calendar.getInstance().toString();
		values = new LinkedHashMap<String, String>();//LinkedHashMap maintains the insertion order
		extractors = new LinkedHashMap<String, Extractor>(); //LinkedHashMap maintains the insertion order
		headers = new ArrayList<String>();
		output = new ArrayList<String>();
		target = new ArrayList<String>();
		outputLabel = "output";
		targetLabel = "target";
		separatorFieldsRawData = ";";
		separatorFieldsRedis = " ";
		separatorTSRedis = ";";
		systemTimeDateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ", Locale.ENGLISH);//by default
	}
	
	
	//////////////////////////////////////////// GETTERS AND SETTERS /////////////////////////////////////////////////
	
	/**
	 * Get time stamp of the record
	 * @return the timestamp of the time record
	 */
	public String getTimeStamp() {
		return timestamp;	
	}
	
	/**
	 * 
	 * @param timestamp
	 */
	public void setTimeStamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	/**
	 * @return the source of the time record
	 */
	public String getSource() {
		return source;	
	}	
	
	/**
	 * Set the source of the record
	 * @param source
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Get the field separator used in the data received from kafka topic(s)
	 * Same fields separator as used in the source data pushed to kafka topic(s)
	 * Default value: ";"
	 * User must set the one used in raw data
	 * @return
	 */
	public String getSeparatorFieldsRawData() {
		return separatorFieldsRawData;
	}


	/**
	 * Set the field separator for data stored/retrieved from Redis
	 * Algorithms using Redis will use such separator to extract the data features
	 * @param separatorRedis
	 */
	public void setSeparatorFieldsRawData(String separatorRedis) {
		this.separatorFieldsRawData = separatorRedis;
	}

	/** 
	 * Get the separator of TimeStamp from rest of features for records stored in Redis
	 * Default value "·;"
	 * User can define its own separator token
	 * @return separatorTSRedis
	 */	 
	public String getSeparatorTSRedis() {
		return separatorTSRedis;
	}

	/** 
	 * Set he separator of TimeStamp from rest of features for records stored in Redis
	 * User can define its own separator token
	 * @param separatorFieldsRedis
	 */
	public void setSeparatorTSRedis(String separatorTSRedis) {
		this.separatorTSRedis = separatorTSRedis;
	}

	/** 
	 * Get the field separator token for data sotred/retreived from Redis
	 * Algorithms using Redis will use such separator to extract the data features 
	 * User can define its own separator token
	 * @return separatorFieldsRedis
	 */
	public String getSeparatorFieldsRedis() {
		return separatorFieldsRedis;
	}

	/** 
	 * Set the field separator token for data sotred/retreived from Redis
	 * Algorithms using Redis will use such separator to extract the data features 
	 * User can define its own separator token
	 * @param separatorFieldsRedis
	 */
	public void setSeparatorFieldsRedis(String separatorFieldsRedis) {
		this.separatorFieldsRedis = separatorFieldsRedis;
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
	
	/** Get the list of headers to the corresponding records features
	 * 
	 * @return list of headers
	 */
	public List<String> getHeaders() {
		return headers;
	}

	/** Set the list of headers to the corresponding records features
	 * 
	 * @param headers
	 */
	public void setHeaders(List<String> headers) {
		this.headers = headers;
	}
	
	/**
	 * Get list target (expected output) parameter
	 * @return the class of the time record
	 */
	public List<String> getTarget() {
		return target;
	}
	
	/**
	 * Setter for target (expected output) parameter
	 * @param classification
	 */
	public void setTarget(List<String> output) {
		setTarget(output, "target");		
	}
	
	
	/** Set a single target
	 * 
	 * @param target
	 */
	public void setTarget(String target) {
		List<String> targetList = new ArrayList<String>();
		targetList.add(target);
		setTarget(targetList, "target");		
	}
	
	/** 
	 * Set a single target 
	 * @param target
	 * @param target label
	 */
	public void setTarget(String target, String label) {
		setTargetLabel(label);
		setTarget(target);
	}
	
	/** 
	 * Set the list of targets 
	 * @param list of targets target
	 * @param label of the target
	 */
	public void setTarget(List<String> target, String label) {
		this.targetLabel=label;
		this.target = target;		
	}

	/** Get the label of the target
	 * 
	 * @return
	 */
	public String getTargetLabel() {
		return targetLabel;
	}

	/** Set the label of the target
	 * 
	 * @param targetLabel
	 */
	public void setTargetLabel(String targetLabel) {
		this.targetLabel = targetLabel;
	}

	/**
	 *  Get the label of the expected output
	 * @return outputLabel
	 */
	public String getOutputLabel() {
		return outputLabel;
	}

	/**
	 * Set the label of the expected output
	 * @param outputLabel
	 */
	public void setOutputLabel(String outputLabel) {
		this.outputLabel = outputLabel;
	}
	
	/**
	 * Retrieves the output to the record (after applying model)
	 * @return output
	 */
	public List<String> getOutput() {
		return output;
	}

	/**
	 * Sets the output to the record (after applying model)
	 * @param output
	 */
	public void setOutput(List<String> output) {
		setOutput(output, "output");
	}
	
	/**
	 * Sets one single output value to the record (after applying model)
	 * @param output
	 */
	public void setOutput(String output) {
		List<String> outList = new ArrayList<String>();
		outList.add(output);
		setOutput(outList, "output");
	}
	
	/**
	 * Add output(s) to the time record	
	 * @param output List of outputs per record
	 * @param label of the outputs
	 */
	public void setOutput(List<String> output, String label) {
		this.output = output;
		this.outputLabel = label;
	}

	
	//////////////////////////////////////////////////// OTHERS ////////////////////////////////////////////////////
	
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

		if(!getTarget().isEmpty()) {
			dev+=separator+GlobalUtils.listToString(getTarget(),getSeparatorFieldsRawData());
		}
		if(!getOutput().isEmpty()) {
			dev+=separator+GlobalUtils.listToString(getOutput(),getSeparatorFieldsRawData());
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
		if(!getTarget().isEmpty()) {
			dev+=separator+"target";
		}
		if(!getOutput().isEmpty()) {
			dev+=separator+"output";
		}
		return dev;
	}
	
	/**
	 * Export record values to a string
	 * @return the name, the timestamp and the values of the time record
	 */
	public String exportValues(){
		String dev = "";
		boolean sep=false;
		for(String k:values.keySet() ){
			if(sep) {
				dev += separatorFieldsRawData;
			}
			sep=true;
			dev += (values.get(k));
		}
		return dev;
	}
	
	/**
	 * Import the values from a string to a Record
	 * @return the name, the timestamp and the values of the time record
	 */
	public void importValues(String line){	
		String[] vals = line.split(getSeparatorFieldsRawData());
		
		for(int i=0; i< vals.length; i++){
			if(!headers.isEmpty()) {
				values.put(headers.get(i), vals[i]);
			} else {
				values.put("value"+i,vals[i]);				
			}		
		}
	}
	
	/**
	 * Set the record time stamp (in streamer time date format) with the original one passed by argument	 
	 * @param ts current time stamp (English time zone by default)
	 * @param format of the time stamp it follows
	 */
	public void fillTimeStamp(String ts, String format) {		
		fillTimeStamp(ts, format, Locale.ENGLISH);				
	}
	

	/**
	 * Set the record time stamp (in streamer time date format) with the original one passed by argument	 
	 * @param ts current time stamp
	 * @param format of the time stamp it follows
	 * @param english
	 */
	public void fillTimeStamp(String ts, String format, Locale timeZone) {
		try {
			timestamp = getSystemTimeDateFormat().format(new SimpleDateFormat(format, timeZone).parse(ts));
		} catch (ParseException e) {
			System.err.println("Error parsing original time stamp into record");
			e.printStackTrace();
		}		
	}
	
	/**
	 * Set the record time stamp (in streamer time date format) with a specific date	 
	 * @param Date to set
	 * @param format of the time stamp it follows
	 */
	public void fillTimeStamp(Date date) {		
		timestamp = getSystemTimeDateFormat().format(date);	
	}


	/**
	 * Extract the source of the record from the record key received from kafka topic(s)
	 * @param source
	 */
	public void setSourceFromKafkaKey(String source) {
		if(source.lastIndexOf("_") != -1) {
			this.source = source.substring(0, source.lastIndexOf("_"));
		}else {
			this.source = source;
		}
	}
	
	/** Get the streamer standard format 
	 * 
	 * @return SimpleDateFormat
	 */
	public SimpleDateFormat getSystemTimeDateFormat() {
		return systemTimeDateFormat;
	}

	/**
	 * Change the time date pattern of the system
	 * @param newPattern
	 * @param time zone
	 */
	public void changeTimeDatePattern(String newPattern, Locale timeZone) {
		systemTimeDateFormat = new SimpleDateFormat(newPattern, timeZone);
	}
	
	/**
	 * Set the streamer standard format 
	 * @param standardDateFormat
	 */
	public void setSystemTimeDateFormat(SimpleDateFormat standardTimeDateFormat) {
		this.systemTimeDateFormat = standardTimeDateFormat;
	}
	
	/**
	 * Default time set format
	 * @return the timestamp of the time record in milliseconds
	 */
	public long getTimeStampMillis() {
		Calendar cal = Calendar.getInstance();	
		Date dateTime;
		try {
			dateTime = systemTimeDateFormat.parse(timestamp);
			cal.setTime(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}			 
				
		return cal.getTimeInMillis();
	}
	
	/**
	 * @return the name, the timestamp and the values of the time record
	 */
	public String fullRecordToString(){		
		return (source+getSeparatorFieldsRawData()+exportToStringFormat(separatorFieldsRawData));
	}	
	
	/**
	 * @return the string of the timestamp and the values of the time record
	 */
	public String toString(){
		String dev = "TimeStamp = "+timestamp;		
		for(String k:values.keySet() ){
			dev += (" "+k+" = "+values.get(k));
		}

		if(!getTarget().isEmpty()) {
			dev+=" Target: "+GlobalUtils.listToString(getTarget(), getSeparatorFieldsRawData());
		}
		if(!getOutput().isEmpty()) {
			dev+=" Output: "+GlobalUtils.listToString(getOutput(), getSeparatorFieldsRawData());
		}
		return dev;
	}
	
	
	
	/**
	 * Method that extracts the timestamp, the name of the captor and the values of the time record
	 * This method is called by the constructor
	 * @param key Kafka Key of the record as stored in the topic
	 * @param value the string which contains the informations of the time record
	 */
	public abstract void fill(String key,String value);

}
