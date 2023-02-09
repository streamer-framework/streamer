package cea.util.monitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.DoubleStream;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import com.google.common.io.Resources;

import cea.util.GlobalUtils;
import cea.util.metrics.Metric;

/**
 * Detector based in Cumulative Sum (cusum) algorithm
 * Detection based on the whole accumulated data
 * https://en.wikipedia.org/wiki/CUSUM
 * We only consider metrics with one evaluation output (no multi-output evaluation metrics allowed)
 * @author sg244422
 *
 */
public class CuSumDetector extends MonitoringDetector{	
	
	/**
	 * Minimum values accumulated to start performing the detection
	 */
	final int INTERVAL_CHECK = 2;
	
	/**
	 * Stores last time in which the cusum method was called
	 * (used in with INTERVAL_CHECK) 
	 */
	private Map<String, Integer> metricsCheckPoint;
	
	/**
	 * Accumulated metric values until the present
	 */
	private Map<Metric, Vector<Vector<Double>>> historicalMetricsValues;
	
	/**
	 * Factor to compute the cusum threshold
	 * Usually 4 or 5
	 * Default: 5
	 */
	private int factor = 5;
	
	/**
	 *  Drift: Likelihood function, but this is common usage. 
	 *  CUSUM_(i+1) = max(0, CUSUM_i+value+ drift))
	 *  Default: 0
	 */
	private double drift = 0;
	
