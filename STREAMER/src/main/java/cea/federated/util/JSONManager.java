package cea.federated.util;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.util.*;

/**
 * Implementation of the JSON manager to enable the server to deserialize and
 * serialize model parameters. In fact, the models parameters are serialized in JSON
 * for the communication between the server and the clients.
 */
public class JSONManager {

    /**
     * Deserializes the model parameters from the JSON format to a list of double
     * to simplify the aggregation of the models parameters. In fact, if the model
     * is a neural network, each of its layers has its own tensor, so the initial
     * {@code jsonArray} is composed of several JSONArray subcomponents, making
     * aggregation operations more complicated.
     * @param parameters The serialized parameters (String object).
     * @return A list of parameters (double).
     * @throws Exception In case of errors in the searchParameters() method.
     */
    public static List<Object> deserializeParams(String parameters) throws Exception {
        JSONParser parser = new JSONParser();
        JSONArray parametersJsonArray = (JSONArray) parser.parse(parameters);
        return searchParameters(parametersJsonArray);
    }

    /**
     * Recursive method that decomposes the initial serialized structure of the model
     * parameters to a list of double.
     * @param jsonArray The serialized parameters (JSONArray object).
     * @return A list of parameters (double).
     * @throws Exception If the elements/sub-elements of the initial jsonArray is not a
     * JSONArray or a double.
     */
    private static List<Object> searchParameters(JSONArray jsonArray) throws Exception {
        Object firstElement = jsonArray.get(0);
        if(firstElement instanceof org.json.simple.JSONArray) {
            List<Object> list = new ArrayList<>();
            for (Object subElement : jsonArray) {
                list.addAll(searchParameters((JSONArray) subElement));
            }
            return list;
        } else if(firstElement instanceof java.lang.Double) {
            return new ArrayList<Object>(jsonArray);
        } else {
            throw new Exception("Error during the parameters deserialization!");
        }
    }

    /**
     * Serializes the aggregated model parameters from a list of double to the JSON format
     * to go back to the initial model parameters format, and finally converts it
     * to a string representation.
     * @param initial_parameters Initial model parameters (useful for finding the format).
     * @param aggregated_parameters Aggregated model parameters to serialize.
     * @return Serialized Parameters (String object).
     * @throws Exception In cas of errors in the updateParameters() method.
     */
    public static String serializeParams(String initial_parameters, Queue<Double> aggregated_parameters) throws Exception {
        JSONParser parser = new JSONParser();
        JSONArray jsonArray = (JSONArray) parser.parse(initial_parameters);
        JSONSerialization jsonSerialization = new JSONSerialization(aggregated_parameters);
        return jsonSerialization.updateParameters(jsonArray).toString();
    }

    /**
     * Implementation of the JSONSerialization class that permits to find the structure
     * of the initial serialized model.
     */
    private static class JSONSerialization {

        /**
         * Aggregated model parameters to serialize.
         */
        Queue<Double> aggregated_parameters;

        /**
         * Constructor of the jSONSerialization object.
         * @param aggregated_parameters Aggregated model parameters to serialize.
         */
        JSONSerialization(Queue<Double> aggregated_parameters) {
            this.aggregated_parameters = aggregated_parameters;
        }

        /**
         *
         * @param jsonArray Initial model parameters, in a JSONArray object that has the
         *                  correct serialization format.
         * @return A JSONArray object that contains the aggregated model parameters in the
         * correct serialization format.
         * @throws Exception If the elements/sub-elements of the initial jsonArray is not a
         * JSONArray or a double.
         */
        private JSONArray updateParameters(JSONArray jsonArray) throws Exception {
            Object firstElement = jsonArray.get(0);
            if(firstElement instanceof org.json.simple.JSONArray) {
                for (int i=0; i<jsonArray.size(); i++) {
                    JSONArray newJsonArray = updateParameters((JSONArray) jsonArray.get(i));
                    jsonArray.set(i, newJsonArray);
                }
                return jsonArray;
            } else if(firstElement instanceof java.lang.Double) {
                for (int i=0; i<jsonArray.size(); i++) {
                    Double aggregated_parameter = aggregated_parameters.poll();
                    jsonArray.set(i, aggregated_parameter);
                }
                return jsonArray;
            } else {
                throw new Exception("Error during the parameters serialization!");
            }
        }

    }

}
