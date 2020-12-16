package cea.util.connectors;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import cea.streamer.ModelRunner;
import cea.streamer.Trainer;
import cea.streamer.algs.MLalgorithms;
import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.metrics.Metric;
import cea.util.prepostprocessors.PrePostProcessor;

/**
 * 
 * @author sgarcia
 *
 */
public class AlgsExternalConnector {
	String id;
	String separator;
	String[] hyperParams;
	PrePostProcessor prePostProcessor;
	Vector<Metric> metrics;
	MLalgorithms algorithm;	
	String recordName;
	
	//Separator fields: ";"

	/**
	 * Constructor of the learning API connector
	 * @param id Identificator of the problem (must be unique)
	 * @param algorithmName Algorithm to use
	 * @param hyperParams List of the hyperparametres values for the algorithm, null if there is none 
	 * @param prePostproc Name of class that contains the pre and post processing methods to apply to the data, null if no processing method is needed
	 */
	public AlgsExternalConnector(String id, String algorithmName, String[] hyperParams, String prePostproc, String metricsNames) {		
		this.id = id;
		this.hyperParams = hyperParams;
		this.recordName = "GenericRecord";

		prePostProcessor = null;

		Class recC;
		if (prePostproc != null) {
			try {
				recC = Class.forName((GlobalUtils.packagePrePostProcessors+".")+prePostproc);
				prePostProcessor = (PrePostProcessor) recC.getDeclaredConstructor().newInstance();
			} catch (InstantiationException | IllegalArgumentException |  InvocationTargetException | SecurityException | IllegalAccessException | NoSuchMethodException |ClassNotFoundException e3) {
				System.err.println(
						"Preprocessor does not exist. Select one from the list:\n" + GlobalUtils.listAvaliablePrePostProcessors());
			}
		}
		
		if (metrics != null) {					
			String[] aux = (metricsNames.trim()).split(",");
			metrics = new Vector<Metric>();				
			for(String metricName: aux) {
				try {
					recC = Class.forName((GlobalUtils.packageMetrics+".") + metricName);				
					metrics.add( (Metric) recC.getDeclaredConstructor().newInstance() );
				} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
						| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e3) {
					System.err.println("The metric "+metricName+" class doen not exist");
				}
			}
		}
		
		MLalgorithms g= null;
		try {
			recC = Class.forName((GlobalUtils.packageAlgs+".")+algorithmName);
			try {
				//Object[] aux = hyperParams;
				algorithm = (MLalgorithms) recC.getDeclaredConstructor(String[].class).newInstance(new Object[]{hyperParams});
				
			} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				e.printStackTrace();
			}
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e3) {
			System.err.println("Algorithm does not exist. Select one from the list:\n" + GlobalUtils.listAvaliableAlgs());
			e3.printStackTrace();
		}

	}
	
	//////////////////// GETTERS AND SETTERS //////////////////////////////////

	public String getClientID() {
		return id;
	}

	public void setClientID(String id) {
		this.id = id;
	}

	public MLalgorithms getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(MLalgorithms algorithm) {
		this.algorithm = algorithm;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public String[] getHyperParams() {
		return hyperParams;
	}

	public void setHyperParams(String[] hyperParams) {
		this.hyperParams = hyperParams;
	}

	public PrePostProcessor getPreprocessor() {
		return prePostProcessor;
	}

	public void setPreprocessor(PrePostProcessor preprocessor) {
		this.prePostProcessor = preprocessor;
	}

	public String getRecordName() {
		return recordName;
	}

	public void setRecordName(String recordName) {
		this.recordName = recordName;
	}
	
	
	/////////////////////////////////// TRAIN AND TEST ///////////////////////////////////////////

	/**
	 * Calls the training of the algorithm
	 * @param file Input data to read from file
	 * @param modelStoredPath Path where trained model will be stored
	 * @return Outputs (if applicable as it is the case of clustering)
	 */
	public Vector<String> learnModel(String file, String modelStoredPath) {
		Vector<String> files = new Vector<String>();
		files.add(file);
		return learnModel(files, modelStoredPath);
	}
	
	/**
	 * Calls the training of the algorithm
	 * @param files List of files from where inputs for the algorithm are read
	 * @param modelStoredPath Path where trained model will be stored
	 * @return Outputs (if applicable as it is the case of clustering)
	 */
	public Vector<String> learnModel(Vector<String> files, String modelStoredPath) {
		Vector<TimeRecord> recordsDB = GlobalUtils.generateTimeRecords(id, recordName, files);

		Trainer train = new Trainer();
		Vector<TimeRecord> records = train.train(id, recordsDB, Long.MAX_VALUE, prePostProcessor,algorithm);

		Object model = RedisConnector.getModelFromRedis(id);
		GlobalUtils.saveModelInFile(model, modelStoredPath+"/model_"+id+"_"+algorithm.getClass().getName());

		return GlobalUtils.getOutputs(records); //extract outputs here from records (if applicable)
	}


	/**
	 * Calls the algorithm to run the model
	 * @param recordsDB Input records
	 * @param modelPath Path of file where trained model is stored. 
	 * 			NULL if model is stored in redis not in disk
	 * @return Vector<String> or results
	 */
	private Vector<String> runModel(Vector<TimeRecord> recordsDB, String modelPath) {		
		
		if(modelPath != null) {
			GlobalUtils.restoreModelFromFile(id, modelPath); //retreives the model from file and stores it in redis
		}
	
		//Call the model
		ModelRunner run = new ModelRunner();
		Vector<TimeRecord> records = run.runModel(id, recordsDB, prePostProcessor, algorithm, metrics);
		
		return GlobalUtils.getOutputs(records); //extract outputs here from records (if applicable)
	}
	

	/**
	 * Runs the model
	 * @param inputs for the model
	 * @param modelPath Path of file where trained model is stored. 
	 * 			NULL if model is stored in redis not in disk
	 * @return Vector<String> or results, for no inputs null outputs
	 */
	public Vector<String> runModel(String[][] inputs, String modelPath) {
		if(inputs != null) {
			Vector<TimeRecord> recordsDB = GlobalUtils.generateTimeRecords(id, recordName, inputs);					
			return runModel(recordsDB, modelPath);
		}else {
			return null;
		}
	}	

	/**
	 * Runs a model
	 * @param fileInputs file in from where we read the inputs
	 * @param modelPath Path of file where trained model is stored. 
	 * 			NULL if model is stored in redis not in disk
	 * @return Vector<String> or results
	 */
	public Vector<String> runModel(String fileInputs, String modelPath) {
		Vector<String> files = new Vector<String>();
		files.add(fileInputs);
		Vector<TimeRecord> recordsDB = GlobalUtils.generateTimeRecords(id, recordName, files);
		
		if(recordsDB.size() > 0) {
			return runModel(recordsDB, modelPath);	
		}else {
			System.err.println( "ERROR: no inputs are provided.");
			return null;
		}
	}
	
	/**
	 * List hyperparams need for the algorithm
	 */
	public void getAlgHyperparams() {
		System.out.println(algorithm.listNeededHyperparams());
	}

}
