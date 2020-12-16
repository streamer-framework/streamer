package cea.streamer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import com.google.common.io.Resources;

import cea.streamer.algs.MLalgorithms;
import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.Log;
import cea.util.connectors.ElasticSearchConnector;
import cea.util.connectors.InfluxDBConnector;
import cea.util.metrics.Metric;
import cea.util.metrics.QuantileSummary;
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
public class ProcessorReader extends AbstractProcessor<String, String> {

	/**
	 * Context of the streaming process
	 */
	private ProcessorContext context;

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
	private long previuosTimeStamp;

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
	private long previuosTrainingTime;

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
	 * Name of the class of the algorithm we use
	 */
	private String className;

	/**
	 * Quantile Summary
	 */
	private QuantileSummary mainQS = new QuantileSummary();

	/**
	 * Class with the preprocessor method for the incoming data. If null means that
	 * no processing is needed
	 */
	private PrePostProcessor prePostProcessor;

	/**
	 * Class with the metrics to evaluate the incoming data. If null means that no
	 * processing is needed
	 */
	private Vector<Metric> metrics;

	/**
	 * Unique identifier of the processor
	 */
	private String id;

	/**
	 * It counts the number of records received so far by the system It is used in
	 * method in process() to form the kvStore key
	 */
	private int countReceivedRecords;
	
	boolean visualization = false;

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

		System.err.println("ProcessorReader starts for " + origin);

		id = (origin/* + counter */);

		if (origin.equals("default")) {
			origin = ".";
		}

		if (Log.showDebug) {
			System.out.println("constructor()");
		}

		this.kvstoreName = kvstoreName;

