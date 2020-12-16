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
 * Class that allows to train al algorithm with all the data from the datafile/influxdb
 *
 */
public class Trainer {

	/**
	 * Train a model. Algorithm to use, source of data and other parameters are specified in the properties
	 * @param origin Data source, options: 
	 * (i) influx 
	 * (ii) folder where the properties files are located; or 
	 * (iii) "" default folder of properties
	 * @throws Exception
	 */
	public Vector<TimeRecord> trainAll(String sourceType, String origin, String modelStoredPath) throws Exception {
		Vector<TimeRecord> records; 		
		
		String pathProperties = "";
		
		if(!origin.equals("default")) {
			pathProperties = origin+"/";	
		}
					
		String problemType=null;
		String className=null;
		Path path = null;
		long trainingMaxData=-1;
		Properties properties = new Properties();
		PrePostProcessor prePostProcessor = null;
		
		try (InputStream props = Resources.getResource("setup/"+pathProperties+"algs.props").openStream() ){		 
			    properties.load(props);
		    	className = (GlobalUtils.packageAlgs+".")+properties.getProperty("algorithm").trim();
		    	trainingMaxData = Long.parseLong(properties.getProperty("training.maxdata"));
	            path = Paths.get( properties.getProperty("training.source") );
	            System.out.println(path);
				Log.displayLogTrain.info(path);				
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
					recC = Class.forName((GlobalUtils.packagePrePostProcessors+".")+properties.getProperty("consumer.prepostprocessor"));
					prePostProcessor = (PrePostProcessor)recC.getDeclaredConstructor().newInstance();
	    		}
			} catch (InstantiationException | IllegalArgumentException |  InvocationTargetException | SecurityException | IllegalAccessException | NoSuchMethodException |ClassNotFoundException e) {
				System.err.println("The postprocessor class doen not exist");
			}		
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
		
		System.out.println("Training the algorithm "+algC);
		records = train(origin, records, trainingMaxData, prePostProcessor, alg);
		
		//We store the model in disk if requested
		if(modelStoredPath != null) {
			Object model = RedisConnector.getModelFromRedis(origin);
			GlobalUtils.saveModelInFile(model, modelStoredPath+"/model_"+origin+"_"+alg.getClass().getName());
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
			//Call the preprocess if there is any
			if(prePostProcessor != null ) {
				records = prePostProcessor.preprocess(records,origin);				
			}
						
			alg.learn(records,origin);	
			
			//Call the postprocess if there is any
			if(prePostProcessor != null ) {
				records = prePostProcessor.postprocess(records,origin);				
			}
		}
		return records;
	}
	

}
