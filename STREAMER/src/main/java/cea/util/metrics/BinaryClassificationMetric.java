package cea.util.metrics;

import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class BinaryClassificationMetric extends Metric {

	private String positive = "1";
	
	private String negative = "0";

	private int truePositive;

	private int trueNegative;

	private int falsePositive;

	private int falseNegative;
	
	public int getTruePositive() {
		return truePositive;
	}

	public void setTruePositive(int truePositive) {
		this.truePositive = truePositive;
	}

	public int getTrueNegative() {
		return trueNegative;
	}

	public void setTrueNegative(int trueNegative) {
		this.trueNegative = trueNegative;
	}

	public int getFalsePositive() {
		return falsePositive;
	}

	public void setFalsePositive(int falsePositive) {
		this.falsePositive = falsePositive;
	}

	public int getFalseNegative() {
		return falseNegative;
	}

	public void setFalseNegative(int falseNegative) {
		this.falseNegative = falseNegative;
	}

	private void reinit() {
		this.truePositive = 0;
		this.trueNegative = 0;
		this.falsePositive = 0;
		this.falseNegative = 0;
	}	

	@Override
	public Vector<Double> evaluate(Vector<TimeRecord> records, String id) {
		this.calculateTrueFalse(records);
		return null;
	}
	
	public double safeDivison(double v1, double v2) {
		double result = v1/v2;
		if(((Double)(result)).isNaN())
			result = 0;
		return result;
	}
	
	public double roundAvoid(double value, int places) {
	    double scale = Math.pow(10, places);
	    return Math.round(value * scale) / scale;
	}

	private void calculateTrueFalse(Vector<TimeRecord> records) {
		reinit();
		String target, output;
		for(TimeRecord record: records) {		
			if (record.getTarget().isEmpty() || record.getOutput().isEmpty())
				continue;
			output = record.getOutput().get(0);
			target = record.getTarget().get(0);
			if (target.equals(output)) {
				if (target.equals(positive)) {
					this.truePositive += 1;
				} else {
					this.trueNegative += 1;
				}
			} else {
				if (target.equals(positive) && output.equals(negative)) {
					this.falseNegative += 1;
				} else {
					this.falsePositive += 1;
				}
			}
		}
	}

}
