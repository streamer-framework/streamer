package cea.util;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Class that implements the different logs
 *
 */
	
/**
* Log used to keep traces of the new models and the classification results during the execution
*/
public class Log {

	public static boolean showDebug = false;
	public static final Logger infoLog = (Logger) Logger.getLogger("infoLog");
	public static final Logger quantileLog = (Logger) Logger.getLogger("quantileLog");
	public static final Logger displayLogTrain = (Logger) Logger.getLogger("displayLogTrain");
	public static final Logger displayLogTest = (Logger) Logger.getLogger("displayLogTest");
	public static final Logger errorLog = (Logger) Logger.getLogger("errorLog");

	public static void separate(Logger log, String scriptFileName) {
		log.info("\n############################################  " + scriptFileName +"  #####################################################\n");
	}

	public static void clearLogs() {
		File dirLog = new File("./logs");
		for (File log : dirLog.listFiles()) {
			if (!log.isDirectory()) {
				try {
					FileWriter fooWriter = new FileWriter(log, false);
					fooWriter.write("");
					fooWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}