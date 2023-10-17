package cea.federated.server;

import cea.federated.server.aggregation.AggregationStrategy;
import cea.federated.server.grpc_server.ClientProxyGRPC;
import cea.federated.util.FederatedLog;
import cea.util.GlobalUtils;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.RedisConnector;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import static java.lang.Double.parseDouble;

/**
 * Implementation of the server class.
 */
public class Server {

    /**
     * Logger of the server.
     */
    public final Logger logger;

    /**
     * Manager of the client proxies.
     */
    public final ClientManager client_manager;

    /**
     * Aggregation strategy to follow during the fitting and evaluation steps.
     */
    private AggregationStrategy aggregation_strategy;

    /**
     * History of the FL experience results.
     */
    private final Map<String, Double> history;

    /**
     * Model parameters of the server.
     */
    private String parameters;

    /**
     * Port of the server.
     */
    public int server_port;

    /**
     * Number of rounds in the FL loops.
     */
    private Integer n_rounds;

    /**
     * Number of clients to sample during the fitting and evaluation steps.
     */
    private Integer n_clients;

    /**
     * Minimum number of clients required to run a sampling.
     */
    private Integer min_n_clients;

    /**
     * Boolean to active/deactivate the client fitting step.
     */
    private boolean do_client_fit_step;

    /**
     * Boolean to active/deactivate the client evaluation step.
     */
    private boolean do_client_eval_step;
    /**
     * Boolean to active/deactivate the server evaluation step.
     */
    private boolean do_server_eval_step;

    /**
     * The path of the python file used for the server evaluation step.
     */
    private String eval_python_file_path;

    /**
     * Constructor of the server object.
     * @param id Identifier of the problem.
     * @throws Exception If an error happens during the FL properties file loading.
     */
    public Server(String id) throws Exception {
        this.logger = FederatedLog.create_logger(this.toString(), "SERVER");
        this.client_manager = new ClientManager();
        this.history = new HashMap<>();
        readPropertiesFiles(id);
    }

    /**
     * Reads the federated learning properties file.
     * @param id Identifier of the problem.
     * @throws Exception If an error happens during the FL properties file loading.
     */
    private void readPropertiesFiles(String id) throws Exception {

        logger.info("Reading properties files");

        Properties properties = new Properties();
        try (InputStream props = new FileInputStream(GlobalUtils.resourcesPathPropsFiles + id + "/federated.props")) {
            properties.load(props);
        } catch (Exception e) {
            logger.warn("Can't find federated.props file!");
            throw e;
        }

        try {
            this.server_port = Integer.parseInt(properties.getProperty("server.port"));
            this.n_rounds = Integer.parseInt(properties.getProperty("n.rounds"));
            Class<?> aggregation_class_name = Class.forName("cea.federated.server.aggregation."+properties.getProperty("aggregation.strategy"));
            this.aggregation_strategy = (AggregationStrategy) aggregation_class_name.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.warn("Missing mandatory properties in the federated.props file!");
            throw e;
        }

        if (properties.containsKey("n.clients")) {
            this.n_clients = Integer.parseInt(properties.getProperty("n.clients"));
        } else {
            this.n_clients = -1;
        }

        if (properties.containsKey("min.n.clients")) {
            this.min_n_clients = Integer.parseInt(properties.getProperty("min.n.clients"));
        } else {
            this.min_n_clients = 1;
        }

        if (properties.containsKey("do.client.fit.step")) {
            this.do_client_fit_step = Boolean.parseBoolean(properties.getProperty("do.client.fit.step"));
        } else {
            this.do_client_fit_step = true;
        }

        if (properties.containsKey("do.client.eval.step")) {
            this.do_client_eval_step = Boolean.parseBoolean(properties.getProperty("do.client.eval.step"));
        } else {
            this.do_client_eval_step = true;
        }

        if (properties.containsKey("do.server.eval.step") & properties.containsKey("eval.python.file.path")) {
            this.do_server_eval_step = Boolean.parseBoolean(properties.getProperty("do.server.eval.step"));
            this.eval_python_file_path = properties.getProperty("eval.python.file.path");
        } else {
            this.do_server_eval_step = false;
            this.eval_python_file_path = null;
        }

    }

