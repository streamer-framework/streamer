package cea.util.prepostprocessors;

import java.util.Vector;

import cea.streamer.core.TimeRecord;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.RedisConnector;

public class PreProcessorS4W extends PrePostProcessor{

	@Override
	public Vector<TimeRecord> preprocess(Vector<TimeRecord> records, String id) {
		RedisConnector.dataToRedis(records, "data"+id);
		CodeConnectors.execRFile("./src/main/resources/algs/smart4water/preprocessingS4W.R", id);
		Vector<TimeRecord> cleanRecords = RedisConnector.redisToRecords("cleandata"+id, records.get(0).getName(), id);
		if (cleanRecords.size() >0) {
			int j = 0;
			while (!cleanRecords.get(0).getTimeStamp().equals(records.get(j).getTimeStamp())) {
				j++;
			}
			for (int i = 0; i < cleanRecords.size(); i++) {
				cleanRecords.get(i).setTarget(records.get(i + j).getTarget());
			}
		}
		return cleanRecords;
	}
	
}
