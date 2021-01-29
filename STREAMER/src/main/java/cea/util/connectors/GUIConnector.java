package cea.util.connectors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.io.Resources;

import cea.streamer.core.TimeRecord;
import cea.util.Log;
import redis.clients.jedis.Jedis;

/**
 * Class that makes the connection between Java and the graphical interface
 *
 */

public class GUIConnector {


	/**
	 * Send the results of the test algo to a JSON file
	 * @param line a line of the output of the algo
	 * @param jsonFileName name of the JSON file
	 */
	protected static void resultToJSON(String line, String jsonFileName) {
		if (!line.equals("[1] \"OK\"")) {
    		JSONObject obj = new JSONObject();
    		JSONArray classifResult = new JSONArray();
    		String cleanLine = line;
    		if (line.contains("[1] ")) {
    			cleanLine = line.substring(4); // remove the "[1] "
    		}
    		String[] split = cleanLine.split(" ");
    		if (split.length>1) {
	    		for (int i = 0;i <split.length;i++) {
	    			classifResult.add(split[i]);
	    		}
	    		obj.put("result", classifResult);
    		} else {
	    		obj.put("result", split[0]);
    		}
    		obj.put("newResult", "true");
    		
    		String directory = jsonFileName.substring(0, jsonFileName.lastIndexOf("/")+1);
    		File file = new File(directory);
    		if(!file.exists()) {
    			new File(directory).mkdirs();
    		}
    		try {
        		FileWriter fw = new FileWriter(jsonFileName);
    			fw.write(obj.toJSONString());
    			System.out.println("Results sent to JSON file : " + jsonFileName);
    			fw.close();
    		} catch (IOException e) {
    			
				e.printStackTrace();
			}
    	}
	}
	
	/**
	 * Send the data to a JSON file
	 * @param records a vector of time records
	 * @param jsonFileName name of the JSON file
	 */
	public static void dataToJSON(Vector<TimeRecord> records, String jsonFileName) {
		JSONObject obj = new JSONObject();
		JSONArray timestampJSON = new JSONArray();
		JSONArray valueJSON = new JSONArray();
		String fieldInflux = records.get(0).getName();
		Iterator<TimeRecord> it = records.iterator();
		while(it.hasNext()) {
			TimeRecord record = it.next();
			timestampJSON.add(record.getTimeStamp());
			for (String keyValue : record.getValues().keySet()) {
				valueJSON.add(record.getValues().get(keyValue)); //to change if multiples features
			}
		}
		obj.put("timestamps", timestampJSON);
		obj.put("values", valueJSON);
		obj.put("newResult", "true");
		obj.put("fieldInflux", fieldInflux);
		try (FileWriter file = new FileWriter(jsonFileName)) {
			file.write(obj.toJSONString());
			System.out.println("Time records sent to JSON file : " + jsonFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void updateConfusionMatrix(Vector<TimeRecord> data, String id, int windowLength) {

		String host="localhost";
		Properties properties = new Properties();
		try (InputStream props = Resources.getResource("setup/redis.props").openStream()) {
			properties.load(props);
			host = properties.getProperty("host").trim();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Jedis jedis = new Jedis(host);
		int[] confMat = new int[4];
		String confMatStr = jedis.get("confMat"+id);

		if (confMatStr != null) {
			String[] confMatArr = confMatStr.replaceAll("[^0-9, ]", "").split(", ");
			for (int i = 0; i<4; i++) {
				confMat[i] = Integer.parseInt(confMatArr[i]);
			}
		}

		String predicted = jedis.get("classifResults"+id);
		if(predicted != null && !predicted.replaceAll("[^0-9]", "").equals("")) {
			predicted = predicted.replaceAll("[^0-9]", "");
			String actual = "";
			for (int i = 0; i < data.size(); i += windowLength) {
				boolean anomaly = false;
				for (int j = i; j < (i + windowLength); j++) {
					if (j < data.size() && Double.parseDouble(data.get(j).getTarget()) != 0) {
						anomaly = true;
						break;
					}
				}
				if (anomaly) {
					actual += "1";
				} else {
					actual += "0";
				}
			}
			for (int i = 0; i < Math.min(actual.length(),predicted.length()); i++) {
				if (actual.charAt(i) == '0' && predicted.charAt(i) == '0') {
					confMat[0] = confMat[0] + 1;
				} else if (actual.charAt(i) == '1' && predicted.charAt(i) == '0') {
					confMat[2] = confMat[2] + 1;
				} else if (actual.charAt(i) == '0' && predicted.charAt(i) == '1') {
					confMat[1] = confMat[1] + 1;
				} else if (actual.charAt(i) == '1' && predicted.charAt(i) == '1') {
					confMat[3] = confMat[3] + 1;
				}
			}

			Log.displayLogTest.info("\n"+id+": ######### New Confusion Matrix #########\n");
			Log.displayLogTest.info("          Ref 0    Ref 1");
			Log.displayLogTest.info("Pred 0        " + confMat[0] + "            " + confMat[2]);
			Log.displayLogTest.info("Pred 1        " + confMat[1] + "            " + confMat[3]);
			if (confMat[0] + confMat[1] + confMat[2] + confMat[3] != 0) {
				double accuracy = (double)(((int)(1000*(100.0*((double)(confMat[0] + confMat[3]) / (double)(confMat[0] + confMat[1] + confMat[2] + confMat[3]))))))/1000.0;
				Log.displayLogTest.info("\nAccuracy : " + accuracy + "%");
			}
			if (confMat[2]+confMat[3] != 0) {
				double detected = (double)(((int)(1000*(100.0*(double)(confMat[3]) / (double)(confMat[2] + confMat[3])))))/1000.0;
				Log.displayLogTest.info("\nAnomaly detected : " + detected + "%");
			}

			jedis.set("confMat" + id, confMat[0] + ", " + confMat[1] + ", " + confMat[2] + ", " + confMat[3] + ", ");

		}
		jedis.close();
	}

}
