package cea.streamer.algs;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.RedisConnector;

public class HoeffdingTreeClassifier extends MLalgorithms {

	public HoeffdingTreeClassifier() {
		updateModel = true;
	}
	
	@Override
	public void learn(Vector<TimeRecord> data, String id) {
		//check later why we have one record stuck
		if(data.size()==1)
			return;
		//this method represent the training/learning phase

		// the path of the training file to perform the training from python
		String learningFile = new GlobalUtils().getAbsoluteBaseProjectPath() + "src/main/resources/algs/HoeffdingTreeClassifierTrain.py"
				+ " " + id + " "+ RedisConnector.getRedisIP() + " " + RedisConnector.getRedisPort();
		// push the data to Redis to be available for the python training file
		RedisConnector.dataToRedis(data,RedisConnector.DATATRAIN_TAG,id);
		// execute the training phase
		CodeConnectors.execPyFile(learningFile, id);
	}
	
	public void run(Vector<TimeRecord> data, String id) {
		//this method represent the testing/prediction phase

		// the path of the testing file to perform the prediction from python
		String testFile = new GlobalUtils().getAbsoluteBaseProjectPath() + "src/main/resources/algs/HoeffdingTreeClassifierTest.py"
				+ " " + id + " "+ RedisConnector.getRedisIP() + " " + RedisConnector.getRedisPort();
		// push the data to Redis to be available for the python testing file
		RedisConnector.dataToRedis(data,RedisConnector.DATATEST_TAG,id);
		// execute the testing phase
		String result = CodeConnectors.execPyFile(testFile, id);
		data = RedisConnector.retrieveOutput(data,id);
	}

}
