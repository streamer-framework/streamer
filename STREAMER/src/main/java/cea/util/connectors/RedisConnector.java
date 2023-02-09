package cea.util.connectors;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import redis.clients.jedis.Jedis;

public class RedisConnector {
	
	public final static String TARGET_TAG="target";
	public final static String OUTPUTS_TAG="outputs";
	public final static String SEPARATOR_TAG="separator";
	public final static String DATATRAIN_TAG="datatrain";
	public final static String DATATEST_TAG="datatest";
	public final static String MODEL_TAG = "model";
	
	/**
	 * Host of the Redis database
	 */
	static String host;	


	/**
	 * Store a vector of time records in Redis
	 *
	 * @param data the vector of time records
	 * @param tag (extra info to id)
	 * @param id
	 */
	public static void dataToRedis(Vector<TimeRecord> data, String tag, String id) {
		String key = tag+id;
		Jedis jedis = getJedis();	
		
		//System.out.println("["+id+"] Connection to Redis server");
		//Clean the of keys of Redis
		jedis.del(key);
		jedis.del(key+TARGET_TAG);
		jedis.del(OUTPUTS_TAG+id);
		
		StringBuilder S=null;
		for (TimeRecord record: data) {			
			S = new StringBuilder(record.getTimeStamp() + record.getSeparatorTSRedis());
			for (String keyValue : record.getValues().keySet()) {
				S.append(record.getValues().get(keyValue) + record.getSeparatorFieldsRedis());
			}
			jedis.rpush(key, S.substring(0, S.lastIndexOf(record.getSeparatorFieldsRedis())).toString());
			if(!record.getTarget().isEmpty()) {//there is a target (expected output)
				jedis.rpush(key+TARGET_TAG, GlobalUtils.listToString(record.getTarget(),record.getSeparatorFieldsRedis()));
			}
		}
		
		jedis.close();
		//System.out.println("["+id+"] Data stored in Redis");
	}
	
	/**
	 * Replace the values of the record by the new ones retrieved from Redis
	 * IMPORTANT: records must preserve the order
	 * 
	 * @param tag (extra info to id)
	 * @param id 
	 * @param containsTargets true if data retrieved from Redis contains the targets (which are stored in the records), false otherwise
	 * @return the vector of time records corresponding to the key
	 */
	public static Vector<TimeRecord> redisToRecords(Vector<TimeRecord> records, String tag, String id, boolean containsTargets) {
		String key = tag+id;
		Jedis jedis = RedisConnector.getJedis();	
		List<String> dataStr = jedis.lrange(key, 0, -1);	
		jedis.close();
		int i=0;
		for(String s : dataStr ) {
			
			int lastIdx = s.length() - 1; //there is a bug in rR so it gives back weird characters from Redis, this is a filter, to remove when bug is solved
			while (Character.isDigit(s.charAt(lastIdx)) || Character.isLetter(s.charAt(lastIdx)) || s.charAt(lastIdx) == ';'
					|| s.charAt(lastIdx) == '-' || s.charAt(lastIdx) == ':' || s.charAt(lastIdx) == ' '
					|| s.charAt(lastIdx) == '.') {//it eliminates weird characters
				lastIdx--;
			}
			String[] fields = (s.substring(lastIdx + 1, s.length())).split(records.get(0).getSeparatorFieldsRawData());
			//String[] fields = s.split(records.get(0).getSeparatorRawData());
			Map<String,String> values = records.get(i).getValues();
			for (String keyValue : values.keySet()) { //replace the values of the record by the new ones stored in Redis
				records.get(i).setValue(values.get(keyValue), fields[0]);					
			}
			if(containsTargets) { //replace also the targets
				records.get(i).setTarget(fields[1], records.get(i).getTargetLabel());
			}
			i++;
		}
		jedis.del(key); //delete old instances just retrieved from
		jedis.close();
		return records;
	}
	
