package cea.util.connectors;

import java.io.IOException;
import java.io.InputStream;
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
 *
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
	 * Relation of the data base
	 */
	static String pointMeassurement = "ds";

	/**
	 * The problem type that indicates the type of data
	 */
	static String problemType = "t";

	/**
	 * Method that initializes the parameters of the connection with the content of
	 * the properties files
	 */
	public static void init() {

		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/influxDB.props").openStream()) {
			properties.load(props);
			dbName = properties.getProperty("dbName").trim();
			host = properties.getProperty("host").trim();
			password = properties.getProperty("password").trim();
			pointMeassurement = properties.getProperty("Point.measurement").trim();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (InputStream props = Resources.getResource("setup/streaming.props").openStream()) {
			properties.load(props);
			problemType = (GlobalUtils.packageTimeRecords + ".") + properties.getProperty("problem.type").trim();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		openConnection();

	}

	/**
	 * Method that cleans the database
	 */
	public static void cleanDB() {
		influxDB.deleteDatabase(dbName);
		influxDB.createDatabase(dbName);
	}

	/**
	 * Open a InfluxDB connection
	 */
	private static void openConnection() {
		influxDB = InfluxDBFactory.connect(host, username, password);
	}

	/**
	 * Method that writes a vector of time records into InfluxDB
	 * 
	 * @param records the vector of time records
	 */
	private static void write(Vector<TimeRecord> records) {
		// Create a 'batch' of example 'points'

		BatchPoints batchPoints = BatchPoints.database(dbName).tag("async", "true").retentionPolicy("autogen")
				.consistency(InfluxDB.ConsistencyLevel.ALL).tag("StoreTag", pointMeassurement) // tag each point in the
																								// batch
				.build();

		for (TimeRecord record : records) {
			if (Log.showDebug) {
				System.out.println(" record storing in DB--------------------------->" + record.toString());
			}
			Point.Builder point1 = Point.measurement(pointMeassurement); // tag the individual point;
			point1 = point1.time(record.getTimeStampMillis(), TimeUnit.MILLISECONDS).tag("source",
					record.getName().replace("\\", "/"));

			/*
			 * for(String k: record.getValues().keySet()){ point1 =
			 * point1.addField(k,record.getValues().get(k)); }
			 */

			point1 = point1.addField("values", record.exportValues());// we store the whole record (timestamp, values,
																		// etc)
			if (record.getTarget() != null) {// if it has a target (desired output)
				point1 = point1.addField("target", record.getTarget());// we store the target
			}

			batchPoints.point(point1.build());
			if (Log.showDebug) {
				System.out.println("Sending... " + record.fullRecordToString());
			}
		}
		// Write them to InfluxDB
		influxDB.write(batchPoints);
	}

	/**
	 * Method that reads and outputs the data of the problem type from the database
	 */
	private static void readDB() {
		Query query = new Query("SELECT * FROM " + pointMeassurement, dbName);
		QueryResult queryResult = influxDB.query(query);
		// iterate the results and print details
		for (QueryResult.Result result : queryResult.getResults()) {
			// print details of the entire result
			System.out.println(result.toString());

			if (result.getSeries() != null) {
				// iterate the series within the result
				for (QueryResult.Series series : result.getSeries()) {
					System.out.println("series.getName() = " + series.getName());
					System.out.println("series.getColumns() = " + series.getColumns());
					System.out.println("series.getValues() = " + series.getValues().size() + series.getValues());
					System.out.println("series.getTags() = " + series.getTags());
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
	 * @param captName the name of the field when the data was stored
	 * @return the vector of time records
	 */
	public static Vector<TimeRecord> getRecordsDB(String captName, long maxdatasize, boolean isModelUpdatable) {
		captName = captName.replace("\\", "/");

		Vector<TimeRecord> records = new Vector<TimeRecord>();
		Properties properties = new Properties();
		Query query = null;
		if (isModelUpdatable || maxdatasize == -1) {
			query = new Query("SELECT * FROM " + pointMeassurement + " where source = '" + captName + "'", dbName);

		} else {
			query = new Query("SELECT * FROM " + pointMeassurement + " where source = '" + captName + "'"
					+ "order by time DESC" + " limit " + maxdatasize, dbName);
		}
		QueryResult queryResult = influxDB.query(query);
		
		// after training the model in online learning algorithms, the data cannot be used anymore for next training phase.
		if (isModelUpdatable) {
			cleanDB();
		}
		
		// iterate the results and print details
		for (QueryResult.Result result : queryResult.getResults()) {
			for (QueryResult.Series series : result.getSeries()) {
				// get indices of the Columns for a TimeRecord
				int indexSource = series.getColumns().indexOf("source");
				int indexTime = series.getColumns().indexOf("time");
				int indexValues = series.getColumns().indexOf("values");
				int indexTarget = series.getColumns().indexOf("target");// it returns -1 if it does not contain any
																		// target

				for (List serie : series.getValues()) {// for each time record in the db

					// delete T and Z in timestamp to get a proper format
					String timestamp = serie.get(indexTime).toString().replace("T", " ").replace("Z", "");
					// String rec = serie.get(indexName) + ";" + timestamp+ ";" +
					// serie.get(indexMeassure);

					// construct a TimeRecord object
					try {
						Class recC = Class.forName(problemType + "Record");
						TimeRecord recObj = (TimeRecord) recC.newInstance();
						// recObj.fill(rec);
						recObj.setSource("" + serie.get(indexSource));
						recObj.setTimestamp(timestamp);
						recObj.importValues("" + serie.get(indexValues));
						if (indexTarget != -1) {// there is a target
							recObj.setTarget("" + serie.get(indexTarget));
						}
						records.add(recObj);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return records;
	}

	/**
	 * Method that deletes the database
	 */
	public static void deleteDB() {
		influxDB.deleteDatabase(dbName);
	}

	/**
	 * Method that stores a vector of time records into the database
	 * 
	 * @param records the vector of time records
	 */
	public static void store(Vector<TimeRecord> records) {
		// openConnection();
		write(records);
		if (Log.showDebug)
			readDB();
	}

}
