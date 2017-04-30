import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {
	Socket clntSocket;
	String document_root;
	private static int SERVER_MAX_LOAD = 1000;
	private static int SERVER_MAX_SOCKET_TIMEOUT = 60000; //60 seconds
	private static AtomicInteger current_load = new AtomicInteger(0);
	private static HashMap<Integer, String> status_code = new HashMap<Integer, String>() {
		{
			put(200, "200 OK");
			put(400, "400 BAD REQUEST");
			put(403, "403 FORBIDDEN");
			put(404, "404 NOT FOUND");
			put(500, "500 INTERNAL SERVER ERROR");
		}
	};
	HashMap<String, String> res_headers = new HashMap<String, String>();
	byte[] body = null;

	public ClientHandler(Socket clntSocket, String document_root) {
		this.clntSocket = clntSocket;
		this.document_root = document_root;
	}

	/* Send response back to client */
	public void sendResponse(OutputStream os) {
		try {
			DataOutputStream out = new DataOutputStream(os);
			for (Entry<String, String> entry : res_headers.entrySet()) {
				String line;
				if (entry.getKey().equals("versionStr")) {
					line = entry.getValue();
				} else {
					line = entry.getKey() + entry.getValue();
				}
				out.writeBytes(line);
				out.writeBytes("\r\n");
			}
			out.writeBytes("\r\n");

			if (body != null) {
				out.write(body);
				body = null;
			}
			out.writeBytes("\r\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* This function constructs body and headers for client */
	public void constructResponse(HTTPRequest req) {
		if (req.method == null || req.method.equals("GET") == false) {
			body = status_code.get(400).getBytes();
			constructResposeHeaders(req, 400, "text/html");
			return;
		}

		int index = req.url.indexOf("/") + 1;
		String pathOfFile = req.url.substring(index);
		if (pathOfFile.isEmpty()) {
			pathOfFile = "index.html";
		}

		index = pathOfFile.lastIndexOf(".") + 1;
		String fileType = pathOfFile.substring(index);
		fileType = getFileType(fileType);

		File fp = new File(document_root + "/" + pathOfFile);
		if (fp.exists() == true) {
			if (fp.canRead() == true) {
				try {
					FileInputStream fs = new FileInputStream(document_root + "/" + pathOfFile);
					body = new byte[fs.available()];
					fs.read(body);
					constructResposeHeaders(req, 200, fileType);
				} catch (Exception e) {
					body = status_code.get(404).getBytes();
					constructResposeHeaders(req, 404, "text/html");
					return;
				}
			} else {
				body = status_code.get(403).getBytes();
				constructResposeHeaders(req, 403, "text/html");
				return;
			}
		} else {
			body = status_code.get(404).getBytes();
			constructResposeHeaders(req, 404, "text/html");
			return;
		}
	}

	/* Constructing content-type for file */
	public String getFileType(String type) {
		if (type.equals("txt")) {
			return "text/plain";
		} else if (type.equals("html")) {
			return "text/html";
		} else if (type.equals("jif")) {
			return "image/jif";
		} else if (type.equals("png")) {
			return "image/png";
		} else if (type.equals("jpeg")) {
			return "image/jpeg";
		} else if (type.equals("jpg")) {
			return "image/jpg";
		} else if (type.equals("js")) {
			return "application/js";
		} else if (type.equals("css")) {
			return "text/css";
		} else if (type.equals("ttf")) {
			return "font/ttf";
		} else {
			return "font/" + type;
		}
	}

	/* Construct headers for the response */
	public void constructResposeHeaders(HTTPRequest req, int code, String contentType) {
		res_headers.put("versionStr", req.version + " " + status_code.get(code));

		Calendar c = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
		df.setTimeZone(TimeZone.getTimeZone("PDT"));
		res_headers.put("Date: ", df.format(c.getTime()));

		if (shouldConnectionBeKeptAlive(req) == true) {
			res_headers.put("Connection: ", "keep-alive");
		} else {
			res_headers.put("Connection: ", "close");
		}

		res_headers.put("Content-Type: ", contentType);
		res_headers.put("Content-Length: ", body.length + "");
	}

	/* Function to decide whether to keep connection alive or not */
	public boolean shouldConnectionBeKeptAlive(HTTPRequest req) {
		if (req.version.equals("HTTP/1.0") && 
				req.hm.containsKey("Connection") && 
				req.hm.get("Connection").contains("keep-alive")) {
			return true;
		} else if (req.version.equals("HTTP/1.1") && req.hm.containsKey("Connection") && 
				req.hm.get("Connection").contains("close")) {
			return false;
		} else if (req.version.equals("HTTP/1.1")) {
			return true;
		}
		return false;
	}
	
	/* Thread execution starts from this function */
	public void run() {
		try {
			current_load.incrementAndGet();
			InputStream is  = clntSocket.getInputStream();
			OutputStream os = clntSocket.getOutputStream();
			clntSocket.setSoTimeout(getCurrentTimeout());
			while (true) {
				HTTPRequest request = HTTPRequest.parseHTTPRequest(is);
				constructResponse(request);
				sendResponse(os);

				if (shouldConnectionBeKeptAlive(request) == true) {
					continue;
				} else {
					break;
				}
			}

			is.close();
			os.close();
			current_load.decrementAndGet();
		} catch (SocketTimeoutException e) {
			try {
				clntSocket.close();
			} catch (Exception e1) {

			}
		} catch (Exception e) {

		}
	}
	
	/* Timeout is based on load on server */
	public int getCurrentTimeout() {
		if (current_load.get() < SERVER_MAX_LOAD) {
			return SERVER_MAX_SOCKET_TIMEOUT; //60 seconds
		} else {
			return SERVER_MAX_SOCKET_TIMEOUT * (SERVER_MAX_LOAD/current_load.get());
		}
	}
}