    /**
     * Launches the FL loop. It runs sequentially the actions performed in each round,
     * as for example the initialization step, the fitting step, the evaluation step,
     * the server evaluation step and the passive clients model updating step.
     */
    public void fl_loop() {

        logger.warn("FL process begin");

        try {
            logger.warn("Model initialization");
            parameters = initialize_parameters();
        } catch (Exception e) {
            return;
        }

        for(int round=1; round<n_rounds+1; round++) {

            logger.warn("");
            logger.warn("ROUND " + round);
            logger.warn("");

            if (do_client_fit_step) {
                logger.warn("Fitting step");
                parameters = fit_step();
            }

            if (do_client_eval_step) {
                logger.warn("Evaluation step");
                Double loss = eval_step();
                history.put("clients_aggregated_loss_"+round, loss);
            }

            if (do_server_eval_step) {
                logger.warn("Server evaluation step");
                Double server_loss = server_eval_step();
                history.put("server_loss_"+round, server_loss);
            }

            if (!this.client_manager.passiveClients.isEmpty()) {
                logger.warn("Updating passive clients model");
                passive_clients_model_updating_step();
            }

        }

        logger.warn("");
        logger.warn("FL process finished");
        logger.info(history);

    }

    /**
     * Runs the initialisation step. It samples one client, sends the initialisation
     * request, waits for the response and finally processes it.
     * @return Initialized model parameters.
     * @throws Exception In case of errors during the step.
     */
    private String initialize_parameters() throws Exception {
        List<ClientProxyGRPC> selected_client = client_manager.sample_clients(1, 1);
        ClientProxyGRPC client = selected_client.get(0);
        client.send_init_request();
        client_manager.wait_for_client_messages(selected_client);
        try {
            return client.receive_init_response();
        } catch (Exception e) {
            client.logger.warn("CLIENT ERROR during the initialization step");
            FederatedLog.errorToLogger(client.logger, e);
            throw e;
        }
    }

    /**
     * Disconnects all the normal and passive clients.
     */
    public void disconnect_all_clients() {
        logger.warn("Disconnect all clients");
        for(ClientProxyGRPC client : client_manager.clients.values()) {
            client.send_disconnection_request();
        }
        for(ClientProxyGRPC passive_client : client_manager.passiveClients.values()) {
            passive_client.send_disconnection_request();
        }
    }

    /**
     * Runs the fitting step. It samples clients, sends the fitting requests, waits
     * for all responses, collects all the updated models parameters and aggregates
     * them by following the {@code aggregation_strategy}.
     * @return The aggregated model parameters.
     */
    private String fit_step() {

        List<ClientProxyGRPC> selected_clients = client_manager.sample_clients(n_clients, min_n_clients);

        // SEND FIT REQUEST TO CLIENTS
        for(ClientProxyGRPC client : selected_clients) {
            client.send_fit_request(parameters);
        }

        // WAIT ALL CLIENT RESPONSES
        client_manager.wait_for_client_messages(selected_clients);

        // RECEIVE FIT RESPONSE FROM CLIENTS AND ADD CLIENT PARAMETERS
        ArrayList<String> list_parameters = new ArrayList<>();
        for(ClientProxyGRPC client : selected_clients) {
            try {
                String client_parameters = client.receive_fit_response();
                list_parameters.add(client_parameters);
            } catch (Exception e) {
                client.logger.warn("CLIENT ERROR during the training step");
                FederatedLog.errorToLogger(client.logger, e);
            }
        }

        // AGGREGATE CLIENT PARAMETERS
        try {
            this.logger.warn(aggregation_strategy.getClass().getName()+" - parameters aggregation");
            String aggregated_parameters = aggregation_strategy.aggregate_fit(list_parameters);
            this.logger.trace(aggregation_strategy.getClass().getName()+" - parameters aggregation -> " + aggregated_parameters);
            return aggregated_parameters;
        } catch (Exception e) {
            this.logger.warn("SERVER ERROR during the aggregation of the training step");
            FederatedLog.errorToLogger(this.logger, e);
            return parameters;
        }

    }

