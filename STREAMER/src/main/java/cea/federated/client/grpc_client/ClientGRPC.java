package cea.federated.client.grpc_client;

import cea.federated.*;
import cea.federated.client.Client;
import cea.federated.util.FederatedLog;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implementation of the client gRPC interface.
 */
public class ClientGRPC {

    /**
     * Client that creates this gRPC interface 'clientGRPC'.
     */
    private final Client client;

    /**
     * Asynchronous client stub that makes non-blocking calls to the server, where the response
     * is returned asynchronously. It is linked to the 'FLComGrpc' service of the server.
     * "On the client side, the client has a local object known as stub (for some languages,
     * the preferred term is client) that implements the same methods as the service. The
     * client can then just call those methods on the local object, and the methods wrap
     * the parameters for the call in the appropriate protocol buffer message type, send
     * the requests to the server, and return the server’s protocol buffer responses."
     * For more information, see https://grpc.io/docs/what-is-grpc/core-concepts/.
     */
    private final FLComGrpc.FLComStub asyncStub;

    /**
     * Interface for the client to send (client) messages to the server.
     */
    private StreamObserver<ClientMessage> requestObserver;

    /**
     * Queue of the client messages that will be sent to the server.
     */
    Queue<ClientMessage> queue = new LinkedList<>();

    /**
     * Constructor of the object 'clientGRPC'.
     * @param client Client that creates this object.
     * @param channel A gRPC channel provides a connection to a gRPC server on a specified host
     *                and port. It is used when creating a client stub.For more information,
     *                see https://grpc.io/docs/what-is-grpc/core-concepts/.
     */
    public ClientGRPC(Client client, Channel channel) {
        this.client = client;
        this.asyncStub = FLComGrpc.newStub(channel);
    }

