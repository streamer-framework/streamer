package cea.util.connectors;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;
import cea.util.metrics.Metric;

public class ElasticSearchConnector {
	/**
	 * Host of the Elastic Search NoSQL store
	 */
	static String host;

	/**
	 * Index to store evaluation metrics in Elastic Search
	 */
	static String evaluationindex;

	/**
	 * Index to store raw data in Elastic Search
	 */
	static String rawdataindex;

	/**
	 * Instantialize the Elasticsearch Connector through:
	 * 1) reading the properties
	 * 2) creating the index (db) in Elasticsearch for handling the evaluation metrics' results and raw data.
	 * 
	 */
	public static void init() {
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/elasticsearch.props").openStream()) {
			properties.load(props);
			host = properties.getProperty("host").trim();
			if (host.equals("localhost"))
				host = host + ":9200";
			evaluationindex = properties.getProperty("index.evaluation").trim();
			rawdataindex = properties.getProperty("index.rawdata").trim();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		createIndex(evaluationindex);
		createIndex(rawdataindex);
	}

	/**
	 * Get an instantiation of Elasticsearch
	 * 
	 * @return instantiation of Elasticsearch
	 */
	public static RestHighLevelClient getElasticSearchClient() {
		RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(host)));
		return client;
	}

	/**
	 * This method is for creating indexes in Elasticsearch.
	 * It first checks if ES is reachable and then check if the index exists or not.
	 * If not, it will create it with the following parameter:
	 * 
	 * @param index_name:    the name of the index to be created
	 */
	public static void createIndex(String index_name) {
		// Here we initialize the connector and the check for the connection of with ES
		try (RestHighLevelClient client = getElasticSearchClient()) {

			// Check if the index already exists in ES
			boolean exists = client.indices().exists(new GetIndexRequest(index_name), RequestOptions.DEFAULT);
			if (!exists) {
				System.out.println("Creating the index: " + index_name);
				// Don't exist, so we have to create a new one
				client.indices().create(new CreateIndexRequest(index_name), RequestOptions.DEFAULT);

				// System.out.println("Index " + index_name + " is created!");
			} else {
				// System.out.println("Index " + index_name + " exists!");
			}

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * This method is for creating indexes in Elasticsearch.
	 * It first checks if ES is reachable and then check if the index exists or not.
	 * If not, it will create it with the following parameters:
	 * 
	 * @param index_name:    the name of the index to be created
	 * @param index_mapping: the mapping of the index (schema of the index in ES)
	 */
	public static void createIndex(String index_name, String index_mapping) {
		// Here we initialize the connector and the check for the connection of with ES
		try (RestHighLevelClient client = getElasticSearchClient()) {

			// Check if the index already exists in ES
			boolean exists = client.indices().exists(new GetIndexRequest(index_name), RequestOptions.DEFAULT);
			if (!exists) {
				System.out.println("Creating the index: " + index_name);
				// Don't exist, so we have to create a new one with mapping
				client.indices().create(new CreateIndexRequest(index_name), RequestOptions.DEFAULT);

				PutMappingRequest request = new PutMappingRequest(index_name).source(index_mapping, XContentType.JSON);

				client.indices().putMapping(request, RequestOptions.DEFAULT);
				//System.out.println("Index " + index_name + " was created successfully!");
			} else {
				//System.out.println("Index " + index_name + " already exists.");
			}

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * ingestRawData is a method for inserting the data to ES.
	 * It inserts all the raw data along with the target and output.
	 * 
	 * @param records:    the records that need to be processed
	 * @param id:         the project name or the hostname depending on the project
	 *                    requirements
	 */
	@SuppressWarnings("unchecked")
	public static void ingestRawData(Vector<TimeRecord> records, String id) {

		try (RestHighLevelClient client = getElasticSearchClient()) {
			for (TimeRecord tr : records) {
				JSONObject obj = new JSONObject();
				obj.put("timestamp", formatTimeES(tr.getTimestamp()));
				obj.put("app", id);
				for (String k : tr.getValues().keySet()) {
					obj.put(k, tr.getValues().get(k));
				}
				// obj.put("measure", tr.getValues().get("meassure"));
				obj.put(tr.getExpectedOutputLabel(), tr.getTarget());
				obj.put(tr.getOutputLabel(), tr.getOutput());
				IndexRequest insertRequest = new IndexRequest(rawdataindex);
				insertRequest.source(obj.toJSONString(), XContentType.JSON);
				client.index(insertRequest, RequestOptions.DEFAULT);
			}
			client.indices().refresh(new RefreshRequest(rawdataindex), RequestOptions.DEFAULT);

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * ingestMetricValues is a method for inserting the metrics values
	 * to ES. It inserts all the evaluation mertrics' values to monitor the
	 * performance of the model
	 * 
	 * @param records:    the records that need to be processed
	 * @param id:         the project name or the hostname depending on the project
	 *                    requirements
	 */
	@SuppressWarnings("unchecked")
	public static void ingestMetricValues(Map<Metric, Vector<Double>> metricsEvaluation, String id) {

		try (RestHighLevelClient client = getElasticSearchClient()) {
			JSONObject obj = new JSONObject();
			obj.put("timestamp",
					formatTimeES(new SimpleDateFormat("dd-MMM-yy HH:mm:ss.SSS", Locale.ENGLISH).format(new Date())));
			obj.put("app", id);
			for (Metric k : metricsEvaluation.keySet()) {
				/*
				 * String values = ""; for (Double i : metricsEvaluation.get(k)) { values += i +
				 * ";"; } values = values.substring(0, values.lastIndexOf(";")); obj.put(k,
				 * values);
				 */
				if (metricsEvaluation.get(k) != null) {
					Double value = metricsEvaluation.get(k).get(0);
					obj.put("metrics." + k.getName(), value);
				}
			}
			IndexRequest insertRequest = new IndexRequest(evaluationindex);
			insertRequest.source(obj.toJSONString(), XContentType.JSON);
			client.index(insertRequest, RequestOptions.DEFAULT);
			client.indices().refresh(new RefreshRequest(evaluationindex), RequestOptions.DEFAULT);

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * formatTimeES is a method for formatting the date in the way that
	 * is best for ES
	 * 
	 * @param d: the date that needs to be formatted
	 * @return String: formatted date as String
	 */
	private static String formatTimeES(String d) {
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy HH:mm:ss.SSS", Locale.ENGLISH);
		Date dateTime = null;
		try {
			dateTime = format.parse(d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String formatted_timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH)
				.format(dateTime);
		return formatted_timestamp;
	}

}
