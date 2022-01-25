package cea.util.monitoring;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.metrics.Metric;

/**
 * Class that indicates if there is an alert
 * @author sg244422
 *
 */
public class Alert extends Metric{

	private boolean alert;
	
	public Alert() {
		alert = false;
	}
	
	public boolean isAlert() {
		return alert;
	}

	public void setAlert(boolean alert) {
		this.alert = alert;
	}

	public Alert(boolean alert) {
		this.alert = alert;
	}
	
	public String getName() {
		return getClass().getName();
	}
	
	/**
	 * Returns the alert in the same format as the metrics
	 * (used for ES connector)
	 * @param null
	 * @param id
	 * @return 0 (no alert) or 1 (alert) in a format of Vector<Double>
	 */
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id){
		Vector<Double> ret = new Vector<Double>();
		if(alert) {
			ret.add(Double.valueOf(1));	
		}else {
			ret.add(Double.valueOf(0));
		}		
		return ret;
	}
		
}
