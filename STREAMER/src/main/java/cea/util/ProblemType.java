package cea.util;

/**
 * class that represents the Type of problem we are working about. 
 * Add problem data format only for OnlineProducer functionality.
 *
 */

public enum ProblemType {
	
	KDDClassification("WaterPollutionRecord","yyyy-MM-dd");
	
	/**
	 * recordClass that we should use for process records, according to the problem we are working about
	 */
	private String recordClass="";
	/**
	 * date format that we should use for process records, according to the problem we are working about
	 */
	private String dateFormat="";
	/**
	 * time format that we should use for process records, according to the problem we are working about
	 */
	private String timeFormat;
	
	
	/** Constructor for the problem type class
	 * @param recordClass
	 * @param dateFormat
	 */
	private ProblemType(String recordClass, String dateFormat) {
		this.recordClass = recordClass;
		this.dateFormat = dateFormat;
		this.timeFormat = " HH:mm:ss";
	}

	/**
	 * @return recordClass that we should use for process records, according to the problem we are working about 
	 */
	public String getRecordClass() {
		return recordClass;
	}

	/**
	 * @return the date format that we should use for process records, according to the problem we are working about
	 */
	public String getDateFormat() {
		return dateFormat;
	}

	/**
	 * @return the time format that we should use for process records, according to the problem we are working about
	 */
	public String getTimeFormat() {
		return timeFormat;
	}
	
	
}
