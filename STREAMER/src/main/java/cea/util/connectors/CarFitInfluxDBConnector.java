package cea.util.connectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

/**
 * Class that makes the connection between Java and InfluxDB
 *
 */
public class CarFitInfluxDBConnector {

	/**
	 * Parameter that allows the connection to InfluxDB
	 */
	static InfluxDB influxDB;
	
	/**
	 * Name of the database
	 */
	static String dbName = "carfit";
	
	/**
	 * Host of the InfluxDB database
	 */
	static String host = "http://localhost:8086";//"http://10.0.238.12:8086"; // //"http://localhost:8086"; 
	
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
	static String pointMeassurement = "situation";
	
	static int count=0;

	private static int countOk;
	
	static int cB=0;
	
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
	private static void openConnection(){
		influxDB = InfluxDBFactory.connect(host, username, password);
	}
	
	/*
	 * (
  vehicle id text,	
	id integer,
	datetime timestamp with time zone NOT NULL,
	report id integer,
	x integer,
	y integer,
	z integer,
	lat double precision,
	lon double precision,
	speed double precision
	course integer,
	);
	 */
	
	/**
	 * Method that writes a vector of time records into InfluxDB
	 * @param records the vector of time records
	 */
	private static void write(String file){
		//System.out.println(file);
		
        // Create a 'batch' of example 'points'
        BatchPoints batchPoints = BatchPoints
                .database(dbName)
        //        .tag("async", "true")                
        //        .retentionPolicy("autogen")
        //        .consistency(InfluxDB.ConsistencyLevel.ALL)
        //        .tag("StoreTag", pointMeassurement) // tag each point in the batch
                .build();
		
		try{
			String headline;
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine(); //head
			if(line != null) {
				String[] head = line.trim().split(",");
				headline = line;
				line = br.readLine(); //head
				String[] fields;
				while(line!=null){
					fields = line.split(",");//ID,DATE/TIME,REPORT ID,X,Y,Z,LAT,LONG,SPEED(m/s),COURSE	
					if(head.length == fields.length && head.length == 10) {
						Point.Builder point1 = Point.measurement(pointMeassurement); // tag the individual point;
			        	point1 = point1.time(countOk, TimeUnit.MILLISECONDS).tag("vehicle_id", file);
						point1 = point1.addField("vehicle_id",file);
						for(int i = 0; i < head.length; i++) {
							//if(i < fields.length)
							if(head[i].equals("X") || head[i].equals("Y") || head[i].equals("Z") || head[i].equals("LAT") || head[i].equals("LONG") || head[i].equals("SPEED(m/s)")  || head[i].equals("COURSE") ){
								point1 = point1.addField(head[i],Double.parseDouble(fields[i]) );
							}else{
								point1 = point1.addField(head[i],fields[i]);
							}				
								
						}
			            batchPoints.point(point1.build());
			            countOk ++;
					}else {
						System.err.println(headline +" ---  "+line+ "  file "+file);
					}
					line = br.readLine();
					count++;
				}
			}else {
				System.err.println(file);
			}
			cB+=batchPoints.getPoints().size();
	//		System.out.println(batchPoints.getPoints().size());
			  // Write them to InfluxDB
	        influxDB.write(batchPoints);
			
			br.close();
			
		}catch(IOException e){
			e.printStackTrace();
			throw new Error("Missing a file for running the experiment");
		}	
		     
	}
	
	
	/**
	 * Method that reads and outputs the data of the problem type from the database
	 */
	private static void readDB(){
		//String command = "SELECT * FROM "+pointMeassurement+" WHERE vehicle_id='../../CarFit/data-equilibrage/VF12RAU1D56581018/SPEED_11MPS_2017-06-28T070904Z.csv'";
//		String command = "SELECT MEAN(Z) as avg FROM "+pointMeassurement;
		String command = "SELECT COUNT(*) FROM "+pointMeassurement+ " GROUP BY vehicle_id";
//		String command = "SELECT COUNT(vehicle_id) FROM "+pointMeassurement;
//		String command = "SELECT SUM(X) as paco FROM "+pointMeassurement+" GROUP BY vehicle_id";
		//String command = "SELECT SUM(X) as moyenneZ, vehicle_id FROM "+pointMeassurement+" GROUP BY vehicle_id ORDER BY moyenneZ";
		//String command = "SELECT SUM(X) FROM "+pointMeassurement;
		
		Query query = new Query(command, dbName);
		
		
		long startTime = System.currentTimeMillis();
		
		QueryResult queryResult = influxDB.query(query);
	    
	    long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
	    System.out.println(command);
	    	    
	    // iterate the results and print details
	    
//	    System.out.println("count "+queryResult.getResults().get(0).toString());
	    
	    for (QueryResult.Result result : queryResult.getResults()) {
	        // print details of the entire result
	      //  System.out.println(result.toString());
	        // iterate the series within the result
	        for (QueryResult.Series series : result.getSeries()) {	        	
	            System.out.println("series.getName() = " + series.getName());
	            System.out.println("series.getColumns() = " + series.getColumns());
	            System.out.println("series.getValues() = "+ series.getValues().size() + series.getValues());
	            System.out.println("series.getTags() = " + series.getTags());
	        }
	    }
	    System.out.println();
	    System.out.println("Elapsed time: "+elapsedTime +"(ms)");

	}
	
		
	
	/**
	 * Method that deletes the database
	 */
	public static void deleteDB(){
        influxDB.deleteDatabase(dbName);
	}

	public static void insert(String[] args) {
		File f;
		for(String arg:args){
			f = new File(arg);
			if(f.isDirectory()){
				for(File aux: f.listFiles()){//we process every file
					if(aux.getName().contains(".csv") && !aux.getName().contains("gz")) {
						write(aux.getPath());
					}
				}
			}else{
				write(arg);
			}
		}
	}

	public static void main(String[] args) {

		args = new String[1];
		args[0] = "../../CarFit/data-equilibrage/VF12RAU1D56581018";///ACC_0MPS-1MPS_2017-08-13T131825Z.csv";
		//args[0] = "../../CarFit/paco";
		
		openConnection();
	//	cleanDB();
	//	insert(args);
			
		System.err.println("total "+count + " well format :"+countOk+ "  "+cB);
		
		readDB();
		
	}
	
}



