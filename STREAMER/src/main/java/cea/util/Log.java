package cea.util;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

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
	public static final Logger displayLogTrain = (Logger) Logger.getLogger("displayLogTrain");
	public static final Logger displayLogTest = (Logger) Logger.getLogger("displayLogTest");
	public static final Logger errorLog = (Logger) Logger.getLogger("errorLog");
	public static final Logger metricsLog = (Logger) Logger.getLogger("metricsLog");

	public static void separate(Logger log, String text) {
		log.info("\n##############  " + text +"  ##############\n");
	}
	
	private static String getLogFolder() {
		Properties logProps = new Properties();
		String path = null;
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles+"log4j.properties")) {
			logProps.load(props);
			path = logProps.getProperty("log");			
		} catch (Exception e) {
			System.err.println("\"visualisation\" field in streaming.props must be \"true\" or \"false\".");
		}
		
		return path;
	}

	public static void configureLogs() {
		String log4JPropertyFile = GlobalUtils.resourcesPathPropsFiles+"log4j.properties";
		Properties p = new Properties();

		try {
		    p.load(new FileInputStream(log4JPropertyFile));
		    PropertyConfigurator.configure(p);
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
	
	public static void clearLogs() {
		configureLogs();
		
		File dirLog = new File(getLogFolder());
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
	
	public static void deleteLog(String name) {
		File log = new File(getLogFolder()+name+".out");
		if (!log.isDirectory()) {
			log.delete();		
		}
	}
	
	
	public static Logger createTempLog(String name) {
		Logger tempLog= (Logger) Logger.getLogger(name);//temporary log where live messages from script are shown
		PatternLayout layout = new PatternLayout();
		layout.setConversionPattern("%m%n");
		RollingFileAppender appender;
		try {
			appender = new RollingFileAppender(layout,getLogFolder()+name+".out",true);
			tempLog.addAppender(appender);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return tempLog;
	}

}