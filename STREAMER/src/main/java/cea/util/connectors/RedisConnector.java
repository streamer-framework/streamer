package cea.util.connectors;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;
import cea.util.GlobalUtils;
import redis.clients.jedis.Jedis;

public class RedisConnector {
	
	/**
	 * Host of the Redis database
	 */
	static String host;

	/**
	 * Store a vector of time records in Redis
	 *
	 * @param data the vector of time records
	 * @param key  the key associated with the data
	 */
	public static void dataToRedis(Vector<TimeRecord> data, String key) {

		Jedis jedis = getJedis();	

		//System.out.println("Connection to Redis server");
		jedis.del(key);
		jedis.del(key+"target");
		//remove the keys for the output concerning the previous testing data
		jedis.del("classifResults"+key);
		jedis.del("separation"+key);
		Iterator<TimeRecord> it = data.iterator();
		while (it.hasNext()) {
			TimeRecord record = it.next();
			//System.err.println(record.toString());			
			String S = record.getTimeStamp() + ";";
			for (String keyValue : record.getValues().keySet()) {
				S += record.getValues().get(keyValue) + " ";
			}
			S = S.substring(0, S.lastIndexOf(" "));
			jedis.rpush(key, S);
			if(record.getTarget() != null) {//there is a target (expected output)
				jedis.rpush(key+"target", record.getTarget());
			}
		}
		
		jedis.close();
		//System.out.println("Data stored in Redis");
	}
	
	/**
	 * Method that gets back the data stored in Redis
	 *
	 * @param key  Redis key
	 * @param name name of the time records when the data have been stored in Redis
	 * @return the vector of time records corresponding to the key
	 */
	public static Vector<TimeRecord> redisToRecords(String key, String name, String id) {
		Jedis jedis = getJedis();	
		Vector<TimeRecord> records = new Vector<TimeRecord>();
		List<String> dataStr = jedis.lrange(key, 0, -1);	
		jedis.close();
		
		Iterator<String> it = dataStr.iterator();
		while (it.hasNext()) {
			String s = it.next();	
			int lastIdx = s.length() - 1;
			/* get the time record */
			while (Character.isDigit(s.charAt(lastIdx)) || Character.isLetter(s.charAt(lastIdx)) || s.charAt(lastIdx) == ';'
					|| s.charAt(lastIdx) == '-' || s.charAt(lastIdx) == ':' || s.charAt(lastIdx) == ' '
					|| s.charAt(lastIdx) == '.') {//it eliminates weird characters
				lastIdx--;
			}
			String recordStr = name + ";" + s.substring(lastIdx + 1, s.length());
			recordStr = recordStr.replace("  "," ");
			//System.err.println(recordStr);
			//Construct TimeRecord instance accordingly to streaming properties
			String origin = id;
			if (id.equals("default")) {
				origin = ".";
			}
			Properties properties = new Properties();
			try (InputStream props = Resources.getResource("setup/"+origin + "/" + "streaming.props").openStream()) {
				properties.load(props);
				String problemType = (GlobalUtils.packageTimeRecords+".") + properties.getProperty("problem.type").trim();
				Class recC = Class.forName(problemType + "Record");
				TimeRecord recObj = (TimeRecord) recC.newInstance();
				recObj.fill(recordStr);
				records.add(recObj);

			} catch (IOException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
		return records;
	}

	/**
	 * Clear and reset the keys in Redis (confusion matrix)
	 */
	public static void cleanKeys(String[] ids) {

		Jedis jedis = getJedis();
		if(ids.length == 0) {//default producer
			jedis.del("confMatdefault");//clear confusion matrix in redis
		}else {
			for(int i =0; i<ids.length; i++) {
				jedis.del("confMat"+ids[i]);//clear confusion matrix in redis
			}
			
		}
		jedis.close();
	}
	
	
	public static Vector<TimeRecord> retreiveOutput(Vector<TimeRecord> data, String key) {
		Jedis jedis = getJedis();
				
		String predicted = jedis.get("classifResults"+key); 
		String separation = jedis.get("separation"+key); 
		
		//List<String> pred = jedis.lrange("prueba", 0, -1);
		if(predicted != null) {
			String[] outputs = predicted.trim().split(separation);
			int i=0;
			Iterator<TimeRecord> it = data.iterator();
			boolean error = false;
			while (it.hasNext() && !error) {
				TimeRecord record = it.next();
			/*	while(outputs[i].trim().equals("") && i<outputs.length) {
					i++;	
				}	*/
				if(i != outputs.length) {
					record.setOutput(outputs[i]);
				}else {
					error = true;
					System.err.println("Output not matching the time records input");
				}
				i++;
			}		
		}
				
		return data;
	}
	
	/**
	 * Get the model stored in redis
	 * @param id Identification of problem
	 * @return model stored in redis under the name "model"+id
	 */
	public static Object getModelFromRedis(String id) {
		Jedis jedis = getJedis();				
		String model = (String)jedis.get("model"+id); //devuelve lista o como???
		return model;
	}

	/**
	 * Get the model stored in redis
	 * @param id Identification of problem
	 * @return model stored in redis under the name "model"+id
	 */
	public static void storeModelInRedis(String id, String model) {
		Jedis jedis = getJedis();
		jedis.del("model"+id);		
		jedis.rpush("model"+id,model);
		jedis.close();
	}
	
	/**
	 * Get an instantiation of Redis
	 * @return instantiation of Redis
	 */
	public static Jedis getJedis() {
		String host="localhost";
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/redis.props").openStream()) {
			properties.load(props);
			host = properties.getProperty("host").trim();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Jedis jedis = new Jedis(host);
		return jedis;
	}
	
	/**
	 * Return the IP to connect to Redis.
	 * It looks in redis.props file
	 * @return IP to connect Redis
	 */
	public static String getRedisIP() {
		String host="localhost";
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/redis.props").openStream()) {
			properties.load(props);
			host = properties.getProperty("host").trim();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return host.replace("http://", "").replace("https://", "").trim().split(":")[0];
	}
	
	/**
	 * Return the port to connect to Redis.
	 * It looks in redis.props file
	 * @return port to connect Redis (in blank if non is indicated)
	 */
	public static String getRedisPort() {
		String host="localhost";
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/redis.props").openStream()) {
			properties.load(props);
			host = properties.getProperty("host").trim();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String[] ipPort = host.replace("http://", "").replace("https://", "").trim().split(":"); 
		if(ipPort.length > 1) {
			return ipPort[1];
		}else {
			//By default, better to send the default port of Redis
			return "6379";
		}
	}

}
