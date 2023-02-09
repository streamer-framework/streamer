package cea.producer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.common.io.Resources;

import cea.util.GlobalUtils;
/**
 * This producer writes in a topic each line (form : "timestamp;value") of a datafile continuously and periodically.
 * The parameters can be changed in the streaming.props
 */
public class TimeStampProducer extends Producer {
	
	/**
	 * Runs a producer in a separate Thread. Its properties are read within from origin folder
	 * @param id folder where properties files are
	 */
	@Override
	public void runProducerInNewThread(String id) {
		Thread onlineProducerThread = new Thread() {
			public void run() {						
					try {
						TimeStampProducer p = new TimeStampProducer();
						p.produce(id);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			onlineProducerThread.start();		
	}
	/**
	 * Activates the producer who simulates the sending data from the source to the topic channel by simulating 
	 * records date and time to current time.
	 * @param id If there are many files to read from
	 * @throws IOException
	 */
	@Override
	public void produce(String id) throws IOException {
		KafkaProducer<String, String> producer;
		Path path = null;
		String[] topics;
		long maxBlocks;
		long recordsPerBlock;
		long producerTimeInterval;	//in milliseconds
		long epsilonne; //in milliseconds
		long scale; //in milliseconds
		String problemType;
		boolean containHeaders = false;
	    try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + id +"/streaming.props")) {
		    Properties properties = new Properties();
		    properties.load(props);            
		    producer = new KafkaProducer<String, String>(properties);            
		    path = Paths.get( properties.getProperty("datafile") );
		    topics= (properties.getProperty("mainTopic").replace(" ","")).split(",");
		    maxBlocks = Long.parseLong( properties.getProperty("maxBlocks") );
		    recordsPerBlock = Long.parseLong( properties.getProperty("recordsPerBlock") );
		    producerTimeInterval = Long.parseLong( properties.getProperty("producerTimeInterval") ); 
		    epsilonne = Long.parseLong( properties.getProperty("epsilonne") ); 
		    scale = Long.parseLong( properties.getProperty("scale") );
		    problemType = properties.getProperty("problem.type").replace(" ","");
		    if (properties.containsKey("containsHeader")) { 
		    	containHeaders = Boolean.parseBoolean(properties.getProperty("containsHeader").replace(" ","").toLowerCase());
		    }
	    }
	       
        String line=null;
       
		BufferedReader br=Files.newBufferedReader(path);
		int countBlocks = 0;
		
		LocalDateTime previousRecordDateTimeBeforeSimulation=null;

		LocalDateTime previousRecordDateTimeAfterSimulation = LocalDateTime.now();
		
		try{
			if(containHeaders)
				br.readLine();
			do{	        
				
				for(int i =0; i< recordsPerBlock; i++){//send from 5 to 5 every 10 seconds
					line = br.readLine();
					
//					RealTimeRecordSimulatorFactory rf = new RealTimeRecordSimulatorFactory();
//					RealTimeRecordSimulator rs = rf.getRealTimeRecordSimulator(problemType);
					
					RawRecord l= new RealTimeRecordSimulator().simulateRecord(line, previousRecordDateTimeBeforeSimulation, previousRecordDateTimeAfterSimulation, epsilonne, scale, problemType);
					
					previousRecordDateTimeBeforeSimulation=l.getRecordDateTimeBeforSimulation();
					previousRecordDateTimeAfterSimulation= l.getRecordDateTimeAfterSimulation();			        
			 			        
					String key;
					if(line.replaceAll("\"", "").replaceAll("\t", "") != "") {//record is not empty
						for(int t=0; t<topics.length; t++) {
							key = getKey(path.toString(),topics[t]);
							producer.send( new ProducerRecord<String, String>(topics[t], key, l.toString()));
							System.out.println("["+id+"] Writing in topic ("+topics[t]+") Key: ["+key+"] Content: "+l.toString());							
						}
					}                
				}
				System.out.println();
				producer.flush();
				countBlocks++;
				
				Thread.sleep(producerTimeInterval);//5 secs
			} while(line!=null && countBlocks < maxBlocks);
        
        } catch (NullPointerException npe) {
			System.out.println("["+id+"] The end of the data source has been reached. There is no more data to send");        	
        } catch (Exception e) {
			e.printStackTrace();        	
		} finally {
			System.out.println("["+id+"] Closing the producer");
			producer.close();
			br.close();
		}
      
	}
	

}