    /**
     * Starts the gRPC connection by calling the bidirectional streaming RPC 'participateToNetwork()'
     * method on the 'asyncStub'. It defines the interface for the server during the communication
     * and manages the client message sending.
     */
    public void grpc_connection() {

        /*
         * The new StreamObserver<ServerMessage>() is the interface for the server during the
         * communication made with the method 'participateToNetwork()'. It defines how the client
         * should react to server actions. In this interface, we:
         * - override the onNext() method to manage the server message processing;
         * - override the onError() method to process the errors sent by the server;
         * - override the onCompleted() method (called when the server required to end the connection)
         *   to end the client message sending manager.
         */
        requestObserver = this.asyncStub.participateToNetwork(new StreamObserver<ServerMessage>() {

            @Override
            public void onNext(ServerMessage server_message) {
                try {
                    server_message_processing_manager(server_message);
                } catch (Exception e) {
                    client.logger.warn("CLIENT ERROR");
                    FederatedLog.errorToLogger(client.logger, e);
                    client.logger.info("SENT -> ERROR");
                    requestObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                client.logger.info("RECEIVE -> ERROR");
                client.logger.warn("SERVER ERROR");
                FederatedLog.errorToLogger(client.logger, t);
                send_alert();
            }

            @Override
            public void onCompleted() {
                client.logger.warn("Connection completed");
                send_alert();
            }

        });

        client.logger.warn("Launch grpc client");

        try {
            client_message_sending_manager();
        } catch (Exception e) {
            client.logger.warn("CLIENT ERROR");
            FederatedLog.errorToLogger(client.logger, e);
            client.logger.info("SENT -> ERROR");
            requestObserver.onError(e);
        }

    }

    /**
     * Manages the server message processing.
     * @param server_message Message coming from the server.
     * @throws Exception If an error happens during the connection with the server.
     */
    private void server_message_processing_manager(ServerMessage server_message) throws Exception {

        client.logger.info("RECEIVED -> " + server_message.getServerMessageOneOfCase());
        String server_parameters;

        switch (server_message.getServerMessageOneOfCase()) {

            case INIT_REQUEST:
                client.logger.warn("Run init process");
                try {
                    String client_parameters = client.initialize_client().toString();
                    send_init_response(client_parameters);
                    break;
                } catch (Exception e) {
                    throw new Exception("Error init process", e);
                }

            case FIT_REQUEST:
                server_parameters = server_message.getFitRequest().getParameters();
                client.logger.trace("Receive parameters -> " + server_parameters);
                client.logger.warn("Run fit process");
                try {
                    String client_parameters = client.fit_client(server_parameters).toString();
                    send_fit_response(client_parameters);
                    break;
                } catch (Exception e) {
                    throw new Exception("Error fit process", e);
                }

            case EVALUATE_REQUEST:
                server_parameters = server_message.getEvaluateRequest().getParameters();
                client.logger.trace("Receive parameters -> " + server_parameters);
                client.logger.warn("Run evaluate process");
                try {
                    double client_loss = client.evaluate_client(server_parameters);
                    send_evaluate_response(client_loss);
                    break;
                } catch (Exception e) {
                    throw new Exception("Error evaluate process", e);
                }

            case MODEL_PARAMETERS:
                server_parameters = server_message.getModelParameters().getParameters();
                client.logger.trace("Receive parameters -> " + server_parameters);
                client.logger.warn("Run updating model process");
                try {
                    client.update_model(server_parameters);
                    send_model_parameters_confirmation();
                    break;
                } catch (Exception e) {
                    throw new Exception("Error updating model process", e);
                }

            case END_CONNECTION:
                break;

            case SERVERMESSAGEONEOF_NOT_SET:
                throw new Exception("The server message is not well configured!");

        }

        synchronized (this) {
            notifyAll();
        }

    }

    /**
     * Creates and adds the initialisation response to the client messages queue.
     * @param client_parameters Parameters coming from the client ML model initialisation.
     */
    private void send_init_response(String client_parameters) {
        client.logger.trace("Send parameters -> " + client_parameters);
        InitReply init_reply = InitReply.newBuilder().setParameters(client_parameters).build();
        ClientMessage client_message = ClientMessage.newBuilder().setInitReply(init_reply).build();
        queue.add(client_message);
    }

    /**
     * Creates and adds the fitting response to the client messages queue.
     * @param client_parameters Parameters coming from the client ML model fitting.
     */
    private void send_fit_response(String client_parameters) {
        client.logger.trace("Send parameters -> " + client_parameters);
        FitReply fit_reply = FitReply.newBuilder().setParameters(client_parameters).build();
        ClientMessage client_message = ClientMessage.newBuilder().setFitReply(fit_reply).build();
        queue.add(client_message);
    }

    /**
     * Creates and adds the evaluation response to the client messages queue.
     * @param client_loss Loss information coming from the client ML model evaluation.
     */
    private void send_evaluate_response(double client_loss) {
        client.logger.debug("Send loss -> " + client_loss);
        EvaluateReply evaluate_reply = EvaluateReply.newBuilder().setLoss(client_loss).build();
        ClientMessage client_message = ClientMessage.newBuilder().setEvaluateReply(evaluate_reply).build();
        queue.add(client_message);
    }

    /**
     * Creates and adds the acknowledgement of receipt of the server model parameters
     * to the client messages queue.
     */
    private void send_model_parameters_confirmation() {
        client.logger.debug("Send model updating confirmation");
        ModelParametersReply model_parameters_reply = ModelParametersReply.newBuilder().setConfirmation(true).build();
        ClientMessage client_message = ClientMessage.newBuilder().setModelParametersReply(model_parameters_reply).build();
        queue.add(client_message);
    }

    /**
     * Manages the client message sending. It sends the first message of the client
     * messages queue and wait for a notification to send the following ones.
     * @throws InterruptedException If an errors happens during the loop.
     */
    private synchronized void client_message_sending_manager() throws InterruptedException {
        send_connection_request();
        while (!queue.isEmpty()) {
            ClientMessage client_message = queue.poll();
            client.logger.info("SENT ->  " + client_message.getClientMessageOneOfCase());
            requestObserver.onNext(client_message);
            wait();
        }
    }

    /**
     * Creates and adds a connection request to the client messages queue.
     */
    private void send_connection_request(){
        client.logger.trace("Send connection request: isPassive -> " + client.isPassive);
        ConnectionRequest connection_request = ConnectionRequest.newBuilder().setIsPassive(client.isPassive).build();
        ClientMessage client_message = ClientMessage.newBuilder().setConnectionRequest(connection_request).build();
        queue.add(client_message);
    }

    /**
     * Unblocks the client_message_sending_manager for sending the next message
     * of the client messages queue.
     */
    private synchronized void send_alert() {
        notifyAll();
    }

}