		programInitTime = System.currentTimeMillis();
		previuosTimeStamp = 0;
		Class recC;
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/" + origin + "/" + "streaming.props").openStream()) {
			properties.load(props);
			readingTimeInterval = Integer.parseInt(properties.getProperty("readingTimeInterval"));
			problemType = (GlobalUtils.packageTimeRecords + ".") + properties.getProperty("problem.type").trim();
			try {
				if (properties.containsKey("visualization")) { 
					visualization = Boolean.parseBoolean( properties.getProperty("visualization").toLowerCase());
				}
			} catch (Exception e) {
				System.err.println("visualisation\" field in streaming.props must be \"true\" or \"false\".");
			}
			try {
				if (properties.containsKey("consumer.prepostprocessor")) {
					recC = Class.forName((GlobalUtils.packagePrePostProcessors + ".")
							+ properties.getProperty("consumer.prepostprocessor"));
					prePostProcessor = (PrePostProcessor) recC.getDeclaredConstructor().newInstance();
				}
			} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
					| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e3) {
				System.err.println("The postprocessor class doen not exist");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (InputStream props = Resources.getResource("setup/" + origin + "/" + "algs.props").openStream()) {
			properties.load(props);
			className = (GlobalUtils.packageAlgs + ".") + properties.getProperty("algorithm").trim();
			previuosTrainingTime = 0;
			trainingInterval = Long.parseLong(properties.getProperty("training.interval"));
			trainingMaxData = Long.parseLong(properties.getProperty("training.maxdata"));

			if (properties.containsKey("evaluation.metrics")) {
				String[] aux = (properties.getProperty("evaluation.metrics").trim()).split(",");
				metrics = new Vector<Metric>();
				for (String metricName : aux) {
					try {
						recC = Class.forName((GlobalUtils.packageMetrics + ".") + metricName);
						metrics.add((Metric) recC.getDeclaredConstructor().newInstance());
					} catch (InstantiationException | IllegalArgumentException | InvocationTargetException
							| SecurityException | IllegalAccessException | NoSuchMethodException
							| ClassNotFoundException e3) {
						System.err.println("The metric " + metricName + " class does not exist");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Method that initializes the processor context
	 */
	@Override
	public void init(ProcessorContext context) {
		// keep the processor context locally because we need it in punctuate()
		// and commit()
		this.context = context;

		// call this processor's punctuate() method every X time units (ms) -->
		// interval the time interval between punctuations
		this.context.schedule(Duration.ofMillis(readingTimeInterval), PunctuationType.STREAM_TIME, (long timestamp) -> {
			this.punctuate(timestamp);
		});

		// retrieve the key-value store named "kvstoreName"
		kvStore = (KeyValueStore) context.getStateStore(kvstoreName);

		if (Log.showDebug) {
			System.out.println("init()");

			KeyValueIterator iter = this.kvStore.all();
			while (iter.hasNext()) {
				KeyValue entry = (KeyValue) iter.next();
				System.err.println("intial record " + entry.key + ", " + entry.value.toString());
			}
			iter.close();
		}
	}

	/**
	 * Method that is called on each of the received record.
	 */
	@Override
	public void process(String dummy, String line) {
		if (previuosTimeStamp == 0) {
			KeyValueIterator iter2 = this.kvStore.all();
			while (iter2.hasNext()) {
				KeyValue entry = (KeyValue) iter2.next();
				kvStore.delete((String) entry.key);
				if (Log.showDebug) {
					System.err.println("deleting record " + entry.key + ", " + entry.value.toString());
				}
			}
		}
		countReceivedRecords++;
		kvStore.put(dummy + "_" + countReceivedRecords, line);

		if (Log.showDebug) {
			System.out.println("process()");

			KeyValueIterator iter = this.kvStore.all();
			while (iter.hasNext()) {
				KeyValue entry = (KeyValue) iter.next();
				System.out.println("processing item: " + entry.key + ", " + entry.value.toString());
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
		cal.setTimeInMillis(previuosTimeStamp);
		cal.add(Calendar.MILLISECOND, readingTimeInterval);

		long currentTime = System.currentTimeMillis();

		if (Log.showDebug) {
			System.out.println("punctuate()");
			System.out.println("" + "TimeStamp:\t\t" + timestamp + "\nProgram init time:\t" + programInitTime
					+ "\nPrevious time+interval:\t" + cal.getTimeInMillis() + "\nCurrent time:\t\t"
					+ System.currentTimeMillis());
		}

		if (currentTime > cal.getTimeInMillis()) {// execute every X seconds

			operateData(currentTime);// work with the data

			KeyValueIterator iter = this.kvStore.all();
			if (Log.showDebug) {
				iter = this.kvStore.all();
				System.out.println("punctuating... items left:");
				while (iter.hasNext()) {
					KeyValue entry = (KeyValue) iter.next();
					System.out.println("punctuate after sending: " + entry.key + ", " + entry.value.toString());
				}
				iter.close();
			}

			previuosTimeStamp = currentTime;

		} else {
			if (Log.showDebug) {
				System.out.println("waiting");
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
			System.out.println("closing... items left:");
			KeyValueIterator iter = this.kvStore.all();
			while (iter.hasNext()) {
				KeyValue entry = (KeyValue) iter.next();
				kvStore.delete((String) entry.key);
				if (Log.showDebug) {
					System.out.println("** " + entry.key + ", " + entry.value.toString());
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

		TimeRecord record = null;// structure to process data in the platform
		Vector<TimeRecord> records = new Vector<TimeRecord>();

		if (Log.showDebug) {
			System.out.println("**** kvSotre in punctuate(operateData()) **");
			int cont = 0;
			KeyValueIterator iter2 = this.kvStore.all();
			while (iter2.hasNext()) {
				cont++;
				KeyValue e = (KeyValue) iter2.next();
				System.out.println("Key: " + e.key + " - Value: " + e.value.toString());
			}
			System.out.println("**** " + cont);
		}

		/* Instead, for the moment, we just send it to an output topic */
		KeyValueIterator iter = this.kvStore.all();

		while (iter.hasNext()) {
			KeyValue entry = (KeyValue) iter.next();

			// context.forward(entry.key, entry.value.toString());// sends the raw record to
			// the out topic

			try {
				Class recC = Class.forName(problemType + "Record");
				TimeRecord recObj = (TimeRecord) recC.getDeclaredConstructor().newInstance();
				recObj.fill(entry.value.toString());

				records.add(recObj);

				this.kvStore.delete((String) entry.key);
			} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
					| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
				System.err.println("The type of time record specified does not exist. Platform is being interrupted");
				e.printStackTrace();
			}
		}
		System.out.println();
		this.kvStore.flush();
		iter.close();

		// Call the preprocess if there is any
		if (prePostProcessor != null) {
			records = prePostProcessor.preprocess(records, id);
		}

		for (TimeRecord t : records) {
			context.forward(t.getSource() + t.getTimeStamp(), t.fullRecordToString());// sends the record to the out
																						// topic
		}

		/* The new received data is also stored in influxDB */
		InfluxDBConnector.store(records);// store in DB

		/* Compute Quantile */
		// mainQS.merge(QuantileSummary.dataToQS(records));
		// Log.quantileLog.info((mainQS.getQuantile()));

		/* Here is where the Algorithm Module will be called to process the data */
		try {

			Class algC = Class.forName(className);
			MLalgorithms alg = (MLalgorithms) algC.getDeclaredConstructor().newInstance();

			/************** TRAINING ***********************/
			if ((trainingInterval >= 0) && ((currentTime - previuosTrainingTime) >= trainingInterval)) {// shall we run
																										// the training
				String captName = records.get(0).getName();
				Vector<TimeRecord> recordsDB = InfluxDBConnector.getRecordsDB(captName.toLowerCase(), trainingMaxData,
						alg.updateModel);
				if (recordsDB.size() <= trainingMaxData) {

					System.out.println("Training the algorithm " + algC);

					/* Create a thread that executes the training algorithm */
					Thread trainingThread = new Thread() {
						public void run() {
							Log.displayLogTrain
									.info("\n ####### New Model for " + id + " (Trained from InfluxDB): #######\n ");
							alg.learn(recordsDB, id);
						}
					};
					trainingThread.start();
				}

				previuosTrainingTime = currentTime;

			}

			/************** TEST ***********************/
			System.out.println("Run the model from " + algC);
			alg.run(records, id);

		} catch (InstantiationException | IllegalArgumentException | InvocationTargetException | SecurityException
				| IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
			System.err.println("The type of algorithm specified does not exist. Platform is being interrupted");
			e.printStackTrace();
		}

		// Call the metrics if there is any
		Map<Metric, Vector<Double>> metricsEvaluation = new HashMap<Metric, Vector<Double>>();
		if (metrics != null) {
			for (Metric metric : metrics) {
				metricsEvaluation.put(metric, metric.evaluate(records, id));
			}
		}
		//TODO: store in files
		if (!metricsEvaluation.isEmpty() && visualization) {	
			ElasticSearchConnector.init();
			ElasticSearchConnector.ingestMetricValues(metricsEvaluation, id);
		}
		// Call the postprocess if there is any
		if (prePostProcessor != null) {
			records = prePostProcessor.postprocess(records, id);
		}

	}

}
