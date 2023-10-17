package cea.federated.server;

import cea.federated.server.grpc_server.ClientProxyGRPC;

import java.util.*;

/**
 * Implementation of the manager of the clients proxies.
 */
public class ClientManager {

    /**
     * Map collection of (non-passive) clients proxies and their identifiers.
     */
    public Map<Integer, ClientProxyGRPC> clients;

    /**
     * Map collection of passive clients proxies and their identifiers.
     */
    public Map<Integer, ClientProxyGRPC> passiveClients;

    /**
     * Counter that generate unique ID each time a client connects to the server.
     */
    public Integer counterID;

    /**
     * Constructor of the client manager.
     */
    public ClientManager() {
        this.clients = new HashMap<>();
        this.passiveClients = new HashMap<>();
        this.counterID = 0;
    }

    /**
     * Initializes a new client proxy.
     * @return A new client proxy.
     */
    public synchronized ClientProxyGRPC create_client() {
        Integer id = counterID;
        counterID++;
        return new ClientProxyGRPC(id);
    }

    /**
     * Registers a client proxy by adding it to its associated collection (normal or passive).
     * @param client Client proxy that has just been created.
     * @throws Exception If the ID of the client proxy already exists.
     */
    public synchronized void register(ClientProxyGRPC client) throws Exception {
        if (client.is_passive) {
            if(!passiveClients.containsKey(client.id)) {
                passiveClients.put(client.id, client);
                notifyAll();
            } else {
                throw new Exception("This ID client already exists!");
            }
        } else {
            if(!clients.containsKey(client.id)) {
                clients.put(client.id, client);
                notifyAll();
            } else {
                throw new Exception("This ID client already exists!");
            }
        }
    }

    /**
     * Unregisters a client proxy by removing it of its associated collection (normal or passive).
     * @param id Boolean that determines if the client is passive or not.
     * @param isPassive Identifier of the client proxy.
     * @throws Exception If the ID of the client proxy doesn't exist.
     */
    public synchronized void unregister(Integer id, boolean isPassive) throws Exception {
        if(isPassive) {
            if (passiveClients.containsKey(id)) {
                passiveClients.get(id).is_alive = false;
                passiveClients.remove(id);
                notifyAll();
            } else {
                throw new Exception("This ID client doesn't exist!");
            }
        } else {
            if (clients.containsKey(id)) {
                clients.get(id).is_alive = false;
                clients.remove(id);
                notifyAll();
            } else {
                throw new Exception("This ID client doesn't exist!");
            }
        }
    }

    /**
     * Randomly samples {@code n_clients} clients. If the clients collections has less
     * than {@code min_n_clients} clients, it waits until enough clients have registered.
     * @param n_clients Number of clients to sample.
     * @param min_n_clients Minimum number of clients required to run a sampling.
     * @return A list of sampled clients proxies.
     */
    public synchronized List<ClientProxyGRPC> sample_clients(Integer n_clients, Integer min_n_clients) {

        try {
            while(this.clients.size() < min_n_clients){
                wait();
            }
        } catch (InterruptedException ignored){}

        List<ClientProxyGRPC> clientsList = new ArrayList<>(this.clients.values());

        if(n_clients < 0) {
            n_clients = clientsList.size();
        }

        Collections.shuffle(clientsList);
        List<ClientProxyGRPC> selected_clients = new ArrayList<>();
        for (int i=0; i<n_clients; i++) {
            selected_clients.add(clientsList.get(i));
        }
        return selected_clients;
    }

    /**
     * Method used in the main server process, i.e. the 'Server' class file.
     * Permits to pause the main process until all clients responses have been received.
     * @param clients List of sampled clients proxies.
     */
    public synchronized void wait_for_client_messages(List<ClientProxyGRPC> clients) {
        try {
            while(check_client_messages(clients)) {
                wait();
            }
        } catch (InterruptedException ignored) {}
    }

    /**
     * Checks if the server has received all the responses of the alive clients.
     * @param clients List of sampled clients proxies
     * @return True if one of the clients responses has not been received and
     * False if all the responses have been received.
     */
    private boolean check_client_messages(List<ClientProxyGRPC> clients) {
        for(ClientProxyGRPC client : clients) {
            if (client.is_alive & client.queue_client_message.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unblocks the sample_clients() method when a new client proxy has been registered.
     */
    public synchronized void send_alert() {
        notifyAll();
    }

}
