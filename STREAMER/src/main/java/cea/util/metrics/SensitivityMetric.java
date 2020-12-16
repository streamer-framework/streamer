package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class SensitivityMetric extends Metric{

	/**
	 * Computes the accuracy of the time records outputs regarding their desired output.
	 * @param records
	 * @return Vector<Double> containing accuracy value (in %) in first position	 * 
	 */

	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		// TODO
		System.out.println("TODO");
		
		double result = -1;
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
				
		return ret;
	}

}
