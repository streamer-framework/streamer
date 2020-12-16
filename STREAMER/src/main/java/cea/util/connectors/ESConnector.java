package cea.util.connectors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.simple.JSONObject;

import cea.streamer.core.TimeRecord;

/**
 * @author MA261439
 *
 */

public class ESConnector {
	// The hostname on which ES is running
	private static String hostname_ES = "http://localhost:9200";
		
	/**
	 * IntializeConnector is a method responsible for initializing the elastic search connector
	 * It first checks if ES is reachable and then check if the index exists
	 * If not, it will create it based on the following parameters:
	 * @param index_name: the name of the index to be created
	 * @param index_mapping: the mapping of the index (schema of the index in ES)
	 */
	public static void initializeConnector(String index_name, String index_mapping) {
		//Here we initialize the connector and the check for the connection of with ES
		try(RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(HttpHost.create(hostname_ES)))){
	
		// Check if the index already exists in ES
		boolean exists = client.indices().exists(new GetIndexRequest(index_name), RequestOptions.DEFAULT);
		if(!exists) {
			System.out.println("Creating the index: " + index_name);
			//Don't exist, so we have to create a new one with mapping
			client.indices().create(new CreateIndexRequest(index_name), RequestOptions.DEFAULT);
					
			PutMappingRequest request = 
					new PutMappingRequest(index_name).source(index_mapping, XContentType.JSON);
			
			client.indices().putMapping(request, RequestOptions.DEFAULT);
			System.out.println("Index " + index_name + " was created successfully!");
		}else {
			System.out.println("Index " + index_name + " already exists.");
		}
			
			
		}catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * ingestRawData is a method responsible for inserting the data to ES
	 * it is the gate in the pipeline that connects the java program to write to ES
	 * 
	 * @param records: the records that need to be processed
	 * @param index_name: the name of the index
	 * @param id: the project name or the hostname depending on the project requirements
	 */
	public static void ingestRawData(Vector<TimeRecord> records, String index_name, String id) {

		try(RestHighLevelClient client = new RestHighLevelClient(
				RestClient.builder(HttpHost.create(hostname_ES)))){
			for(TimeRecord tr: records) {
				JSONObject obj = new JSONObject();
				obj.put("timestamp", formatTimeES(tr.getTimeStamp()));
				obj.put("hostname", id);
				obj.put("measure", tr.getValues().get("meassure"));
								
				IndexRequest insertRequest = new IndexRequest(index_name);
				insertRequest.source(obj.toJSONString(), XContentType.JSON);
				client.index(insertRequest, RequestOptions.DEFAULT);
			}
			client.indices().refresh(new RefreshRequest(index_name), RequestOptions.DEFAULT);
			
			
		}catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * ingestDetectedData is a method responsible for inserting the data to ES
	 * it is the gate in the pipeline that connects the java program to write to ES
	 * Here, we are inserting the result of the detection to another index to make the kibana dashboard richer
	 * @param result_detected: the count of the records that matched anomalies (number of anomalies detected)
	 * @param index_name: the name of the index
	 * @param id: the project name or the hostname depending on the project requirements
	 */	
	public static void ingestDetectedData(String result_detected, String index_name, String id) {

		try(RestHighLevelClient client = new RestHighLevelClient(
			RestClient.builder(HttpHost.create(hostname_ES)))){

			JSONObject obj = new JSONObject();
			obj.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH).format(new Date()));
			obj.put("hostname", id);
			if(result_detected == null || result_detected.isEmpty())
				return;
			
			obj.put("count_detected", Integer.parseInt(result_detected));
						
			IndexRequest insertRequest = new IndexRequest(index_name);
			insertRequest.source(obj.toJSONString(), XContentType.JSON);
			client.index(insertRequest, RequestOptions.DEFAULT);

			client.indices().refresh(new RefreshRequest(index_name), RequestOptions.DEFAULT);
			
			
		}catch(Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	
	/**
	 * formatTimeES is a method responsible for formatting the date in the way that is best for ES
	 * 
	 * @param d: the date that needs to be formatted
	 */
	private static String formatTimeES(String d) {
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy HH:mm:ss.SSS", Locale.ENGLISH);
		Date dateTime = null;
		try {
			dateTime = format.parse(d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String formatted_timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH).format(dateTime);
		return formatted_timestamp;
	}
	

}
