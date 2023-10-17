package cea.federated.server.grpc_server;

import cea.federated.*;
import cea.federated.util.FederatedLog;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implementation of the gRPC clients proxies.
 * Clients proxies are defined at the server level as abstract clients with which
 * the server can interact. It can send them requests, receive results from them
 * or disconnect them from the network. Each client proxy represents one single
 * client that could be solicited by the server during the different phases.
 */
public class ClientProxyGRPC {

    /**
     * Logger of the client proxy.
     */
    public final Logger logger;

    /**
     * Identifier of the client proxy.
     */
    public final Integer id;

    /**
     * Boolean that determines if the client is connected or not.
     * (default=true)
     */
    public boolean is_alive;

    /**
     * Boolean that determines if the client is passive or not.
     * (default=false, but the boolean will be updated directly once
     * the client connection request has been processed.)
     */
    public boolean is_passive;

    /**
     * Queue of the messages that coming from the server and need to be sent
     * to the client.
     */
    public Queue<ServerMessage> queue_server_message;

    /**
     * Queue of the messages that coming from the client and need to be processed
     * by the server.
     */
    public Queue<ClientMessage> queue_client_message;

    /**
     *
     * @param id Identifier of the client.
     */
    public ClientProxyGRPC(Integer id) {
        this.id = id;
        this.logger = FederatedLog.create_logger(this.toString(),"CLIENT " + id);
        this.is_alive = true;
        this.is_passive = false;
        this.queue_server_message = new LinkedList<>();
        this.queue_client_message = new LinkedList<>();
    }

    /**
     * Creates and adds a model initialisation request to the server messages queue.
     */
    public void send_init_request() {
        ServerMessage server_message = ServerMessage.newBuilder().setInitRequest(true).build();
        queue_server_message.add(server_message);
        send_alert();
    }

    /**
     * Creates and adds a model fitting request to the server messages queue.
     * @param server_parameters Model parameters that will be sent to the client for
     *                          an ML model fitting process.
     */
    public void send_fit_request(String server_parameters) {
        logger.trace("Send parameters -> " + server_parameters);
        FitRequest fit_request = FitRequest.newBuilder().setParameters(server_parameters).build();
        ServerMessage server_message = ServerMessage.newBuilder().setFitRequest(fit_request).build();
        queue_server_message.add(server_message);
        send_alert();
    }

    /**
     * Creates and adds a model evaluation request to the server messages queue.
     * @param server_parameters Model parameters that will be sent to the client for
     *                          an ML model evaluation process.
     */
    public void send_evaluate_request(String server_parameters) {
        logger.trace("Send parameters -> " + server_parameters);
        EvaluateRequest evaluate_request = EvaluateRequest.newBuilder().setParameters(server_parameters).build();
        ServerMessage server_message = ServerMessage.newBuilder().setEvaluateRequest(evaluate_request).build();
        queue_server_message.add(server_message);
        send_alert();
    }

    /**
     * Creates and adds a message with the server model parameters to the server messages queue.
     * It permits to send the server model parameters without requesting any other actions (used
     * especially for the 'OfflineClient's).
     * @param server_parameters Model parameters that will be sent to the client.
     */
    public void send_model_parameters(String server_parameters) {
        logger.trace("Send parameters -> " + server_parameters);
        ModelParameters model_parameters = ModelParameters.newBuilder().setParameters(server_parameters).build();
        ServerMessage server_message = ServerMessage.newBuilder().setModelParameters(model_parameters).build();
        queue_server_message.add(server_message);
        send_alert();
    }

    /**
     * Creates and adds a disconnection request to the server messages queue.
     */
    public void send_disconnection_request() {
        ServerMessage server_message = ServerMessage.newBuilder().setEndConnection(true).build();
        queue_server_message.add(server_message);
        send_alert();
    }

    /**
     * Receives and processes the initialisation response that coming from the client.
     * @return Client model parameters that have been initialized.
     * @throws Exception If an errors happens during the processing of the message.
     */
    public String receive_init_response() throws Exception {
        if(!queue_client_message.isEmpty()){
            ClientMessage client_message = queue_client_message.poll();
            if(client_message.hasInitReply()) {
                String client_parameters = client_message.getInitReply().getParameters();
                logger.trace("Receive parameters -> " + client_parameters);
                return client_parameters;
            } else {
                throw new Exception("The init response is not well configured!");
            }
        } else {
            throw new Exception("No init response received!");
        }
    }

    /**
     * Receives and processes the fitting response that coming from the client.
     * @return Client model parameters that have been updated.
     * @throws Exception If an errors happens during the processing of the message.
     */
    public String receive_fit_response() throws Exception {
        if(!queue_client_message.isEmpty()){
            ClientMessage client_message = queue_client_message.poll();
            if(client_message.hasFitReply()) {
                String client_parameters = client_message.getFitReply().getParameters();
                logger.trace("Receive parameters -> " + client_parameters);
                return client_parameters;
            } else {
                throw new Exception("The fit response is not well configured!");
            }
        } else {
            throw new Exception("No fit response received!");
        }
    }

    /**
     * Receives and processes the evaluation response that coming from the client.
     * For the moment, the loss is the only one metric that is returned by the
     * evaluation step, but it will soon be extended to others.
     * @return Loss information that have been generated during the evaluation step.
     * @throws Exception If an errors happens during the processing of the message.
     */
    public double receive_evaluate_response() throws Exception {
        if(!queue_client_message.isEmpty()){
            ClientMessage client_message = queue_client_message.poll();
            if(client_message.hasEvaluateReply()) {
                double client_loss = client_message.getEvaluateReply().getLoss();
                logger.debug("Receive loss -> " + client_loss);
                return client_loss;
            } else {
                throw new Exception("The evaluate response is not well configured!");
            }
        } else {
            throw new Exception("No evaluate response received!");
        }
    }

    /**
     * Receives and processes the acknowledgement of receipt of the model parameters
     * that coming for the client.
     * @throws Exception If an errors happens during the processing of the message.
     */
    public void receive_model_parameters_confirmation() throws Exception {
        if(!queue_client_message.isEmpty()){
            ClientMessage client_message = queue_client_message.poll();
            if(client_message.hasModelParametersReply()) {
                boolean model_parameters_confirmation = client_message.getModelParametersReply().getConfirmation();
                logger.debug("Receive model parameters confirmation -> " + model_parameters_confirmation);
            } else {
                throw new Exception("The model parameters confirmation response is not well configured!");
            }
        } else {
            throw new Exception("No model parameters confirmation response received!");
        }
    }

    /**
     * Method used in the participateToNetwork() method of the ServerGRPC file.
     * Permits to pause the client communication process to ensure the
     * synchronization of all clients by waiting the next server request.
     * @throws InterruptedException In case of interruption.
     */
    public synchronized void wait_for_alert() throws InterruptedException {
        wait();
    }

    /**
     * Unblocks the wait_for_alert() method when the new server request has finished
     * being set up. It will also be called when the communication is completed
     * or when an error happened to unblock and end the process.
     */
    public synchronized void send_alert() {
        notifyAll();
    }

}
