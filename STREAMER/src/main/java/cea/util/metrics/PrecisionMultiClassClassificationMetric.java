package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class PrecisionMultiClassClassificationMetric extends MultiClassClassificationMetric {
	
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
	 * This method is for evaluating the Precision metric for the Multi-Class Classification problems
	 * 
	 * @return Vector<Double> which contains the calculation of the Precision
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		// summation of TP/ Summation of TP + Summation of False Positive
		Vector<Double> ret = super.evaluate(records, id);
		if(ret == null )
			return null;
		double result = -1;
		double total_precision = 0;
		double total_tp = 0;
		double total_fp = 0;
		for (String c : this.getClasses()) {
			total_tp += this.getTruePositive(c);
			total_fp += this.getFalsePositive(c);
			//trying to calculate precision for each class and then mean -- bad results
			//double precision_c = safeDivison(((double) (this.getTruePositive(c))),((double) (this.getTruePositive(c) + this.getFalsePositive(c))));
			//total_precision += precision_c;
		}
		total_precision = safeDivison(total_tp, total_tp+total_fp);
		//total_precision = total_precision / this.getClasses().size();
		result = this.roundAvoid(total_precision, 3);
		
		ret.add(result);
		System.out.println("["+id+"] Precision metric True Positives " + total_tp);
		System.out.println("["+id+"] Precision metric False Positives " + total_fp);
		return ret;
	}
}
