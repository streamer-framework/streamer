package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

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
	 * It evaluates the Accuracy metric for the Multi-Class Classification problems.
	 * It computes the accuracy of the time records outputs regarding their target (expected output). 
	 * It considers target/output with just one value.
	 *  
	 * @return Vector<Double> which contains the calculation of the Accuracy
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result=Double.NaN;
		Vector<Double> ret = new Vector<Double>();
		if(GlobalUtils.containsOutputs(records)) {
			ret = super.evaluate(records, id);
			if(ret != null ) {			
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
					double accuracy_c = GlobalUtils.safeDivison(((double) (this.getTrueNegative(c) + this.getTruePositive(c))),
							((double) (this.getTrueNegative(c) + this.getTruePositive(c) + this.getFalseNegative(c)
									+ this.getFalsePositive(c))));
					if(((Double)accuracy_c).isNaN()) {//there might not be any example of this class
						accuracy_c = 0;						
					}
					total_accuracy += accuracy_c;
					double class_summation = (double) this.getTrueNegative(c) + this.getTruePositive(c) + this.getFalseNegative(c)
					+ this.getFalsePositive(c);
					if(class_summation > 0) {
						valid_classes ++;
					}
				}
				total_accuracy = GlobalUtils.safeDivison(total_accuracy, valid_classes);				
//				total_accuracy = safeDivison(total_tn + total_tp, total_tn + total_tp + total_fn + total_fp);		
				result = GlobalUtils.roundAvoid(total_accuracy, 3);
			}else {
				 ret = new Vector<Double>();
			}
		}
		
		ret.add(result);
		
		return ret;
	}

}
