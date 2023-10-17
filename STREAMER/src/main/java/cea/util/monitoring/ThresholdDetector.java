package cea.util.monitoring;

import cea.util.GlobalUtils;
import cea.util.metrics.Metric;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Detector based in thresholds. Per observed metric, when the value goes above a specified
 * threshold (absolute value) for more than robustness iterations, an alarm is raised. We only
 * consider metrics with one evaluation output (no multi-output evaluation metrics allowed).
 */
public class ThresholdDetector extends MonitoringDetector{

	/**
	 * Map of the 'ALERT_GREATER' boolean values by metrics.
	 * ALERT_GREATER --> Alert is raised if value is greater [true] or lower [false] than threshold.
	 */
	Map<String, Boolean> ALERT_GREATER_MAP;

	/**
	 * Map of the 'ABS_THRESHOLD' boolean values by metrics.
	 * ABS_THRESHOLD --> Threshold is considered in its absolute value [true], otherwise [false].
	 */
	Map<String, Boolean> ABS_THRESHOLD_MAP;

	/**
	 * Map of the numbers of NaN values iteration in a row by metrics.
	 */
	Map<String, Integer> nan_iteration_counters;

	/**
	 * Constructor of the threshold object.
	 */
	public ThresholdDetector() {
		super();

		readCommonProperties("threshold");

		ALERT_GREATER_MAP = new LinkedHashMap<>();
		ABS_THRESHOLD_MAP = new LinkedHashMap<>();
		nan_iteration_counters = new LinkedHashMap<>();

		Properties properties = new Properties();
		try (InputStream props = new FileInputStream(GlobalUtils.resourcesPathPropsFiles + "/monitoring.props")) {
			properties.load(props);
			String[] threshold = (properties.getProperty("threshold.threshold")).replace(" ","").split(",");
			String[] ALERT_GREATER_list = properties.getProperty("threshold.threshold.alert-if-greater").replace(
					" ","").split(",");
			String[] ABS_THRESHOLD_list = properties.getProperty("threshold.threshold.abs").replace(
					" ","").split(",");

			int i=0;
			for(String name: metricNames) {

				if(threshold.length == 1) {//if there is only 1 value, we assign it to all metric detectors
					metricsThreshold.put(name, Double.valueOf(threshold[0]));
				}else {
					metricsThreshold.put(name, Double.valueOf(threshold[i]));
				}

				if(ALERT_GREATER_list.length == 1) {//if there is only 1 value, we assign it to all metric detectors
					ALERT_GREATER_MAP.put(name, Boolean.parseBoolean(ALERT_GREATER_list[0]));
				}else {
					ALERT_GREATER_MAP.put(name, Boolean.parseBoolean(ALERT_GREATER_list[i]));
				}

				if(ABS_THRESHOLD_list.length == 1) {//if there is only 1 value, we assign it to all metric detectors
					ABS_THRESHOLD_MAP.put(name, Boolean.parseBoolean(ABS_THRESHOLD_list[0]));
				}else {
					ABS_THRESHOLD_MAP.put(name, Boolean.parseBoolean(ABS_THRESHOLD_list[i]));
				}

				nan_iteration_counters.put(name, 0);

				i++;
			}

		} catch (IOException e) {
			System.err.println("Parameters for detector not correctly specified in monitoring.props");
			e.printStackTrace();
		}

	}

	/**
	 * Threshold detector for metrics deviation. Note that we only consider metrics
	 * with only one evaluation output (no multi-output evaluation metrics allowed).
	 * @return Alert (true) or not alert (false).
	 */
	@Override
	public boolean detec(Map<Metric, Vector<Double>> newValues, String id, long iteration) {
		for(String metricName: metricsThreshold.keySet()) {
			for(Metric metric: newValues.keySet()) {
				if((metric.getClass().getName()).equals(GlobalUtils.packageMetrics+"."+metricName)){ //we launch one detector per metric
					detecByMetric(newValues.get(metric), id, metricName, iteration);
				}
			}
		}
		printDetections(id, iteration);
		return raiseAlert(id, iteration);
	}

	/**
	 * Detects the deviation with a threshold.
	 * @param metricValues Values of the metric.
	 * @param id Identifier of the problem.
	 * @param metricName Name of the metric.
	 * @param iteration Iteration during which this function is called.
	 */

	private void detecByMetric(Vector<Double> metricValues, String id, String metricName, long iteration) {

		double threshold = metricsThreshold.get(metricName);
		boolean ABS_THRESHOLD = ABS_THRESHOLD_MAP.get(metricName);
		boolean ALERT_GREATER = ALERT_GREATER_MAP.get(metricName);
		boolean valueIsNan = false;
		boolean detected = false;
		double aux = -1;

		for(Double value:metricValues) { // Take only the last value
			if(value.isNaN()){
				valueIsNan = true;
			} else {
				if(ABS_THRESHOLD){
					value = Math.abs(value);
				}
				if ((value > threshold && ALERT_GREATER) || (value < threshold && !ALERT_GREATER)) {
					detected = true;
					aux = value;
				}
			}
		}

		// The NaN iteration counter is need to correctly stop the detection (and the alert)
		int nan_iteration_counter = nan_iteration_counters.get(metricName);

		if (valueIsNan) {
			System.out.println("[" + id + "] (" + iteration + ") " + metricName + " - NaN detection --> ignored by detector");
			nan_iteration_counter++;
		} else {

			List<Detection<Long>> detections = getDetections(metricName);
			Detection<Long> lastDetection = null;
			if(!detections.isEmpty()){
				lastDetection = detections.get(detections.size() - 1);
			}

			if (detections.isEmpty() || lastDetection.hasStop()) { // use robustness to activate the detection
				int robustness = robustnessCounter.get(metricName);
				if (detected) {
					robustness--;
					if (robustness == 0) {
						detections.add(new Detection<>(metricName, iteration, aux));
						System.out.println("[" + id + "] (" + iteration + ") " + metricName + " - ADD detection " + iteration + " " + aux);
						robustness = robustness_default.get(metricName);
						nan_iteration_counter = 0;
					}
				} else {
					robustness = robustness_default.get(metricName);
				}
				robustnessCounter.put(metricName, robustness);
			} else { // use the nan_iteration_counter to correctly stop the detection
				if (detected) {
					lastDetection.touch(iteration);
					System.out.println("[" + id + "] (" + iteration + ") " + metricName + " - TOUCH detection " + iteration + " " + aux);
					nan_iteration_counter=0;
				} else if ((iteration - nan_iteration_counter - lastDetection.getTouched()) == robustness_default.get(metricName)) {
					lastDetection.setStop(iteration);
					System.out.println("[" + id + "] (" + iteration + ") " + metricName + " - STOP detection " + iteration + " " + aux);
				}
			}

		}

		nan_iteration_counters.put(metricName, nan_iteration_counter);

	}

}
