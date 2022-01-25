package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class PrecisionBinaryClassificationMetric extends BinaryClassificationMetric {
	
	/**
	 * This method is used to unify the names of all the Precision metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "precision";
	}
	
	/**
	 * This method is for evaluating the Precision metric for the Binary Classification problems
	 * 
	 * @return Vector<Double> which contains the calculation of the Precision
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result=Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			super.evaluate(records,id);
			result = GlobalUtils.safeDivison(((double) (this.getTruePositive())),((double) (this.getTruePositive() + this.getFalsePositive() )));
			result = GlobalUtils.roundAvoid(result, 3);
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
	
	
}
