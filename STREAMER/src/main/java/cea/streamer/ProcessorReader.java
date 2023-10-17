package cea.streamer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import cea.federated.client.Client;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import cea.streamer.algs.MLalgorithms;
import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.Log;
import cea.util.connectors.ElasticSearchConnector;
import cea.util.connectors.InfluxDBConnector;
import cea.util.metrics.Metric;
import cea.util.monitoring.Alert;
import cea.util.monitoring.MonitoringDetector;
import cea.util.prepostprocessors.PrePostProcessor;

/*
 * Users can define their customized stream processor by implementing the Processor interface, which provides 
 * two main API methods: process() and punctuate()

    process() is called on each of the received record.
    punctuate() is called periodically based on advanced record timestamps. For example, if processing-time is used as the record timestamp, then punctuate() will be triggered every specified period of time.

	The Processor interface also has an init() method, which is called by the Kafka Streams library during task 
	construction phase. Processor instances should perform any required initialization in this method. The init() 
	method passes in a ProcessorContext instance, which provides access to the metadata of the currently processed record, 
	including its source Kafka topic and partition, its corresponding message offset, and further such information. 
	This context instance can also be used to schedule the punctuation period (via ProcessorContext#schedule()) for punctuate(), 
	to forward a new record as a key-value pair to the downstream processors (via ProcessorContext#forward()), and to commit the 
	current processing progress (via ProcessorContext#commit())
 */

/**
 * Class that implements the stream processor
 */
public class ProcessorReader implements Processor<String, String,String,String> {

	/**
	 * Context of the streaming process
	 */
	private ProcessorContext<String,String> context;

	/**
	 * Map where records are stored once they are read from input topic
	 */
	private KeyValueStore<String, String> kvStore;

	/**
	 * Name of the ktble store is not the default one
	 */
	private String kvstoreName;

	/**
	 * Timestamp that indicates the last time we read a topic
	 */
	private long previousTimeStamp;

	/**
	 * The time we initialize the program
	 */
	private long programInitTime;

	/**
	 * Interval (in milliseconds) between 2 readings of a topic
	 */
	private int readingTimeInterval;

	/**
	 * The problem type that indicates the type of data
	 */
	private String problemType;

	// For training & test ALGS
	/**
	 * Timestamp of the previous training step (Last time we trained the model)
	 */
	private long previousTrainingTime;

	/**
	 * Interval (in milliseconds) between 2 trainings of a model
	 */
	private long trainingInterval;

	/**
	 * Max amount of time records stored in InfluxDB from which the model is
	 * trained. Beyond that limit, the model is no longer trained
	 */
	private long trainingMaxData;
	
	/**
	 *	Model's inference activation (default=true)
	 */
	private boolean onlineTrain = true;
	
	/**
	 * Model is trained online (default=true)
	 */
	private boolean onlineInference = true;
	
	/**
	 * Name of the class of the algorithm we use
	 */
	private String className;

	/**
	 * Class with the preprocessor method for the incoming data. If null means that
	 * no processing is needed
	 */
	private PrePostProcessor prePostProcessor;

	/**
	 * Class with the metrics to evaluate the incoming data. If null means that no
	 * processing is needed
	 */
	private Vector<Metric> metricsName;
	
	/**
	 * Detector for deviation in results evaluations (metrics) while monitoring.
	 * It is normally used to call the update model policy
	 */
	private MonitoringDetector monitoringDetector;

	/**
	 * Unique identifier of the processor
	 */
	private String id;

	/**
	 * It counts the number of records received so far by the system It is used in
	 * method in process() to form the kvStore key
	 */
	private int countReceivedRecords;
	
	/**
	 * It sends data to ES for visualization
	 */
	private boolean visualization = false;
	
	/**
	 * Number of stream which is being processed (start by 0)
	 */
	private long iteration;
	
	/**
	 * Alert launched for retraining
	 */
	private Alert alert;

	/**
	 * Boolean that determines if the client is connected to the server or not.
	 */
	private boolean notConnected = true;

