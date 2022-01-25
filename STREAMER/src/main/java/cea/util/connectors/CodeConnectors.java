package cea.util.connectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.google.common.io.Resources;

import cea.util.GlobalUtils;
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
		String lastLine = null;
		try {
			System.out.println("["+id+"] Started execution " + scriptFileName + " with args " +id+ " " + redisIP + " " + redisPort);
			//Process p = Runtime.getRuntime().exec("Rscript " + scriptFileName + " " +id+ " " + redisIP + " " + redisPort);
			//p.waitFor();
			
			String r_exec_command = "Rscript";
			Properties properties = new Properties();
			InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+"codeConnectors.props").openStream();
			properties.load(props);
			r_exec_command = properties.getProperty("r_exec_path").replace(" ","");
			props.close();
			
			Process p = Runtime.getRuntime().exec(r_exec_command + " " + scriptFileName + " " + id + " " + redisIP + " " + redisPort);
			lastLine = launchScript(p, id, scriptFileName);

			System.out.println("["+id+"] Finished execution " + scriptFileName + " with args " +id+ " " + redisIP + " " + redisPort);

			//Send data to JSON
			if (scriptFileName.contains("Test")) {
				String jsonFileName = "./json/result" + id + ".json";
				GUIConnector.resultToJSON(lastLine, jsonFileName);
			}

		} catch (IOException/* | InterruptedException */e) {
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
		System.out.println("["+id+"] Started execution " + scriptFileName + " with args " +id+ " " + redisIP + " " + redisPort);

		String lastLine="";
		try {			
			Process p = null;
			String py_exec_command = "python";
			Properties properties = new Properties();
			InputStream props = Resources.getResource(GlobalUtils.resourcesPathPropsFiles+"codeConnectors.props").openStream();
			properties.load(props);
			py_exec_command = properties.getProperty("py_exec_path").replace(" ","");
			props.close();
			
			if (py_exec_command.contains("conda")) {
				ProcessBuilder pb = new ProcessBuilder();
				pb.command("cmd", "/c",py_exec_command + " " + scriptFileName + " " + id + " " + redisIP + " " + redisPort);
				p = pb.start();
				lastLine = launchScript(p, id, scriptFileName);
			} else {
				p = Runtime.getRuntime().exec(py_exec_command + " " + scriptFileName + " " + id + " " + redisIP + " " + redisPort);
				//p.waitFor();
				lastLine = launchScript(p, id, scriptFileName);
			}
			
			System.out.println("["+id+"] Finished execution " + scriptFileName + " with args " +id+ " " + redisIP + " " + redisPort);
			
			//Send data to JSON
			/*if (scriptFileName.toLowerCase().contains("test")) {
				String jsonFileName = "./json/result" + args + ".json";
				GUIConnector.resultToJSON(lastLine, jsonFileName);
			}*/
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lastLine;
	}
	
	/**
	 * Shows in "scipt.out" live messages from the script which is running
	 * @param p process
	 * @param id id of the process
	 * @param scriptFileName which is being executed by p
	 * @return last line of the info message
	 */
	private static String launchScript(Process p, String id, String scriptFileName) {
		StringBuffer errorMessages = new StringBuffer();
		StringBuffer infoMessages = new StringBuffer();

		// Temp log
		String tempLogName="temp_"+id;
		Logger tempLog = Log.createTempLog(tempLogName);
		Log.separate(tempLog, "["+id+"] "+scriptFileName);		
		
		Timer infoTimer = new Timer();
		Timer errorTimer = new Timer();
		Process finalP = p;
		infoTimer.scheduleAtFixedRate(new TimerTask() {//info messages
			@Override
			public void run() {
				try(BufferedReader reader = new BufferedReader(new InputStreamReader(finalP.getInputStream()))) {
					String c;	
					while((c = reader.readLine()) != null) {
						tempLog.info("["+id+"] (info) "+c);
						infoMessages.append(c+"\n");					
					}
				} catch (IOException ignored) {}
			}
		}, 0, 250);
		errorTimer.scheduleAtFixedRate(new TimerTask() {//error messages
			@Override
			public void run() {
				try(BufferedReader errorReader = new BufferedReader(new InputStreamReader(finalP.getErrorStream())))
				{
						String c;
						while((c = errorReader.readLine()) != null) {
							tempLog.info("["+id+"] (err) "+c);
							errorMessages.append(c+"\n");
						}
				}
				catch (IOException ignored) {}
			}
		}, 0, 250);
		
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		
		infoTimer.cancel();
		errorTimer.cancel();
		Log.deleteLog(tempLogName);		
				
		Log.separate(Log.infoLog, "["+id+"] "+scriptFileName);
		Log.infoLog.info(infoMessages.toString());
		Log.separate(Log.errorLog, "["+id+"] "+scriptFileName);
		Log.errorLog.info(errorMessages.toString());
		
		String[] aux = infoMessages.toString().split("\n");		
		return aux[aux.length-1];
	}
	
}