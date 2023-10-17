package cea.producer;

import cea.util.GlobalUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Block producer that adds a constraint for each lines of a same block. If two successive
 * lines have not the same group (a value that determines the group of a line), the first
 * one will be the last line of the current block and the second one will be cached for
 * the next sending iteration (next block). Warning, each row of the datafile needs to have
 * a group value in the first column.
 */
public class BlocksProducerByGroup extends Producer{

    @Override
    public void runProducerInNewThread(String id) {
        Thread producerThread = new Thread(() -> {
            try {
                BlocksProducerByGroup p = new BlocksProducerByGroup();
                p.produce(id);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        producerThread.start();
    }

    @Override
    public void produce(String id) throws IOException{

        KafkaProducer<String, String> producer;
        Path path;
        String[] topics;
        long maxBlocks;
        long recordsPerBlock;
        long producerTimeInterval;	//in milliseconds
        boolean containHeaders = false;

        try (InputStream props = new FileInputStream(GlobalUtils.resourcesPathPropsFiles+id+"/streaming.props")) {
            Properties properties = new Properties();
            properties.load(props);
            producer = new KafkaProducer<String, String>(properties);
            path = Paths.get( properties.getProperty("datafile") );
            topics= (properties.getProperty("mainTopic").replace(" ","")).split(",");
            maxBlocks = Long.parseLong( properties.getProperty("maxBlocks") );
            recordsPerBlock = Long.parseLong( properties.getProperty("recordsPerBlock") );
            producerTimeInterval = Long.parseLong( properties.getProperty("producerTimeInterval") );
            if (properties.containsKey("containsHeader")) {
                containHeaders = Boolean.parseBoolean(properties.getProperty("containsHeader").replace(" ","").toLowerCase());
            }
        }

        String line=null;
        String savedLine=null;

        BufferedReader br=Files.newBufferedReader(path);
        int countBlocks = 0;

        int group_line;
        Integer group_previous_line = null;

        String key;

        try{
            if(containHeaders)
                br.readLine();
            do{
                for(int i=0; i<recordsPerBlock; i++){

                    line = br.readLine();

                    if(line.replaceAll("\"", "").replaceAll("\t", "") != "") {//record is not empty

                        group_line = Integer.parseInt(line.split(" ")[0]);

                        if(savedLine != null) {
                            for (String topic : topics) {
                                key = getKey(path.toString(), topic);
                                producer.send(new ProducerRecord<String, String>(topic, key, savedLine));
                                System.out.println("[" + id + "] Writing in topic (" + topic + ") Key: [" + key + "] Content: " + savedLine);
                            }
                            i++;
                            savedLine = null;
                        }

                        if((group_previous_line != null) && (group_previous_line != group_line)) {
                            savedLine = line;
                            group_previous_line = group_line;
                            break;
                        } else {
                            for (String topic : topics) {
                                key = getKey(path.toString(), topic);
                                producer.send(new ProducerRecord<String, String>(topic, key, line));
                                System.out.println("[" + id + "] Writing in topic (" + topic + ") Key: [" + key + "] Content: " + line);
                            }
                            group_previous_line = group_line;
                        }

                    }

                }

                System.out.println();
                producer.flush();
                countBlocks++;

                Thread.sleep(producerTimeInterval);

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

