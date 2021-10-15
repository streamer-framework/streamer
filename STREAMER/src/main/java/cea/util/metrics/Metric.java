package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public abstract class Metric {

	public String getName() {
		return getClass().getName();
	}
	
	/**
	 * Evaluates the solution with the specified metric
	 * @param records input
	 * @param id
	 * @return list of metrics evaluation values
	 */
	public abstract Vector<Double> evaluate(Vector<TimeRecord> records, String id);
		
}
