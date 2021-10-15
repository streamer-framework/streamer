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

public class XGBoost extends MLalgorithms {

	String objective;
	String num_class;
	String nround;
	String colsubsample;
	String maxdepth_minchild;
	String gamma;

	public XGBoost(String[] args) throws Exception {
		objective = args[0];
		num_class = args[1];
		nround = args[2];
		colsubsample = args[3];
		maxdepth_minchild = args[4];
		gamma = args[5];
		
		if(args.length < 6) {
			throw new Exception("All needed hyperparms are not given.\n "+listNeededHyperparams());
		}
	
		connect();
	}

	public XGBoost() {

		Properties properties = new Properties();
		try (InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+"xgboost.props").openStream()) {

			properties.load(props);
			// String booster = properties.getProperty("booster").replace(" ","");
			objective = properties.getProperty("objective").replace(" ","");
			num_class = properties.getProperty("num.class").replace(" ","");

			nround = properties.getProperty("nround.tuning").replace(" ","");
			colsubsample = properties.getProperty("colsub.sample.tuning").replace(" ","");
			maxdepth_minchild = properties.getProperty("maxdepth.minchild.tuning").replace(" ","");
			gamma = properties.getProperty("gamma.tuning").replace(" ","");

			connect();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void connect() {
		Jedis jedis = RedisConnector.getJedis();
		jedis.del("param");
		jedis.del("tuning");
		// jedis.rpush("param","booster " + booster);
		jedis.rpush("param", "objective " + objective);
		jedis.rpush("param", "num_class " + num_class);
		jedis.rpush("tuning", "nround " + nround);
		jedis.rpush("tuning", "colsubsample " + colsubsample);
		jedis.rpush("tuning", "maxdepth_minchild " + maxdepth_minchild);
		jedis.rpush("tuning", "gamma " + gamma);
		jedis.close();
	}

	@Override
	public void learn(Vector<TimeRecord> data, String id) {
		String learningFile = new GlobalUtils().getAbsoluteBaseProjectPath()+"src/main/resources/algs/xgbmodelTrain.R";
//		String testFile = "./src/main/resources/algs/xgbmodelTrain.R";
		RedisConnector.dataToRedis(data,RedisConnector.DATATRAIN_TAG,id);
		CodeConnectors.execRFile(learningFile, id);
	}

	@Override
	public void run(Vector<TimeRecord> data, String id) {
		String testFile = new GlobalUtils().getAbsoluteBaseProjectPath()+"src/main/resources/algs/xgbmodelTest.R";
//		String testFile = "./src/main/resources/algs/xgbmodelTest.R";
		RedisConnector.dataToRedis(data,RedisConnector.DATATEST_TAG,id);
		CodeConnectors.execRFile(testFile, id);
	}

	@Override
	public String listNeededHyperparams() {
		String params =" objective;\r\n" + 
				"	num_class;\r\n" + 
				"	colsubsample;\r\n" + 
				"	maxdepth_minchild;\r\n" + 
				"	gamma;\r\n";
		return params;
	}

}
