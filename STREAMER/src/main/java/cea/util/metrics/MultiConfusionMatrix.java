package cea.util.metrics;

import java.io.Serializable;
import java.util.*;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class MultiConfusionMatrix<T extends Comparable> implements Serializable {
	private Map<T, Multiset> matrix;
	private List<T> classes;
	private int total_records;

	/**
	 * Creates an empty confusion Matrix
	 */
	public MultiConfusionMatrix(List<T> classes) {
		this.matrix = new HashMap<>();
		this.classes = classes;
		this.total_records = 0;
	}

	public MultiConfusionMatrix() {
	}

	/**
	 * Creates a new ConfusionMatrix initialized with the contents of another
	 * ConfusionMatrix.
	 */
	public MultiConfusionMatrix(MultiConfusionMatrix<T> other) {
		this(other.getClasses());
		this.add(other);
	}

	/**
	 * Increments the entry specified by actual and predicted by one.
	 */
	public void add(T actual, T predicted) {
		add(actual, predicted, 1);
		this.total_records++;
	}

	/**
	 * Increments the entry specified by actual and predicted by count.
	 */
	public void add(T actual, T predicted, int count) {
		if (matrix.containsKey(actual)) {
			matrix.get(actual).add(predicted, count);
		} else {
			Multiset<T> counts = HashMultiset.create();
			counts.add(predicted, count);
			matrix.put(actual, counts);
		}
	}

	/**
	 * Adds the entries from another confusion matrix to this one.
	 */
	public void add(MultiConfusionMatrix<T> other) {
		for (T actual : other.matrix.keySet()) {
			Multiset<T> counts = other.matrix.get(actual);
			for (T predicted : counts.elementSet()) {
				int count = counts.count(predicted);
				this.add(actual, predicted, count);
			}
		}
	}

	/**
	 * Gives the applyTransformToDestination of all classes in the confusion matrix.
	 */
	public List<T> getClasses() {
		return classes;
	}

	/**
	 * Gives the count of the number of times the "predicted" class was predicted
	 * for the "actual" class.
	 */
	public int getCount(T actual, T predicted) {
		if (!matrix.containsKey(actual)) {
			return 0;
		} else {
			return matrix.get(actual).count(predicted);
		}
	}

	/**
	 * Computes the total number of times the class was predicted by the classifier.
	 */
	public int getPredictedTotal(T predicted) {
		int total = 0;
		for (T actual : classes) {
			total += getCount(actual, predicted);
		}
		return total;
	}

	/**
	 * Computes the total number of times the class actually appeared in the data.
	 */
	public int getActualTotal(T actual) {
		if (!matrix.containsKey(actual)) {
			return 0;
		} else {
			int total = 0;
			for (Object elem : matrix.get(actual).elementSet()) {
				T telem = (T) elem;
				total += matrix.get(actual).count(telem);
			}
			return total;
		}
	}

	@Override
	public String toString() {
		return matrix.toString();
	}

	/**
	 * Outputs the ConfusionMatrix as comma-separated values for easy import into
	 * spreadsheets
	 */
	public String toCSV() {
		StringBuilder builder = new StringBuilder();

		// Header Row
		builder.append(",,Predicted Class,\n");

		// Predicted Classes Header Row
		builder.append(",,");
		for (T predicted : classes) {
			builder.append(String.format("%s,", predicted));
		}
		builder.append("Total\n");

		// Data Rows
		String firstColumnLabel = "Actual Class,";
		for (T actual : classes) {
			builder.append(firstColumnLabel);
			firstColumnLabel = ",";
			builder.append(String.format("%s,", actual));

			for (T predicted : classes) {
				builder.append(getCount(actual, predicted));
				builder.append(",");
			}
			// Actual Class Totals Column
			builder.append(getActualTotal(actual));
			builder.append("\n");
		}

		// Predicted Class Totals Row
		builder.append(",Total,");
		for (T predicted : classes) {
			builder.append(getPredictedTotal(predicted));
			builder.append(",");
		}
		builder.append("\n");

		return builder.toString();
	}

	public int getFalseNegative(T c) {
		int total = 0;
		for (T actual : this.getClasses()) {
			if (actual.equals(c))
				continue;
			total += this.getCount(actual, c);
		}
		return total;
	}

	public int getFalsePositive(T c) {
		if (!matrix.containsKey(c)) {
			return 0;
		} else {
			int total = 0;
			for (Object elem : matrix.get(c).elementSet()) {
				T telem = (T) elem;
				if (telem.equals(c))
					continue;
				total += matrix.get(c).count(telem);
			}
			return total;
		}
	}

	public int getTruePositive(T c) {
		if (!matrix.containsKey(c))
			return 0;
		// int count = matrix.get(c).count(c);
		int count = this.getCount(c, c);
		return count;
	}

	public int getTrueNegative(T c) {
		if (!matrix.containsKey(c))
			return 0;
		int total = 0;
		for (T actual : this.getClasses()) {
			for(T predicted: this.getClasses()) {
			if (actual.equals(c) || predicted.equals(c))
				continue;
			total += this.getCount(actual, predicted);
			}
		}
		return total;
		/*int tp = this.getTruePositive(c);
		int fn = this.getFalseNegative(c);
		int fp = this.getFalseNegative(c);
		int result = this.total_records - (this.getTruePositive(c) + this.getFalseNegative(c) + this.getFalsePositive(c));		
		return result;*/
	}

	public static void main(String[] args) {
		List<String> classes = new LinkedList<String>();
		classes.add("1");
		classes.add("2");
		classes.add("3");
		classes.add("4");

		MultiConfusionMatrix cm = new MultiConfusionMatrix<String>(classes);
		cm.add("1", "2");
		cm.add("2", "1");
		cm.add("3", "1");
		cm.add("4", "3");
		cm.add("2", "2");
		cm.add("2", "2");
		cm.add("2", "2");
		cm.add("2", "2");
		cm.add("1", "2");
		cm.add("2", "1");
		cm.add("3", "1");
		cm.add("4", "3");
		cm.add("2", "3");
		cm.add("2", "3");
		cm.add("2", "4");
		cm.add("3", "2");
		cm.add("3", "1");
		cm.add("4", "3");
		int predictedT = cm.getPredictedTotal("2");
		int actualT = cm.getActualTotal("2");
		System.out.println("The total predicted values:" + predictedT + " and the actual are:" + actualT);
		cm.getFalsePositive("2");
		cm.getFalseNegative("2");
		cm.getTruePositive("5");
		cm.getTrueNegative("2");
		System.out.println(cm.toString());
		System.out.println(cm.toCSV());
	}
}
