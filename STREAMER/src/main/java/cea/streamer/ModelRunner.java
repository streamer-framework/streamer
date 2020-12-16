package cea.streamer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
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
 * Class that allows to train al algorithm with all the data from the datafile/influxdb
 *
 */
public class ModelRunner {

	/**
	 * Run a model. Algorithm to use, source of data and other parameters are specified in the properties
	 * 
	 * @param sourceType From where data must be retreived. Option: [influx , file]
	 * @param origin Problem id. Option: [influx data base DB name, folder where the properties files are located] 
	 * @param modelStoredPath
	 * @return
	 * @throws Exception
	 */
	public Vector<TimeRecord> run(String sourceType, String origin, String modelStoredPath) throws Exception {
		Vector<TimeRecord> records; 		
		
		String pathProperties = "";
		
		if(!origin.equals("default")) {
			pathProperties = origin+"/";	
		}
					
		String problemType=null;
		String className=null;
		Long trainingMaxData=null;
		Path path = null;
		Properties properties = new Properties();
		PrePostProcessor prePostProcessor = null;
		Vector<Metric> metrics = null;
		try (InputStream props = Resources.getResource("setup/"+pathProperties+"algs.props").openStream() ){		 
			    properties.load(props);
		    	className = (GlobalUtils.packageAlgs+".")+properties.getProperty("algorithm").trim();		    	
		    	trainingMaxData = Long.parseLong(properties.getProperty("training.maxdata"));	
				if (properties.containsKey("evaluation.metrics")) {					
					String[] aux = (properties.getProperty("evaluation.metrics").trim()).split(",");
					metrics = new Vector<Metric>();				
					for(String metricName: aux) {
						try {
							Class recC = Class.forName((GlobalUtils.packageMetrics+".") + metricName);				
							metrics.add( (Metric) recC.getDeclaredConstructor().newInstance() );
						} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
								| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e3) {
							System.err.println("The metric "+metricName+" class doen not exist");
						}
					}
				}
	 	} catch (IOException e) {
			e.printStackTrace();
		}		
		Class recC;
        try (InputStream props = Resources.getResource("setup/"+pathProperties+"streaming.props").openStream()) {
            properties = new Properties();
            properties.load(props);            
	    	problemType = (GlobalUtils.packageTimeRecords+".")+properties.getProperty("problem.type").trim();	    			
    		try {
	    		if(properties.containsKey("consumer.prepostprocessor")) {	    				
					recC = Class.forName((GlobalUtils.packagePrePostProcessors+".")+properties.getProperty("consumer.preprocessor"));
					prePostProcessor = (PrePostProcessor)recC.getDeclaredConstructor().newInstance();
	    		}
			} catch (InstantiationException | IllegalArgumentException |  InvocationTargetException | SecurityException | IllegalAccessException | NoSuchMethodException |ClassNotFoundException e3) {
				System.err.println("The postprocessor class doen not exist");
			}	
			path = Paths.get( properties.getProperty("datafile") );
			System.out.println(path);
			Log.displayLogTrain.info(path);
        }catch (IOException e) {
			e.printStackTrace();
		}
		
		Class algC = Class.forName(className);
		MLalgorithms alg = (MLalgorithms)algC.getDeclaredConstructor().newInstance();	
		
		if(sourceType.trim().toLowerCase().equals("influx")) {//take the data from influxDB
			InfluxDBConnector.init();
			//String name = path.toString().substring(0, path.toString().length()-4).replace('\\', '/').toLowerCase();
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader("json/data"+origin+".json"));
			JSONObject jsonObject = (JSONObject) obj;
			String name = (String) jsonObject.get("fieldInflux");
			System.err.println(name);
			records = InfluxDBConnector.getRecordsDB(name, trainingMaxData, alg.updateModel);
			System.err.println(records);
		}else{//read it from file
			
			records = new Vector<TimeRecord> ();
			
	        recC = Class.forName(problemType+"Record");	//we create the time record
			TimeRecord recObj;
	        
	        String line=null;
			BufferedReader br=Files.newBufferedReader(path);
			line = br.readLine();
			while(line!=null){	        			
		        line = line.replaceAll("\"", "").replaceAll("\t", "");
		    	
		        recObj = (TimeRecord)recC.getDeclaredConstructor().newInstance();
				recObj.fill(origin+";"+line);				
				records.add( recObj );
				
				line = br.readLine();
			}
			br.close();				
		}
		
		//We retreives the model in disk if requested
		if(modelStoredPath != null) {
			GlobalUtils.restoreModelFromFile(origin, modelStoredPath); //retreives the model from file and stores it in redis
		}
				
		
		//Calls the model
		System.out.println("Running the algorithm "+algC);
		records = runModel(origin, records, prePostProcessor, alg, metrics);
		
		return records;
		
	}

	/**
	 * Runs the model
	 * @param origin
	 * @param recordsDB
	 * @param preprocessor
	 * @param postprocessor
	 * @param algC
	 * @param algorithm
	 * @return
	 */
	public Vector<TimeRecord> runModel(String origin, Vector<TimeRecord> recordsDB, PrePostProcessor prePostProcessor, MLalgorithms algorithm, Vector<Metric> metrics) {
		//Call the preprocess if there is any
		if(prePostProcessor != null ) {
			recordsDB = prePostProcessor.preprocess(recordsDB,origin);				
		}
				
		//Run model
		algorithm.run(recordsDB,origin);
		
		Iterator<TimeRecord> it = recordsDB.iterator();
		while (it.hasNext()) {
			TimeRecord record = it.next();
			System.err.println(record.toString() );
		}
		
		// Call the metrics if there is any
		Map<String, Vector<Double>> metricsEvaluation = new TreeMap<String, Vector<Double>>();
		if (metrics != null) {
			for(Metric metric: metrics) {
				metricsEvaluation.put(metric.getClass().getName(),metric.evaluate(recordsDB, origin));
			}
		}
		
		//Call the postprocess if there is any
		if(prePostProcessor!= null ) {
			recordsDB = prePostProcessor.postprocess(recordsDB,origin);				
		}
		return recordsDB;
	}
	

}
