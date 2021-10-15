package cea.util.metrics;

import java.util.ArrayList;
import java.util.Vector;

import cea.streamer.core.TimeRecord;

public abstract class RegressionMetric extends Metric {

	
	public double safeDivison(double v1, double v2) {
		double result = v1/v2;
		if(((Double)(result)).isNaN())
			result = 0;
		return result;
	}
	
	public double roundAvoid(double value, int places) {
	    double scale = Math.pow(10, places);
	    return Math.round(value * scale) / scale;
	}
	
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
