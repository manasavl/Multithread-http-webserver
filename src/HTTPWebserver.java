import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPWebserver {
	// Default port number
	public static int port = 8000;
	public static String document_root = null;
	
	public static void main(String args[]) {
		if (args.length <= 1) {
			System.out.println("Wrong number of arguments");
			System.out.println(usage());
			return;
		}
		
		/* Parsing command line arguments */
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-port")) {
					port = Integer.parseInt(args[i+1]);
					i++;
				} else if (args[i].equals("-document_root")) {
					document_root = args[i+1];
					i++;
				} else {
					System.out.println("Unknown argument");
					System.out.println(usage());
					return;
				}
			}
			
			/* Validate document root */
			if (document_root == null) {
				System.out.println("document_root argument not provided");
				System.out.println(usage());
				return;
			} else {
				File f = new File(document_root);
				if (f.exists() == false || f.isDirectory() == false) {
					System.out.println("Given document root is not a directory path");
					return;
				}
				System.out.println("Document root: " + document_root);
			}
			
			/* Validating port */
			if (port < 8000 || port > 9999) {
				System.out.println("Port number is not in range of 8000-9999");
				return;
			} else {
				System.out.println("HTTP webserver port: " + port);
			}
		} catch(Exception e) {
			System.out.println("Invalid arguments: " + usage());
		}
		
		/* Starting HTTP web server */
		try {
			ServerSocket servSocket = new ServerSocket(port, 100);
			
			while(true) {
				Socket clntSocket = servSocket.accept();
				
				ClientHandler ch = new ClientHandler(clntSocket, document_root);
				Thread th = new Thread(ch);
				th.start();
			}
		} catch(Exception e) {
			System.out.println("Problem occured when creating/accepting socket");
			return;
		}
	}
	
	public static String usage() {
		return "Usage: ./server -document_root <path> -port <port no>";
	}
}
