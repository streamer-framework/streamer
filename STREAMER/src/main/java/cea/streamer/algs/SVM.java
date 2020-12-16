package cea.streamer.algs;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.GUIConnector;
import cea.util.connectors.RedisConnector;

/**
 * Class that implements SVM model
 *
 */
public class SVM extends MLalgorithms{

	
	public void learn(Vector<TimeRecord> data, String id) {
		String learningFile = new GlobalUtils().getAbsoluteBaseProjectPath()+"src/main/resources/algs/svmTrain.R";
		//String learningFile = "./src/main/resources/algs/svmTrain.R";
		RedisConnector.dataToRedis(data, "datatrain"+id);
		CodeConnectors.execRFile(learningFile,id);
	}
	
	
	public void run(Vector<TimeRecord> data, String id) {
		/* The data are stored temporally in a JSON file (graphical interface)*/

		if (data.size()>0){
			GUIConnector.dataToJSON(data, "./json/data" + id + ".json");
		}

		/* Test part */
		String testFile = new GlobalUtils().getAbsoluteBaseProjectPath()+"src/main/resources/algs/svmTest.R";
		//String testFile = "./src/main/resources/algs/svmTest.R";
		RedisConnector.dataToRedis(data, "datatest"+id);
		CodeConnectors.execRFile(testFile,id);
	}


}
