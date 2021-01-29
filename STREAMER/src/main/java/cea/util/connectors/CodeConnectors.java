package cea.util.connectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cea.util.Log;


/**
 * Class that makes the connection between R and Java
 */
public class CodeConnectors {

	/**
	 * Method that executes in command line a R script and prints the output into a log file
	 *
	 * @param scriptFileName the path of the R script
	 */
	public static void execRFile(String scriptFileName, String id) {
		String redisIP =  RedisConnector.getRedisIP();
		String redisPort = RedisConnector.getRedisPort();
		try {
			System.out.println(id+": Starts execution " + scriptFileName + " with args " +id+ " " + redisIP + " " + redisPort);
			Process p = Runtime.getRuntime().exec("Rscript " + scriptFileName + " " +id+ " " + redisIP + " " + redisPort);
			p.waitFor();

			//Separating logs between two executions
			Log.separate(Log.infoLog, id+": "+scriptFileName);
			Log.separate(Log.errorLog, id+": "+scriptFileName);

			//Read Outputs
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(p.getInputStream())
			);
			String line;
			String lastLine = "";
			while ((line = reader.readLine()) != null) {
				if (scriptFileName.contains("Train")) {
					Log.displayLogTrain.info(line);
				}
				Log.infoLog.info(line);
				lastLine = line;
			}

			//Read Errors
			BufferedReader readerErr = new BufferedReader(
					new InputStreamReader(p.getErrorStream())
			);
			String lineErr;
			while ((lineErr = readerErr.readLine()) != null) {
				Log.errorLog.info(lineErr);
			}

			System.out.println(id+": Finishes execution " + scriptFileName + " with args " +id+ " " + redisIP + " " + redisPort);

			//Send data to JSON
			if (scriptFileName.contains("Test")) {
				String jsonFileName = "./json/result" + id + ".json";
				GUIConnector.resultToJSON(lastLine, jsonFileName);
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Method that executes in command line a Python script and prints the output into a log file
	 * 
	 * @param scriptFileName the path of the Python script
	 * @param id the arguments needed for the Python script (if needed)
	 */
	public static String execPyFile(String scriptFileName, String id) {
		String redisIP =  RedisConnector.getRedisIP();
		String redisPort =  RedisConnector.getRedisPort();
		System.out.println(id+": Starts execution " + scriptFileName + " with args " + id+ " " + redisIP + " " + redisPort);
		String lastLine = "";

		try {
			
			Process p = null;
			
			String OS = System.getProperty("os.name").toLowerCase();
			
			if (OS.startsWith("windows")) {
				ProcessBuilder pb = new ProcessBuilder();
				pb.command("cmd", "/c", "conda activate ts_env && python " + scriptFileName + " " +id+ " " + redisIP + " " + redisPort);
				p = pb.start();
			} else if (OS.equals("linux")) {//MAC
				p = Runtime.getRuntime().exec("python3 " + scriptFileName + " " +id+ " " + redisIP + " " + redisPort);
				p.waitFor();			
			} else if (OS.equals("mac")) {//MAC
				p = Runtime.getRuntime().exec("python3 " + scriptFileName + " " +id+ " " + redisIP + " " + redisPort);
				p.waitFor();
			} else if (OS.equals("nix") || OS.equals("nux") || OS.equals("aix") ) {//UNIX
				p = Runtime.getRuntime().exec("python3 " + scriptFileName + " " +id+ " " + redisIP + " " + redisPort);
				p.waitFor();
			} 
			
			// Separating logs between two executions
			Log.separate(Log.infoLog, id+": "+scriptFileName);
			Log.separate(Log.errorLog, id+": "+scriptFileName);

			// Read Outputs
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				if (scriptFileName.toLowerCase().contains("train")) {
					Log.displayLogTrain.info(line);
				}
				Log.infoLog.info(line);
				lastLine = line;

			}
			// Read Errors
			BufferedReader readerErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String lineErr;
			while ((lineErr = readerErr.readLine()) != null) {
				Log.errorLog.info(lineErr);
			}
			
			p.waitFor();
			System.out.println(id+": Finishes execution " + scriptFileName + " with args " +id+ " " + redisIP + " " + redisPort);
			
			//Send data to JSON
			/*if (scriptFileName.toLowerCase().contains("test")) {
				String jsonFileName = "./json/result" + args + ".json";
				GUIConnector.resultToJSON(lastLine, jsonFileName);
			}*/
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.out.println("entered2?");
		}
		return lastLine;
	}
	

	public static void main(String[] args) {
		String redisIP =  RedisConnector.getRedisIP();
		String redisPort =  RedisConnector.getRedisPort();
		String filepath = "C:\\Users\\ma261439\\git\\dsplatform\\data\\streamops_data\\testing-jython\\test.py"+ " " + redisIP + " " + redisPort;
		execPyFile(filepath, "");
	}	
	
}

