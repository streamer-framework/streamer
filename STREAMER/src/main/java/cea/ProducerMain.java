package cea;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.Resources;

import cea.producer.Producer;
import cea.producer.ProducerFactory;
import cea.util.GlobalUtils;

/**
 * Launches STREAMER producer (data ingester)
 * Its function is to push data in a topic(s) continuously and periodically. 
 * Producing setup can be changed in streaming.props
 */
public class ProducerMain {

	/**
	 * Activates the producer who simulates the sending data from the source to the
	 * topic channel.
	 * 
	 * @param args folders where the properties files are [1...N]. One folder per application.
	 * 				Example: "application1 application2"
	 * 			STREAMER will run one instance per argument (application) in parallel
	 * 			if no argument provided, STREAMER works with "default" id and root algs.props and streaming.props
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		if (args.length > 0) {// a different thread per producer
			for (int i = 0; i < args.length; i++) {
				run(args[i]);
			}
		} else {
			run(".");
		}
	}
	
	private static void run(String id) {
		ProducerFactory producerFactory = new ProducerFactory();
		Properties properties = new Properties();
		String producerType = "";

		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles + id + "/" + "streaming.props")
				.openStream()) {
			properties.load(props);
			producerType = properties.getProperty("producerType").replace(" ","");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Producer p = producerFactory.getProducer(producerType);
		p.runProducerInNewThread(id);
	}


}