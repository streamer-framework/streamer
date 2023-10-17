package cea.util.monitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import cea.util.GlobalUtils;
import cea.util.metrics.Metric;

/**
 * Online detector based in Cumulative Sum (cusum) algorithm
 * Detection based on the current data
 * https://en.wikipedia.org/wiki/CUSUM
 * We only consider metrics with one evaluation output (no multi-output evaluation metrics allowed)
 * @author sg244422
 *
 */
public class CuSumDetectorOnLine extends CuSumDetector{	
	
	/**
	 * Minimum values accumulated to start performing the detection
	 */
	final int INTERVAL_CHECK = 5;
	
	/**
	 * Negative cusum per metric
	 */
	private Map<String, Double> cusumMinMetrics;
	
	/**
	 * Positive cusum per metric
	 */
	private Map<String, Double> cusumMaxMetrics;
	
	/**
	 * Standard deviation per metric
	 */
	private Map<String, Double> stdMetrics;
	
	/**
	 * Average per metric
	 */
	private Map<String, Double> averageMetrics;
	
	/**
	 * Factor to compute the cusum threshold
	 * Usually 4 or 5
	 * Default: 5
	 */
	private int factor = 5; //4 or 5
	
	/**
	 *  Drift: Likelihood function, but this is common usage. 
	 *  CUSUM_(i+1) = max(0, CUSUM_i+value+ drift))
	 *  Default: 0
	 */
	private double drift = 0;
	
	public CuSumDetectorOnLine() {	
		super();
		readCommonProperties("cusumonline");
		stdMetrics = new LinkedHashMap<String, Double>();
		averageMetrics = new LinkedHashMap<String, Double>();
		cusumMaxMetrics = new LinkedHashMap<String, Double>();
		cusumMinMetrics = new LinkedHashMap<String, Double>();
		
		String[] std=null;
		String[] average=null;
		Properties properties = new Properties();
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + "/monitoring.props")) {
			properties.load(props);
			drift = Double.parseDouble(properties.getProperty("cusumonline.drift"));
			if (properties.containsKey("cusumonline.factor")) { //optional
				factor = Integer.parseInt(properties.getProperty("cusumonline.factor"));
			}
			String[] threshold=null;
			if (properties.containsKey("cusum.threshold")) { //optional
				threshold = (properties.getProperty("cusum.threshold")).replace(" ","").split(",");
			}else {
				std = (properties.getProperty("cusumonline.std")).replace(" ","").split(",");
				average = (properties.getProperty("cusumonline.average")).replace(" ","").split(",");
			}
			int i=0;
			for(String name: metricNames) {
				detections.put(name, new ArrayList<Detection<Long>>());
				cusumMaxMetrics.put(name, Double.valueOf(0));
				cusumMinMetrics.put(name, Double.valueOf(0));
				stdMetrics.put(name, Double.parseDouble(std[i]));
				averageMetrics.put(name, Double.parseDouble(average[i]));
				if(threshold != null) {
					if(threshold.length == 1) {//if there is only 1 value, we assign it to all metric detectors
						metricsThreshold.put(name, Double.valueOf(threshold[0]));	
					}else {
						metricsThreshold.put(name, Double.valueOf(threshold[i]));
					}
				}else {//we compute threshold based on the std
					if(std.length == 1) {//if there is only 1 value, we assign it to all metric detectors
						metricsThreshold.put(name, Double.valueOf(std[0])*factor);	
					}else {
						metricsThreshold.put(name, Double.valueOf(std[i])*factor);//4 or 5
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
	 * CUSUM detector for metrics deviation
	 * Sources:
	 * 		https://en.wikipedia.org/wiki/CUSUM#
	 * 		https://github.com/O5ten/halp/blob/master/src/main/java/com/osten/halp/impl/profiling/detector/Cusum.java
	 * Note that we only consider metrics with only one evaluation output (no multi-output evaluation metrics allowed) 
	 * @param metricValues Historical evaluation results under the same metric
	 * @return Alert (true) or not alert (false)
	 */
	public boolean detec(Map<Metric, Vector<Double>> newValues, String id, long iteration) {
		for(String metricName: metricsThreshold.keySet()) {
			for(Metric metric: newValues.keySet()) {
				if((metric.getClass().getName()).equals(GlobalUtils.packageMetrics+"."+metricName)){//we launch one detector per metric
					detecByMetric(newValues.get(metric), id, metricName, iteration);
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
	 * @param metricValues
	 * @param id
	 */
	private void detecByMetric(Vector<Double> metricValues, String id, String metricName, long iteration) {	
		double negativeCusum = cusumMinMetrics.get(metricName);
		double positiveCusum = cusumMaxMetrics.get(metricName);

		int robustness = robustness_default.get(metricName);		        
		double threshold = metricsThreshold.get(metricName); 
		
		double value = metricValues.get(0);//we only consider metrics with only one evaluation output 
		//normalize value, but avoid division by zero. Cause that's bad.
		value = (value - averageMetrics.get(metricName));
		if( stdMetrics.get(metricName) != 0 ){
			value = (value/stdMetrics.get(metricName)); 
		}

		List<Detection<Long>> detections = getDetections(metricName);
		Detection<Long> lastDetection = null;
		if( !detections.isEmpty() ){
			lastDetection = detections.get( detections.size() - 1 );
		}

		//CUSUM algorithm performed on residuals
		positiveCusum = Math.max( 0, positiveCusum + value + drift);
		negativeCusum = Math.max( 0, negativeCusum - value -drift);

		if( (positiveCusum > threshold) || (negativeCusum > threshold) ){//values passed the threshold limits
			robustness--;
			if( getDetections(metricName).isEmpty() || lastDetection.hasStop() ){//no detections pending to be stopped
				if( robustness == 0 ){//add detection since robustness is passed
					detections.add( new Detection<Long>(metricName, Long.valueOf(iteration), Math.max( positiveCusum, negativeCusum ) ) );
System.out.println("["+id+"] ("+iteration+") add detection "+iteration+" +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);
					positiveCusum = 0;
					negativeCusum = 0;
					robustness = robustness_default.get(metricName);
				}
			}else{//there was a detection
				if( (iteration - lastDetection.getTouched()) > robustness_default.get(metricName) ){
					lastDetection.setStop(Long.valueOf(iteration)); //stops the detection
System.out.println("["+id+"] ("+iteration+") STOP "+iteration+" +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);
					robustness = robustness_default.get(metricName);	
				}else{
						lastDetection.touch(Long.valueOf(iteration));
System.out.println("["+id+"] ("+iteration+") TOUCH "+iteration+" +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);					
				}
			}
		}else{//values within the threshold limits
			if( !getDetections(metricName).isEmpty() && !lastDetection.hasStop() ){//no detections pending to be stopped
				if( (iteration - lastDetection.getTouched() ) > robustness_default.get(metricName) ){
					lastDetection.setStop(Long.valueOf(iteration)); //stops the detection
System.out.println("["+id+"] ("+iteration+") STOP "+iteration+" +Cusum "+positiveCusum+ " -Cusum "+negativeCusum+ " valor "+value);
				}
			}
			robustness = robustness_default.get(metricName);
		}
		
	}//detect
	
}
