package cea.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import cea.streamer.core.TimeRecord;
import cea.util.connectors.RedisConnector;


/**
 * Class with basic utilities needed by the code 
 * @author sgarcia
 *
 */
public class GlobalUtils {
	
	/* Location paths */
	static String pathPrePostProcessors = "./src/main/java/cea/util/prepostprocessors";
	static String pathAlgs = "./src/main/java/cea/streamer/algs";
	static String pathTimeRecords = "./src/main/java/cea/streamer/core";

	/* Package paths */
	static public String packageMetrics = "cea.util.metrics";
	static public String packageMonitoring = "cea.util.monitoring";
	static public String packagePrePostProcessors = "cea.util.prepostprocessors";
	static public String packageAlgs = "cea.streamer.algs";
	static public String packageTimeRecords = "cea.streamer.core";
	
	/* Resources paths */
	static public String resourcesPathPropsFiles = "src/main/resources/setup/";
	

	/**
	 * Convert params in array of string to a LinkedHashMap
	 * @param hyperparms
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getMLAlgorithmHyperParams(String[] hyperparms) throws Exception{
		Map hyperParams =new LinkedHashMap<String, String>();
		for(int i = 0; i<hyperparms.length;i++) {
			String[]param =hyperparms[i].split("=");
			hyperParams.put(param[0],param[1]);
			if(param[0]=="normalized") {
				int normalized = Integer.parseInt(param[1]);
				if(normalized==1) throw new Exception("Data are normalized, preprocessing for normalization is not needed. refer to DStream External connector developper guide for more informations ");
			}
		}
	return hyperParams;
	}
	
	
/////////////////////////////////////////////// MODEL ///////////////////////////////////////////////
	

	/**
	 * Save the model in file
	 * 
	 * @param object model to save
	 */
	public static void saveModelInFile(Object serObj, String modelPathFile) {
		try {
			FileOutputStream fileOut = new FileOutputStream(modelPathFile);
			ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(serObj);
			objectOut.close();
		} catch (IOException ex) {
			System.err.println("Model could not be stored in file path: "+modelPathFile);
			ex.printStackTrace();
		}
	}

	/**
	 * Restore the model read from file into Redis
	 *
	 * @param id of the process
	 * @param filePath file path where the model is stored
	 */
	public static  String fileFromDiskToRedis(String id, String filePath) {
		String model = null;
		try {
		/*	FileInputStream fileIn = new FileInputStream(modelPathFile);
			//ObjectInputStream objectIn = new ObjectInputStream(fileIn);
			//model = (String)objectIn.readObject();
			//objectIn.close();
			BufferedReader br = new BufferedReader(new InputStreamReader(fileIn));
			String aux = br.readLine();
			while(aux != null) {
				model += aux;
				aux = br.readLine();
			}
			br.close();
					*/
			byte[] content = Files.readAllBytes(new File(filePath).toPath());
			//store in redis
			RedisConnector.storeModelInRedis(id, content);	
		} catch (IOException /*| ClassNotFoundException */e) {
			System.err.println("Content could not be extracted from file path: "+filePath);
			e.printStackTrace();			
		}	
		
		return model;
	}


	
///////////////////////////////////////////////// RECORDS ///////////////////////////////////////////////
	
	
	/**
	 * Extract the outputs after running the model
	 * 
	 * @param records
	 * @return
	 */
	public static Vector<List<String>> getOutputs(Vector<TimeRecord> records) {
		Vector<List<String>> outputs = new Vector<List<String>>();
		for(TimeRecord record: records) {
			if(!record.getOutput().isEmpty()) {
				outputs.add(record.getOutput());
			}
		}
		return outputs;
	}

