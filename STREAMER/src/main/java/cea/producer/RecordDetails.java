package cea.producer;


/**
 * Class that implements a record details
 *
 */
public abstract class RecordDetails {
	/**
	 * Mesurement of the record
	 */
	protected String mesurement;
	/**
	 * Class of the record if any 
	 */
	protected String classification;
	/**
	 * Date format of the record's date
	 */
	protected String dateFormat;
	
	/**
	 * @return the mesurement of the record 
	 */
	public String getMesurement() {
		return mesurement;
	}
	/**
	 * setter for {@mesurement} parameter
	 * @param mesurement
	 */
	public void setMesurement(String mesurement) {
		this.mesurement = mesurement;
	}
	/**
	 * @return the class of the record if any
	 */
	public String getClassification() {
		return classification;
	}
	/**
	 * Setter for {@classification} parameter
	 * @param classification
	 */
	public void setClassification(String classification) {
		this.classification = classification;
	}
	/**
	 * @return the date format of the record
	 */
	public String getDateFormat() {
		return dateFormat;
	}
	/**setter for {@dateFormat} parameter
	 * @param dateFormat
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
	
	}
