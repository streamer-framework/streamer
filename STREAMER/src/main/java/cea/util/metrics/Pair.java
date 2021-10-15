package cea.util.metrics;

/**
 * Class that implements a 2-tuple (value,weight)
 * This class is used by the QuantileSummary class
 */
public class Pair {

	final private Integer value;
	final private Integer weight;
	
	public Pair(Integer value, Integer weight) {
		this.value = value;
		this.weight = weight;
	}

	public Integer getValue() {
		return value;
	}

	public Integer getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return "(" + value +" ; "+ weight + ")";
	}
	
	

}
