package cea.util.metrics;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;

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

	public double roundAvoid(double value, int places) {
		double scale = Math.pow(10, places);
		return Math.round(value * scale) / scale;
	}

	public double safeDivison(double v1, double v2) {
		double result = v1 / v2;
		if (((Double) (result)).isNaN())
			result = 0;
		return result;
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
			if(id.equals("default")) {
				id=".";
			}
			try (InputStream props = Resources.getResource("setup/" + id + "/" + "algs.props").openStream()) {
				properties.load(props);
				classes_number = Integer.parseInt(properties.getProperty("classes.number"));
			} catch (Exception e) {
				System.out.println("This metric require the field classes.number to be specified in the file: "
						+ "setup/" + id + "/" + "algs.props");
				// TODO Auto-generated catch block
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
		return results;
	}

	private void calculateConfusionMatrix(Vector<TimeRecord> records) {
		this.multicm = new MultiConfusionMatrix<String>(classes);
		Iterator<TimeRecord> it = records.iterator();

		while (it.hasNext()) {
			TimeRecord record = it.next();
			if (record.getTarget() == null || record.getOutput() == null)
				continue;
			this.multicm.add(record.getTarget(), record.getOutput());
		}

		System.out.println(this.multicm.toString());
	}

}