    /**
     * Runs the evaluation step. It samples clients, sends the evaluation requests,
     * waits for all responses, collects all the clients losses and aggregates them
     * by following the {@code aggregation_strategy}. For the moment, the loss is
     * the only one metric that is returned by the evaluation step, but it will soon
     * be extended to others.
     * @return The aggregated loss.
     */
    private Double eval_step() {

        List<ClientProxyGRPC> selected_clients = client_manager.sample_clients(n_clients, min_n_clients);

        // SEND EVALUATE REQUEST TO CLIENTS
        for(ClientProxyGRPC client : selected_clients) {
            client.send_evaluate_request(parameters);
        }

        // WAIT ALL CLIENT RESPONSES
        client_manager.wait_for_client_messages(selected_clients);

        // RECEIVE EVALUATE RESPONSE FROM CLIENTS AND ADD CLIENT LOSSES
        ArrayList<Double> list_loss = new ArrayList<>();
        for(ClientProxyGRPC client : selected_clients) {
            try {
                double client_loss = client.receive_evaluate_response();
                list_loss.add(client_loss);
            } catch (Exception e) {
                client.logger.warn("CLIENT ERROR during the evaluation step");
                FederatedLog.errorToLogger(client.logger, e);
            }
        }

        // AGGREGATE CLIENT LOSSES
        try {
            double aggregated_loss = aggregation_strategy.aggregate_evaluate(list_loss);
            this.logger.warn(aggregation_strategy.getClass().getName()+" - losses aggregation -> " + aggregated_loss);
            return aggregated_loss;
        } catch (Exception e) {
            this.logger.warn("SERVER ERROR during the aggregation of the evaluation step");
            FederatedLog.errorToLogger(this.logger, e);
            return null;
        }

    }

    /**
     * Runs the server evaluation step. It evaluates the model by executing the python
     * file with the tag 'evaluate'. Same as the evaluation step, the loss is the only
     * one metric that is returned for the moment, but it will soon be extended to others.
     * @return The loss of the server evaluation.
     */
    private Double server_eval_step() {
        String tag = "evaluate";
        RedisConnector.storeModelInRedis("server", parameters);
        CodeConnectors.execPyFile(eval_python_file_path, "server", tag, "pickle");
        List<String> metrics = RedisConnector.getMetricsFromRedis("server");
        Double server_loss = parseDouble(metrics.get(0));
        this.logger.warn("Server loss -> " + server_loss);
        return server_loss;
    }

    /**
     * Runs the passive clients model updating step. It sends the model updating
     * requests to all passive clients, waits for all responses, and verifies all
     * the confirmations of receipt.
     */
    private void passive_clients_model_updating_step() {

        List<ClientProxyGRPC> passive_clients = new ArrayList<>(client_manager.passiveClients.values());

        // SEND EVALUATE REQUEST TO CLIENTS
        for(ClientProxyGRPC passive_client : passive_clients) {
            passive_client.send_model_parameters(parameters);
        }

        // WAIT ALL CLIENT RESPONSES
        client_manager.wait_for_client_messages(passive_clients);

        // RECEIVE CONFIRMATION RESPONSE FROM PASSIVE CLIENTS
        for(ClientProxyGRPC passive_client : passive_clients) {
            try {
                passive_client.receive_model_parameters_confirmation();
            } catch (Exception e) {
                passive_client.logger.warn("CLIENT ERROR during the passive clients model updating step");
                FederatedLog.errorToLogger(passive_client.logger, e);
            }
        }
    }

}
