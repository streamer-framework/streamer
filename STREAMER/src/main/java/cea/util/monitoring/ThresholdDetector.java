package cea.util.monitoring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.google.common.io.Resources;

import cea.util.GlobalUtils;
import cea.util.metrics.Metric;

/**
 * Detector based in thresholds
 * Per observed metric:
 * 		When the value goes above a specified threshold (absolute value) for more than robustness iterations, an alarm is raised
 * https://en.wikipedia.org/wiki/CUSUM
 * We only consider metrics with one evaluation output (no multi-output evaluation metrics allowed)
 * @author sg244422
 *
 */
public class ThresholdDetector extends MonitoringDetector{	
	
	/**
	 * Values: true or false. Alert is raised if value is greater [true] or lower [false] than threshold
	 */
	boolean ALERT_GREATER = true;
	/**
	 * Threshold is considered in its absolute value [true], otherwise [false]
	 */
	boolean ABS_THRESHOLD = true;
		
	public ThresholdDetector() {	
		super();
		readCommonProperties("threshold");
		
		Properties properties = new Properties();
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + "/monitoring.props")) {
			properties.load(props);
			String[] threshold = (properties.getProperty("threshold.threshold")).replace(" ","").split(",");
			ALERT_GREATER = Boolean.parseBoolean(properties.getProperty("threshold.threshold.alert-if-greater").replace(" ",""));
			ABS_THRESHOLD = Boolean.parseBoolean(properties.getProperty("threshold.threshold.abs").replace(" ",""));
			int i=0;			
			for(String name: metricNames) {
				if(threshold.length == 1) {//if there is only 1 value, we assign it to all metric detectors
					metricsThreshold.put(name, Double.valueOf(threshold[0]));	
				}else {
					metricsThreshold.put(name, Double.valueOf(threshold[i]));
				}
				i++;
			}
		} catch (IOException e) {
			System.err.println("Parameters for detector not correctly specified in monitoring.props");
			e.printStackTrace();
		}
	}


	/**
	 * Threshold detector for metrics deviation
	 * 
	 * Note that we only consider metrics with only one evaluation output (no multi-output evaluation metrics allowed) 
	 * @param metricValues Historical evaluation results under the same metric
	 * @return Alert (true) or not alert (false)
	 */
	@Override
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
		//System.err.println("Check detection by Threshold");
		
		double threshold = metricsThreshold.get(metricName); 

		List<Detection<Long>> detections = getDetections(metricName);
		Detection<Long> lastDetection = null;
		if( !detections.isEmpty() ){
			lastDetection = detections.get( detections.size() - 1 );
		}
		
		boolean detected = false;
		double aux=-1;
		for(Double value:metricValues) {//a metric can return several values
			if(ABS_THRESHOLD){
				value = Math.abs(value);
			}
			if( (value > threshold && ALERT_GREATER) ||
					(value < threshold && !ALERT_GREATER) ){//if any of those values do not respect the threshold
				detected = true;
				aux = value;
			}
		}
		
		int robustness = robustnessCounter.get(metricName);
		
		if( detected ){//values passed the threshold
			robustness--;
			if( getDetections(metricName).isEmpty() || lastDetection.hasStop() ){//no detections pending to be stopped
				if( robustness <= 0 ){//add detection
					detections.add( new Detection<Long>(metricName, Long.valueOf(iteration), aux ) );
					robustness = robustness_default.get(metricName);
System.out.println("["+id+"] ("+iteration+") ADD detection "+iteration+" "+aux); 					
				}
			}else{//there was a detection
				if( (iteration - lastDetection.getTouched()) > robustness_default.get(metricName) ){
					lastDetection.setStop(Long.valueOf(iteration));
System.out.println("["+id+"] ("+iteration+") STOP detection "+iteration+" "+aux);	
					robustness = robustness_default.get(metricName);
				}else{
					lastDetection.touch(Long.valueOf(iteration));
System.out.println("["+id+"] ("+iteration+") TOUCH detection "+iteration+" "+aux);					
				}
			}
		}else{//values within the threshold limits
			if( !getDetections(metricName).isEmpty() && !lastDetection.hasStop() ){//no detections pending to be stopped		
					if( ( iteration - lastDetection.getTouched() ) > robustness_default.get(metricName) /*robustness*/ ){
						lastDetection.setStop(Long.valueOf(iteration));
System.out.println("["+id+"] ("+iteration+") STOP detection "+iteration+" "+aux);
				}
			}
			robustness = robustness_default.get(metricName);
		}		
		robustnessCounter.put(metricName,robustness);//update robustness counter
		
	}//detect

}
