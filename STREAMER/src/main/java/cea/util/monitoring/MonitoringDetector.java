package cea.util.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.google.common.io.Resources;

import cea.util.GlobalUtils;
import cea.util.Log;
import cea.util.metrics.Metric;

public abstract class MonitoringDetector {
	
	protected Map<String,List<Detection<Long>>> detections;
	
	protected Map<String, Double> metricsThreshold;
	
	protected Map<String,Integer> robustness_default;
	
	protected Map<String,Integer> robustnessCounter;
	
	protected String[] metricNames;
	
	public MonitoringDetector() {
		detections = new LinkedHashMap<String,List<Detection<Long>>>();
		metricsThreshold = new LinkedHashMap<String, Double>();
		robustness_default = new LinkedHashMap<String, Integer>();
		robustnessCounter = new LinkedHashMap<String,Integer> ();		
	}
	
	/**
	 * Read common parameters from monitoring.props (threshold and metrics)
	 * 
	 * @param detector Type of detector
	 */
	public void readCommonProperties(String detector) {		
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles + "/monitoring.props").openStream()) {
			properties.load(props);
			metricNames = properties.getProperty("metrics").replace(" ","").split(",");
			String[] robustness = (properties.getProperty("threshold.robustness")).replace(" ","").split(",");
			int i=0;			
			for(String name: metricNames) {
				if(robustness.length == 1) {
					robustness_default.put(name, Integer.parseInt(robustness[0]));	
				}else {
					robustness_default.put(name, Integer.parseInt(robustness[i]));
				}
				detections.put(name, new ArrayList<Detection<Long>>());
				robustnessCounter.put(name, robustness_default.get(name) );
				i++;
			}
		} catch (IOException e) {
			System.err.println("Parameters for detector not correctly specified in monitoring.props");
			e.printStackTrace();
		}
	}

	public String getName() {
		return getClass().getName();
	}
	
	
    public List<Detection<Long>> getDetections(String metricName){
        return detections.get(metricName);
    }
    

    public Map<String,List<Detection<Long>>> getDetections(){
        return detections;
    }
    
    public void addDetection(String metricName,List<Detection<Long>> detection){
        detections.put(metricName, detection);
    } 
  
	public void reset() {
		detections = new LinkedHashMap<String,List<Detection<Long>>>();		
		for(String metricName: metricNames) {
			detections.put(metricName, new ArrayList<Detection<Long>>());
		}
	}
    
    /**
     * Print the detections
     * @param id
     * @param iteration
     */
    public void printDetections(String id, long iteration){
    	boolean print = false;
        StringBuffer sb = new StringBuffer();
        sb.append("["+id+"] Detection set: " + this.getName());
        sb.append(". Metric - Time:[from, to, touched] --> {Magnitude}" );
        for( String metricName: detections.keySet()){
	        for( Detection<Long> detection : detections.get(metricName)){
	            sb.append("\n\t"+detection.getMetricName()+" - [" + detection.getStart() + "," + detection.getStop() + ","+detection.getTouched()+"] --> {" + detection.getMagnitude() + "}" );
	            print = true;
	        }
        }
        String msg = "["+id+"] ("+iteration+") "+ sb.toString();
        if(print) {
        	System.out.println(msg);
        	Log.infoLog.info(msg);
        }
    }
    
    /**
     * Stop all detections
     * @return number of detections stopped
     */
    public int stopAllDetections(long iteration) {
    	int n = 0;
		for(String metricName:detections.keySet()) {
			for(Detection<Long> detection: detections.get(metricName)) {
				if(!detection.hasStop()) {//detection ongoing
					detection.setStop(Long.valueOf(iteration));
					n++;
				}
			}
    	}
    	return n;
    }
	
    /**
     * Raise an alarm if there is at least one ongoing (non stopped) detection
     * @param iteration
	 * @return Alert (true) or not alert (false)
     */
	protected boolean raiseAlert(String id, long iteration) {
	/*	boolean alert = true;
		for(String metricName:detections.keySet()) {
			if(alert) {
				alert=false;
				for(Detection<Long> detection: detections.get(metricName)) {
					if(!detection.hasStop()) {//detection ongoing
						alert=true;
					}
				}
			}
		}*/
		boolean alert = false;
		Iterator<String> itr = detections.keySet().iterator();
		String metricName=null;
		while(itr.hasNext() && !alert) {
			metricName = itr.next();
			
			List<Detection<Long>> detections = getDetections(metricName);
			if( !detections.isEmpty() ){
				if(!(detections.get( detections.size() - 1 )).hasStop()) {//last detection ongoing
					alert=true;
				}
			}
		}
		if(alert) {
			//stopAllDetections(iteration);
			String msg = "["+id+"] ("+iteration+") ALERT!!";
			Log.infoLog.info(msg);
			System.err.println(msg);
		}
		return alert;
	}
    
	/**
	 * CUSUM detector for metrics deviation
	 * Sources:
	 * 		https://en.wikipedia.org/wiki/CUSUM#
	 * 		https://github.com/O5ten/halp/blob/master/src/main/java/com/osten/halp/impl/profiling/detector/Cusum.java
	 * 
	 * @param metricValues Historical evaluation results under the same metric
	 * @return Alert (true) or not alert (false)
	 */
	public abstract boolean detec(Map<Metric, Vector<Double>> newValues, String id, long iteration);

}
