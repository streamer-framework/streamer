package cea.federated.server.aggregation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Implementation of the FedAvg aggregation strategy.
 */
public class FedAvgStrategy extends AggregationStrategy {

    /**
     * Aggregates the models parameters by following the FedAvg strategy.
     * @param list_parameters List of the fitted models parameters.
     * @return Aggregated model parameters.
     */
    @Override
    Queue<Double> aggregate_parameters(List<List<Object>> list_parameters) {
        int parameters_size = list_parameters.get(0).size();
        Queue<Double> aggregated_parameters = new LinkedList<>();
        for (int i=0; i<parameters_size; i++) {
            double aggregated_parameter = 0;
            for (List<Object> parameters: list_parameters) {
                aggregated_parameter += (double) parameters.get(i);
            }
            aggregated_parameter = aggregated_parameter / list_parameters.size();
            aggregated_parameters.add(aggregated_parameter);
        }
        return aggregated_parameters;
    }

    /**
     * Aggregates the evaluation losses by following the FedAvg strategy.
     * @param list_loss List of the evaluation losses.
     * @return Aggregated loss.
     */
    @Override
    Double aggregate_losses(ArrayList<Double> list_loss) {
        double aggregated_loss = 0;
        for (Double loss: list_loss) {
            aggregated_loss += loss;
        }
        aggregated_loss = aggregated_loss / list_loss.size();
        return aggregated_loss;
    }

}
