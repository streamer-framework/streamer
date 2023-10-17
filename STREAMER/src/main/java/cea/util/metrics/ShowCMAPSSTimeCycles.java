package cea.util.metrics;

import cea.streamer.core.TimeRecord;

import java.util.Collections;
import java.util.Vector;

/**
 * Metric that return a vector of time cycles that correspond to the records.
 * Useful to plot the results according to the time cycles of the engine.
 * It is specific to the CMAPSS use case.
 */
public class ShowCMAPSSTimeCycles extends Metric {

	/**
	 * This method is used to unify the names.
	 * @return Unified name.
	 */
	@Override
	public String getName() {
		return "time_cycles";
	}
	
	/**
	 * Used to send the value of the time cycles directly to ES.
	 * @return Vector<Double> which contains the targets.
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		Vector<Double> ret = new Vector<>();
			
		for(TimeRecord record: records) {
			if (!record.getTarget().isEmpty() && !record.getOutput().isEmpty() &&
					!record.getOutput().equals(Collections.singletonList("nan"))) {
				ret.add(Double.parseDouble(record.getValue("time_cycles")));
			}
		}		
			
		if(ret.isEmpty()) {
			ret.add(Double.NaN);	
		}
		
		return ret;
	}

}
