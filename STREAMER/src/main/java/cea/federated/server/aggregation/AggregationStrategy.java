package cea.federated.server.aggregation;

import cea.federated.util.JSONManager;

import java.util.*;

/**
 * Implementation of the abstract AggregationStrategy class.
 */
public abstract class AggregationStrategy {

    /**
     * Runs the aggregation process for the fitting step. It includes the cases
     * where the list is empty or contains only one model parameters, and the
     * serialization/deserialization of all the models parameters.
     * @param list_parameters List of the fitted models parameters.
     * @return Aggregated model parameters.
     * @throws Exception If the list of models parameters is empty.
     */
    public String aggregate_fit(ArrayList<String> list_parameters) throws Exception {
        if (list_parameters.isEmpty()) {
            throw new Exception(this.getClass().getName()+" - list_parameters is empty");
        } else if (list_parameters.size() == 1) {
            return list_parameters.get(0);
        } else {

            List<List<Object>> list_deserialized_parameters = new ArrayList<>();
            for (String parameters: list_parameters) {
                List<Object> deserialized_parameters = JSONManager.deserializeParams(parameters);
                list_deserialized_parameters.add(deserialized_parameters);
            }

            Queue<Double> aggregated_parameters = aggregate_parameters(list_deserialized_parameters);

            return JSONManager.serializeParams(list_parameters.get(0), aggregated_parameters);
        }
    }

    /**
     * Abstract method for determining how the models parameters will be aggregated.
     * @param list_parameters List of the fitted models parameters.
     * @return Aggregated model parameters.
     */
    abstract Queue<Double> aggregate_parameters(List<List<Object>> list_parameters);

    /**
     * Runs the aggregation process for the evaluation step. It includes the cases
     * where the list is empty or contains only one loss.
     * @param list_loss List of the evaluation losses.
     * @return Aggregated loss.
     * @throws Exception If the list of losses is empty.
     */
    public double aggregate_evaluate(ArrayList<Double> list_loss) throws Exception {
        if (list_loss.isEmpty()) {
            throw new Exception(this.getClass().getName()+" - list_loss is empty");
        } else if (list_loss.size() == 1) {
            return list_loss.get(0);
        } else {
            return aggregate_losses(list_loss);
        }
    }

    /**
     * Abstract method for determining how the evaluation losses will be aggregated.
     * @param list_loss List of the evaluation losses.
     * @return Aggregated loss.
     */
    abstract Double aggregate_losses(ArrayList<Double> list_loss);

}
