package cea.federated;

import cea.federated.server.Server;
import cea.federated.server.grpc_server.ServerGRPC;

/**
 * Class that defines the server launcher. It :
 * - creates a server;
 * - initializes a gRPC server and starts it;
 * - launches the FL loop of the server;
 * - disconnects the clients when the FL loop process is finished;
 * - stops the gRPC server.
 */
public class ServerLauncher {

    public static void main(String[] args) throws Exception {

        String id;

        if (args.length > 0) {
            id = args[0];
        } else {
            id = "cmapss/server"; // the id_problem of the distributed learning module demo.
        }

        // -------------------------------------------------------------------

        Server server = new Server(id);

        ServerGRPC grpc_server = new ServerGRPC(server.client_manager, server.server_port, server.logger);
        grpc_server.start();

        server.fl_loop();
        server.disconnect_all_clients();

        grpc_server.stop();

    }

}
