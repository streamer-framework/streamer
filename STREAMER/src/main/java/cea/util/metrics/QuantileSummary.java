package cea.util.metrics;

import java.util.*;

import cea.streamer.core.TimeRecord;

/**
 * Class that implements the weighted quantile sketch algorithm ( https://arxiv.org/pdf/1603.02754.pdf )
 * 
 */
public class QuantileSummary {

	/**
	 * Subset of value in the multi-set
	 */
	private ArrayList<Integer> S = new ArrayList<Integer>();
	
	/**
	 * the rPlus function defined on S
	 */
	private Map<Integer,Integer> rPlus = new HashMap<Integer,Integer>();
	

	/**
	 * the rMinus function defined on S
	 */
	private Map<Integer,Integer> rMinus = new HashMap<Integer,Integer>();
	

	/**
	 * the omega function defined on S
	 */
	private Map<Integer,Integer> omega = new HashMap<Integer,Integer>();
	
	/**
	 * Total sum of weights
	 */
	private double omegaSum = 0;
	
	/**
	 * Approximation error
	 */
	private double epsilon = 0;


	
	/**
	 * Put a vector of time record into a quantile summary (the weights are by default 1)
	 * @param data vector of time record
	 * @return a quantile summary that corresponds to the multi-set of (value = record , weight = 1)
	 */
	public QuantileSummary dataToQS(Vector<TimeRecord> data) {
		ArrayList<Pair> multiSet = new ArrayList<Pair>();
		for (TimeRecord record : data) {
			multiSet.add(new Pair(Integer.valueOf(record.getValues().get("meassure")),1));
		}
		return new QuantileSummary(multiSet);
	}

	
	/**
	 * Constructor that used a multi-set of (value , weight) to initialize the 4-tuple (S, rplus, rminus, omega)
	 * and weight sum (omegaSum)
	 */
	public QuantileSummary(ArrayList<Pair> multiSet) {
		
		for (Pair e : multiSet) {
			
			Integer resPlus = 0;
			Integer resMinus = 0;
			Integer resOmega = 0;
			
			Integer xi = e.getValue();
			
			for (Pair p : multiSet) {
				
				Integer value = p.getValue();
				Integer weight = p.getWeight();
				
				if (value < xi) {
					resMinus += weight;
				}
				if (value <= xi) {
					resPlus += weight;
				}
				if (value.equals(xi)) {
					resOmega += weight;
				}
			}
			
			rPlus.put(xi, resPlus);
			rMinus.put(xi, resMinus);
			omega.put(xi, resOmega);

			if (!S.contains(xi)) {
				S.add(xi);
			}
			
			omegaSum += e.getWeight();
		}
		Collections.sort(S);
		
	}
	
	public QuantileSummary() {
	}
	

	public ArrayList<Integer> getS() {
		ArrayList<Integer> other = S;
		return other;
	}
	

	public double getOmegaSum() {
		double omegaSumCopy = omegaSum;
		return omegaSumCopy;
	}
	
	public double getEpsilon() {
		double epsilonCopy = epsilon;
		return epsilonCopy;
	}


	/**
	 * rMinus function with extended domain (according to DEF A.2 in APPENDIX https://arxiv.org/pdf/1603.02754.pdf)
	 * 
	 */
	public int rMinusExtended(int y) {
		
		if (S.size() == 0) {
			return 0;
		}
		
		if (rMinus.containsKey(y)) { // if y in S
			return rMinus.get(y);
		} else if (y > S.get(S.size()-1)) { // if y > xk
			return rPlus.get(S.get(S.size()-1));
		} else if (y < S.get(0)) { // if y < x1
			return 0;
		} else { // if y in [xi,xi+1]
			
			/* dichotomic search */
			int a = 0;
			int b = S.size()-1;
			int c = (a+b)/2; 
			while(!(S.get(c) < y && S.get(c+1) > y)) {
				if (S.get(c) < y) {
					a = c;
				} else {
					b = c;
				}
				c = (a+b)/2;
			}
			
			Integer xi = S.get(c);
			return rMinus.get(xi) + omega.get(xi);
		}
	}
	
