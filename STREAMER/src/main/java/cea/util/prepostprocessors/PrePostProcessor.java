package cea.util.prepostprocessors;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class PrePostProcessor {

	/**
	 * Pre process the set of time records
	 * @param records input
	 * @return processed time records
	 */
	public Vector<TimeRecord> preprocess(Vector<TimeRecord> records, String id){
		return records;
	}
	
	/**
	 * Post process the set of time records
	 * @param records input
	 * @return processed time records
	 */
	public Vector<TimeRecord> postprocess(Vector<TimeRecord> records, String id){
		return records;
	}
		
}
