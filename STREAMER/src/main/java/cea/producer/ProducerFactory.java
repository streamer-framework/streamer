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
	public  IProducer getProducer(String producerType) {
		
		return (producerType.equals("ONLINE"))?new OnlineProducer():new BlocksProducer();
	}

}
