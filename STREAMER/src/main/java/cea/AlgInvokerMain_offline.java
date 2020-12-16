package cea;

import java.util.Vector;

import cea.streamer.ModelRunner;
import cea.streamer.Trainer;
import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.Log;

/**
 * Class that allows to train the water pollution model with all the data from the datafile
 *
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
	 * @param (4) [Optional] Path from where to store/retreive the trained model
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String dataSourceType = "file"; /* default value */
		String origin = "default"; /* default value */	
		boolean train = true; /* default value */
		boolean test = true; /* default value */
		String modelStoredPath=null; /* default value */
		
		if (args.length >= 4) { //if arguments are complete
			dataSourceType = args[0].toLowerCase().trim();
			origin = args[1].toLowerCase().trim();;
			train = Boolean.parseBoolean(args[2].toLowerCase().trim());
			test=Boolean.parseBoolean(args[3].toLowerCase().trim());
			if(args.length > 4) {
				modelStoredPath=args[4];
			}
		} 
		
		Vector<TimeRecord> records;
		if(train) {
			Log.displayLogTrain.info("\n #### New Model for " + origin + " (trained offline from "+dataSourceType+") ####\n ");
			
			Trainer trainer = new Trainer();
			records = trainer.trainAll(dataSourceType, origin, modelStoredPath);
			GlobalUtils.printSet(records,false);
			GlobalUtils.saveRecordsToFile(records, "./"+origin+"_resultsTrain.csv", ";", false);
		}
		
		if(test) {
			Log.displayLogTest.info("\n #### Model of " + origin + " (run offline from "+dataSourceType+") ####\n ");

			ModelRunner model = new ModelRunner();
			records= model.run(dataSourceType, origin, modelStoredPath);
			GlobalUtils.printSet(records, true);
			GlobalUtils.saveRecordsToFile(records, "./"+origin+"_resultsTest.csv", ";", true);
		}
	}

}