	/**
	 * Boolean that determines if the distributed mode is activated or not.
	 */
	private boolean distributedMode = false;

	/**
	 * Address of the server needed to establish the connection.
	 */
	private String server_address;

	/**
	 * Name of the python file for the distributed ('federated') module.
	 */
	private String pythonFilePath;

	/**
	 * Constructor that initialized the parameters of the streaming process
	 * according to the properties files
	 */
	public ProcessorReader() {
		this("default", "Counts");
	}

	/**
	 * Constructor that initialized the parameters of the streaming process
	 * according to the properties files
	 * 
	 * @param origin The original source, properties are stored in a folder with the
	 *               same name
	 */

	public ProcessorReader(String origin, String kvstoreName/* , int counter */) {
		
		id = (origin/* + counter */);

		System.out.println("["+id+"] ProcessorReader starts ");
		
		if (Log.showDebug) {
			System.out.println("["+id+"] constructor()");
		}

		this.kvstoreName = kvstoreName;
	}


	/**
	 * Method that initializes the processor context
	 */
	@Override
	public void init(ProcessorContext<String,String> context) {
		iteration = 0;
		readPropertiesFiles();
		// keep the processor context locally because we need it in punctuate()
		// and commit()
		this.context = context;

		// call this processor's punctuate() method every X time units (ms) -->
		// interval the time interval between punctuations
		this.context.schedule(Duration.ofMillis(readingTimeInterval), PunctuationType.STREAM_TIME, (long timestamp) -> {
			this.punctuate(timestamp);
		});

		// retrieve the key-value store named "kvstoreName"
		kvStore = (KeyValueStore<String,String>) context.getStateStore(kvstoreName);

		if (Log.showDebug) {
			System.out.println("["+id+"] init()");

			KeyValueIterator<String,String> iter = this.kvStore.all();
			while (iter.hasNext()) {
				KeyValue<String,String> entry = (KeyValue<String,String>) iter.next();
				System.err.println("["+id+"] intial record " + entry.key + ", " + entry.value.toString());
			}
			iter.close();
		}
	}

	/**
	 * Method that is called on each of the received record.
	 */
	@Override
	public void process(Record<String, String> record) {
	
		if (previousTimeStamp == 0) {
			KeyValueIterator<String,String> iter2 = this.kvStore.all();
			while (iter2.hasNext()) {
				KeyValue<String,String> entry = (KeyValue<String,String>) iter2.next();
				kvStore.delete((String) entry.key);
				if (Log.showDebug) {
					System.err.println("["+id+"] deleting record " + entry.key + ", " + entry.value.toString());
				}
			}
		}
		countReceivedRecords++;
		kvStore.put(record.key() + "_" + countReceivedRecords, record.value());

		if (Log.showDebug) {
			System.out.println("["+id+"] process()");

			KeyValueIterator<String,String> iter = this.kvStore.all();
			while (iter.hasNext()) {
				KeyValue<String,String> entry = (KeyValue<String,String>) iter.next();
				System.out.println("["+id+"] processing item: " + entry.key + ", " + entry.value.toString());
			}
			iter.close();
		}
		kvStore.flush();
	}

	/**
	 * Method that is called periodically based on advanced record timestamps
	 * 
	 * @param timestamp the wallclock time when this method is being called
	 */

	public void punctuate(long timestamp) {

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(previousTimeStamp);
		cal.add(Calendar.MILLISECOND, readingTimeInterval);

		long currentTime = System.currentTimeMillis();

		if (Log.showDebug) {
			System.out.println("["+id+"] punctuate()");
			System.out.println("["+id+"] TimeStamp:\t\t" + timestamp + "\nProgram init time:\t" + programInitTime
					+ "\nPrevious time+interval:\t" + cal.getTimeInMillis() + "\nCurrent time:\t\t"
					+ System.currentTimeMillis());
		}

		if (currentTime > cal.getTimeInMillis()) {// execute every X seconds

			operateData(currentTime);// work with the data

			KeyValueIterator<String,String> iter = this.kvStore.all();
			if (Log.showDebug) {
				iter = this.kvStore.all();
				System.out.println("["+id+"] punctuating... items left:");
				while (iter.hasNext()) {
					KeyValue<String,String> entry = (KeyValue<String,String>) iter.next();
					System.out.println("["+id+"] punctuate after sending: " + entry.key + ", " + entry.value.toString());
				}
				iter.close();
			}

			previousTimeStamp = currentTime;

		} else {
			if (Log.showDebug) {
				System.out.println("["+id+"] waiting");
			}
		}

		context.commit(); // commit the current processing progress

	}

