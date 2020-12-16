package cea.streamer.algs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import cea.util.connectors.CodeConnectors;
import cea.util.connectors.RedisConnector;
import redis.clients.jedis.Jedis;

public class NeuralNetwork extends MLalgorithms {
	
	String regressionProblem;
	String hiddenLayers;
	String seed;
	String dependantVariable;
	String predicatorVariables;
	
	public NeuralNetwork(String[] args) throws Exception {
		regressionProblem=args[0];
		hiddenLayers=args[1];		
		dependantVariable=args[2];
		predicatorVariables=args[3];
		seed=args[4];
		if(args.length < 5) {
			throw new Exception("Missing hyperparms:\n "+listNeededHyperparams());
		}	
	}
	
	
	public NeuralNetwork() {		
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/neuralNetwork.props").openStream()) {			
			properties.load(props);
			regressionProblem = properties.getProperty("regression.problem").trim();
			hiddenLayers = properties.getProperty("hidden.layers").trim();
			seed = properties.getProperty("seed").trim();
			dependantVariable = properties.getProperty("dependant.variable").trim();
			predicatorVariables = properties.getProperty("predicator.variables").trim();		
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void storeParams(String id) {
		
		Jedis jedis = RedisConnector.getJedis();	

		jedis.del("param"+id);
		jedis.del("regTuning"+id);
		
		jedis.rpush("param"+id, "regressionProblem " + regressionProblem);
		jedis.rpush("param"+id, "hiddenLayers " + hiddenLayers);
		jedis.rpush("param"+id, "seed " + seed);
		
		if (regressionProblem.equals("TRUE")) {
			jedis.rpush("regTuning"+id, "dependantVariable " + dependantVariable);
			jedis.rpush("regTuning"+id, "predicatorVariables " + predicatorVariables);
		}

		jedis.close();

}

	@Override
	public void learn(Vector<TimeRecord> data, String id) {
		storeParams(id);
		String learningFile = new GlobalUtils().getAbsoluteBaseProjectPath()+"src/main/resources/algs/neuralNetworkTrain.R";		
		RedisConnector.dataToRedis(data, "datatrain" + id);		
		CodeConnectors.execRFile(learningFile, id);
	}

	@Override
	public void run(Vector<TimeRecord> data, String id) {		
		storeParams(id);
		String testFile = new GlobalUtils().getAbsoluteBaseProjectPath()+"src/main/resources/algs/neuralNetworkTest.R";
		RedisConnector.dataToRedis(data, "dataTest"+id);
		CodeConnectors.execRFile(testFile, id);		
		
		RedisConnector.retreiveOutput(data, id);
	}

	@Override
	public String listNeededHyperparams() {
		String params =" regressionProble;\r\n" + 
				"	hiddenLayers;\r\n" + 
				"	dependantVariable;\r\n" + 
				"	predicatorVariables;\r\n" + 
				"	seed;\r\n";
		return params;
	}

}
