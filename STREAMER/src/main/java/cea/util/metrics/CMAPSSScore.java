package cea.util.metrics;

import java.util.Collections;
import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

/**
 * Score metric, specific to the CMAPSS use case.
 */
public class CMAPSSScore extends RegressionMetric {

	/**
	 * This method is used to unify the names.
	 * @return Unified name.
	 */
	@Override
	public String getName() {
		return "CMAPSS_Score";
	}
	
	/**
	 * This method is for evaluating the score metric applied to the CMAPSS dataset.
	 * It considers target/output with just one value.
	 * @return Vector<Double> which contains the calculation of the Score function.
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result=Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			double d_i;
			double s_i;
			result = 0;
			int number_valid_record = 0;
			for(TimeRecord record: records) {
				if (record.getTarget().isEmpty() || record.getOutput().isEmpty() ||
						record.getOutput().equals(Collections.singletonList("nan")))
					continue;
				d_i = Double.parseDouble(record.getOutput().get(0))
						- Double.parseDouble(record.getTarget().get(0)); // predicted-target
				if(d_i < 0) {
					s_i = Math.exp((-1)*(d_i/13)) - 1;
				}else {
					s_i = Math.exp(d_i/10) - 1;
				}
				result+=s_i;
				number_valid_record++;
			}
			if (number_valid_record != 0) {
				result = GlobalUtils.roundAvoid(result, 3);
			} else {
				result=Double.NaN;
			}
		}
		Vector<Double> ret = new Vector<>();
		ret.add(result);
		return ret;
	}

}
