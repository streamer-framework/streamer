package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class F1scoreMultiClassClassificationMetric extends MultiClassClassificationMetric {
	
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
	 * This method is for evaluating the F1-score metric for the Multi-Class Classification problems
	 * 
	 * @return Vector<Double> which contains the calculation of the F1-score
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		Vector<Double> ret = super.evaluate(records, id);
		if(ret == null )
			return null;
		double result = -1;
		double total_tp = 0;
		double total_fp = 0;
		double total_fn = 0;
		// calculate the precision and recall
		double total_precision = 0;
		double total_sensitivity = 0;
		for (String c : this.getClasses()) {
			total_tp += this.getTruePositive(c);
			total_fp += this.getFalsePositive(c);
			total_fn += this.getFalseNegative(c);
		}
		total_precision = safeDivison(total_tp, total_tp+total_fp);
		total_sensitivity = safeDivison(total_tp, total_tp+total_fn);
		
		// calculate the F1-score
		result = 2 * (safeDivison((total_precision * total_sensitivity),(total_precision + total_sensitivity)));
		result = this.roundAvoid(result, 3);

		ret.add(result);

		System.out.println("Computing F1-Score Metric for " + id + "=" + result);

		return ret;
	}
}