	public CuSumDetector() {	
		super();
		readCommonProperties("cusum");
		historicalMetricsValues = new LinkedHashMap<Metric,Vector<Vector<Double>>>();
		metricsCheckPoint = new LinkedHashMap<String, Integer>();
		
		Properties properties = new Properties();
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + "/monitoring.props")) {
			properties.load(props);
			drift = Double.parseDouble(properties.getProperty("cusum.drift"));
			if (properties.containsKey("cusum.factor")) { //optional
				factor = Integer.parseInt(properties.getProperty("cusum.factor"));
			}
			String[] threshold=null;
			if (properties.containsKey("cusum.threshold")) { //optional
				threshold = (properties.getProperty("cusum.threshold")).replace(" ","").split(",");
			}
			
			int i=0;
			for(String name: metricNames) {
				metricsCheckPoint.put(name, Integer.valueOf(0));
				if(threshold != null) {
					if(threshold.length == 1) {//if there is only 1 value, we assign it to all metric detectors
						metricsThreshold.put(name, Double.valueOf(threshold[0]));	
					}else {
						metricsThreshold.put(name, Double.valueOf(threshold[i]));
					}
				}
				i++;
			}
		} catch (IOException e) {
			System.err.println("Parameters for detector not correctly specified in monitoring.props");
			e.printStackTrace();
		}
	}  
	
	/**
	 * Reset detections and cusum variables
	 */
	public void reset() {
		super.reset();
		for(String metricName: metricsCheckPoint.keySet()) {
			metricsCheckPoint.put(metricName, Integer.valueOf(0));
		}
	}


	/**
	 * CUSUM detector for metrics deviation
	 * Sources:
	 * 		https://en.wikipedia.org/wiki/CUSUM#
	 * 		https://github.com/O5ten/halp/blob/master/src/main/java/com/osten/halp/impl/profiling/detector/Cusum.java
	 * Note that we only consider metrics with only one evaluation output (no multi-output evaluation metrics allowed) 
	 * @param metricValues Historical evaluation results under the same metric
	 * @param id
	 * @param iteration
	 * @return Alert (true) or not alert (false)
	 */
	@Override
	public boolean detec(Map<Metric, Vector<Double>> newValues, String id, long iteration) {
		reset();//we reset the detections list
		acum(newValues); //we add the new values to the history
		int size;
		for(String metricName: metricsCheckPoint.keySet()) {
			for(Metric metric: newValues.keySet()) {
				if(historicalMetricsValues.containsKey(metric)) {//metric historical does exist
					size = historicalMetricsValues.get(metric).size();
					if((metric.getClass().getName()).equals(GlobalUtils.packageMetrics+"."+metricName) && ((size-metricsCheckPoint.get(metricName)) >= INTERVAL_CHECK)) {//we launch one detector per metric
						metricsCheckPoint.put(metricName, size);
						//poner un check para cada metrica
						detecByMetric(historicalMetricsValues.get(metric), id, metricName);
					}
				}
			}			
		}
		printDetections(id, iteration);
		return raiseAlert(id, iteration);
	}
	
	/**
	 * CUSUM detector for metrics deviation
	 * Sources:
	 * 		https://en.wikipedia.org/wiki/CUSUM#
	 * 		https://github.com/O5ten/halp/blob/master/src/main/java/com/osten/halp/impl/profiling/detector/Cusum.java
	 * @param Historical evaluation results under the same metric
	 * @param id
	 * @param metric to be observed
	 */
	private void detecByMetric(Vector<Vector<Double>> metricValues, String id, String metricName) {	
		double negativeCusum = 0;
		double positiveCusum = 0;

		int robustness = robustness_default.get(metricName);
		double[] values = new double[metricValues.size()];
		for(int i=0; i< metricValues.size(); i++) {
			values[i] = metricValues.get(i).get(0);//we only consider metrics with only one evaluation output 
		}
		
        DoubleSummaryStatistics stats = DoubleStream.of(values).summaryStatistics();
        double average = stats.getAverage();
		double std = new StandardDeviation().evaluate(values);
		
	System.err.println(metricName+" Av "+average);
	System.err.println(metricName+" std "+std);
		
		double threshold; 
		if(metricsThreshold.containsKey(metricName)) { //a threshold for this metric was defined in the props file
			threshold = metricsThreshold.get(metricName); 
		}else{
			threshold = factor*std; 	//H = 5 * values.std()
		}
		
		for( int i = 0; i < values.length; i++ ){
			List<Detection<Long>> detections = getDetections(metricName);
			Detection<Long> lastDetection = null;
			if( !detections.isEmpty() ){
				lastDetection = detections.get( detections.size() - 1 );				
			}
			

			double value = values[i];
			//normalize value, but avoid division by zero. Cause that's bad.
			value = (value - average);
			if( std != 0 ){
				value = (value/std); 
			}

			//CUSUM algorithm performed on residuals
			positiveCusum = Math.max( 0, positiveCusum + value + drift);
			negativeCusum = Math.max( 0, negativeCusum - value -drift);

			if( (positiveCusum > threshold) || (negativeCusum > threshold) ){//values passed the threshold limits
				robustness--;

				if( getDetections(metricName).isEmpty() || lastDetection.hasStop() ){//no detections pending to be stopped
					if( robustness == 0 ){//add detection since robustness is passed
						detections.add( new Detection<Long>(metricName, Long.valueOf(i), Math.max( positiveCusum, negativeCusum ) ) );
	System.out.println("["+id+"] ("+i+") ADD detection "+i+" +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);
						positiveCusum = 0;
						negativeCusum = 0;
						robustness = robustness_default.get(metricName);
					}
				}else{//there was a detection
					if( (i - lastDetection.getTouched()) > robustness_default.get(metricName) ){
						lastDetection.setStop(Long.valueOf(i)); //stops the detection
System.out.println("["+id+"] ("+i+") STOP +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);
						robustness = robustness_default.get(metricName);
					}else{
						lastDetection.touch(Long.valueOf(i));
System.out.println("["+id+"] ("+i+") TOUCH +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);					
					}
				}
			}else{//values within the threshold limits
				if( !getDetections(metricName).isEmpty() && !lastDetection.hasStop() ){//no detections pending to be stopped				
					if( ( i - lastDetection.getTouched() ) > robustness ){
						lastDetection.setStop(Long.valueOf(i)); //stops the detection
System.out.println("["+id+"] ("+i+") STOP +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);
					}
				}
				robustness = robustness_default.get(metricName);
			}
		}
/*		if(!getDetections(metricName).isEmpty()){ //stop the alert at the end
			Detection<Long> lastDetection = getDetections(metricName).get( getDetections(metricName).size() - 1 );
			if( !lastDetection.hasStop() ){
				lastDetection.setStop(Long.valueOf(values.length)); //stops the detection since data is finished
		System.out.println("["+id+"] STOP end +Cusum "+positiveCusum+ " -Cusum "+negativeCusum);				
			}
		} */
		
	}//detect

	/**
	 * Store new values in the historical set
	 * @param newValues
	 */
    private void acum(Map<Metric, Vector<Double>> newValues) {
    	Vector<Vector<Double>> aux;
    	for (Metric k : newValues.keySet()) {
    		Vector<Double> values = newValues.get(k);
			if(!values.get(0).isNaN() && !values.get(0).isInfinite()){//if the value is a real number			
	    		if(historicalMetricsValues.get(k)==null) {//if it is fist time we store
	    			aux = new Vector<Vector<Double>>();//we build an empty list 
	    			aux.add(values);//and add first element
	    			historicalMetricsValues.put(k,aux);
	    		}else {
	    			historicalMetricsValues.get(k).add(values);//we add the new value to the history
	    		}
			}
    	}
    }
	
	
}
