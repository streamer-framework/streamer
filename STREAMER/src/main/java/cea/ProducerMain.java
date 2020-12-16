package cea;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.Resources;

import cea.producer.IProducer;
import cea.producer.ProducerFactory;

/**
 * This producer writes in a topic each line (form : "timestamp;value") of a
 * datafile continuously and periodically. The parameters can be changed in the
 * streaming.props
 */
public class ProducerMain {

	/**
	 * Activates the producer who simulates the sending data from the source to the
	 * topic channel.
	 * 
	 * @param args If there are many files to read from
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		/*
		 * args = new String[7]; args[0]="captor1"; args[1]="captor2";
		 * args[2]="leak-detec_sec1"; args[3]="leak-detec_sec2";
		 * args[4]="leak-detec_sec3";args[5]="water-pol_cap1"; args[6]="water-pol_cap2";
		 */

		/*
		 * args = new String[2]; args[0]="captor1"; args[1]="captor2";
		 */

		Properties properties = new Properties();
		String producerType = "";

		ProducerFactory producerFactory = new ProducerFactory();

		if (args.length > 0) {// a different thread per producer
			for (int i = 0; i < args.length; i++) {
				try (InputStream props = Resources.getResource("setup/" + args[i] + "/" + "streaming.props")
						.openStream()) {
					properties.load(props);
					producerType = properties.getProperty("producerType").trim();
				} catch (IOException e) {
					e.printStackTrace();
				}
				IProducer p = producerFactory.getProducer(producerType);
				p.runProducerInNewThread(args[i] + "/");
			}
		} else {
			try (InputStream props = Resources.getResource("setup/streaming.props").openStream()) {
				properties.load(props);
				producerType = properties.getProperty("producerType").trim();
			} catch (IOException e) {
				e.printStackTrace();
			}
			IProducer p = producerFactory.getProducer(producerType);
			p.produce("");
		}
	}

}