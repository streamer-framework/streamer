package cea.util.prepostprocessors;

import java.util.TreeMap;
import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class PrePostProcessorNormalizer extends PrePostProcessor{

	TreeMap<String,double[]> name_min_max;
	String output;
	
	public PrePostProcessorNormalizer() {
		super();
		//distance;duration;power;speed;used_jauge;driverprofile
		//0.0:100.0;0:50400000.0;-2000.0:7500.0;0.0:120.0;0.0:100.0;1.0:5.0
		name_min_max = new TreeMap<String, double[]>();
		name_min_max.put("distance", new double[] {0,100});
		name_min_max.put("duration", new double[] {0,50400000});
		name_min_max.put("power", new double[] {-2000,7500});
		name_min_max.put("speed", new double[] {0,120});
		name_min_max.put("used_jauge", new double[] {0,100});
		name_min_max.put("driverprofile", new double[] {1,5});	
		output = "used_jauge";
	}

	
	@Override
	public Vector<TimeRecord> preprocess(Vector<TimeRecord> records, String id) {
		for(TimeRecord rec:records) {
			for(String key: rec.getValues().keySet()) {
				if(GlobalUtils.isNumeric(rec.getValue(key))) {
					rec.setValue(key, ""+( (Double.parseDouble(rec.getValue(key)) - name_min_max.get(key)[0]) / (name_min_max.get(key)[1] - name_min_max.get(key)[0])) );
				}
			}			
		}
		return records;
	}
	
	@Override
	public Vector<TimeRecord> postprocess(Vector<TimeRecord> records, String id) {
		for(TimeRecord rec:records) {
			for(String key: rec.getValues().keySet()) {
				if(GlobalUtils.isNumeric(rec.getValue(key))) {
					rec.setValue(key, ""+( (Double.parseDouble(rec.getValue(key)) * (name_min_max.get(key)[1] - name_min_max.get(key)[0])) + name_min_max.get(key)[0]) );
				}				
			}	
			if( rec.getOutput() != null && GlobalUtils.isNumeric(rec.getOutput()) ) {
				rec.setOutput(""+( (Double.parseDouble(rec.getOutput()) * (name_min_max.get(output)[1] - name_min_max.get(output)[0])) + name_min_max.get(output)[0]) );
			}
		}
		return records;
	}
	
}
