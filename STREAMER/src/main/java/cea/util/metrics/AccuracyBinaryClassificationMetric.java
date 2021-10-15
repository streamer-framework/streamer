package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class AccuracyBinaryClassificationMetric extends BinaryClassificationMetric {

	/**
	 * This method is used to unify the names of all the Accuracy metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "accuracy";
	}
	
	/**
	 * It evaluates the Accuracy metric for the Binary Classification problems.
	 * It computes the accuracy of the time records outputs regarding their target (expected output). 
	 * It considers target/output with just one value.
	 * 
	 * @return Vector<Double> which contains the calculation of the Accuracy
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		super.evaluate(records,id);
		double result = -1;
		
		result = safeDivison(((double) (this.getTrueNegative() + this.getTruePositive())),((double) (this.getTrueNegative() + this.getTruePositive() + this.getFalseNegative() + this.getFalsePositive())));
		result = this.roundAvoid(result, 3);
		
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		return ret;
	}
}
