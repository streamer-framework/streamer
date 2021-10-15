package cea.streamer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import com.google.common.io.Resources;

import cea.util.GlobalUtils;
import cea.util.Log;
import cea.util.connectors.InfluxDBConnector;
import cea.util.connectors.KibanaConnector;
import cea.util.connectors.RedisConnector;

/**
 * STREAMER launcher
 */

public class Launcher {

	/**
	 * Launchs the streaming platform. It allows having separate processes, each of
	 * them to read and process a different input channel
	 * 
	 * @param ids Folder where the properties are
	 * @throws IOException
	 */
	public void launch(String[] ids) throws IOException {

		InfluxDBConnector.init();
		// cleaning the database in InfluxDB
		InfluxDBConnector.cleanDB();
		Log.clearLogs();
		RedisConnector.cleanKeys(ids);//does not clean the trained models
		//RedisConnector.cleanModel("kdd-cup-99_23class");//just for KDD

		if (ids.length == 0) {// default producer
			ids = new String[1];
			ids[0] = ".";// root properties
		}
		Properties streamsConfiguration = new Properties();
		String[] inputTopics = null;
		String outputTopic = null;
		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles + ids[0] + "/streaming.props").openStream()) {
			streamsConfiguration.load(props);
		}
		
		try {
			if (streamsConfiguration.containsKey("visualization")) { 
				boolean visualization = Boolean.parseBoolean( streamsConfiguration.getProperty("visualization").toLowerCase());
				if(visualization) {
					// Import Kibana metrics dashboard
					KibanaConnector.init();
					KibanaConnector.importMetricsDashboard();
				}
			}
		} catch (Exception e) {
			System.err.println("\"visualisation\" field in streaming.props must be \"true\" or \"false\".");
		}

		streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "PlatformTest");
//		streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//		streamsConfiguration.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, "localhost:2181");
		streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");// latest, earliest, none

		// Records should be flushed every 10 seconds. This is less than the default in
		// order to keep this example interactive.
		streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);// in ms

		// Set up serializers and deserializers, which we will use for overriding the
		// default serdes specified above
		// final Serde<String> stringSerde = Serdes.String();
		// final Serde<Long> longSerde = Serdes.Long();

		StoreBuilder storebuilder;// use StoreSupplier
		// TopologyBuilder builder = new TopologyBuilder();
		Topology builder = new Topology();
		StringSerializer stringSerializer = new StringSerializer();
		StringDeserializer stringDeserializer = new StringDeserializer();

		for (int i = 0; i < ids.length; i++) {
			String id;
			if (ids[i].equals(".")) {
				id = "default";
			} else {
				id = ids[i];
			}
			try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles + ids[i] + "/" + "streaming.props")
					.openStream()) {
				streamsConfiguration.load(props);
				inputTopics = (streamsConfiguration.getProperty("mainTopic").replace(" ","")).split(",");
				outputTopic = streamsConfiguration.getProperty("outputTopic").replace(" ","");
			}

			storebuilder = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore("Counts" + id),
					new Serdes.StringSerde(), new Serdes.StringSerde()).withCachingDisabled();

			builder.addSource("Source" + id, stringDeserializer, stringDeserializer, inputTopics)
					.addProcessor("Process" + id, () -> new ProcessorReader(id, "Counts" + id),"Source" + id)
					.addStateStore(storebuilder, "Process" + id)
					.addSink("sink" + id, outputTopic, stringSerializer, stringSerializer, "Process" + id);

			/*
			 * storeSup = Stores.create("Counts"+origin) .withKeys(Serdes.String())
			 * .withValues(Serdes.String()) .persistent() .build();
			 * builder.addSource("Source"+origin, stringDeserializer, stringDeserializer,
			 * inputTopics) .addProcessor("Process"+origin, () -> new
			 * ProcessorReader(origin, "Counts"+origin), "Source"+origin)
			 * .addStateStore(storeSup, "Process"+origin) .addSink("sink"+origin,
			 * outputTopic, stringSerializer, stringSerializer, "Process"+origin);
			 */
		}

		KafkaStreams streams = new KafkaStreams(builder, streamsConfiguration);

		if (System.getProperty("os.name").toLowerCase().contains("windows")) {

			// If Kafka is running, then we cannot delete the file (other process is using it)
			try (AdminClient client = KafkaAdminClient.create(streamsConfiguration)) {
				ListTopicsResult topics = client.listTopics();				
			} catch (Exception ex) {
				// Kafka is not running, clean the files
				cleanKafkaLogsForWindows(outputTopic, inputTopics, ids[0]);
			}
		} else {
			streams.cleanUp();
		}

		streams.start();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

		// Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
		/*
		 * Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
		 * 
		 * @Override public void run() { streams.close(); } }));
		 */

		/*
		 * try { Thread.sleep(10000); } catch (InterruptedException e) {
		 * e.printStackTrace(); } streams.close();
		 */
	}
	
	private void cleanKafkaLogsForWindows(String outputTopic, String[] inputTopics, String id) {
		try {
			// streams.cleanUp() on windows is not working, so I figured out a replacement
			// for it.
			String kafka_streams_path = "C:\\tmp\\kafka-streams\\test\\0_0\\";
			File kafka_streams_log_file = new File(kafka_streams_path);
			if (kafka_streams_log_file.exists() && kafka_streams_log_file.isDirectory())
				FileUtils.cleanDirectory(kafka_streams_log_file);

			String kafka_logs_1 = outputTopic;
			List<String> kafka_logs_inputs = new ArrayList<String>();
			for (String topic_input : inputTopics) {
				kafka_logs_inputs.add(topic_input);
			}
			String kafka_logs_2 = "test-Counts" + id + "-changelog";

			File kafka_logs_folder = new File("C:\\tmp\\kafka-logs\\");
			File[] files = kafka_logs_folder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					for (String topic_input : kafka_logs_inputs) {
						if (name.contains(topic_input))
							return name.contains(topic_input);
					}

					return name.contains(kafka_logs_1) || name.contains(kafka_logs_2);
				}
			});
			for (File file : files) {
				FileUtils.forceDelete(file);
			}

		} catch (Exception ex2) {
			System.out.println("["+id+"] The log files of Kafka cannot be cleaned because Kafka is running!");
			ex2.printStackTrace();
		}
	}

}