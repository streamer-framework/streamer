package cea.producer;

import java.io.IOException;

/**
 *Interface for creating an object
 *
 */
public abstract class Producer {
	
	//String fieldsSeparator = ";";
	long count=0;
	
	public Producer() {
		count=0;
	}
	
	/**
	 * Runs a producer in a separate Thread. Its properties are read within from origin folder
	 * @param id folder where properties files are
	 */
	public abstract void runProducerInNewThread(String id);
	
	/**
	 * Activates the producer who simulates the sending data from the source to the topic channel.
	 * @param args If there are many files to read from
	 * @throws IOException
	 */
	public abstract void produce(String id) throws IOException;
	
	/**
	 * Compose the key of the record to be sent to kafka by the producer.
	 * Counter use and incremented by 1 unit per record
	 * @param path of the file from we are reading
	 * @param topic: kafka channel 
	 * @return the key [fileName_topic_count] for the KStreamProducer object
	 */
	public String getKey(String path, String topic) {		
		String fileName = path.replace(" ","").substring(path.lastIndexOf("/")+1,path.lastIndexOf("."));		
		//String key = (fileName+"_"+topic+"_"+(count)).replace(" ","");		//key = file name + topic + counter
		count++;		
		//return key;
		return fileName;
	}

}
