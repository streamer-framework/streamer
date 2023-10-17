package cea.util.metrics;

import java.util.Collections;
import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class ShowOutputs extends Metric {
	/**
	 * This method is used to unify the names 
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "estimated";
	}
	
	/**
	 * Used to send the value of the outputs directly to ES
	 * @return Vector<Double> which contains the targets
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		Vector<Double> ret = new Vector<Double>();
		
		for(TimeRecord record: records) {
			if (!record.getTarget().isEmpty() && !record.getOutput().isEmpty() &&
					!record.getOutput().equals(Collections.singletonList("nan"))) {
				ret.add(Double.parseDouble(record.getOutput().get(0)));
			}
		}			
		if(ret.isEmpty()) {
			ret.add(Double.NaN);	
		}
		return ret;
	}
}
