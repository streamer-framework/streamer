package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class AccuracyMultiClassClassificationMetric extends MultiClassClassificationMetric {
	
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
	 * This method is for evaluating the Accuracy metric for the Multi-Class Classification problems
	 * 
	 * @return Vector<Double> which contains the calculation of the Accuracy
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		Vector<Double> ret = super.evaluate(records, id);
		if(ret == null )
			return null;
		double result = -1;
		double total_accuracy = 0;
		int valid_classes = 0;
		/*double total_tp = 0;
		double total_fp = 0;
		double total_tn = 0;
		double total_fn = 0;*/
		for (String c : this.getClasses()) {
			/*total_tp += this.getTruePositive(c);
			total_fp += this.getFalsePositive(c);
			total_tn += this.getTrueNegative(c);
			total_fn += this.getFalseNegative(c);*/
			double accuracy_c = safeDivison(((double) (this.getTrueNegative(c) + this.getTruePositive(c))),
					((double) (this.getTrueNegative(c) + this.getTruePositive(c) + this.getFalseNegative(c)
							+ this.getFalsePositive(c))));
			total_accuracy += accuracy_c;
			double class_summation = (double) this.getTrueNegative(c) + this.getTruePositive(c) + this.getFalseNegative(c)
			+ this.getFalsePositive(c);
			if(class_summation > 0) {
				valid_classes ++;
			}
			
		}

		total_accuracy = safeDivison(total_accuracy, valid_classes);
		
//		total_accuracy = safeDivison(total_tn + total_tp, total_tn + total_tp + total_fn + total_fp);		
		result = this.roundAvoid(total_accuracy, 3);
		
		//Vector<Double> ret = new Vector<Double>();
		ret.add(result);

		System.out.println("Computing Accuracy Metric for " + id + "=" + result);
		
		return ret;
	}
}
