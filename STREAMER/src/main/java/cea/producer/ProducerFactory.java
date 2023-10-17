package cea.producer;

/**
 *Class that implements Producer Factory : allow to create a producer
 *
 */
public class ProducerFactory{

	/**
	 * method that allow to create an producer according to specified Producer type
	 * without specifying the exact object to be created
	 * @param producerType
	 * @return appropriated object according to specified Producer type
	 */
	public  Producer getProducer(String producerType) {

		if(producerType.equals("TIMESTAMP")) {
			return new TimeStampProducer();
		} else if(producerType.equals("BLOCKBYGROUP")) {
			return new BlocksProducerByGroup();
		} else {
			return new BlocksProducer();
		}
	}

}
