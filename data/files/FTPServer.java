/*Dmitri Grozdanov
Computer Science 431
PID: 720463463
This program scans lines of input from a text file and determines whether the commands presented to it are valid.
It does this by breaking up the input lines into tokens, which are then tested separately for compliance.
The program first checks that the command matches one of the six possibilities, than checks the parameter
(if there is one), and lastly checks compliance to the "\r\n" line termination technique.*/

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class FTPServer {
	
	private static DataOutputStream dOS;
	//private static BufferedInputStream bIS;
	private final static String CRLF = "\r\n";
	//Defines booleans that will be used to monitor sequencing, errors, and valid commands
	private static boolean validQuit = false, enteredUser = false, enteredPass = false, missingUser = false, missingPass = false;
	private static boolean missingPort = false, enteredPort = false, enteredRetr = false, loggedIn = false;
	
	//private static String updatedPort = "";
	private static String getHost = "";
	private static int getPort = 0;
	
	static String returnLine = "";
	
	//Defines a counter for the increment of the file number of the retr command
	private static int retrCount = 1;
	
	private static void server(String s) throws IOException {
		
		//Defines the various types of errors encountered, as well as the boolean indicating parameter need			
		boolean needParameter = false, errorNotImplemented = false, errorDataConnection = false, errorCommand = false, errorUser = false, errorPass = false, errorType = false, errorPort = false, errorRetr = false, errorLoggedIn = false;
		boolean validUser = false, validPass = false, validTypeI = false, validTypeA = false, validGet = false, validSyst = false, validNoop = false, validPort = false, validRetr = false;
		String updatedPort = "";	
		
		//Reads a line from input file and stores it as an input String
		String input = s;
					
		String words[] = input.split(" ");
		boolean errorCRLF = !(words[words.length - 1].contains("\r\n"));
		String command, parameter = "";
		//Defines a token system based off of input from BufferedReader
		StringTokenizer token = new StringTokenizer(input);
		//Creates a counter that counts how many tokens (additional words) are present in the line
		int tokenCount = token.countTokens();
					
		//Obtains the next token available
		command = token.nextToken();
					
		//Prints the input provided for that line
		System.out.print(input);
					
		//Checks to see if the user is missing their user name
		if (!enteredUser && !(command.equalsIgnoreCase("user") || command.equalsIgnoreCase("quit"))) {
						
			missingUser = true;
		}
					
		//Checks to see if the user is missing their password
		if (enteredUser && !enteredPass && !(command.equalsIgnoreCase("pass") || command.equalsIgnoreCase("quit"))) {
						
			missingPass = true;
		}
					
		if (enteredUser && enteredPass) {
						
			loggedIn = true;
		}
					
		//Determines whether the user command requires a parameter
		if (command.equalsIgnoreCase("user") ||
				command.equalsIgnoreCase("pass") ||
				command.equalsIgnoreCase("type") ||
				command.equalsIgnoreCase("port") ||
				command.equalsIgnoreCase("retr")) {
						
			needParameter = true;
						
			if (!(token.hasMoreTokens())) {
							
				//Checks the command for how many spaces follow it, thereby helping to detect the appropriate errors
				boolean extraSpace = false, twoSpaces = false, threeSpaces = false;
				int i = 0;
							
				for (char c : input.toCharArray()) {
							   
					if (i == 5) {
									
						break;
					}
								
					if (Character.isWhitespace(c)) {
							      
						if (twoSpaces) {
										
							threeSpaces = true;
						}
									
						if (extraSpace) {
										
							twoSpaces = true;
						}
									
						extraSpace = true;
						i--;
					}
							    
					i++;
				}
							
				//Checks to see if there is an excess of spaces following the command, leadind to an error
				if (threeSpaces && command.equalsIgnoreCase("user")) {
								
					errorUser = true;
				}
							
				else if (threeSpaces && command.equalsIgnoreCase("pass")) {
								
					errorPass = true;
				}
							
				else if (threeSpaces && command.equalsIgnoreCase("type")){
								
					errorType = true;
				}
							
				else if (threeSpaces && command.equalsIgnoreCase("port")){
								
					errorType = true;
				}
							
				else if (threeSpaces && command.equalsIgnoreCase("retr")){
								
					errorRetr = true;
				}
							
				else {
								
					errorCommand = true;
				}
			}
						
			//Monitors if the user is correctly logged in
			else if (command.equalsIgnoreCase("user")) {
							
				validUser = true;
				enteredUser = true;
				missingUser = false;
							
				if (loggedIn) {
								
					loggedIn = false;
					enteredPass = false;
				}
			}
						
			//Monitors if the user is correctly logged in
			else if (command.equalsIgnoreCase("pass")) {
							
				if (loggedIn) {
								
					errorLoggedIn = true;
				}
							
				validPass = true;
				enteredPass = true;
				missingPass = false;
			}
						
			//Monitors if the user has entered a port command before a retr command
			else if (command.equalsIgnoreCase("retr")) {
							
				if (!enteredPort) {
								
					missingPort = true;
				}
			}
		}
					
		//Determines whether the user command doesn't require a parameter	
		else if (command.equalsIgnoreCase("syst") || 
				command.equalsIgnoreCase("noop") || 
				command.equalsIgnoreCase("quit")) {
						
			needParameter = false;

			if (token.hasMoreTokens()) {
							
				errorCRLF = true;
			}
						
			else if (command.equalsIgnoreCase("syst")) {
							
				validSyst = true;
			}
						
			else if (command.equalsIgnoreCase("quit")) {
							
				validQuit = true;
			}
		}
		
		//If the command isn't one of the ones above but is 3 or 4 characters long, this error will print
		else if ((command.length() == 3) || (command.length() == 4)) {
			
			errorNotImplemented = true;
		}
					
		//Solution if the command does not conform to one of the eight choices	
		else {
						
			errorCommand = true;
		}
					
		//Runs if the command requires a parameter, has yet to give an error, and while there are more tokens available
		//This line checks the competence of the parameter portion of the line
		while (tokenCount > 1 && needParameter && !errorCommand) {
						
			parameter = token.nextToken();
						
			//Checks the port command for validity and returns a correctly modified port sequence
			if (command.equalsIgnoreCase("port")) {
							
				//Throws an error if there is no port that follows
				if (token.countTokens() != 0) {
								
					errorPort = true;
				}
							
				else {
								
					String portSubstring = parameter;
					int port = 0;
					int portAddress = 0;
					int commas = 0;
								
					if (!(portSubstring.contains(","))) {
									
						errorPort = true;
					}
								
					while (portSubstring.contains(",")) {
								
						commas++;
									
						if (commas > 5) {
										
							errorPort = true;
							break;
						}
									
						String shortenedPort = portSubstring.substring(0, portSubstring.indexOf(','));
									
						//Checks to see if port contains valid ASCII 48-57 code
						for (int i = 0; i < shortenedPort.length(); i++) {
										
							int c = shortenedPort.charAt(i);
							if (!(c >= 48 && c <= 57)) {
											
								errorPort = true;
								break;
							}
						}
									
						if (errorPort) {
										
							break;
						}
									
						port = Integer.parseInt(shortenedPort);
									
						//Checks to see that the port integers are within the 0-255 range
						if (port < 0 || port > 255) {
										
							errorPort = true;
							break;
						}
									
						portSubstring = portSubstring.substring(portSubstring.indexOf(',')+1);
									
						//Launches after the program reaches the last portion of the port
						if (commas == 5 && !(portSubstring.contains(","))) {
										
							//Checks to see if final portion of port contains valid integers
							for (int i = 0; i < portSubstring.length(); i++) {
											
								int c = portSubstring.charAt(i);
								if (!(c >= 48 && c <= 57)) {
												
									errorPort = true;
									break;
								}
							}
										
							//Checks to see that the port integers are within the 0-255 range
							if (Integer.parseInt(portSubstring) < 0 || 
									Integer.parseInt(portSubstring) > 255) {
											
								errorPort = true;
								break;
							}
										
							//Defines the refined port address and exits, should the port be valid
							if (!errorPort) {
									
								//Recalculates port values
								portAddress = (256 * port) + Integer.parseInt(portSubstring);
								updatedPort += portAddress;
								validPort = true;
								enteredPort = true;
										
								//Updates the values of the getHost and getPort variables
								getHost = updatedPort.substring(0, updatedPort.indexOf(","));
								getPort = portAddress;
											
								break;
							}
						}
									
						//Adds a period to the end of a port sequence, unless it's before the last one
						if (commas != 4) {
										
							updatedPort += port + ".";
						}
									
						//Adds a comma before the last port sequence
						else {
										
							updatedPort += port + ",";
						}
					}
								
					if (!validPort) {
									
						errorPort = true;
					}
				}
			}
						
			if (command.equalsIgnoreCase("retr")) {
							
				if (!enteredRetr) {
								
					try {
									
						String filePath = "";
									
						//Strips the retr parameter of unnecessary slashes
						if (parameter.startsWith("/") || parameter.startsWith("\\")) {
										
							filePath = parameter.substring(1, parameter.length());
						}
									
						else {
										
							filePath = parameter;
						}
									
						ProcessBuilder pathMaker = new ProcessBuilder("ls", filePath);
						Process p = pathMaker.start();
						int exitStatus = p.waitFor();
									
						Socket transferSocket = null;
									
						//Throws an error if the file path cannot be found
						if (exitStatus != 0) {
										
							errorRetr = true;
						}
									
						else if (!missingPort && !errorRetr) {
										
							messageClient("150 File status okay." + CRLF);
							//Copies the file into an incremented file in the retr_files folder
							String fileName = "retr_files/file" + retrCount;
							
							//Attempts to establish a data connection with the client
							boolean socketConnection = false;
							//System.out.println("Attempting to establish socket connection at " + getHost + " at port " + getPort);
							while (!socketConnection) {
											
								try {
												
									transferSocket = new Socket(getHost, getPort);
									socketConnection = true;
								}
											
								catch (IOException e) {
									
								}
							}
							//System.out.println("Attempting to create a new file with name: " + fileName);		
							//Attempts to send a given file to the client
							File file = new File(filePath);
							//System.out.println("File name created");
							byte[] transferFile = new byte[(int) file.length()];
							//System.out.println("Attempting to create file input stream");
							FileInputStream fIS = new FileInputStream(file);
							//System.out.println("File input stream created");
							BufferedInputStream bIS = new BufferedInputStream(fIS);
							//System.out.println("Buffered input stream created");
							//System.out.println("Length of transferFile: " + transferFile.length);
							bIS.read(transferFile, 0, transferFile.length);
							//System.out.println("Attempting to create file output stream");
							OutputStream oS = transferSocket.getOutputStream();
							oS.write(transferFile, 0, transferFile.length);
							oS.flush();
								
							//Notifies the client of a successful file transfer and closes the data connection
							messageClient("250 Requested file action completed." + CRLF);
							retrCount++;
							//bIS.close();
							//oS.close();
							
							//if (transferSocket != null) {
							
								transferSocket.close();
							//}
							
							validGet = true;
						}
					}
								
					//Catches errors and determines the retr command to be invalid
					catch (InterruptedException e) {
									
						//e.printStackTrace();
						errorRetr = true;
						enteredRetr = false;
						errorDataConnection = true;
					}
								
					catch (IOException e) {
								
						//e.printStackTrace();	
						errorRetr = true;
						enteredRetr = false;
						errorDataConnection = true;
					}
								
					if (!errorRetr) {
									
						validRetr = true;
									
						//Requires another valid port command before another retr command can be processed
						if (enteredPort) {
										
							enteredPort = false;
						}
					}
				}
			}
						
			else {
							
		    //Checks the parameter character-by-character for validity, incrementing the character each loop
			for (int i = 0; i < parameter.length(); i++) {

				int c = parameter.charAt(i);

				if (command.equalsIgnoreCase("type")) {
				
					//Determines whether the type parameter is longer than 1 character
					if (parameter.length() > 1) {
									
						errorType = true;
					}
					
					//Determines whether the parameter matches 'A' or 'I' for the type command
					else if (!(parameter.matches("[AI]"))) {
									
						errorType = true;
					}
								
					if (!errorType) {
									
						if (parameter.matches("I")) {
										
							validTypeI = true;
						}
									
						else if (parameter.matches("A")) {
										
							validTypeA = true;
						}
					}
				}
							
				//Checks whether the parameter is a valid ASCII character
				else if (c > 0x7F) {
								
					if (command.equalsIgnoreCase("user")) {
									
						errorUser = true;
					}
								
					else {
									
						errorPass = true;
					}
				}
			}
		}
							
		tokenCount--;
							
		}
					
		//Determines whether the line produced a certain error and prints out that error
		if (errorCommand) {
						
			messageClient("500 Syntax error, command unrecognized." + CRLF);
		}
					
		else if (errorUser || errorPass || errorType || errorPort) {
						
			messageClient("501 Syntax error in parameter." + CRLF);
		}
		
		else if (errorNotImplemented) {
			
			messageClient("502 Command not implemented." + CRLF);
		}
		
		else if (errorDataConnection) {
			
			messageClient("425 Can not open data connection." + CRLF);
		}
					
		else if (errorRetr) {
						
			messageClient("550 File not found or access denied." + CRLF);
		}
					
		else if (errorCRLF && !validGet) {
						
			messageClient("501 Syntax error in parameter." + CRLF);
		}
						
		else if (validQuit) {
						
			messageClient("221 Goodbye." + CRLF);
			returnLine = "QUIT";
		}
					
		else if (missingPass || errorLoggedIn) {
						
			messageClient("503 Bad sequence of commands." + CRLF);
		}
					
		else if (missingUser) {
						
			messageClient("530 Not logged in." + CRLF);
		}
					
		else if (missingPort) {
						
			messageClient("503 Bad sequence of commands." + CRLF);
		}
					
		else if (validUser) {
						
			messageClient("331 Guest access OK, send password." + CRLF);
		}
					
		else if (validPass) {
						
			messageClient("230 Guest login OK." + CRLF);
		}
					
		else if (validTypeI) {
						
			messageClient("200 Type set to I." + CRLF);
		}
					
		else if (validTypeA) {
						
			messageClient("200 Type set to A." + CRLF);
		}
					
		else if (validSyst) {
						
			messageClient("215 UNIX Type: L8." + CRLF);
		}
					
		else if (validRetr) {
						
			//retrCount++;
		}
					
		else if (validPort) {
						
			messageClient("200 Port command successful (" + updatedPort + ")." + CRLF);
		}
					
		else {
					
			messageClient("200 Command OK." + CRLF);
		}
	}

	//Prints the output and then sends it to the client
	public static void messageClient(String s) throws IOException {
		
		System.out.print(s);
		
		dOS.writeBytes(s);
		dOS.flush();
	}
	
	public static void main(String[] args) throws IOException {
		
		Socket socket = null;
		ServerSocket listenerSocket = null;
		FTPReadLine rL = null;
		
		//Takes in an argument to define the port number
		int port = Integer.parseInt(args[0]);
		
		//Attempts to create a server port
		try {
			
			listenerSocket = new ServerSocket(port);
		}
		
		//Notifies the user of a port that is already in use
		catch (BindException e) {
			
			System.out.printf("Port already in use\n");
		}
		
		while (true) {
			
			try {
				
				//Awaits a connection to its welcoming socket
				socket = listenerSocket.accept();
				
				//Defines buffered reader and data output variables, while setting the quit boolean to false
				BufferedReader buff = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				rL = new FTPReadLine(buff);
				dOS = new DataOutputStream(socket.getOutputStream());
				validQuit = false;
				
				//Notifies the client of a successful connection
				messageClient("220 COMP 431 FTP server ready." + CRLF);
				
				//Runs in a loop while the client has yet to send an exit command
				while (!validQuit) {
					
					String input = rL.readLine();
					if (input == null || input.length() < 4) {
						
						break;
					}
					
					server(input);
					
					if (socket == null || socket.isClosed() || socket.equals(-1)) {
						
						validQuit = true;
					}
					
					//Runs if the client sends an exit command
					if (validQuit) {
						
						if (socket != null) {
							
							//Attempts to close all connections to the client
							try {
							
								dOS.close();
								socket.close();
							}
							
							catch (IOException e) {
								
							}
						}
						
						break;
					}
				}	
			}
				
			catch (IOException e) {
					
			}
		}
	}
}
//The program never terminates on its own
