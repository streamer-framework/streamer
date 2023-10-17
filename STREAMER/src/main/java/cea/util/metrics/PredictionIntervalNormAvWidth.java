package cea.util.metrics;

import java.util.Collections;
import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

/**
 * Sharpness Metric for Interval Regression problems
 * Source: https://arxiv.org/pdf/1911.08160.pdf
 * @author sg244422
 *
 */
public class PredictionIntervalNormAvWidth extends RegressionMetric {
		
	/**
	 * Target range (AL use case)
	 * 	Max = 4.64901391144444
	 *  Min = -1.16108771735395
	 */
	private double A = (4.64901391144444-(-1.16108771735395));	
	/**
	 * Returns the name of the metric
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "prediction_interval_normalized_average_width (PINAW)";
	}
		
	/**
	 * This method is for evaluating the PINAW metric (Intervals Regression)
	 * @param Time records where outputs are multivalue: [output, lowerBound, UpperBound]
	 * @return Vector<Double> which contains the calculation of the alfa
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result=Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
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
					double lowerBound = Double.parseDouble(record.getOutput().get(1));
					double upperBound = Double.parseDouble(record.getOutput().get(2));				
					acum += (upperBound-lowerBound);	
					number_valid_records++;
				}	
			}
			if(!ok) {
				System.err.println("At least one output does not contain 3 values [prediction, lowerBound, UpperBound]");
			}
			if(number_valid_records > 0) {
				result = GlobalUtils.safeDivison(acum,(number_valid_records*A));
				result = GlobalUtils.roundAvoid(result, 4);
			}
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		
		return ret;
	}
	
}