	/**
	 * rPlus function with extended domain (according to DEF A.2 in APPENDIX https://arxiv.org/pdf/1603.02754.pdf)
	 * 
	 */
	public int rPlusExtended(int y) {

		if (S.size() == 0) {
			return 0;
		}
		
		if (rPlus.containsKey(y)) { // if y in S
			return rPlus.get(y);
		} else if (y > S.get(S.size()-1)) { // if y > xk
			return rPlus.get(S.get(S.size()-1));
		} else if (y < S.get(0)) { // if y < x1
			return 0;
		} else { // if y in [xi,xi+1]
			
			/* dichotomic search */
			int a = 0;
			int b = S.size()-1;
			int c = (a+b)/2; 
			while(!(S.get(c) < y && S.get(c+1) > y)) {
				if (S.get(c) < y) {
					a = c;
				} else {
					b = c;
				}
				c = (a+b)/2;
			}
			
			Integer xinext = S.get(c+1);
			return rPlus.get(xinext)- omega.get(xinext);
		}
		
	}
	
	
	/**
	 * Omega function with extended domain (according to DEF A.2 in APPENDIX https://arxiv.org/pdf/1603.02754.pdf)
	 * 
	 */
	public int omegaExtended(int y) {
		
		if (omega.containsKey(y)) { // if y in S
			return omega.get(y);
		} else {
			return 0;
		}
	}
	

	
	/**
	 * Merge two quantile summaries into one without increasing the approximation error
	 * 
	 */
	public void merge(QuantileSummary otherQ) {
		
		ArrayList<Integer> newS = new ArrayList<Integer>();
		Map<Integer,Integer> newRPlus = new HashMap<Integer,Integer>();
		Map<Integer,Integer> newRMinus = new HashMap<Integer,Integer>();
		Map<Integer,Integer> newOmega = new HashMap<Integer,Integer>();
		
		
		int iS = 0;
		int iOtherS = 0;
		while(iS < S.size() && iOtherS < otherQ.getS().size()) {
			Integer xi = S.get(iS);
			Integer xiOtherQ = otherQ.getS().get(iOtherS);
			if (xi < xiOtherQ) {
				if (!newS.contains(xi)) {
					newS.add(xi);
				}
				iS++;
			} else if (xi > xiOtherQ) {
				if (!newS.contains(xiOtherQ)) {
					newS.add(xiOtherQ);
				}
				iOtherS++;
			} else {
				if (!newS.contains(xi)) {
					newS.add(xi);
				}
				iS++;
				iOtherS++;
			}
			/* update rmin, rmax and omega with merge operation formulas*/
			Integer newXi = newS.get(newS.size()-1);
			Integer resRMinus = rMinusExtended(newXi) + otherQ.rMinusExtended(newXi);
			Integer resRPlus = rPlusExtended(newXi) + otherQ.rPlusExtended(newXi);
			Integer resOmega = omegaExtended(newXi) + otherQ.omegaExtended(newXi);
			newRMinus.put(newXi,resRMinus);
			newRPlus.put(newXi,resRPlus);
			newOmega.put(newXi,resOmega);
		}
		
		ArrayList<Integer> remainedS = (iS < S.size())? S : otherQ.getS();
		int i = (iS < S.size())? iS : iOtherS;
		
		while(i < remainedS.size()) {
			Integer newXi = remainedS.get(i);
			if (!newS.contains(newXi)) {
				newS.add(newXi);
			}
			/* update rmin, rmax and omega with merge operation formula*/
			Integer resRMinus = rMinusExtended(newXi) + otherQ.rMinusExtended(newXi);
			Integer resRPlus = rPlusExtended(newXi) + otherQ.rPlusExtended(newXi);
			Integer resOmega = omegaExtended(newXi) + otherQ.omegaExtended(newXi);
			newRMinus.put(newXi,resRMinus);
			newRPlus.put(newXi,resRPlus);
			newOmega.put(newXi,resOmega);
			i++;
		}
		S = newS;
		rMinus = newRMinus;
		rPlus = newRPlus;
		omega = newOmega;
		omegaSum += otherQ.getOmegaSum();
		epsilon = Math.max(epsilon, otherQ.getEpsilon());
		
	}


