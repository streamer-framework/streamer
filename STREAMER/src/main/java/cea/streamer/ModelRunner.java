package cea.streamer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.io.Resources;

import cea.streamer.algs.MLalgorithms;
import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.Log;
import cea.util.connectors.InfluxDBConnector;
import cea.util.metrics.Metric;
import cea.util.prepostprocessors.PrePostProcessor;

/**
 * Performs the inference and evaluate the results using the specified metrics
 * Setup in streaming.props
 * Data obtained from database or from a file (path specified in streaming.props)
 */
public class ModelRunner {

	/**
	 * Run a model. Algorithm to use, source of data and other parameters are specified in the properties
	 * 
	 * @param sourceType From where data must be retrieved. Option: [influx , file]
	 * @param id Problem id. Option: [influx data base DB name, folder where the properties files are located] 
	 * @param modelStoredPath where the model is stored
	 * @return Vector<TimeRecord> records returned after running the model and the pre-post processing
	 * @throws Exception throwing exception when the properties files reading fails
	 */
	public Vector<TimeRecord> run(String sourceType, String id, String modelStoredPath) throws Exception {
		Vector<TimeRecord> records; 		
		
		String origin = id;
		if (origin.equals("default")) {
			origin = ".";
		}
					
		String problemType=null;
		String className=null;
		Long trainingMaxData=null;
		Path path = null;
		Properties properties = new Properties();
		PrePostProcessor prePostProcessor = null;
		Vector<Metric> metrics = null;
		boolean containHeaders = false;
		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+origin+"/algs.props").openStream() ){		 
			    properties.load(props);
		    	className = (GlobalUtils.packageAlgs+".")+properties.getProperty("algorithm").replace(" ","");		    	
		    	trainingMaxData = Long.parseLong(properties.getProperty("training.maxdata"));	
				if (properties.containsKey("evaluation.metrics")) {
					String[] aux = (properties.getProperty("evaluation.metrics")).replace(" ","").split(",");
					metrics = new Vector<Metric>();				
					for(String metricName: aux) {
						try {
							Class<?> recC = Class.forName((GlobalUtils.packageMetrics+".") + metricName);				
							metrics.add( (Metric) recC.getDeclaredConstructor().newInstance() );
						} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
								| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e2) {
							e2.printStackTrace();
							System.err.println("["+id+"] The metric "+metricName+" class doen not exist");
						}
					}
				}
	 	} catch (IOException e) {
			e.printStackTrace();
		}		
		Class recC;
        try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+origin+"/streaming.props").openStream()) {
            properties = new Properties();
            properties.load(props);            
	    	problemType = (GlobalUtils.packageTimeRecords+".")+properties.getProperty("problem.type").replace(" ","");	    			
    		try {
	    		if(properties.containsKey("consumer.prepostprocessor")) {	    				
					recC = Class.forName((GlobalUtils.packagePrePostProcessors+".")+properties.getProperty("consumer.prepostprocessor").replace(" ",""));
					prePostProcessor = (PrePostProcessor)recC.getDeclaredConstructor().newInstance();
	    		}
	    		if (properties.containsKey("containsHeader")) { 
			    	containHeaders = Boolean.parseBoolean(properties.getProperty("containsHeader").replace(" ","").toLowerCase());
			    }
			} catch (InstantiationException | IllegalArgumentException |  InvocationTargetException | SecurityException | IllegalAccessException | NoSuchMethodException |ClassNotFoundException e3) {
				System.err.println("["+id+"] The postprocessor class doen not exist");
			}	
			path = Paths.get( properties.getProperty("datafile") );
			Log.displayLogTrain.info("["+id+"] "+path);
        }catch (IOException e) {
			e.printStackTrace();
		}
		
		Class<?> algC = Class.forName(className);
		MLalgorithms alg = (MLalgorithms)algC.getDeclaredConstructor().newInstance();	
		
		if(sourceType.replace(" ","").toLowerCase().equals("influx")) {//take the data from influxDB
			InfluxDBConnector.init();
			//String name = path.toString().substring(0, path.toString().length()-4).replace('\\', '/').toLowerCase();
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader("json/data"+id+".json"));
			JSONObject jsonObject = (JSONObject) obj;
			String name = (String) jsonObject.get("fieldInflux");
			System.err.println("["+id+"] "+name);
			//records = InfluxDBConnector.getRecordsDB(name, trainingMaxData, alg.isUpdateModel());
			records = InfluxDBConnector.getRecordsDB(id, trainingMaxData, alg.isUpdateModel());
			System.err.println(records);
		}else{//read it from file
			
			records = new Vector<TimeRecord> ();
			
	        recC = Class.forName(problemType+"Record");	//we create the time record
			TimeRecord recObj;
	        
	        String line=null;
			BufferedReader br=Files.newBufferedReader(path);
			if(containHeaders) {
				br.readLine();	// we ignore the header, it is not values
			}			
			line = br.readLine();
			while(line!=null){	        			
		        line = line.replaceAll("\"", "").replaceAll("\t", "");
		    	
		        recObj = (TimeRecord)recC.getDeclaredConstructor().newInstance();
				recObj.fill(id,line);				
				records.add( recObj );
				
				line = br.readLine();
			}
			br.close();				
		}
		
		//We retreives the model from disk if requested
		if(modelStoredPath != null) {
			GlobalUtils.modelFromDiskToRedis(id, modelStoredPath); //retreives the model from file and stores it in redis
		}
				
		
		//Calls the model
		System.out.println("["+id+"] Inference: algorithm "+algC);
		records = runModel(id, records, prePostProcessor, alg, metrics);
		
		return records;
		
	}

	/**
	 * Runs the model
	 * @param id
	 * @param recordsDB
	 * @param preprocessor
	 * @param postprocessor
	 * @param algC
	 * @param algorithm
	 * @return
	 */
	public Vector<TimeRecord> runModel(String id, Vector<TimeRecord> recordsDB, PrePostProcessor prePostProcessor, MLalgorithms algorithm, Vector<Metric> metrics) {

		/************** PRE PROCESSING ***********************/
		if(prePostProcessor != null ) {
			recordsDB = prePostProcessor.preprocess(recordsDB,id);				
		}
				
		/************** INFERENCE ***********************/
		algorithm.run(recordsDB,id);
		
		for(TimeRecord record: recordsDB) {
			System.err.println(record.toString() );
		}

		/************** POST PROCESSING ***********************/
		if(prePostProcessor!= null ) {
			recordsDB = prePostProcessor.postprocess(recordsDB,id);				
		}
		
		/********************* METRICS ***********************/
		Map<Metric, Vector<Double>> metricsEvaluation = new LinkedHashMap<Metric, Vector<Double>>();
		if (metrics != null) {
			for (Metric metric : metrics) {
				Vector<Double> values = metric.evaluate(recordsDB, id);
				metricsEvaluation.put(metric, values);
				String metricLog = (id+": "+metric.getName()+" =");
				for(double d:values) {
					metricLog+=(" "+d);
				}				
				Log.metricsLog.info(metricLog);
			}
		}
		
		return recordsDB;
	}
	

}
