package cea.util.metrics;

import java.util.Collections;
import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class MeanAbsolutePercentageError extends RegressionMetric {
	/**
	 * This method is used to unify the names of all the Mean Percentage Error metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "mean_absolute_percentage_error (MAPE)";
	}
	
	/**
	 * This method is for evaluating the MAPE metric for the Regression problems
	 * It considers target/output with just one value.
	 * @return Vector<Double> which contains the calculation of the Mean Absolute Error
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result = Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			double aux = 0;
			int number_valid_records = 0;
			for(TimeRecord record: records) {
				if (record.getTarget().isEmpty() || record.getOutput().isEmpty() ||
						record.getOutput().equals(Collections.singletonList("nan")))
					continue;
				aux += Math.abs( ( Double.parseDouble(record.getTarget().get(0))- Double.parseDouble(record.getOutput().get(0)) ) / Double.parseDouble(record.getTarget().get(0)) );
				number_valid_records++;
			}
			if(number_valid_records > 0) {
				result = GlobalUtils.safeDivison(aux,number_valid_records);
				result = GlobalUtils.roundAvoid(result, 3);
			}
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
}
