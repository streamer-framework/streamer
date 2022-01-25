package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class AccuracyMetricRegression extends Metric{	
	
	/**
	 * Computes the accuracy of the time records outputs regarding their target (expected output). 
	 * It considers target/output with just one value.
	 * @param records
	 * @return Vector<Double> containing accuracy value (in %) in first position
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result=Double.NaN;
		if(GlobalUtils.containsOutputs(records)) {
			result=0;
			int cont=0;
			for(TimeRecord record: records) {
				if(record.getTarget().isEmpty() || record.getOutput().isEmpty())
					continue;
				
				if(GlobalUtils.isNumeric(record.getTarget().get(0)) && GlobalUtils.isNumeric(record.getOutput().get(0))) {
					result += Math.abs(Double.parseDouble(record.getTarget().get(0)) - Double.parseDouble(record.getOutput().get(0)) / Double.parseDouble(record.getTarget().get(0)));
					cont++;	
				}
			}		
			result = 100-result/cont;
		}
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		
		return ret;
	}

	
//	#accuracy=1-abs(mean(deviation[,dependantVariable]))
	//		deviation=((actual-predicted)/actual)
	//% error = (accepted - experimental) / accepted *100%

}
