package fr.emse.tscserver;

import de.datenzone.tpm4java.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;

public class Server implements Runnable {

	public static final Integer DEF_LENPORT = new Integer(30303);
	public static final Integer DEF_WENPORT = new Integer(40404);
	public static final String DEF_NAME = "alice";
	private static final String CERTIFILE = "certif.ca";
	TssHighLevel tpm_high = TssFactory.getHighLevel();
	TssLowlevel tpm_low = TssFactory.getLowlevel();
	TSCPrivacyCa ca = null;

	String name = DEF_NAME;
	Integer lenport = DEF_LENPORT;
	Integer wenport = DEF_WENPORT;

	ServerSocket lenSocket = null;
	ServerSocket wenSocket = null;

	boolean stop = false;
	int tpm_vers= 0, tpm_sub= 0;

	private Multiplexor mux = null;

	public void run() {
		while (true) {
			Chunk chunk = mux.read();
			System.out.println( chunk );
		}
	}

	public Server() {
		mux = new Multiplexor();
		try {
			lenSocket = new ServerSocket(lenport);
			wenSocket = new ServerSocket(wenport);
		} catch (IOException e) {
			System.err.println("Could not listen on port: ");
			if (lenSocket == null) System.err.println(lenport.toString());
			else System.err.println(wenport.toString());
			System.exit(1);
		}
		try {
			int tpm_gcv = tpm_low.TPM_GetCapability_Version();
			tpm_vers = tpm_gcv >> 24;
			tpm_sub = tpm_gcv >> 16 & 0xFF;

		}
		catch (TPMException e) {
			System.err.println("TPM Error: " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.err.println("IO Error: " + e.getMessage());
			System.exit(1);
		}

		try {
			ca = TSCPrivacyCa.loadCa(CERTIFILE);
		} catch (IOException e1) {
			System.err.println("LoadCa IO Error: " + e1.getMessage());
			System.exit(1);
		} catch (ClassNotFoundException e2) {
			System.err.println("LoadCa content Error: " + e2.getMessage());
			System.exit(1);
		}

		PublicKey pub = ca.getPublicKey();

		System.out.println("    Name: " + name);
		System.out.println("    Listening on LEN port: " + lenport.toString());
		System.out.println("    Listening on WEN port: " + wenport.toString());
		System.out.println("    Version TPM : " + tpm_vers + "." + tpm_sub);
		System.out.println("    Certificat Label: " + ca.getLabel().toString());

		new Thread( this ).start();
	}
	// Listen for new connections; when the come in, hand them off to
	// the Multiplexor
	public void listen() {
		System.out.println( "Listening...." );
		try {
			while (true) {
				// Get a connection
				Socket socket = lenSocket.accept();
				System.out.println( "Connection from "+socket );

				// Hand it over
				mux.addSocket( socket );
			}
			// Deal with socket problems
		} catch( IOException ioe ) {
			System.err.println( "Exception in listening on " + ioe.getMessage() );
		} finally {
			try {
				close();
			} catch( IOException ioe ) {
				System.err.println( "Can't close " + ioe.getMessage() );
			}
		}
	}

	public void close() throws IOException {
		lenSocket.close();
		lenSocket = null;
		wenSocket.close();
		wenSocket = null;
	}
	
	public void doService() {
		System.out.println( "Servicing...." );

		while (! stop) {
			Socket clientSocket = null;
			String inputLine, outputLine;
			PrintWriter out = null;
			BufferedReader in = null;

			try {
				clientSocket = lenSocket.accept();
			} catch (IOException e) {
				System.err.println("Accept failed.");
				continue;
			}
			
			mux.addSocket(clientSocket);
			
			System.out.println("TSC Connexion " + clientSocket.getInetAddress().toString() + " accepted.");

			try {
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			} catch (IOException e) {
				System.err.println("Socket Stream Access error: " + e.getMessage());
				try {
					clientSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				continue;
			}

			TENProtocol ten = new TENProtocol();

			System.out.println("TSC Connecting...");
			try {
				inputLine = in.readLine();
			} catch (IOException e) {
				System.err.println("readLine error: " + e.getMessage());
				try {
					clientSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				continue;				
			}

			String isConnected = ten.connect(name, inputLine);
			if (isConnected == null) {
				System.err.println("Connection error.");
				try {
					clientSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				continue;
			}
			out.println(isConnected);
			out.flush();
			System.out.println("Connected.");

			try {
				inputLine = in.readLine();
			} catch (IOException e) {
				System.err.println("ReadLine error: " + e.getMessage());
				try {
					clientSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				continue;
			}

			while (inputLine != null) {
				outputLine = ten.processInput(inputLine);
				out.println(outputLine);
				if (outputLine.equals("QUIT")) break;
				try {
					inputLine = in.readLine();
				} catch (IOException e) {
					System.err.println("ReadLine error: " + e.getMessage());
					try {
						clientSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					continue;
				}

			}
			out.close();
			try {
				in.close();
				clientSocket.close();
			} catch (IOException e) {
				System.err.println("Close error: " + e.getMessage());
			}
		}
		try {
			lenSocket.close();
		} catch (IOException e) {
			System.err.println("Close error: " + e.getMessage());
		}

	}

	public static void main(String[] args) {

		Server server = new Server();

		System.out.println("TSC Server.");

		server.doService();

		System.out.println("TSC Server ended.");
	}
}
