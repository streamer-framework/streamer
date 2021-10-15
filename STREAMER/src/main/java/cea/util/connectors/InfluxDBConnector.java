package cea.util.connectors;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.Log;

/**
 * Class that makes the connection between Java and InfluxDB
 * Timestamp is the index key in influxDB
 * Several records with same time stamp can coexist if they have different tag
 */
public class InfluxDBConnector {

	/**
	 * Parameter that allows the connection to InfluxDB
	 */
	static InfluxDB influxDB;

	/**
	 * Name of the database
	 */
	static String dbName = "default";

	/**
	 * Host of the InfluxDB database
	 */
	static String host = "http://localhost:8086"; // "http://10.0.238.12:8086"; //

	/**
	 * User name to connect to the database
	 */
	static String username = "root";

	/**
	 * Password to connect to the database
	 */
	static String password = "root";


	/**
	 * Method that initializes the parameters of the connection with the content of
	 * the properties files
	 */
	public static void init() {

		Properties properties = new Properties();
		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+"influxDB.props").openStream()) {
			properties.load(props);
			dbName = properties.getProperty("dbName").replace(" ","");
			host = properties.getProperty("host").replace(" ","");
			password = properties.getProperty("password").replace(" ","");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		openConnection();
	}


	/**
	 * Open a InfluxDB connection
	 */
	private static void openConnection() {
		influxDB = InfluxDBFactory.connect(host, username, password);
	}
	
	/**
	 * Method that stores a vector of time records into the database
	 * 
	 * @param records the vector of time records
	 */
	public static void store(Vector<TimeRecord> records, String id) {
		// openConnection();
		write(records, id);
		if (Log.showDebug)
			readDB(id);
	}

	/**
	 * Method that writes a vector of time records into InfluxDB
	 * Timestamp is the index key in influxDB
	 * Several records with same time stamp can coexist if they have different tag
	 * 
	 * @param records the vector of time records
	 */
	private static void write(Vector<TimeRecord> records, String id) {
		// Create a 'batch' of example 'points'
		
		String id_db = id.replace("-", "");//it cannot contain "-"

		BatchPoints batchPoints = BatchPoints.database(dbName).tag("async", "true").retentionPolicy("autogen")
				.consistency(InfluxDB.ConsistencyLevel.ALL).tag("batch_id", id_db) // tag each point in the batch
				.build();

		for (TimeRecord record : records) {
			if (Log.showDebug) {
				System.out.println("["+id+"] record storing in DB--------------------------->" + record.toString());
			}
			Point.Builder point1 = Point.measurement(id_db); // tag the individual point;
			point1 = point1.time(record.getTimeStampMillis(), TimeUnit.MILLISECONDS).tag("source",record.getSource());
			
			 //for(String k: record.getValues().keySet()){ point1 =
			 // point1.addField(k,record.getValues().get(k)); }			 

			//point1 = point1.addField("source", record.getSource());// we store the whole record (timestamp, values,
			point1 = point1.addField("values", record.exportValues());// we store the whole record (timestamp, values,etc)
			if (!record.getTarget().isEmpty()) {// if it has a target (expected output)
				point1 = point1.addField("target", GlobalUtils.listToString(record.getTarget(),record.getSeparatorFieldsRawData()));// we store the target
			}

			batchPoints.point(point1.build());
			if (Log.showDebug) {
				System.out.println("["+id+"] Sending... " + record.fullRecordToString());
			}
		}
		// Write them to InfluxDB
		influxDB.write(batchPoints);	
	}

	/**
	 * Method that reads and outputs the data of the problem type from the database
	 */
	private static void readDB(String id) {
		String id_db = id.replace("-", "");//it cannot contain "-"
		Query query = new Query("SELECT * FROM " + id_db, dbName);
		QueryResult queryResult = influxDB.query(query);
		// iterate the results and print details
		for (QueryResult.Result result : queryResult.getResults()) {
			// print details of the entire result
			System.out.println("["+id+"] "+result.toString());

			if (result.getSeries() != null) {
				// iterate the series within the result
				for (QueryResult.Series series : result.getSeries()) {
					System.out.println("["+id+"] series.getName() = " + series.getName());
					System.out.println("["+id+"] series.getColumns() = " + series.getColumns());
					System.out.println("["+id+"] series.getValues() = " + series.getValues().size() + series.getValues());
					System.out.println("["+id+"] series.getTags() = " + series.getTags());
				}
			}
			System.err.println("No series stored in the DB");
		}
		System.out.println();
	}

	/**
	 * Method that gets the data of the problem type from InfluxDB and puts them
	 * into a vector of time record
	 * 
	 * Timestamp is the index key in influxDB
	 * Several records with same time stamp can coexist if they have different tag
	 * 
	 * @param source the name of the field when the data was stored
	 * @param maximum number of records to store in Influx
	 * @param isModelUpdatable If it is a model that learns incrementally, only the new data is retreive, 
	 * 	otherwise the full data (with maxsize limit) is retreived 
	 * @return the vector of time records
	 */
	public static Vector<TimeRecord> getRecordsDB(String id, long maxdatasize, boolean isModelUpdatable) {
		String id_db = id.replace("-", "");//it cannot contain "-"
		Vector<TimeRecord> records = new Vector<TimeRecord>();
		Query query = null;
		if (isModelUpdatable || maxdatasize == -1) {
			query = new Query("SELECT * FROM \"" + id_db+"\"", dbName);

		} else {
			query = new Query("SELECT * FROM \"" + id_db+"\" order by time ASC" + " limit " + maxdatasize, dbName);
		}
		QueryResult queryResult = influxDB.query(query);
		
		// after training the model in online learning algorithms, the data cannot be used anymore for next training phase.
		if (isModelUpdatable) {
			//cleanDB();
			cleanElementsID(id_db);
		}
		String origin = id;
		if (origin.equals("default")) {
			origin = ".";
		}
		Properties properties = new Properties();
		String problemType=null;
		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+origin+"/streaming.props").openStream()) {
			properties.load(props);
			problemType = (GlobalUtils.packageTimeRecords + ".") + properties.getProperty("problem.type").replace(" ","");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		// iterate the results and print details
		for (QueryResult.Result result : queryResult.getResults()) {
			for (QueryResult.Series series : result.getSeries()) {
				// get indices of the Columns for a TimeRecord
				//int indexId = series.getColumns().indexOf("id");
				int indexSource = series.getColumns().indexOf("source");
				int indexTime = series.getColumns().indexOf("time");
				int indexValues = series.getColumns().indexOf("values");
				int indexTarget = series.getColumns().indexOf("target");// it returns -1 if it does not contain any target

				for (List serie : series.getValues()) {// for each time record in the db

					// delete T and Z in timestamp to get a proper format
					String timestamp = serie.get(indexTime).toString().replace("T", " ").replace("Z", "");

					// construct a TimeRecord object
					try {
						Class<?> recC = Class.forName(problemType + "Record");
						TimeRecord recObj= (TimeRecord) recC.getDeclaredConstructor().newInstance();
						
						// recObj.fill(rec);
						recObj.setSource("" + serie.get(indexSource));
						recObj.setTimeStamp(timestamp);
						recObj.importValues("" + serie.get(indexValues));
						if (indexTarget != -1) {// if there is a target
							String target = ""+serie.get(indexTarget);
							recObj.setTarget(Arrays.asList(target.split(recObj.getSeparatorFieldsRawData())) );
						}
						records.add(recObj);
					} catch (ClassNotFoundException | InstantiationException | 
							IllegalAccessException | IllegalArgumentException | 
							InvocationTargetException | NoSuchMethodException | SecurityException e) {
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println("["+id+"] "+records.size()+" records for training");					

		return records;
	}

	/**
	 * Method that cleans the FULL database
	 * (by deleting it and creating it again)
	 */
	public static void cleanDB() {
		QueryResult queryResult = influxDB.query(new Query("DROP DATABASE " +dbName)); //delete database
		queryResult = influxDB.query(new Query("CREATE DATABASE " +dbName)); //create a new one
	}
	
	/**
	 * Method that cleans the points with id tag
	 * @param id tag
	 */
	public static void cleanElementsID(String id) {
		String id_db = id.replace("-", "");//it cannot contain "-"
		try {
			QueryResult queryResult = influxDB.query(new Query("DELETE FROM \"" +id_db+"\"",dbName)); //deletes all points from a series in a database. Unlike DROP SERIES, DELETE does not drop the series from the index.
			//QueryResult queryResult = influxDB.query(new Query("DROP SERIES FROM \"" +id_db+"\"",dbName)); //deletes all points from a series in a database, and it drops the series from the index.
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that deletes the database
	 * @deprecated
	 */
	public static void deleteDB() {
		influxDB.deleteDatabase(dbName);
	}

}
