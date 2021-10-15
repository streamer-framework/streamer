package cea.producer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


/**
 * class that implements a raw record with specified fields needed for
 * real time record simulation based on old records. these two fields 
 * {@recordDateTimeBeforSimulation} and {@recordDateTimeAfterSimulation} are
 * needed for simulator algorithm for the next simulation, so we keep it with this object which is the 
 * simulator method return type. 
 *
 */
public class RawRecord extends RecordDetails {
	
	/**
	 * Date and time of the old record 
	 */
	private LocalDateTime recordDateTimeBeforSimulation;
	
	/**
	 * Date and time of the new created record (the simulated one)
	 */
	private LocalDateTime recordDateTimeAfterSimulation;
	
	
	/**
	 * @return the Date and time of the record before simulation
	 */
	public LocalDateTime getRecordDateTimeBeforSimulation() {
		return recordDateTimeBeforSimulation;
	}
	
	
	/**
	 * @return the Date and time of the record after simulation
	 */
	public LocalDateTime getRecordDateTimeAfterSimulation() {
		return recordDateTimeAfterSimulation;
	}

	/**
	 * setter for {@recordDateTimeAfterSimulation} parameter
	 * @param recordDateTimeAfterSimulation
	 */
	public void setRecordDateTimeAfterSimulation(LocalDateTime recordDateTimeAfterSimulation) {
		this.recordDateTimeAfterSimulation = recordDateTimeAfterSimulation;
	}

	/**
	 * setter for {@recordDateTimeBeforSimulation} parameter
	 * @param recordDateTimeBeforSimulation
	 */
	public void setRecordDateTimeBeforSimulation(LocalDateTime recordDateTimeBeforSimulation) {
		this.recordDateTimeBeforSimulation = recordDateTimeBeforSimulation;
	}


	/**
	 *Write the record details in the same format we had it before simulation.
	 */
	@Override
	public String toString() {
		if(classification==null) classification ="";
		String t=recordDateTimeAfterSimulation.toLocalTime().getSecond()==0?recordDateTimeAfterSimulation.toLocalTime()+":00":recordDateTimeAfterSimulation.toLocalTime()+"";
		String line = "\""+recordDateTimeAfterSimulation
				.toLocalDate()
					.format(DateTimeFormatter
						.ofPattern(dateFormat,Locale.ENGLISH))+" "+t+"\";\""+mesurement+"\";\""+classification+"\"";
		return line;
	}
	
	
}
