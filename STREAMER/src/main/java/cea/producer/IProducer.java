package cea.producer;

import java.io.IOException;

/**
 *Interface for creating an object
 *
 */
public interface IProducer {
	/**
	 * Runs a producer in a separate Thread. Its properties are read within from origin folder
	 * @param origin folder where properties files are
	 */
	public void runProducerInNewThread(String origin);
	
	/**
	 * Activates the producer who simulates the sending data from the source to the topic channel.
	 * @param args If there are many files to read from
	 * @throws IOException
	 */
	public void produce(String origin) throws IOException;

}
