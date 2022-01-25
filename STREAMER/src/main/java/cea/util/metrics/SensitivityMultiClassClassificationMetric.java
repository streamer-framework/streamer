package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class SensitivityMultiClassClassificationMetric extends MultiClassClassificationMetric {
	
	/**
	 * This method is used to unify the names of all the Sensitivity metrics
	 * 
	 * @return unified name precised
	 */
	@Override
	public String getName() {
		return "sensitivity/recall";
	}
	
	/**
	 * This method is for evaluating the Sensitivity metric for the Multi-Class Classification problems
	 * 
	 * @return Vector<Double> which contains the calculation of the Sensitivity
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result = Double.NaN;
		Vector<Double> ret = new Vector<Double>();
		if(GlobalUtils.containsOutputs(records)) {
			// summation of TP / summation of TP + summation of FN
			ret = super.evaluate(records, id);
			if(ret != null ) {			
				double total_sensitivity = 0;
				double total_tp = 0;
				double total_fn = 0;
				for (String c : this.getClasses()) {
					total_tp += this.getTruePositive(c);
					total_fn += this.getFalseNegative(c);
					//trying to calculate sensitivity for each class and then mean -- bad results
					//double sensitivity_c = safeDivison(((double) (this.getTruePositive(c))),((double) (this.getTruePositive(c) + this.getFalseNegative(c))));
					//total_sensitivity += sensitivity_c;
				}
				total_sensitivity = GlobalUtils.safeDivison(total_tp, total_tp+total_fn);
				//total_sensitivity = total_sensitivity / this.getClasses().size();
				result = GlobalUtils.roundAvoid(total_sensitivity, 3);
				System.out.println("["+id+"] True Positive " + total_tp);
				System.out.println("["+id+"] False Negative " + total_fn);
			}
		}
		ret.add(result);
		return ret;	
	}
}
