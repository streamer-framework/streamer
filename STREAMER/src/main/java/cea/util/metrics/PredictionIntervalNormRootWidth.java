package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

/**
 * Sharpness Metric for Interval Regression problems
 * Source: https://arxiv.org/pdf/1911.08160.pdf
 * @author sg244422
 *
 */
public class PredictionIntervalNormRootWidth extends RegressionMetric {
	
	
	/**
	 * Target range
	 * 	Max = 4.64901391144444
	 *  Min = -1.16108771735395
	 */
	private double A = (4.64901391144444-(-1.16108771735395)); //by default (1 to be ignored since we do not know the range a priori) 
	
	/**
	 * Returns the name of the metric
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "prediction_interval_normalized_root_mean_square_width (PINRW)";
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
			for(TimeRecord record: records) {
				if (record.getTarget().isEmpty() || record.getOutput().isEmpty())
					continue;
				if(record.getOutput().size() !=3) {
					ok = false;
				}else {
					double lowerBound = Double.parseDouble(record.getOutput().get(1));
					double upperBound = Double.parseDouble(record.getOutput().get(2));				
					acum += Math.pow((upperBound-lowerBound),2);				
				}	
			}
			result = Math.sqrt(GlobalUtils.safeDivison(acum,records.size()));
			result = GlobalUtils.safeDivison(result,A);
			result = GlobalUtils.roundAvoid(result, 4);
			if(!ok) {
				System.err.println("At least one output does not contain 3 values [prediction, lowerBound, UpperBound]");
			}
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		
		return ret;
	}
	
}
