package cea.streamer.algs;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.RedisConnector;
import java.util.Vector;

/**
 * Federated learning algorithm launcher for the CMAPSS use case.
 * The learn() method is empty because the fitting step is controlled by the server.
 */
public class CMAPSSAlg extends MLalgorithms {

    String pythonFile = new GlobalUtils().getAbsoluteBaseProjectPath() + "/src/main/resources/algs/cmapss/alg_launcher.py";

    @Override
    public void learn(Vector<TimeRecord> data, String id) {}

    public void run(Vector<TimeRecord> data, String id) {
        String tag = "inference";
        RedisConnector.dataToRedis(data, "data", id);
        CodeConnectors.execPyFile(pythonFile, id, tag, "redis");
        data = RedisConnector.retrieveOutput(data, id);
    }

}