	/**
	 * Method that closes the key-value store
	 */
	@Override
	public void close() {
		if (Log.showDebug) {
			System.out.println("["+id+"] closing... items left:");
			KeyValueIterator<String,String> iter = this.kvStore.all();
			while (iter.hasNext()) {
				KeyValue<String,String> entry = (KeyValue<String,String>) iter.next();
				kvStore.delete((String) entry.key);
				if (Log.showDebug) {
					System.out.println("["+id+"] " + entry.key + ", " + entry.value.toString());
				}
			}
		}
		kvStore.close();
	}

	/**
	 * Method that works with the data at current time. The data are read from a
	 * topic, stored in InfluxDB and sent to the algorithm module
	 */
	private void operateData(long currentTime) {
		
		iteration++;
		
		System.out.println("\n["+id+"] Stream "+iteration);

		Vector<TimeRecord> records = new Vector<TimeRecord>();

		if (Log.showDebug) {
			System.out.println("["+id+"] **** kvSotre in punctuate(operateData()) **");
			int cont = 0;
			KeyValueIterator<String,String> iter2 = this.kvStore.all();
			while (iter2.hasNext()) {
				cont++;
				KeyValue<String,String> e = (KeyValue<String,String>) iter2.next();
				System.out.println("["+id+"] Key: " + e.key + " - Value: " + e.value.toString());
			}
			System.out.println("["+id+"] **** " + cont);
		}

		/* Instead, for the moment, we just send it to an output topic */
		KeyValueIterator<String,String> iter = this.kvStore.all();
		while (iter.hasNext()) {
			KeyValue<String,String> entry = (KeyValue<String,String>) iter.next();
			//context.forward(new Record<String, String>(entry.key, entry.value,-1));// sends the raw record to the output topic

			try {
				Class<?> recC = Class.forName(problemType + "Record");
				TimeRecord recObj = (TimeRecord) recC.getDeclaredConstructor().newInstance();
				recObj.fill(entry.key.toString(),entry.value.toString());
				records.add(recObj);
				this.kvStore.delete((String) entry.key);
			} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
					| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
				System.err.println("["+id+"] The type of time record specified does not exist. STTREAMER is being interrupted");
				e.printStackTrace();
			}
		}
		this.kvStore.flush();
		iter.close();

		System.out.println("["+id+"] "+records.size()+" records retreived from topic(s)");

		
		/************** PRE PROCESSING ***********************/
		if (prePostProcessor != null) {
			if(!records.isEmpty()) {
				records = prePostProcessor.preprocess(records, id);
			}else {
				System.err.println("["+id+"] Pre-processing not performed, records set is empty");
			}
		}
		
		if(!records.isEmpty()) {		
			for (TimeRecord t : records) {
				context.forward(new Record<String, String>(t.getSource() + t.getTimeStamp(), t.fullRecordToString(),t.getTimeStampMillis()));// sends the preprocessed record to the output topic
			}
			
			/* The new received data is also stored in influxDB */		
			InfluxDBConnector.store(records, id);
		}else {
			System.err.println("["+id+"] Records set is empty, nothing pushed to output topic");
		}

		/* Compute Quantile */
		// mainQS.merge(QuantileSummary.dataToQS(records));
		// Log.quantileLog.info((mainQS.getQuantile()));

