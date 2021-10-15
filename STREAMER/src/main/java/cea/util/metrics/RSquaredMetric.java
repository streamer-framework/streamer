package cea.util.metrics;

import java.util.ArrayList;
import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class RSquaredMetric extends RegressionMetric {
	/**
	 * This method is used to unify the names of all the R Square Error metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "r_square";
	}
	
	/**
	 * This method is for evaluating the R2 metric for the Regression problems
	 * It considers target/output with just one value.
	 * https://medium.com/analytics-vidhya/mae-mse-rmse-coefficient-of-determination-adjusted-r-squared-which-metric-is-better-cd0326a5697e
	 * @return Vector<Double> which contains the calculation of the R Square Error
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result = -1;
		double sum_sqaure_errors = 0;
		double sum_sqaure_means = 0;
		ArrayList<Double> targets = new ArrayList<Double>();
		
		for(TimeRecord record: records) {
			if (record.getTarget().isEmpty() || record.getOutput().isEmpty())
				continue;			
			targets.add(Double.parseDouble(record.getTarget().get(0)));			
			sum_sqaure_errors += Math.pow((Double.parseDouble(record.getTarget().get(0))- Double.parseDouble(record.getOutput().get(0))), 2);
		}
		
		double mean = calculateMean(targets);
		for(double t: targets) {
			sum_sqaure_means += Math.pow(t - mean, 2);
		}	
		
		result = 1 - (safeDivison(sum_sqaure_errors,sum_sqaure_means));
		result = this.roundAvoid(result, 3);
		
		//if (result < 0) result= 0;
		
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
}
