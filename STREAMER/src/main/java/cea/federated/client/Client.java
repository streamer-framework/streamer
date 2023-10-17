package cea.federated.client;

import cea.federated.client.grpc_client.ClientGRPC;
import cea.federated.util.FederatedLog;
import cea.streamer.core.TimeRecord;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.InfluxDBConnector;
import cea.util.connectors.RedisConnector;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.log4j.Logger;
import java.util.List;
import java.util.Vector;
import static java.lang.Double.parseDouble;

/**
 * Implementation of the distributed layer built on top of the STREAMER instance.
 * It allows a connection with the server to get its instructions.
 */
public class Client {

    /**
     * Logger of the client.
     */
    public final Logger logger;

    /**
     * Identifier of the client.
     */
    public final String id;

    /**
     * Boolean that determines if the client is online or not. In case of offline,
     * the rest of STREAMER modules won't be used.
     * (default=false)
     */
    public boolean isOnline;

    /**
     * Boolean that determines if the client is passive or not. if passive, the training
     * is disabled, and it only retrieves the updated model and uses it for inference.
     * (default=false)
     */
    public boolean isPassive;

    /**
     * Database of the records. Only used if isOnline=true.
     * (default=null)
     */
    public Vector<TimeRecord> recordsDB;

    /**
     * The way of storing the data for the ml python algorithm.
     * (default=redis, option=redis/pickle)
     */
    public String data_type;

    /**
     * Path of the ml python algorithm.
     */
    public String python_file_path;

    /**
     * Constructor of the object 'client'.
     * @param id Identifier of the client.
     * @param python_file_path Path of the ml python algorithm.
     */
    public Client(String id, String python_file_path) {
        this.logger = FederatedLog.create_logger(this.toString(), id);
        this.id = id;
        this.python_file_path = python_file_path;
        this.isOnline = false;
        this.isPassive = false;
        this.data_type = "redis";
        this.recordsDB = null;
    }

    /**
     * Starts the client gRPC interface. Then, the client will listen to the server
     * orders and follow its instruction. The server will determine which step to
     * perform, as for instances initialize the model, fit it, evaluate it, or just
     * replace it.
     * @param server_address Address of the server needed to establish the connection.
     */
    public void start_client(String server_address) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(server_address).usePlaintext().build();
        ClientGRPC grpc_client = new ClientGRPC(this, channel);
        grpc_client.grpc_connection();
        logger.warn("END Client.java");
        channel.shutdown();
    }

    /**
     * Initializes the model by executing the python file with the tag 'init'.
     * @return Initialized model.
     */
    public Object initialize_client() {
        String tag = "init";
        CodeConnectors.execPyFile(python_file_path, id, tag, data_type);
        return RedisConnector.getModelFromRedis(id);
    }

    /**
     * Fits the model by executing the python file with the tag 'train'.
     * @param parameters Parameters of the model.
     * @return Fitted Model.
     */
    public Object fit_client(String parameters) {
        String tag = "train";
        if(isOnline){
            recordsDB = InfluxDBConnector.getRecordsDB(id, -1, true);
            RedisConnector.dataToRedis(recordsDB, "data"+tag, id);
        }
        RedisConnector.storeModelInRedis(id, parameters);
        CodeConnectors.execPyFile(python_file_path, id, tag, data_type);
        return RedisConnector.getModelFromRedis(id);
    }

    /**
     * Replaces/updates the actual client model parameters by storing the server
     * model parameters in redis.
     * @param parameters Parameters of the model.
     */
    public void update_model(String parameters) {
        RedisConnector.storeModelInRedis(id, parameters);
    }

    /**
     * Evaluates the model by executing the python file with the tag 'evaluate'.
     * For the moment, the loss is the only one metric that is returned by the
     * evaluation step, but it will soon be extended to others.
     * @param parameters Parameters of the model.
     * @return Loss information.
     */
    public double evaluate_client(String parameters) {
        String tag = "evaluate";
        if(isOnline){
            RedisConnector.dataToRedis(recordsDB, "data"+tag, id);
        }
        RedisConnector.storeModelInRedis(id, parameters);
        CodeConnectors.execPyFile(python_file_path, id, tag, data_type);
        List<String> metrics = RedisConnector.getMetricsFromRedis(id);
        return parseDouble(metrics.get(0));
    }

}
