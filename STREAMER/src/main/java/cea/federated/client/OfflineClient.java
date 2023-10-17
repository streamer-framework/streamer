package cea.federated.client;

import cea.federated.util.FederatedLog;
import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.connectors.RedisConnector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;

/**
 * Implementation of the offline client overlay. As it is offline, the process
 * will be directly invoked without using the rest of STREAMER modules. This
 * implementation compensates the missing elements of the rest of STREAMER modules
 * that are necessary for the functioning of a client object.
 */
public class OfflineClient extends Client {

    /**
     * Address of the server needed to establish the connection.
     */
    public String server_address;

    /**
     * Problem type that indicates the type of data.
     */
    private String problem_type;

    /**
     * Path of the data.
     */
    private Path data_path;

    /**
     * Boolean that determines if the data file contains a header.
     */
    private boolean containsHeader;

    /**
     * Constructor of the object "offline client". It instantiates a client, reads
     * the FL properties file, cleans the redis databases, and prepares the data in
     * redis if the data_type=redis.
     * @param id_problem Identifier of the problem.
     * @throws Exception If an error happens during the properties files reading
     * or the use of RedisConnector.
     */
    public OfflineClient(String id_problem) throws Exception {

        super(id_problem, null);

        try {
            logger.info("Reading FL properties file");
            readFLPropertiesFile(id_problem);
        } catch (Exception e) {
            logger.warn("CLIENT ERROR during readFLPropertiesFiles()");
            FederatedLog.errorToLogger(logger, e);
            throw e;
        }

        try {
            RedisConnector.cleanModel(id);
            RedisConnector.cleanKeys(new String[]{id});
        } catch (Exception e) {
            logger.warn("CLIENT ERROR with Redis");
            FederatedLog.errorToLogger(logger, e);
            throw e;
        }

        if(Objects.equals(data_type, "redis")) {
            logger.warn("Redis data preparation process");
            redisDataPreparationProcess(id_problem);
        }

    }

    /**
     * Reads the federated learning properties file (to get the server_address,
     * python_file_path and data_type).
     * @param id_problem Identifier of the problem.
     * @throws Exception If an error happens during the FL properties file loading.
     */
    private void readFLPropertiesFile(String id_problem) throws Exception {

        Properties properties = new Properties();

        try (InputStream props = new FileInputStream(GlobalUtils.resourcesPathPropsFiles + id_problem + "/federated.props")) {
            properties.load(props);
            server_address = properties.getProperty("server.address");
            python_file_path = properties.getProperty("python.file.path");
            data_type = properties.getProperty("data.type");
        } catch (Exception e) {
            throw new Exception("federated.props ERROR!", e);
        }

        if (properties.containsKey("is.passive")) {
            this.isPassive = Boolean.parseBoolean(properties.getProperty("is.passive"));
        }

    }

    /**
     * Prepares the data in redis. It reads some properties, sets up the recordDB,
     * and sends it to redis.
     * @param id_problem Identifier of the problem.
     * @throws Exception If an error happens during the streaming/algs properties files reading,
     * or during the set-up of the recordsDB.
     */
    private void redisDataPreparationProcess(String id_problem) throws Exception {
        try {
            logger.info("Reading properties files to prepare the implementation of the records database");
            readPropertiesFiles(id_problem);
        } catch (Exception e) {
            logger.warn("CLIENT ERROR during readPropertiesFiles()");
            FederatedLog.errorToLogger(logger, e);
            throw e;
        }

        try {
            logger.info("Setting up records database");
            this.recordsDB = set_up_recordsDB();
        } catch (Exception e) {
            logger.warn("CLIENT ERROR during set_up_recordsDB()");
            FederatedLog.errorToLogger(logger, e);
            throw e;
        }

        logger.info("Push records database to redis");
        RedisConnector.dataToRedis(recordsDB, "data", id);
    }

    /**
     * Reads the streaming and algs properties files (to get the problem_type,
     * containsHeader and data_path).
     * @param id_problem Identifier of the problem.
     * @throws Exception If an error happens during the streaming/algo properties files loading.
     */
    private void readPropertiesFiles(String id_problem) throws Exception {

        Properties properties = new Properties();

        try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + id_problem + "/streaming.props")) {
            properties.load(props);
            problem_type = (GlobalUtils.packageTimeRecords + ".") + properties.getProperty("problem.type").replace(" ","");
            containsHeader = Boolean.parseBoolean(properties.getProperty("containsHeader").replace(" ","").toLowerCase());
        } catch (Exception e) {
            throw new Exception("streaming.props ERROR!", e);
        }

        try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles + id_problem + "/algs.props")) {
            properties.load(props);
            data_path = Paths.get(properties.getProperty("training.source") );
        } catch (Exception e) {
            throw new Exception("algs.props ERROR!", e);
        }

    }

    /**
     * Sets up the record database. It reads the data file according to the problem_type
     * and stores the records with the associated (problem_type)Record class.
     * @return Records database.
     * @throws Exception If an error happens during the data file reading.
     */
    private Vector<TimeRecord> set_up_recordsDB() throws Exception {
        Vector<TimeRecord> records = new Vector<> ();
        Class<?> recC = Class.forName(problem_type+"Record");
        TimeRecord recObj;

        BufferedReader br = Files.newBufferedReader(data_path);

        if(containsHeader) {
            br.readLine();
        }

        String line;
        line = br.readLine();
        while(line!=null){
            line = line.replaceAll("\"", "").replaceAll("\t", "");
            recObj = (TimeRecord)recC.getDeclaredConstructor().newInstance();
            recObj.fill(id, line);
            records.add(recObj);
            line = br.readLine();
        }
        br.close();

        return records;
    }

}
