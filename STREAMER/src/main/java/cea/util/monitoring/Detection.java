package cea.util.monitoring;

public class Detection<Long> {
	
	private String metricName;
    private Long start;
    private Long stop;
    private Double magnitude;
    private Long lastTouched;

    public Detection(String metricName, Long start, Double magnitude){
    	this.metricName = metricName;
        this.start = start;
        this.lastTouched = start;
        this.magnitude = magnitude;
        this.stop = start;
    }

    public Long getStart() {
        return start;
    }

    public Long getTouched(){
        return lastTouched;
    }

    public void touch(Long time){
        this.lastTouched = time;
    }

    public Long getStop(){
        return stop;
    }

    public boolean hasStop(){
        return start != stop;
    }

    public void setStop(Long stop){
        this.stop = stop;
    }

    public Double getMagnitude() {
        return magnitude;
    }
    
    public String getMetricName() {
    	return metricName;
    }
	
}