	/**
	 * Generate generic time records from Files
	 * 
	 * @param id
	 * @param recordName
	 * @param sourceFiles
	 * 
	 * @return vector of time records
	 */
	public static  Vector<TimeRecord> generateTimeRecords(String id, String recordName, Vector<String> sourceFiles) {
		Vector<TimeRecord> records = new Vector<TimeRecord>();

		TimeRecord recObj;
		String line = null;
		BufferedReader br = null;
		Class<?> recC;
		for (String file : sourceFiles) {
			try {
				br = Files.newBufferedReader(Paths.get(file));
				line = br.readLine();
				while (line != null) {
					line = line.replaceAll("\"", "").replaceAll("\t", "");

					try {
						recC = Class.forName((GlobalUtils.packageTimeRecords+".")+recordName);
						recObj = (TimeRecord)recC.getDeclaredConstructor().newInstance();						
						recObj.fill(id, line);
						records.add(recObj);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
							IllegalArgumentException | InvocationTargetException | NoSuchMethodException
							| SecurityException e) {
						System.err.println("Record do not exist in the platform. Please select one from the list:\n"+listAvaliableTimeRecords());
						e.printStackTrace();
					}
					line = br.readLine();
				}
				br.close();
			} catch (IOException e) {
				System.err.println("File "+file+" does not exist.");
				e.printStackTrace();
			}
		}
		return records;
	}
	
	/**
	 * Generate generic time records from inputs
	 * 
	 * @param id
	 * @param recordName
	 * @param list of inputs
	 * 
	 * @return vector of time records
	 */
	public static  Vector<TimeRecord> generateTimeRecords(String id, String recordName, String[][] inputs) {
		Vector<TimeRecord> records = new Vector<TimeRecord>();

		TimeRecord recObj;
		String line="";
		Class recC;
		for (String[] a: inputs) {
			line="";
			for (int i =0; i< (a.length-1);i++) {
				line+=""+a[i]+";";
			}
			line+=""+a[a.length-1];
			try {
				recC = Class.forName((GlobalUtils.packageTimeRecords+".")+recordName);
				recObj = (TimeRecord)recC.getDeclaredConstructor().newInstance();
				recObj.fill(id,line);
				records.add(recObj);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | 
					IllegalArgumentException | InvocationTargetException | NoSuchMethodException
					| SecurityException e) {
				System.err.println("Record do not exist in the platform. Please select one from the list:\n"+listAvaliableTimeRecords());
				e.printStackTrace();
			}						
		}
		return records;
	}
	
	/**
	 * Save set of records to file
	 * @param records to save
	 * @param file path
	 * @param separator of values
	 * @param labels True if write headers of the data
	 */
	public static void saveRecordsToFile(Vector<TimeRecord> records, String file, String separator, boolean labels) {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter( file));
		
			for(TimeRecord record: records) {
				if(labels) {
					bw.write(record.exportHeaderToStringFormat(separator)+"\n");
					labels=false;
				}
				bw.write(record.exportToStringFormat(separator)+"\n");
			}
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
///////////////////////////////////////////////// PRINTS  ///////////////////////////////////////////////
	

	/**
	 * List available algorithms in DSplatform that can be used
	 * 
	 * @return string with the list
	 */
	public static  String listAvaliableAlgs() {		
		String avaliableAlgs = "";
		List<String> algs = new ArrayList<String>();
		File file = new File(pathAlgs);
		algs = Arrays.asList(file.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".java")) {
					return true;
				}
				return false;
			}
		}));

		for (String alg : algs) {
			String al = alg.split("\\.")[0];
			avaliableAlgs = avaliableAlgs + al + "\n";
		}

		return avaliableAlgs;
	}

	/**
	 * List available processors in DSplatform that can be used
	 *
	 * @param algFolder
	 * @return string with the list
	 */
	public static  String listAvaliablePrePostProcessors() {		
		String avaliableAlgs = "";
		List<String> algs = new ArrayList<String>();
		File file = new File(pathPrePostProcessors);
		algs = Arrays.asList(file.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".java") && name.contains("Processor")) {
					return true;
				}
				return false;
			}
		}));

		for (String alg : algs) {
			String al = alg.split("\\.")[0];
			avaliableAlgs = avaliableAlgs + al + "\n";
		}

		return avaliableAlgs;
	}
	
	/**
	 * List available time records in DSplatform that can be used
	 * 
	 * @param algFolder
	 * 
	 * @return string with the list
	 */
	public static  String listAvaliableTimeRecords() {		
		String avaliableAlgs = "";
		List<String> algs = new ArrayList<String>();
		File file = new File(pathTimeRecords);
		algs = Arrays.asList(file.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".java") && name.contains("Record")) {
					return true;
				}
				return false;
			}
		}));

		for (String alg : algs) {
			String al = alg.split("\\.")[0];
			avaliableAlgs = avaliableAlgs + al + "\n";
		}

		return avaliableAlgs;
	}
	
	/**
	 * Print the set of records
	 * @param records to print
	 */
	public static void printSet(Vector<TimeRecord> records, boolean labels) {
		for(TimeRecord record: records) {
			if(labels) {
				System.out.println(record.exportHeaderToStringFormat(" "));
				labels=false;
			}
			System.out.println(record.toString() );
		}
	}
	
	
	
