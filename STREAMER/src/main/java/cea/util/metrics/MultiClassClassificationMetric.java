package cea.util.metrics;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;

public class MultiClassClassificationMetric extends Metric {

	MultiConfusionMatrix<String> multicm = null;
	static List<String> classes = null;

	public MultiClassClassificationMetric() {
		/*
		 * classes = new LinkedList<String>(); for (int i = 0; i < 23; i++) {
		 * classes.add("" + i); }
		 */
		// classes.add("0");
		// classes.add("1");
		// classes.add("3");classes.add("4");
		// this.multicm = new MultiConfusionMatrix<String>(classes);
	}
	

	public int getActualTotal(String actual) {
		return multicm.getActualTotal(actual);
	}

	public int getPredictedTotal(String predicted) {
		return multicm.getPredictedTotal(predicted);
	}

	public int getTruePositive(String c) {
		return multicm.getTruePositive(c);
	}

	public int getTrueNegative(String c) {
		return multicm.getTrueNegative(c);
	}

	public int getFalsePositive(String c) {
		return multicm.getFalsePositive(c);
	}

	public int getFalseNegative(String c) {
		return multicm.getFalseNegative(c);
	}

	public List<String> getClasses() {
		return multicm.getClasses();
	}

	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		Vector<Double> results = new Vector<Double>();
		if (classes == null) {
			Properties properties = new Properties();
			/**
			 * the classes_number is used to evaluate the incoming data in the case
			 * multiclass classification precising the number of classes for the confusion matrix
			 * to be calculated
			 */
			int classes_number = -1;
			String origin = id;
			if(origin.equals("default")) {
				origin=".";
			}
			try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles + origin + "/algs.props").openStream()) {
				properties.load(props);
				classes_number = Integer.parseInt(properties.getProperty("classes.number"));
			} catch (Exception e) {
				System.out.println("["+id+"] This metric require the field classes.number to be specified in the file: "
						+ GlobalUtils.resourcesPathPropsFiles + id + "/" + "algs.props");
				e.printStackTrace();
				return null;
			}
			classes = new LinkedList<String>();
			for (int i = 0; i < classes_number; i++) {
				classes.add("" + i);
			}
			this.multicm = new MultiConfusionMatrix<String>(classes);
		}

		calculateConfusionMatrix(records);

		System.out.println("["+id+"] "+this.multicm.toString());
		return results;
	}

	/**
	 * Method to compute the confusion matrix
	 * @param records
	 */
	private void calculateConfusionMatrix(Vector<TimeRecord> records) {
		this.multicm = new MultiConfusionMatrix<String>(classes);
		for(TimeRecord record: records) {
			if (record.getTarget().isEmpty() || record.getOutput().isEmpty())
				continue;
			this.multicm.add(record.getTarget().get(0), record.getOutput().get(0));
		}		
	}

}
