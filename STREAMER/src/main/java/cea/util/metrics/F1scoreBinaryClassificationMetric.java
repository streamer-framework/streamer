package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

/**
 * @author MA261439
 *
 */

public class F1scoreBinaryClassificationMetric extends BinaryClassificationMetric {
	
	/**
	 * This method is used to unify the names of all the F1-score metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "f1score";
	}
	
	/**
	 * This method is for evaluating the F1-score metric for the Binary Classification problems
	 * 
	 * @return Vector<Double> which contains the calculation of the F1-score
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result = Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			super.evaluate(records,id);
			double recall = GlobalUtils.safeDivison(((double) (this.getTruePositive())),((double) (this.getTruePositive() + this.getFalseNegative() )));
			double precision = GlobalUtils.safeDivison(((double) (this.getTruePositive())),((double) (this.getTruePositive() + this.getFalsePositive() )));
			result = 2 * (GlobalUtils.safeDivison((precision*recall),(precision+recall)));
			result = GlobalUtils.roundAvoid(result, 3);	
		}		
		
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
}