///////////////////////////////////////////////// OTHERS ///////////////////////////////////////////////
	
	/**
	 * Get the absolute path of the project (base.dir)
	 * @return absolute project path (base.dir)
	 */
	public String getAbsoluteBaseProjectPath() {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		String project_name = "STREAMER";
		File pom_file = new File("pom.xml");

		if(pom_file.exists()) {		
			try {
				Model model = reader.read(new FileReader(pom_file));
				project_name = model.getName();
				
			} catch (IOException | XmlPullParserException e) {
				e.printStackTrace();
			}
		}else {
			project_name = "framework/";
			return project_name;
		}
		String aux = this.getClass().getClassLoader().getResource("algs/neuralNetworkTrain.R").toString();	
		String projectTree = project_name + "/";//"dsplatform/STREAMER/";
		aux = aux.replace("file:", "");
		if (System.getProperty("os.name").replace(" ","").toLowerCase().contains("windows")) {
			// MA261439 added this for windows users
			// TO WORK ON WINDOWS, WE NEED TO SKIP THE FIRST "/"
			// OR WE CAN DO IT IN ANOTHER WAY
			// System.out.println(System.getProperty("user.dir"));
			return aux.substring(1, aux.lastIndexOf(projectTree)+projectTree.length());
		}
		return aux.substring(0, aux.lastIndexOf(projectTree)+projectTree.length());
	}	
	
	/**
	 * Check if the string can be converted into double
	 * @param str
	 * @return true if is a double, false otherwise
	 */
	public static boolean isNumeric(String str) {
		/*
		 * for (char c : str.toCharArray()){ if (!Character.isDigit(c)) return false; }
		 * return true;
		 */
//			return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
		boolean numeric = true;
		try {
			double num = Double.parseDouble(str);
		} catch (NumberFormatException e) {
			numeric = false;
		}
		return numeric;
	}

	/**
	 * Convert a list in to a String 
	 * @param list of elements
	 * @param separator between elements
	 * @return String with the list
	 */
	public static String listToString(List<String> list, String separator) {
		String ret = "";		
		for(int i=0; i<list.size();i++) {
			ret+=list.get(i);
			if(i <(list.size()-1)) {//if it is not the last element
				ret+=separator;
			}
		}
		return ret;		
	}

	/**
	 * Check that there is at least one record from the set who contains an output
	 * @param records set
	 * @return output contained (true or false)
	 */
	public static boolean containsOutputs(Vector<TimeRecord> records) {
		for(TimeRecord record: records) {
			if(!((record.getOutput()).isEmpty()) ){
				return true;
			}
		}
		return false;
	}
	
	//////////////////////////// MATH OPERATIONS //////////////////////////////
	
	public static double safeDivison(double v1, double v2) {
		double result = v1/v2;
	//	if(((Double)(result)).isNaN())
	//		result = 0;
		return result;
	}
	
	public static double roundAvoid(double value, int places) {
	    double scale = Math.pow(10, places);
	    return Math.round(value * scale) / scale;
	}
	
	
}
