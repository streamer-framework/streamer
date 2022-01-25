package cea;

import java.util.Vector;

import cea.streamer.ModelRunner;
import cea.streamer.Trainer;
import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.Log;
import cea.util.connectors.InfluxDBConnector;
import cea.util.connectors.RedisConnector;

/**
 * Batch trainer and tester
 * For being used in an offline manner.
 * Train and test data can be read from a file (defined in streaming.props for test and algs.props for train) or from database
 */
public class AlgInvokerMain_offline {

	
	/**
	 * Main class. 
	 * Invoke the train and/or test of the selected algorithm. 
	 * 
	 * @param (0) Data source type: From where data must be retreived. Option: [influx , file] 
	 * @param (1) Origin. Option: [influx data base DB name, folder where the properties files are located] 
	 * @param (2) Perform training [true or false] (train)
	 * @param (3) Perform model running [true or false] (test)
	 * @param (4) [Optional] Path from where to store/retrieve the trained model
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		String dataSourceType = "file"; /* default value */
		String id = "default"; /* default value */	
		boolean train = true; /* default value */
		boolean test = true; /* default value */
		String modelStoredPath=null; /* default value */
		
		
		if (args.length >= 4) { //if arguments are complete
			dataSourceType = args[0].toLowerCase().replace(" ","");
			id = args[1].toLowerCase().replace(" ","");
			train = Boolean.parseBoolean(args[2].toLowerCase().replace(" ",""));
			test=Boolean.parseBoolean(args[3].toLowerCase().replace(" ",""));
			if(args.length > 4) {
				modelStoredPath=args[4];
			}
			System.out.println("Data source: "+dataSourceType + "\tId: " + id+ "\tTrain: "+train+ "\tTest: "+test);

		}else {
			System.out.println("Less that 4 arguments provided, default parameters are considered.");
		}
		
		// Clean logs and db
		InfluxDBConnector.init();
		InfluxDBConnector.cleanDB();
		Log.clearLogs();
		String[] origins = new String[1];
		origins[0] = id;
		RedisConnector.cleanKeys(origins);	
		//RedisConnector.cleanModel(id);
		
		Vector<TimeRecord> records;
		if(train) {
			Log.displayLogTrain.info("\n#### ["+id+"] New Model (trained offline from "+dataSourceType+") ####\n ");
			
			Trainer trainer = new Trainer();
			records = trainer.trainAll(dataSourceType, id, modelStoredPath);
			GlobalUtils.printSet(records,false);
			GlobalUtils.saveRecordsToFile(records, "./"+id+"_resultsTrain.csv", ";", false);
		}
		
		if(test) {
			Log.displayLogTest.info("\n#### ["+id+"] New Model (run offline from "+dataSourceType+") ####\n ");

			ModelRunner model = new ModelRunner();
			records= model.run(dataSourceType, id, modelStoredPath);
			GlobalUtils.printSet(records, true);
			GlobalUtils.saveRecordsToFile(records, "./"+id+"_resultsTest.csv", ";", true);
		}
	}

}