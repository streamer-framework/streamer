package cea.util.metrics;

import java.util.Iterator;
import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class AccuracyMetric extends Metric{	
	
	/**
	 * Computes the accuracy of the time records outputs regarding their desired output.
	 * @param records
	 * @return Vector<Double> containing accuracy value (in %) in first position
	 */
	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		double result=0;
		Iterator<TimeRecord> it = records.iterator();
		int cont=0;
		while (it.hasNext()) {
			TimeRecord record = it.next();
			if(record.getTarget() == null || record.getOutput() == null)
				continue;
			
			if(GlobalUtils.isNumeric(record.getTarget()) && GlobalUtils.isNumeric(record.getOutput())) {
				result += Math.abs(Double.parseDouble(record.getTarget()) - Double.parseDouble(record.getOutput()) / Double.parseDouble(record.getTarget()));
				cont++;	
			}
		}		
		result = 100-result/cont;
		Vector<Double> ret = new Vector<Double>();
		ret.add(result);
		
		System.out.println("Computing Accuracy Metric for "+id +"=" + result);
		
		return ret;
	}

	
//	#accuracy=1-abs(mean(deviation[,dependantVariable]))
	//		deviation=((actual-predicted)/actual)
	//% error = (accepted - experimental) / accepted *100%

}
