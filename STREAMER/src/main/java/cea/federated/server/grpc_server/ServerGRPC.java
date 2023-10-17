package cea.federated.server.grpc_server;

import cea.federated.*;

import cea.federated.server.ClientManager;
import cea.federated.util.FederatedLog;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Implementation of the server gRPC interface.
 */
public class ServerGRPC {

    /**
     * Address and port to listen for client requests.
     */
    private final int port;

    /**
     * Server that creates this gRPC interface 'serverGRPC'.
     */
    private final Server server;

    /**
     * Logger of the server.
     */
    private final Logger logger;

    /**
     * Constructor of the 'serverGRPC' object.
     * @param client_manager Manager of the client proxies.
     * @param port Address and port to listen for client requests.
     * @param logger Logger of the server.
     */
    public ServerGRPC(ClientManager client_manager, Integer port, Logger logger) {
        this.port = port;
        this.server = ServerBuilder.forPort(port).addService(new ServerGRPC.FLComService(client_manager)).build();
        this.logger = logger;
    }

    /**
     * Starts the gRPC server so that clients can use the service 'FLComService'
     * (see fl.proto).
     * @throws IOException In case of errors.
     */
    public void start() throws IOException {
        server.start();
        logger.warn("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("Shutting down gRPC server since JVM is shutting down");
            stop();
            logger.warn("Server shut down");
        }));
    }

    /**
     * Stops the gRPC server.
     */
    public void stop() {
        server.shutdownNow();
    }

    /**
     * Implementation of the gRPC service 'FLComService'.
     */
    public static class FLComService extends FLComGrpc.FLComImplBase {

        /**
         * Manager of the client proxies (the 'clientProxyGRPC's).
         */
        private final ClientManager client_manager;

        /**
         * Constructor of the service 'fLComService'.
         * @param client_manager Manager of the client proxies.
         */
        public FLComService(ClientManager client_manager) {
            this.client_manager = client_manager;
        }

        /**
         * Starts and keeps the connections with the client. It defines what the server needs
         * to do when clients connect to the service and call the 'participateToNetwork()' method.
         * @param responseObserver Interface for the server to communicate with the client
         *                         (it sends server messages and errors, or ends the connection).
         * @return a StreamObserver that represents the interface for the client during the
         * communication.
         */
        @Override
        public StreamObserver<ClientMessage> participateToNetwork(final StreamObserver<ServerMessage> responseObserver) {

            ClientProxyGRPC client = client_manager.create_client();

            /*
             * Return an anonymous StreamObserver which is the interface for the client during the communication.
             * It defines how the server should react when the clients use the method 'participateToNetwork()'.
             * In this interface, we:
             * - override the onNext() method to manage the client message processing and the server message sending;
             * - override the onError() method to process the errors sent by the client;
             * - override the onCompleted() method (called when the client required to end the connection)
             *   to notify the client that the gRPC connection will be completed.
             */
            return new StreamObserver<ClientMessage>() {

                @Override
                public void onNext(ClientMessage client_message) {
                    try {
                        client_message_processing_manager(client, client_message);
                        client.wait_for_alert();
                        server_message_sending_manager(client, responseObserver);
                    } catch (Exception e) {
                        client.logger.warn("SERVER ERROR");
                        FederatedLog.errorToLogger(client.logger, e);
                        client.logger.info("SENT -> ERROR");
                        responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asException());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    client.logger.warn("CLIENT ERROR");
                    FederatedLog.errorToLogger(client.logger, t);
                    try {
                        client_manager.unregister(client.id, client.is_passive);
                        client.logger.warn("has disconnected");
                    } catch (Exception e) {
                        client.logger.warn("SERVER ERROR");
                        FederatedLog.errorToLogger(client.logger, e);
                    }
                    client.send_alert();
                }

                @Override
                public void onCompleted() {
                    client.logger.info("Client request to complete the connection");
                    responseObserver.onCompleted();
                    try {
                        client_manager.unregister(client.id, client.is_passive);
                        client.logger.warn("has disconnected");
                    } catch (Exception e) {
                        client.logger.warn("SERVER ERROR");
                        FederatedLog.errorToLogger(client.logger, e);
                    }
                    client.send_alert();
                }

            };

        }

        /**
         * Manages the client message processing. If it is a connection request, the
         * server registers the client, and if not, the server adds the new client
         * message to the queue of the associated client.
         * @param client Client proxy that sends the message.
         * @param client_message Message coming from the client.
         */
        private void client_message_processing_manager(ClientProxyGRPC client, ClientMessage client_message) throws Exception {
            client.logger.info("RECEIVED -> " + client_message.getClientMessageOneOfCase());
            if (client_message.hasConnectionRequest()) {
                client.is_passive = client_message.getConnectionRequest().getIsPassive();
                client_manager.register(client);
            } else {
                client.queue_client_message.add(client_message);
                client_manager.send_alert();
            }
        }

        /**
         * Manages the server message sending. When called, it sends the first server
         * message of the associated client's 'queue_server_message'.
         * @param client Client proxy to which the message will be sent.
         * @param responseObserver Interface for the server to communicate with the client.
         * @throws Exception In case of errors during the configuration/definition of the message.
         */
        private void server_message_sending_manager(ClientProxyGRPC client, StreamObserver<ServerMessage> responseObserver) throws Exception {
            if(!client.queue_server_message.isEmpty()){
                ServerMessage server_message = client.queue_server_message.poll();
                client.logger.info("SENT -> " + server_message.getServerMessageOneOfCase());
                if(server_message.getServerMessageOneOfCase() == ServerMessage.ServerMessageOneOfCase.SERVERMESSAGEONEOF_NOT_SET) {
                    throw new Exception("The server message is not well configured!");
                } else {
                    if (server_message.getServerMessageOneOfCase() == ServerMessage.ServerMessageOneOfCase.END_CONNECTION) {
                        responseObserver.onCompleted();
                    } else {
                        responseObserver.onNext(server_message);
                    }
                }
            } else {
                throw new Exception("No server message has been defined!");
            }
        }

    }

}
