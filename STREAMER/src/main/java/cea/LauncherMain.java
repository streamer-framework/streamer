package cea;

import java.io.IOException;

import cea.streamer.Launcher;

/**
 * Demonstrates, using the high-level KStream DSL, how to implement the
 * WordCount program that computes a simple word occurrence histogram from an
 * input text.
 *
 * In this example, the input stream reads from a topic named
 * "streams-file-input", where the values of messages represent lines of text;
 * and the histogram output is written to topic "streams-wordcount-output" where
 * each record is an updated count of a single word.
 *
 * Before running this example you must create the source topic (e.g. via
 * bin/kafka-topics.sh --create ...) and write some data to it (e.g. via
 * bin-kafka-console-producer.sh). Otherwise you won't see any data arriving in
 * the output topic.
 */

public class LauncherMain{

	/**
	 * Launchs the streaming platform. 
	 * It allows having separate processes, each of them to read and process a different input channel 
	 * 
	 * @param origins Folder where the properties are.
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {
		
		/*
		 * args = new String[5]; args[0]="captor1"; args[1]="captor2";
		 * args[2]="leak-detec_sec1"; args[3]="leak-detec_sec2";
		 * args[4]="leak-detec_sec3";
		 */		
	/*	args = new String[7]; args[0]="captor1"; args[1]="captor2";
		 args[2]="leak-detec_sec1"; args[3]="leak-detec_sec2"; args[4]="leak-detec_sec3";args[5]="water-pol_cap1"; args[6]="water-pol_cap2";
*/	
		
		Launcher l = new Launcher();
		l.launch(args);
	}
}