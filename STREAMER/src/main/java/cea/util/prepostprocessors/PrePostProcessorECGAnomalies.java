package cea.util.prepostprocessors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import cea.streamer.core.TimeRecord;

public class PrePostProcessorECGAnomalies extends PrePostProcessor {

	@Override
	public Vector<TimeRecord> preprocess(Vector<TimeRecord> records, String id) {
		/*String filepath = "../data/streamops_data/detect_anom/logging_preprocessing";
		File file = new File(filepath);
		FileWriter fr = null;
		try {
			fr = new FileWriter(file, true);

			BufferedWriter br = new BufferedWriter(fr);

			for (TimeRecord rec : records) {
				br.write("preprocessing: " + rec);

			}
			br.close();
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return records;
	}

	@Override
	public Vector<TimeRecord> postprocess(Vector<TimeRecord> records, String id) {
		/*String filepath = "../data/streamops_data/detect_anom/logging_postprocessing";
		File file = new File(filepath);
		FileWriter fr = null;
		try {
			fr = new FileWriter(file, true);

			BufferedWriter br = new BufferedWriter(fr);

			for (TimeRecord rec : records) {
				br.write("postprocessing: " + rec);

			}
			br.close();
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return records;
	}

}
