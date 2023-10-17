package cea.federated.util;

import org.apache.log4j.*;

/**
 * Implementation of the federated module logger.
 */
public class FederatedLog {

    /**
     * Level of the logger.
     */
    static Level level = Level.INFO;

    /**
     * Creates a logger.
     * @param name Name of the logger.
     * @param id Identifier of the object that requests the logger.
     * @return Federated module logger.
     */
    public static Logger create_logger(String name, String id) {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(name);
        ConsoleAppender ca = new ConsoleAppender();
        ca.setName("console");
        Layout layout = create_layout(id);
        ca.setLayout(layout);
        ca.activateOptions();
        logger.addAppender(ca);
        logger.setLevel(level);
        return logger;
    }

    /**
     * Creates the layout for the federated module logger.
     * @param id Identifier of the object that requests the logger.
     * @return Layout for the federated module logger.
     */
    private static PatternLayout create_layout(String id){
        String motif = "%p - [" + id + "] %m %n";
        return new PatternLayout(motif);
    }

    /**
     * Processes the errors and displays them one by one.
     * @param logger Federated module logger.
     * @param t Errors.
     */
    public static void errorToLogger(Logger logger, Throwable t) {
        logger.warn("Error -> " + t.getMessage());
        while (t.getCause() != null) {
            t = t.getCause();
            logger.warn("Error -> " + t.getMessage());
        }
    }

}