	/**
	 * Method that gets back the data stored in Redis
	 * IMPORTANT: records must preserve the order
	 * 
	 * @param records Current time records vector 
	 * @param tag (extra info to id)
	 * @param id 
	 * @return Vector of time records updated with redis values corresponding to the key
	 */
	public static Vector<TimeRecord> retrieveOutput(Vector<TimeRecord> records, String id) {
		String key = OUTPUTS_TAG+id;		
		Jedis jedis = getJedis();
		List<String> predicted = jedis.lrange(key, 0, -1);		
		String separatorFields = records.get(0).getSeparatorFieldsRedis();//by default, user can define its own separator token in source algorithm and set its value in this line
		if(jedis.exists(SEPARATOR_TAG+id)) {
			separatorFields = jedis.get(SEPARATOR_TAG+id); 		
		}
					
		if(predicted != null) {
			if(predicted.size() != records.size()) {
				System.err.println("["+id+"] Outputs size ("+predicted.size()+") not matching the time records size ("+ records.size()+"). Outputs not stored!");
			}else {
				int i=0;
				String[] outputs;
				for(TimeRecord record: records) {
					outputs = predicted.get(i).split(separatorFields);//we split if the output has several values					
					record.setOutput(Arrays.asList(outputs));
					i++;
				}
			}		
		}
	/*	//we clean the information just retrieved from Redis
		jedis.del(OUTPUTS_LABEL+id);
		jedis.del(SEPARATOR_LABEL+id); */
		jedis.close();
						
		return records;
	}
	

	/**
	 * Clear and reset the keys in Redis (confusion matrix)
	 */
	public static void cleanKeys(String[] ids) {

		Jedis jedis = getJedis();
		if(ids.length == 0) {//default producer
			ids = new String[1];
			ids[0] = "default";
		}
		
		for(String id : ids) {
			jedis.del("confMat"+id);//clear confusion matrix in redis (used in 2 algs)
			jedis.del(DATATRAIN_TAG+id);
			jedis.del(DATATEST_TAG+id);
			jedis.del(DATATRAIN_TAG+id+TARGET_TAG);
			jedis.del(DATATEST_TAG+id+TARGET_TAG);
			jedis.del(OUTPUTS_TAG+id);
			jedis.del(SEPARATOR_TAG+id);
			System.out.println("["+id+"] Cleaning Redis Keys");
		}							
		jedis.close();
	}
	
	public static void cleanModel(String id) {
		Jedis jedis = getJedis();
		jedis.del(id+MODEL_TAG);
		jedis.close();
		System.out.println("["+id+"] Cleaning Redis model");
	}
	
	
	/**
	 * Get the model stored in redis
	 * @param id Identification of problem
	 * @return model stored in redis under the name id+model
	 */
	public static Object getModelFromRedis(String id) {
		Jedis jedis = getJedis();				
		String model = (String)jedis.get(id+MODEL_TAG); //serialized model
		return model;
	}

	/**
	 * Get the model stored in redis
	 * @param id Identification of problem
	 * @return model stored in redis under the name MODEL_TAG+id
	 */
	public static void storeModelInRedis(String id, String model) {
		Jedis jedis = getJedis();
		jedis.del(id+MODEL_TAG);		
		jedis.set(id+MODEL_TAG,model);
		jedis.close();
	}
	
	/**
	 * Get the model stored in redis
	 * @param id Identification of problem
	 * @param content in byte[] 
	 * @return model stored in redis under the name MODEL_TAG+id
	 */
	public static void storeModelInRedis(String id, byte[] content) {
		Jedis jedis = getJedis();
		jedis.del(id/*+MODEL_TAG*/);	
		//String key = (id+MODEL_TAG);
		jedis.set(id.getBytes(),content);
		jedis.close();
	}
	
	/**
	 * Get an instantiation of Redis
	 * @return instantiation of Redis
	 */
	public static Jedis getJedis() {
		Jedis jedis = new Jedis(getRedisIP(),Integer.parseInt(getRedisPort()));
		return jedis;
	}
	
	/**
	 * Return the IP to connect to Redis.
	 * It looks in redis.props file
	 * @return IP to connect Redis
	 */
	public static String getRedisIP() {
		String host="localhost:6379";
		Properties properties = new Properties();
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles+"redis.props")) {
			properties.load(props);
			host = properties.getProperty("host").replace(" ","");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return host.replace("http://", "").replace("https://", "").replace(" ","").split(":")[0];
	}
	
	/**
	 * Return the port to connect to Redis.
	 * It looks in redis.props file
	 * @return port to connect Redis (in blank if non is indicated)
	 */
	public static String getRedisPort() {
		String host="localhost:6379";
		Properties properties = new Properties();
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles+"redis.props")) {
			properties.load(props);
			host = properties.getProperty("host").replace(" ","");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String[] ipPort = host.replace("http://", "").replace("https://", "").replace(" ","").split(":"); 
		if(ipPort.length > 1) {
			return ipPort[1];
		}else {
			//By default, better to send the default port of Redis
			return "6379";
		}
	}

}
