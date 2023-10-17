package cea.util.metrics;

import java.util.Collections;
import java.util.Vector;

import cea.streamer.core.TimeRecord;

/**
 * Metric that return a vector of unit_nr (engine unit number) that correspond to the records.
 * Useful to group the results according to their engine unit number.
 * It is specific to the CMAPSS use case.
 */
public class ShowCMAPSSUnit extends Metric {

	/**
	 * This method is used to unify the names.
	 * @return Unified name.
	 */
	@Override
	public String getName() {
		return "engine_unit";
	}
	
	/**
	 * Used to send the value of the engine unit directly to ES.
	 * @return Vector<Double> which contains the targets.
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		Vector<Double> ret = new Vector<>();
			
		for(TimeRecord record: records) {
			if (!record.getTarget().isEmpty() && !record.getOutput().isEmpty() &&
					!record.getOutput().equals(Collections.singletonList("nan"))) {
				ret.add(Double.parseDouble(record.getValue("unit_nr")));
			}
		}		
			
		if(ret.isEmpty()) {
			ret.add(Double.NaN);	
		}
		
		return ret;
	}

}
