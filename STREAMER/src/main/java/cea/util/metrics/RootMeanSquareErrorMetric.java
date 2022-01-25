package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class RootMeanSquareErrorMetric extends RegressionMetric {
	/**
	 * This method is used to unify the names of all the Root Mean Square Error metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "root_mean_square_error (RMSE)";
	}
	
	/**
	 * This method is for evaluating the RMSE metric for the Regression problems
	 * It considers target/output with just one value.
	 * @return Vector<Double> which contains the calculation of the Root Mean Square Error
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result=Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			double sum_sqaure_errors = 0;
			for(TimeRecord record: records) {
				if (record.getTarget().isEmpty() || record.getOutput().isEmpty())
					continue;
	
				sum_sqaure_errors += Math.pow((Double.parseDouble(record.getTarget().get(0))- Double.parseDouble(record.getOutput().get(0))), 2);
	
			}
			result = Math.sqrt(GlobalUtils.safeDivison(sum_sqaure_errors,records.size()));
			result = GlobalUtils.roundAvoid(result, 3);
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
}
