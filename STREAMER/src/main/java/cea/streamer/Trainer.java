package cea.streamer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import cea.util.connectors.RedisConnector;
import cea.util.prepostprocessors.PrePostProcessor;

/**
 * Train an algorithm (setup in algs.props)
 * Data obtained from database or from a file (path specified in streaming.props) *
 */
public class Trainer {

	/**
	 * Train a model. Algorithm to use, source of data and other parameters are specified in the properties
	 * @param id Data source, options: 
	 * (i) influx 
	 * (ii) folder where the properties files are located; or 
	 * (iii) "" default folder of properties
	 * @throws Exception
	 */
	public Vector<TimeRecord> trainAll(String sourceType, String id, String modelStoredPath) throws Exception {
		Vector<TimeRecord> records; 		
		
		String origin = id;
		if (origin.equals("default")) {
			origin = ".";
		}
					
		String problemType=null;
		String className=null;
		Path path = null;
		long trainingMaxData=-1;
		Properties properties = new Properties();
		PrePostProcessor prePostProcessor = null;
		boolean containHeaders = false;
		
		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+origin+"/algs.props").openStream() ){		 
			    properties.load(props);
		    	className = (GlobalUtils.packageAlgs+".")+properties.getProperty("algorithm").replace(" ","");
		    	trainingMaxData = Long.parseLong(properties.getProperty("training.maxdata"));
	            path = Paths.get( properties.getProperty("training.source") );
				Log.displayLogTrain.info("["+id+"]: "+path);				
	 	} catch (IOException e) {
			e.printStackTrace();
		}		
		Class<?> recC;
        try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+origin+"/streaming.props").openStream()) {
            properties = new Properties();
            properties.load(props);            
	    	problemType = (GlobalUtils.packageTimeRecords+".")+properties.getProperty("problem.type").replace(" ","");	    			
	    	try {
	    		if(properties.containsKey("consumer.prepostprocessor")) {	    				
					recC = Class.forName((GlobalUtils.packagePrePostProcessors+".")+properties.getProperty("consumer.prepostprocessor"));
					prePostProcessor = (PrePostProcessor)recC.getDeclaredConstructor().newInstance();
	    		}
	    		if (properties.containsKey("containsHeader")) { 
			    	containHeaders = Boolean.parseBoolean(properties.getProperty("containsHeader").replace(" ","").toLowerCase());
			    }
			} catch (InstantiationException | IllegalArgumentException |  InvocationTargetException | SecurityException | IllegalAccessException | NoSuchMethodException |ClassNotFoundException e) {
				System.err.println("["+id+"] The postprocessor class doen not exist");
			}		
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
			System.err.println(name);
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
		
		System.out.println("["+id+"] Training the algorithm "+algC);
		records = train(id, records, trainingMaxData, prePostProcessor, alg);
		
		//We store the model in disk if requested
		if(modelStoredPath != null) {
			Object model = RedisConnector.getModelFromRedis(id);
			GlobalUtils.saveModelInFile(model, modelStoredPath+"/model_"+id+"_"+alg.getClass().getName());
		}
		return records;
				
	}

	/**
	 * Calls the training algorithm
	 * @param origin
	 * @param records
	 * @param trainingMaxData
	 * @param preprocessor
	 * @param postprocessor
	 * @param data.algorithm
	 * @param alg
	 * @return
	 */
	public Vector<TimeRecord> train(String origin, Vector<TimeRecord> records, long trainingMaxData,
			PrePostProcessor prePostProcessor, MLalgorithms alg) {
		//Call training algorithm
		if (records.size() <= trainingMaxData) {
			/************** PRE PROCESSING ***********************/
			if(prePostProcessor != null ) {
				records = prePostProcessor.preprocess(records,origin);				
			}

			/************** TRAINING ***********************/						
			alg.learn(records,origin);	
			
			/************** POST PROCESSING ***********************/
			if(prePostProcessor != null ) {
				records = prePostProcessor.postprocess(records,origin);				
			}
		}else {
			System.err.println("["+origin+"] Training set exceeds trainingMaxData defined in algs.props file. Training not executed");
		}
		return records;
	}
	

}