	/**
	 * Algorithm 4 in APPENDIX A.3 (https://arxiv.org/pdf/1603.02754.pdf)
	 * @param d a rank
 	 * @return a xi in S whose rank is close to d
	 */
	private Integer query(double d) {
		Integer x1 = S.get(0);
		Integer xk = S.get(S.size()-1);
		if ( d < 0.5 * (rMinus.get(x1) + rPlus.get(x1)) ) {
			return x1;
		} 
		if ( d >= 0.5 * (rMinus.get(xk) + rPlus.get(xk)) ) {
			return xk;
		}
		
		/* dichotomic search */
		int a = 0;
		int b = S.size()-1;
		int c = (a+b)/2;
		Integer xi = S.get(c);
		Integer xinext = S.get(c+1);
		while(!((d >= 0.5 * ((rMinus.get(xi) + rPlus.get(xi)))) && (d < 0.5 * ((rMinus.get(xinext) + rPlus.get(xinext)))))) {
			if (d >= 0.5 * ((rMinus.get(xinext) + rPlus.get(xinext)))) {
				a = c;
			} else {
				b = c;
			}
			c = (a+b)/2;
			xi = S.get(c);
			xinext = S.get(c+1);
		}
		
		if (2*d < rMinus.get(xi) + omega.get(xi) + rPlus.get(xinext) - omega.get(xinext)) {
			return xi;
		} else {
			return xinext;
		}
		
	}
	
	
	/**
	 * Reduce the number of elements in the summary to b+1.
	 * Then, the approximation error increases from epsilon to epsilon + 1/b
	 * @param b
	 */
	public void prune(int b) {
		
		if (!(b <= (S.size() - 1) && b > 0)) {
			throw new IllegalArgumentException("The memory budget b for pruning must be greater than 1 and less than size of S");
		}
		
		ArrayList<Integer> newS = new ArrayList<Integer>();
		Map<Integer,Integer> newRPlus = new HashMap<Integer,Integer>();
		Map<Integer,Integer> newRMinus = new HashMap<Integer,Integer>();
		Map<Integer,Integer> newOmega = new HashMap<Integer,Integer>();
		
		for (int i = 1; i <= (b+1); i++) {
			Integer xiPrime = query( ((((double)i-1)/(double)b)) * omegaSum);
			newS.add(xiPrime);
			newRPlus.put(xiPrime, rPlus.get(xiPrime));
			newRMinus.put(xiPrime, rMinus.get(xiPrime));
			newOmega.put(xiPrime, omega.get(xiPrime));
		}
		
		S = newS;
		rMinus = newRMinus;
		rPlus = newRPlus;
		omega = newOmega;
		epsilon += 1/(double)b;
	}
	
	
	/**
	 * Compute the approximated percentile based on the quantile summary
	 * @param p a number between 0 and 1
	 * @return the p-th percentile 
	 */
	public double computePercentile(double p) {
		if (S.size() == 1) {
			return S.get(0);
		}
		int i = 0;
		Integer xi = S.get(i); 
		Integer xinext = S.get(i+1); 
		if (p*omegaSum < rPlus.get(xi)) {
			return xi;
		} else if (p*omegaSum == rPlus.get(S.get(S.size()-1))) {
			return S.get(S.size()-1);
		}	
		
		/* dichotomic search */
		int a = 0;
		int b = S.size()-1;
		int c = (a+b)/2;
		xi = S.get(c);
		xinext = S.get(c+1);
		while(!(p*omegaSum > rPlus.get(xi) && p*omegaSum < rPlus.get(xinext) || (p*omegaSum == rPlus.get(xi)))) {
			if (p*omegaSum <= rPlus.get(xi)) {
				b = c;
			} else if (p*omegaSum >= rPlus.get(xinext)) {
				a = c;
			}
			c = (a+b)/2;
			xi = S.get(c);
			xinext = S.get(c+1);
		}
		
		if (rPlus.get(xi) == p * omegaSum) {
			return (double)(xi + xinext)/2;
		} else {
			return xinext;
		}
	}
	
	
	/**
	 * @return a string of the approximated quantile based on the quantile summary
	 */
	public String getQuantile() {
		return "\nApproximate Quantiles (epsilon = " + epsilon + " / weight of multi-set : " + omegaSum +") : " 
	+ "\n0% : " + computePercentile(0) + "\n25% : " + computePercentile(0.25) + "\n50% : " + computePercentile(0.5)
	+ "\n75% : " + computePercentile(0.75) + "\n100% : " + computePercentile(1);
	}

	
	@Override
	public String toString() {
		String strS = "S      : ";
		String strRPlus = "\nrPlus  : ";
		String strRMinus = "\nrMinus : ";
		String strOmega = "\nOmega  : ";
		String strOmegaSum = "\nOmegaSum : " + omegaSum;
		String strEpsilon = "\nEpsilon : " + epsilon;
		for (int i = 0; i <S.size(); i++) {
			Integer xi = S.get(i);
			strS += String.valueOf(xi) + " ";
			strRPlus += String.valueOf(rPlus.get(xi)) + " ";
			strRMinus += String.valueOf(rMinus.get(xi)) + " ";
			strOmega += String.valueOf(omega.get(xi)) + " ";
		}
		return strS + strRPlus + strRMinus + strOmega + strOmegaSum + strEpsilon;
	}
	

