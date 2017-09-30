/*Dmitri Grozdanov
Computer Science 431
PID: 720463463
This program scans lines of input from a text file and determines whether the commands presented to it are valid.
It does this by breaking up the input lines into tokens, which are then tested separately for compliance.
The program first checks that the command matches one of the three possibilities, than checks the parameters
for compliance, and formats and prints the correct output. If the program receives a quit command, it will exit the program.
If the program does not have a CONNECT command to begin with, however, it will throw an error.*/

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class FTPClient {
	
	private static Socket socket;
	private static DataOutputStream dOS;
	private static FTPReadLine rL;
	private static FTPClientAnalyzer cA;
	private static String serverReply = "";
	private static String myIP;
	private static int port;
	private static String CRLF = "\r\n";
	private static String LF = "\n";
	private static boolean validConnection = false;
	private static boolean alreadyConnected = false;
	private static boolean validQuit = false;
	private static int fileIncrement = 0, dataConnectionPort = 0, firstPort = 0, secondPort = 0;
	private static InputStream inputStream;
		
	private static void client(String s) throws IOException {
				
		//Creates a local copy of the String input and prints it to console
		String input = s;
		System.out.printf("%s", input);
		
		//Initiates parameters and server specifics to be used to analyze and return the results of the program
		String parameter = "";
		String serverHost = "";
		String serverPort = "";
					
		//Defines a token system based off of input from BufferedReader
		StringTokenizer token = new StringTokenizer(input);
		//Creates a counter that counts how many tokens (additional words) are present in the line
		int tokenCount = token.countTokens();
		//Obtains the next token available
		String command = token.nextToken();
		tokenCount--;
					
		//Initializes the booleans to be used throughout the program
		boolean errorRequest = false, errorServerHost = false, errorServerPort = false, errorPathname = false;
		boolean validConnect = false, validGet = false;
					
		//If the user connects to the program, the connection is set as valid
		if (command.equalsIgnoreCase("connect")) {
						
						//validConnection = true;
						//System.out.println("Initial tokens: " + tokenCount);
		}
					
		//An empty if statement to facilitate the other type of possible command
		else if (command.equalsIgnoreCase("get")) {
						
		}
					
		//If the user quits the program, the connection is set to be invalid and the program immediately exits
		else if (command.equalsIgnoreCase("quit")) {

			//Error if QUIT is followed by other text
			if (tokenCount != 0) {
							
				errorRequest = true;
			}
		}
					
		//Throws a request error if the user has not entered a valid command
		else {
						
			errorRequest = true;
		}
					
		//Runs if there are tokens that follow the initial user command
		if (token.hasMoreTokens()) {
						
			//Cycles to the next token and decrements the tokenCount
			parameter = token.nextToken();
			tokenCount--;
						
			//Sets all get parameters to begin after the spaces following the get command
			if (command.equalsIgnoreCase("get")) {
							
				int startParameter = 3;
							
				while (input.substring(startParameter, startParameter + 1).equals(" ")) {
								
					startParameter++;
				}
							
				parameter = input.substring(startParameter, input.length());
							
				//Cycles the token count back to 0, since the get command won't need the counter any more
				while (tokenCount > 0) {
								
					tokenCount--;
				}
			}
					    
			//Defines a String called sub that is to be manipulated later on in this if statement
			String sub = parameter;
						
			//Checks the parameter character-by-character for validity, incrementing the character each loop
			for (int i = 0; i < parameter.length(); i++) {

				int c = parameter.charAt(i);
							
				//Checks whether the get parameter is a valid ASCII character with a value less than 128
				if (command.equalsIgnoreCase("get")) {
								
					if (c > 0x7F) {
								
						errorPathname = true;
					}
								
					if (!(errorPathname) && !(errorRequest)) {
									
						validGet = true;
					}
				}
							
				//Checks whether a connect command will throw an errorServerHost
				else if (command.equalsIgnoreCase("connect")) {
								
					if (sub.contains(".")) {
								
						sub = sub.substring(0, sub.indexOf("."));
					}
								
					//Checks to make sure that the parameter contains only upper-case/lower-case letters, numbers, or  and/or periods
					if (!(c == 46) && !((c >= 48) && (c <= 57)) && !((c >= 65) && (c <= 90)) && !((c >= 97) && (c <= 122))) {
									
						errorServerHost = true;
					}
								
					else {
									
						serverHost = parameter;
					}
				}
			}
		}
					
		//Throws an appropriate error, given that the user command was not followed by a parameter
		else {
			
			
			//Checks the command for how many spaces follow it, thereby helping to detect the appropriate errors
			boolean extraSpace = false, twoSpaces = false;
			int i = 0;
				
			for (char c : input.toCharArray()) {
				   
				if (i == 5) {
						
					break;
				}
					
				if (Character.isWhitespace(c)) {
						
					if (extraSpace) {
							
						twoSpaces = true;
					}
						
			    	extraSpace = true;
			    	i--;
			    }
				    
				i++;
			}
				
			//Checks to see if there is an excess of spaces following the command, leading to an error
			if (twoSpaces && command.equalsIgnoreCase("connect")) {
					
				errorRequest = true;
			}
				
			else if (twoSpaces && command.equalsIgnoreCase("get")) {
					
				errorRequest = true;
			}
				
			else if (twoSpaces && command.equalsIgnoreCase("quit")){
					
				errorRequest = true;
			}
				
			//Checks the quit command to see that it is a proper command, and if it is, quits the program
			if (!errorRequest && validConnection && command.equalsIgnoreCase("quit")) {
				
				validConnection = false;
				validQuit = true;
			}
					
			if (command.equalsIgnoreCase("connect")) {
							
				errorRequest = true;
			}
						
			else if (command.equalsIgnoreCase("get")) {
							
				errorPathname = true;
			}
		}
					
		//Checks whether the server port for the connect command throws an errorServerPort
		if (command.equalsIgnoreCase("connect") && tokenCount == 1) {
						
			serverPort = token.nextToken();
			tokenCount--;
			
			int intServerPort = 0;
									
			try {
							
				intServerPort = Integer.parseInt(serverPort);
			}
						
			catch (NumberFormatException e) {
							
				errorServerPort = true;
			}
						
			if ((intServerPort < 0) || (intServerPort > 65535)) {
							
				errorServerPort = true;
			}
						
			//If the connect command does not throw any errors, it is deemed to be valid
			if (!(errorServerHost) && !(errorServerPort) && !(errorRequest)) {
						
				validConnect = true;
				validConnection = true;
			}
		}
					
		//Flags server host or port errors if a connect command with a certain, irregular token count was processed
		//A proper connect command should only have one token count
		else if (command.equalsIgnoreCase("connect") && tokenCount == 0) {
						
			errorServerHost = true;
		}
					
		else if (command.equalsIgnoreCase("connect") && tokenCount != 1) {
						
			errorServerPort = true;
		}
					
		//Prints an appropriate error for any potential invalid commands
		if (errorRequest) {
						
			System.out.printf("ERROR -- request%s", LF);
		}
					
		else if (errorServerHost) {
						
			System.out.printf("ERROR -- server-host%s", LF);
		}
					
		else if (errorServerPort) {
						
			System.out.printf("ERROR -- server-port%s", LF);
		}
					
		else if (errorPathname) {
						
			System.out.printf("ERROR -- pathname%s", LF);
		}
					
		else if (validQuit) {
						
			System.out.printf("QUIT accepted, terminating FTP client%s", LF);
			messageServer("QUIT" + CRLF);
		}
					
		else if (!validConnection) {
						
			System.out.printf("ERROR -- expecting CONNECT%s", LF);
		}
					
		//Notifies the user of a successful connect command and fulfills it
		else if (validConnect) {
					
			if (socket != null) {
				
				
				try {
					socket.close();
				}
				
				catch (IOException e) {
					
				}
				
				serverReply = "";
				
				
			}
			
			
			port = Integer.parseInt(serverPort);
						
			try {
							
				//Attempts to connect to the server and defines new variables
				socket = new Socket(serverHost, port);
				inputStream = socket.getInputStream();
				BufferedReader buff = new BufferedReader(new InputStreamReader(inputStream));
				rL = new FTPReadLine(buff);
				dOS = new DataOutputStream(socket.getOutputStream());

				//Lists the new host and port numbers
				System.out.printf("CONNECT accepted for FTP server at host %s and port %s%s",  parameter, serverPort, LF);
							
				//Attempts to parse the response of the server
				if (rL != null) {
								
					cA = new FTPClientAnalyzer(rL.readLine());
					cA.analyze();
				}
							
				//Sends the server messages following a valid connection
				messageServer("USER anonymous" + CRLF);
				messageServer("PASS guest@" + CRLF);
				messageServer("SYST" + CRLF);
				messageServer("TYPE I" + CRLF);
				
				alreadyConnected = true;
			}
						
			//Notifies the user of a failed connection
			catch (IOException e) {
	
				System.out.printf("CONNECT failed%s", LF);
			}
		}
					
		//Notifies the user of a successful get command and fulfills it
		else if (validGet) {
						
			System.out.printf("GET accepted for %s", parameter);
						
			//Produces the current host IP address
			String myIP;
			InetAddress myInet;
			myInet = InetAddress.getLocalHost();
			myIP = myInet.getHostAddress();
						
			//Changes the previous IP address to one that uses commas 
			String subIP = "";			
			while (myIP.contains(".")) {
							
				subIP = subIP + myIP.substring(0, myIP.indexOf(".")) + ",";
				myIP = myIP.substring(myIP.indexOf(".") + 1, myIP.length());
			}
						
			subIP = subIP + myIP;
						
			try {
							
				//Attempts to message the server with the port and retr it needs following the GET command
				messageServer("PORT " + subIP + "," + firstPort + "," + secondPort + CRLF);
				int getCommandPort = ((firstPort * 256) + secondPort);
				
				messageServer("RETR " + parameter.trim() + CRLF);
							
							
				Socket transferSocket;
				ServerSocket listenerSocket = null;
				
				try {
					
					listenerSocket = new ServerSocket(getCommandPort);
				}
				
				catch (Error e) {
					
					System.out.printf("GET failed, FTP-data port not allocated.%s", LF);
				}

				if (serverReply.contains("150")) {
						
					//Increments the port for the port command and appropriately resets it if one count gets higher than 255
					secondPort++;
								
					if (secondPort == 256) {
									
						firstPort++;
						secondPort = 0;
					}
					fileIncrement++;
					transferSocket = listenerSocket.accept();
					byte[] incomingFile = new byte[10000000];
					InputStream fIS = transferSocket.getInputStream();
					FileOutputStream fOS = new FileOutputStream("retr_files/file" + fileIncrement);
					BufferedOutputStream bOS = new BufferedOutputStream(fOS);
								
					for (int i = 0; i < fIS.read(incomingFile, 0, incomingFile.length); i++) {
									
						bOS.write(incomingFile[i]);
						bOS.flush();
					}
					
					//bOS.flush();
					
					cA = new FTPClientAnalyzer(rL.readLine());
					cA.analyze();
					serverReply = "";
								
					//fIS.close();
					//fOS.close();
					//bOS.close();
								
					//if (transferSocket != null) {
								
						transferSocket.close();
					//}
					
					//if (listenerSocket != null) {
					
						listenerSocket.close();	
					//}
				}
							
				else {
								
					//if (listenerSocket != null) {
						
						listenerSocket.close();
					//}
				}			
			}
						
			catch (IOException e) {
							
			}
		}
	}
	
	public static void messageServer(String s) throws IOException {
		
		dOS.writeBytes(s);
		dOS.flush();
		
		System.out.print(s);
		
		cA = new FTPClientAnalyzer(rL.readLine());
		serverReply = cA.analyze();
	}
	
	public static void main(String[] args) throws IOException {
		
		//Produces the current host IP address
		InetAddress myInet;
		myInet = InetAddress.getLocalHost();
		myIP = myInet.getHostAddress();
		
		dataConnectionPort = Integer.parseInt(args[0]);
		
		//Defines the ports to be used in the PORT portion of the program
		while (dataConnectionPort >= 256) {
						
			dataConnectionPort = dataConnectionPort - 256;
			firstPort++;
		}
					
		secondPort = dataConnectionPort;
				
		//Defines a buffered reader that is based on the CRLF-checking FTPReadLine class
		BufferedReader buff = new BufferedReader(new InputStreamReader(System.in));
		FTPReadLine buffLine = new FTPReadLine(buff);
		
		//Creates an empty socket to be connected to in the following while loop
		Socket socket = null;
		
		//Defines the input string that will scan buffered responses from the user
		String input = "";

		//boolean newConnect = false;
		
		while (!validQuit) {
			
			input = buffLine.readLine();
						
			if (input == null) {
				
				break;
			}
			
			//Runs the main program that checks the validity of the user input and communicates it to the server
			//if (!newConnect) {
				
				client(input);
			//}
			
			//newConnect = false;
			
			if (validQuit) {
				
				try {
						
					dOS.close();
					validConnection = false;
						
					if (socket != null) {
							
						socket.close();
					}
				}
					
				catch (IOException e) {
						
				}
				
				break;
			}
			
			serverReply = "";
		}
	}
}
//Terminates upon the user entering a quit command
