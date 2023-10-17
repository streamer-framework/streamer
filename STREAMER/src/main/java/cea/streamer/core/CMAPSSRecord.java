package cea.streamer.core;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Record class for the CMAPSS use case.
 */
public class CMAPSSRecord extends TimeRecord {

    public CMAPSSRecord() {
        super();
        setSeparatorFieldsRawData(" ");
        List<String> headers_list = Arrays.asList(
                "unit_nr", "time_cycles", "setting_1", "setting_2", "setting_3",
                "s_1", "s_2", "s_3", "s_4", "s_5", "s_6", "s_7", "s_8", "s_9", "s_10", "s_11",
                "s_12", "s_13", "s_14", "s_15", "s_16", "s_17", "s_18", "s_19", "s_20", "s_21",
                "label");
        headers.addAll(headers_list);
    }

    @Override
    public void fill(String key, String data) {
        if(!Objects.equals(data, "")) {
            try {
                TimeUnit.MICROSECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            setSourceFromKafkaKey(key);
            fillTimeStamp(new Date());
            String[] features = data.split(getSeparatorFieldsRawData());

            for(int i=0;i<features.length-1;i++) {
                values.put(headers.get(i), features[i]);
                extractors.put(headers.get(i), new NumericalExtractor());
            }

            setTarget(features[features.length-1]);
            extractors.put("rul", new NumericalExtractor());
        }
    }

}
