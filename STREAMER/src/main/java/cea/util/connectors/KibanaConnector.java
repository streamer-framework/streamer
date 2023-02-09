package cea.util.connectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.io.Resources;

import cea.util.GlobalUtils;

public class KibanaConnector {
	/**
	 * Host of the Elastic Search NoSQL store
	 */
	static String host = "http://localhost:5601";

	public static void init() {
		Properties properties = new Properties();
		try (InputStream props = new FileInputStream (GlobalUtils.resourcesPathPropsFiles+"kibana.props")) {
			properties.load(props);
			host = properties.getProperty("host").replace(" ","");
			if (host.equals("localhost"))
				host = "http://" + host + ":5601";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean checkIfDashboardExists(String id) {
		try {
			// Export URL
			URL url = new URL(host + "/api/kibana/dashboards/export?dashboard=" + id);
			// POST request for importing the dashboard
			URLConnection con = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) con;

			http.setRequestMethod("GET");
			// Reading the json file that is the back up of the dashboard
			http.setDoOutput(true);

			String response = null;
			String json_output = "";
			// Read the response
			BufferedReader d = new BufferedReader(new InputStreamReader(http.getInputStream()));
			while (null != ((response = d.readLine()))) {
				json_output += response + "\n";
				//System.out.println(response);
			}
			JSONParser parser = new JSONParser();
			JSONObject data = (JSONObject) parser.parse(json_output);
			JSONArray objects = (JSONArray) data.get("objects");
			JSONObject object = (JSONObject) objects.get(0);
			JSONObject error = (JSONObject) object.get("error");
			if (error != null) {
				long statusCode = (Long) error.get("statusCode");
				if (statusCode == 404) {
					return false;
				}
			}
			d.close();
		} catch (ConnectException e) {
			//in this case kibana is not running or not required
			System.out.println("["+id+"] Kibana is missing! If Kibana is not required, just ignore this line");
			return true;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static void importMetricsDashboard() {

		try {
			boolean exists = checkIfDashboardExists("a7eda4f0-0ede-11eb-9643-f1b6ac54d61c");
			if (exists)
				return;
			// Import URL
			URL url = new URL(host + "/api/kibana/dashboards/import");
			// POST request for importing the dashboard
			URLConnection con = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) con;

			http.setRequestMethod("POST");
			http.setRequestProperty("kbn-xsrf", "true");
			http.setRequestProperty("Content-Type", "application/json");
			// Reading the json file that is the back up of the dashboard
			File file = new File(
					new GlobalUtils().getAbsoluteBaseProjectPath() + "src/main/resources/setup/dashboard.json");
			JSONParser parser = new JSONParser();

			// Use JSONObject for simple JSON and JSONArray for array of JSON.
			JSONObject data;

			// Parse the file to JSON
			data = (JSONObject) parser.parse(new FileReader(file.getAbsolutePath()));
			http.setDoOutput(true);

			// Write the JSON with the POST request
			OutputStream outStream = http.getOutputStream();
			OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "UTF-8");
			outStreamWriter.write(data.toJSONString());
			outStreamWriter.flush();
			outStreamWriter.close();
			outStream.close();

			String response = null;

			// Read the response
			BufferedReader d = new BufferedReader(new InputStreamReader(http.getInputStream()));
			while (null != ((response = d.readLine()))) {
				//System.out.println(response);
			}
			d.close();
			System.out.println("Metrics Dashboard was imported successfully!");
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		importMetricsDashboard();
	}
}
