package cea.util.metrics;

import java.util.Collections;
import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

/**
 * Reliability Metric for Interval Regression problems
 * Source: https://arxiv.org/pdf/1911.08160.pdf
 * @author sg244422
 *
 */
public class AverageCoverageError extends RegressionMetric {
	
	/**
	 * Confidence interval
	 */
	private double q = 0.05; //5% by default 
	
	/**
	 * Returns the name of the metric
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "average_coverage_error (ACE)";
	}
	
	
	/**
	 * This method is for evaluating the ACE metric (Intervals Regression)
	 * @param Time records where outputs are multivalue: [output, lowerBound, UpperBound]
	 * @return Vector<Double> which contains the calculation of the alfa
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result = Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			result = PICP(records,id) - PINC(q);
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
	
	/**
	 * This method is for evaluating the PIPC metric (Intervals Regression)
	 * @param Time records where outputs are multivalue: [prediction, lowerBound, UpperBound]
	 * @param id
	 * @return Vector<Double> which contains the calculation of the alfa
	 */
	public double PICP(Vector<TimeRecord> records, String id) {
		double result = -1;		
		double acum = 0;
		boolean ok = true;
		int number_valid_records = 0;
		for(TimeRecord record: records) {
			if (record.getTarget().isEmpty() || record.getOutput().isEmpty() ||
					record.getOutput().equals(Collections.singletonList("nan")))
				continue;
			if(record.getOutput().size() !=3) {
				ok = false;
			}else {
				double target = Double.parseDouble(record.getTarget().get(0));
				double lowerBound = Double.parseDouble(record.getOutput().get(1));
				double upperBound = Double.parseDouble(record.getOutput().get(2));
				if( (lowerBound <= target) && (target <= upperBound) ) {
					acum += 1;
				}else {
					acum += 0;
				}
				number_valid_records++;
			}	
		}
		if(number_valid_records > 0) {
			result = GlobalUtils.safeDivison(acum,number_valid_records);
			result = GlobalUtils.roundAvoid(result, 4);
		}
		
		if(!ok) {
			System.err.println("At least one output does not contain 3 values [prediction, lowerBound, UpperBound]");
		}

		return result;
	}
	
	/**
	 * Computes alfa (prediction interval nominal coverage) for a specified q
	 * @return alfa
	 */
	public double PINC(double q) {
		return 1-2*q;
	}
}
