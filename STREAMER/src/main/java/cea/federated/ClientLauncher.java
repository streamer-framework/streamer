package cea.federated;

import cea.federated.client.OfflineClient;
import cea.util.Log;

/**
 * Class that defines the client launcher. It runs as many offline clients as there
 * are input arguments (the 'id_problem's) to the main function.
 */
public class ClientLauncher {

    public static void main(String[] args) {

        Log.clearLogs();
        if (args.length > 0) {
            for (String arg : args) {
                run(arg);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        } else {
            run("cmapss/client_0"); // the id_problem of the distributed learning module demo.
        }

    }

    /**
     * Runs an offline client in a new thread by instantiating it and starts its
     * gRPC client interface.
     * @param id_problem Identifier of the problem.
     */
    private static void run(String id_problem) {
        Thread clientThread = new Thread(() -> {
            try {
                OfflineClient client = new OfflineClient(id_problem);
                client.start_client(client.server_address);
            } catch (Exception ignored) {}
        });
        clientThread.start();
    }

}
