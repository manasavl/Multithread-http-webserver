import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class HTTPRequest {
	String method;
	String url;
	String version;
	HashMap<String, String> hm = new HashMap<String, String>();

	public HTTPRequest(String method, String url, String version, HashMap<String, String> hm) {
		this.method = method;
		this.version = version;
		this.url = url;
		this.hm = hm;
	}

	public static HTTPRequest parseHTTPRequest(InputStream is) {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		String line;
		try {
			while (true) {
				line = br.readLine();
				if (line == null) {
					continue;
				} else if (line.equals("\n")) {
					continue;
				} else if (line.equals("\r")) {
					continue;
				} else if (line.length() == 0) {
					continue;
				} else {
					break;
				}
			}

			String arr[] = line.split("\\s+");
			HashMap<String, String> local_hm = new HashMap<String, String>();
			while ((line = br.readLine()) != null && line.length() != 0) {
				String tmp[] = line.split(":");
				local_hm.put(tmp[0], tmp[1]);
			}
			HTTPRequest hr = new HTTPRequest(arr[0], arr[1], arr[2], local_hm);
			
			return hr;
		} catch (Exception e) {
			System.out.println("Error in http request parsing");
			e.printStackTrace();
		}
		return null;
	}
}