		/* Here is where the Algorithm Module will be called to process the data */
		try {

			Class<?> algC = Class.forName(className);
			MLalgorithms alg = (MLalgorithms) algC.getDeclaredConstructor().newInstance();

			/************** TRAINING ***********************/

			if(distributedMode) {

				if(notConnected) {

					notConnected = false;
					System.out.println("[" + id + "] Distributed mode activated");

					Thread clientThread = new Thread(() -> {
						Client client = new Client(id, pythonFilePath);
						client.isOnline = true;
						if (!onlineTrain) {
							client.isPassive = true;
						}
						client.start_client(server_address);
					});
					clientThread.start();

				}

			} else {

				if ((onlineTrain) && ( ((trainingInterval >= 0) && ((currentTime - previousTrainingTime) >= trainingInterval)) || alert.isAlert()) ) {
					//we retrain if it is time or there is an alert
					Vector<TimeRecord> recordsDB = InfluxDBConnector.getRecordsDB(id, trainingMaxData,alg.isUpdateModel());
					if(alert.isAlert()) System.out.println("["+id+"] ("+iteration+") Training, alert activated");
					if (recordsDB.size() <= trainingMaxData) {
						if(!recordsDB.isEmpty()){
							System.out.println("["+id+"] Training: algorithm " + algC);

							/* Create a thread that executes the training algorithm */
							Thread trainingThread = new Thread() {
								public void run() {
									Log.displayLogTrain
											.info("\n ####### ["+id+"] New Model (trained from InfluxDB): #######\n ");
									alg.learn(recordsDB, id);
								}
							};
							trainingThread.start();
						}else {
							System.err.println("["+id+"] Training not performed, records set is empty");
						}
					}else {
						System.err.println("["+id+"] Training not performed, records training set ("+recordsDB.size()+") is greater than the maximum size defined ("+trainingMaxData+")");
					}
					previousTrainingTime = currentTime;
					alert.setAlert(false);
				}

			}

			/************** INFERENCE ***********************/
			if(onlineInference) {
				if(!records.isEmpty()){ 
					System.out.println("["+id+"] Inference: model from " + algC);
					alg.run(records, id);
				}else {
					System.err.println("["+id+"] Inference not performed, records set is empty");
				}
			}

		} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
				| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
			System.err.println("["+id+"] The type of algorithm specified does not exist. Platform is being interrupted");
			e.printStackTrace();
		}
		

		/************** POST PROCESSING ***********************/
		if (prePostProcessor != null) {
			if(!records.isEmpty()){ 
				records = prePostProcessor.postprocess(records, id);
			}else {
				System.err.println("["+id+"] Post-processing not performed, records set is empty");
			}
		}
		
		/********************* METRICS ***********************/
		Map<Metric, Vector<Double>> metricsEvaluation = new LinkedHashMap<Metric, Vector<Double>>();
		if(!records.isEmpty()/* && GlobalUtils.containsOutputs(records)*/){ //metrics are usually computed if outputs are produced most of the times but not necessarily
			if (metricsName != null) {
				for (Metric metric : metricsName) {
					Vector<Double> values = metric.evaluate(records, id);
					metricsEvaluation.put(metric, values);
					StringBuilder metricLog = new StringBuilder("["+id+"] ("+iteration+") "+metric.getName()+" =");
					for(double d:values) {
						metricLog.append(" "+d);
					}				
					Log.metricsLog.info(metricLog.toString());
					System.out.println(metricLog.toString());
				}
				//insert the accumulated data
				if(monitoringDetector != null) { //deviation detection is required		
					/* Create a thread that executes the monitoring algorithm */
					//Thread monitoringThread = new Thread() {
					//	public void run() {
							alert.setAlert(monitoringDetector.detec(metricsEvaluation, id,iteration));
							metricsEvaluation.put(alert, alert.evaluate(null, id));

					//	}
					//};
					//monitoringThread.start();
				}
			}
		}else {
			System.err.println("["+id+"] Metrics not computed, records set is empty");
		}

		/************** VISUALIZATION ***********************/
		if (!metricsEvaluation.isEmpty() && visualization) {
			ElasticSearchConnector.init();
			ElasticSearchConnector.ingestMetricValues(metricsEvaluation, id);
		}

	}

	/**
	 * Read Setup configuration (from properties files)
	 */
	private void readPropertiesFiles() {
		String origin = id;
		if (origin.equals("default")) {
			origin = ".";
		}
		
		programInitTime = System.currentTimeMillis();
		previousTimeStamp = 0;
		Class<?> recC;
		Properties properties = new Properties();
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + origin + "/streaming.props")) {
			properties.load(props);
			readingTimeInterval = Integer.parseInt(properties.getProperty("readingTimeInterval"));
			problemType = (GlobalUtils.packageTimeRecords + ".") + properties.getProperty("problem.type").replace(" ","");
			try {
				if (properties.containsKey("visualization")) { 
					visualization = Boolean.parseBoolean( properties.getProperty("visualization").toLowerCase());
				}
				if (properties.containsKey("online.train")) { 
					onlineTrain = Boolean.parseBoolean( properties.getProperty("online.train").toLowerCase());
				}
				if (properties.containsKey("online.inference")) { 
					onlineInference = Boolean.parseBoolean( properties.getProperty("online.inference").toLowerCase());
				}
			} catch (Exception e) {
				System.err.println("["+id+"] visualisation\" field in streaming.props must be \"true\" or \"false\".");
			}
			try {
				if (properties.containsKey("consumer.prepostprocessor")) {
					recC = Class.forName((GlobalUtils.packagePrePostProcessors + ".")
							+ properties.getProperty("consumer.prepostprocessor").replace(" ",""));
					prePostProcessor = (PrePostProcessor) recC.getDeclaredConstructor().newInstance();
				}
			} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
					| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e3) {
				System.err.println("["+id+"] The postprocessor class doen not exist");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + origin + "/algs.props")) {
			properties.load(props);
			className = (GlobalUtils.packageAlgs + ".") + properties.getProperty("algorithm").replace(" ","");
			previousTrainingTime = 0;
			trainingInterval = Long.parseLong(properties.getProperty("training.interval"));
			trainingMaxData = Long.parseLong(properties.getProperty("training.maxdata"));

			if (properties.containsKey("evaluation.metrics")) {
				String[] aux = (properties.getProperty("evaluation.metrics")).replace(" ","").split(",");
				metricsName = new Vector<Metric>();
				for (String metricName : aux) {
					try {
						recC = Class.forName((GlobalUtils.packageMetrics + ".") + metricName);
						metricsName.add((Metric) recC.getDeclaredConstructor().newInstance());
					} catch (InstantiationException | IllegalArgumentException | InvocationTargetException
							| SecurityException | IllegalAccessException | NoSuchMethodException
							| ClassNotFoundException e3) {
						System.err.println("["+id+"] The metric " + metricName + " class does not exist");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		alert = new Alert();//alert default at false
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles  + "/monitoring.props")) {
			properties.load(props);
			if (properties.containsKey("detector")) {
				String dec = (properties.getProperty("detector")).replace(" ","");
				try {					
					recC = Class.forName((GlobalUtils.packageMonitoring + ".") + dec);
					monitoringDetector = ((MonitoringDetector) recC.getDeclaredConstructor().newInstance());					
				} catch (InstantiationException | IllegalArgumentException | InvocationTargetException
						| SecurityException | IllegalAccessException | NoSuchMethodException
						| ClassNotFoundException e3) {
					System.err.println("["+id+"] The detector " + dec + " is not correctly specified.");
					e3.printStackTrace();
				}
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (InputStream props = new FileInputStream(GlobalUtils.resourcesPathPropsFiles + id + "/federated.props")) {
			properties.load(props);
			server_address = properties.getProperty("server.address");
			distributedMode = Boolean.parseBoolean(properties.getProperty("distributed.mode"));
			pythonFilePath = properties.getProperty("python.file.path");
		} catch (Exception ignored) {}

	}
	
}
