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
		//System.out.println(" train " + data);
		// the path of the training file to perform the training from python
		String learningFile = new GlobalUtils().getAbsoluteBaseProjectPath() + "/src/main/resources/algs/HoeffdingTreeClassifierTrain.py";
		// push the data to Redis to be available for the python training file
		RedisConnector.dataToRedis(data, "datatrain"+id);
		// execute the training phase
		CodeConnectors.execPyFile(learningFile, id);
	}
	
	public void run(Vector<TimeRecord> data, String id) {
		//this method represent the testing/prediction phase
		//System.out.println(" test " + data);
		//System.out.println(data.size() + "");
		// the path of the testing file to perform the prediction from python
		String testFile = new GlobalUtils().getAbsoluteBaseProjectPath() + "/src/main/resources/algs/HoeffdingTreeClassifierTest.py";
		// push the data to Redis to be available for the python testing file
		RedisConnector.dataToRedis(data, "datatest"+id);
		// execute the testing phase
		String result = CodeConnectors.execPyFile(testFile, id);
		data = RedisConnector.retreiveOutput(data, id);
	}

}
