package cea;

import java.io.IOException;

import cea.streamer.Launcher;

/**
 * STREAMER launcher routine
 */

public class LauncherMain{

	/**
	 * Launchs STREAMER 
	 * It allows having separate processes, each of them to read and process a different input channel 
	 * 
	 * @param args folders where the properties files are. One folder per application
	 * 				Example: "application1 application2"
	 * 			STREAMER will run one instance per argument (application) in parallel
	 * 			if no argument provided, STREAMER works with "default" id and root algs.props and streaming.props 
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {	
		Launcher l = new Launcher();
		l.launch(args);
	}
}