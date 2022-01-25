package cea.util.metrics;

import java.util.ArrayList;
import java.util.Vector;

import cea.streamer.core.TimeRecord;

public abstract class RegressionMetric extends Metric {

	public double calculateMean(ArrayList<Double> data) {
		// The mean average
		double mean = 0.0;
		for (int i = 0; i < data.size(); i++) {
		        mean += data.get(i);
		}
		mean /= data.size();

		// The variance
		/*double variance = 0;
		for (int i = 0; i < data.size(); i++) {
		    variance += Math.pow(data.get(i) - mean, 2);
		}
		variance /= data.size();
		*/
		return mean;
	}
	
	@Override
	public abstract Vector<Double> evaluate(Vector<TimeRecord> records, String id);

}
