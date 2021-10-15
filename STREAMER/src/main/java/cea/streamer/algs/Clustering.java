package cea.streamer.algs;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.RedisConnector;

public class Clustering extends MLalgorithms{
	
	public Clustering(String [] args) {
		this();
	}
	
	public Clustering() {
		super();
	}
	
	@Override
	public void learn(Vector<TimeRecord> data, String id) {		
		String learningFile = new GlobalUtils().getAbsoluteBaseProjectPath()+"/src/main/resources/algs/clustering.R";		
//		String learningFile = "./src/main/resources/algs/clustering.R";
		RedisConnector.dataToRedis(data, "dataclus",id);
		CodeConnectors.execRFile(learningFile, id);		
		RedisConnector.retrieveOutput(data,id);//store clusters in time records
	}

}
