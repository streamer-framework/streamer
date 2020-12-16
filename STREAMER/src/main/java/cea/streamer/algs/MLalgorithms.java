package cea.streamer.algs;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

/**
 * Interface class that implements the algorithm module  (machine learning models and statistic algorithm).
 * Each algorithm has to implement a test and a learn method
 *
 */

public abstract class MLalgorithms {
	
	/**
	 * This attribute is used to avoid storing the data in case of online learning algorithms. When the model is updated, the data is not needed
	 * anymore in the next training phase. 
	 */
	public boolean updateModel = false;
	/**
	 * Train (construct) the ML model
	 * @param data vector of time records that the ML algorithm uses to learn
	 * @param unique identifier of the process
	 */
	public abstract void learn(Vector<TimeRecord> data, String id);

	/**
	 * Test the ML model
	 * @param data vector of time records that the ML algorithm uses to test
	 * @param unique identifier of the process
	 */
	public void run(Vector<TimeRecord> data, String id) {}
	
	/**
	 * List hyperparams need for the algorithm
	 * @return
	 */
	public String listNeededHyperparams() {
			String param = "No parameters needed\n";
			return param;
	}
}
