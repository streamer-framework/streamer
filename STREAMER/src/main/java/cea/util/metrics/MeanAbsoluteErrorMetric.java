package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class MeanAbsoluteErrorMetric extends RegressionMetric {
	/**
	 * This method is used to unify the names of all the Mean Absolute Error metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "mean_absolute_error (MAE)";
	}
	
	/**
	 * This method is for evaluating the MAE metric for the Regression problems
	 * It considers target/output with just one value.
	 * @return Vector<Double> which contains the calculation of the Mean Absolute Error
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result = Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			double sum_absolute_errors = 0;
			for(TimeRecord record: records) {
				if (record.getTarget().isEmpty() || record.getOutput().isEmpty())
					continue;
				sum_absolute_errors += Math.abs(Double.parseDouble(record.getTarget().get(0))- Double.parseDouble(record.getOutput().get(0)));
			}
			result = GlobalUtils.safeDivison(sum_absolute_errors,records.size());
			result = GlobalUtils.roundAvoid(result, 3);
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
}