	/*public static void main(String[] args) {
		
		
		ArrayList<Pair> multiSet3 = new ArrayList<Pair>();
		ArrayList<Pair> multiSet1 = new ArrayList<Pair>();
		ArrayList<Pair> multiSet2 = new ArrayList<Pair>();
		ArrayList<Pair> multiSet4 = new ArrayList<Pair>();
		multiSet4.add(new Pair(5,1));
		multiSet4.add(new Pair(3,1));
		
		int max = 100;
		int min = 10;
		int max2 = 20;
		int min2 = 5;
		Random rand = new Random();

		for (int i = 0; i<rand.nextInt((max2 - min2) + 1) + min2; i++) {
			int v = rand.nextInt((max - min) + 1) + min;
			int w = rand.nextInt((max - min) + 1) + min;
			multiSet1.add(new Pair(v,1));
			multiSet3.add(new Pair(v,w));
		}
		
		
		for (int i = 0; i<rand.nextInt((max2 - min2) + 1) + min2; i++) {
			int v = rand.nextInt((max - min) + 1) + min;
			int w = rand.nextInt((max - min) + 1) + min;
			multiSet2.add(new Pair(v,w));
			multiSet3.add(new Pair(v,w));
		}
		
		QuantileSummary q1 = new QuantileSummary(multiSet1);
		QuantileSummary q2 = new QuantileSummary(multiSet2);
		QuantileSummary q3 = new QuantileSummary(multiSet3);
		QuantileSummary q4 = new QuantileSummary(multiSet4);
		
		q4.prune(1);*/
		
		/*multiSet1.add(new Pair(16,1));
		multiSet1.add(new Pair(12,1));
		multiSet1.add(new Pair(1,1));
		multiSet1.add(new Pair(5,1));
		multiSet1.add(new Pair(7,1));
		multiSet1.add(new Pair(11,1));
		multiSet1.add(new Pair(8,1));
		multiSet1.add(new Pair(2,1));
		multiSet1.add(new Pair(3,1));
		multiSet1.add(new Pair(9,1));
		multiSet1.add(new Pair(10,1));
		multiSet1.add(new Pair(15,1));
		multiSet1.add(new Pair(17,1));
		multiSet1.add(new Pair(20,1));
		multiSet1.add(new Pair(19,1));
		multiSet1.add(new Pair(18,1));
		
		multiSet2.add(new Pair(8,1));
		multiSet2.add(new Pair(12,1));
		multiSet2.add(new Pair(1,1));
		multiSet2.add(new Pair(3,1));
		multiSet2.add(new Pair(2,1));
		multiSet2.add(new Pair(16,1));
		

		multiSet3.add(new Pair(3,3));
		multiSet3.add(new Pair(8,9));
		multiSet3.add(new Pair(1,8));
		multiSet3.add(new Pair(2,4));
		multiSet3.add(new Pair(7,2));
		multiSet3.add(new Pair(6,1));
		multiSet3.add(new Pair(5,2));
		multiSet3.add(new Pair(12,1));
		multiSet3.add(new Pair(11,4));
		multiSet3.add(new Pair(10,3));
		multiSet3.add(new Pair(9,6));
		multiSet3.add(new Pair(3,1));
		
		multiSet3.add(new Pair(26,2));
		multiSet3.add(new Pair(47,1));
		multiSet3.add(new Pair(15,3));
		multiSet3.add(new Pair(7,5));
		

		multiSet4.add(new Pair(6,2));
		multiSet4.add(new Pair(1,6));
		multiSet4.add(new Pair(4,1));
		multiSet4.add(new Pair(2,7));
		multiSet4.add(new Pair(3,3));
		multiSet4.add(new Pair(7,7));
		
		
		
		QuantileSummary q1 = new QuantileSummary(multiSet1);
		QuantileSummary q2 = new QuantileSummary(multiSet2);
		QuantileSummary q3 = new QuantileSummary(multiSet3);
		QuantileSummary q4 = new QuantileSummary(multiSet4);

		QuantileSummary q5 = new QuantileSummary();
		System.out.println(q1.toString());
		
		q5.merge(q1);
		System.out.println(q5.toString());*/
		/*System.out.println(q.toString());
		System.out.println(q.rMinusExtended(5));
		System.out.println(q.rPlusExtended(5));
		System.out.println(q.omegaExtended(5));*/
		
		/*for(Pair p : multiSet1) {
			System.out.println(p.toString());
		}
		System.out.println("");
		for(Pair p : multiSet2) {
			System.out.println(p.toString());
		}
		System.out.println("");
		for(Pair p : multiSet3) {
			System.out.println(p.toString());
		}*/

		/*System.out.println(q1.query(8));
		System.out.println("q1 :");
		System.out.println(q1.toString());
		q1.prune(5);
		System.out.println("\nq1 pruned :");
		System.out.println(q1.toString());*/
		
		/*System.out.println("q4 :");
		System.out.println(q4.toString());
		q4.prune(3);
		
		System.out.println(q4.getQuantile());*/
		
		//q1.prune(5);
		//System.out.println("\nq1 pruned :");
		//System.out.println(q1.toString());
		
		/*q1.merge(q2);
		System.out.println("\nq2 :");
		System.out.println(q2.toString());
		System.out.println("\nmerge :");
		System.out.println(q1.toString());
		System.out.println("");
		System.out.println("\nmerge expected :");
		System.out.println(q3.toString());*/
		

	//}
	
	
	
}
