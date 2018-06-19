package edu.ucdavis.cs.cra;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import edu.ucdavis.cs.cra.sensors.CpuSensor;
import edu.ucdavis.cs.cra.sensors.DiskSensor;
import edu.ucdavis.cs.cra.sensors.NetworkSensor;
import edu.ucdavis.cs.cra.sensors.RamSensor;
import edu.ucdavis.cs.cra.sensors.Sensor;

/**
 * Main class for execution of the control module of this experiment.
 * 
 * @author Mac Crompton
 *
 */
public class ControlMain {

	// The process and thread for managing malware
	private static Thread processReader;
	private static Process malwareProc;
	// The process and thread for managing the client software
	private static Thread clientReader;
	private static Process clientProc;
	
	private static Process tcpdumpProc;

	// The start timestamp of the current run.
	private static long startTime = 0;

	// The EXPERIMENT_NAME of this machine
	private static String hostname = "UNKNOWN";
	// The run identifier
	private static int id = -1;
	// Any metadata (usually the duplicate identifier)
	private static String metadata = "";

	// A flag representing if this machine is also a client machine
	private static ControlType type = ControlType.NONE;

	private enum ControlType {
		NONE,
		CLIENT,
		ATTACKER,
		BC,
		AUTH
	}

	public static void main(String[] args) throws Exception {
		// The first argument indicates which server to connect to (the command server)
		String server = "localhost";
		if(args.length >= 1)
			server = args[0];

		// The second argument indicates if this machine is a client
		if(args.length >= 2) {
			switch(args[1].toLowerCase()) {
			case "client":
				type = ControlType.CLIENT;
				break;
			case "attacker":
				type = ControlType.ATTACKER;
				break;
			case "bc":
				type = ControlType.BC;
				break;
			case "auth":
				type = ControlType.AUTH;
				break;
			}
		}

		// Connect to the command server on port 52017
		Socket client = new Socket(server, 52017);
		System.out.println("Connected!");

		// Open the input and output streams to the command server
		DataOutputStream toServer = new DataOutputStream(client.getOutputStream());
		BufferedReader fromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));

		// Keep track of our sensors and running threads for those sensors
		ArrayList<Sensor> sensors = new ArrayList<Sensor>();
		ArrayList<Thread> sThreads = new ArrayList<Thread>();

		// Keep going until we're done
		while(true) {
			// Wait for a command from the command server
			System.out.println("Waiting for message from server...");
			String message = fromServer.readLine();
			message = message.replace("\0", "");
			System.out.println(message);
			// This is a hack to prevent Java from complaining about the infinite while loop.
			if(System.currentTimeMillis() == 0)
				break;

			// Parse out the message's parameters
			String[] params = message.split(" ");

			if(message.contains("start")) {
				// If this is the start command, set up all of the threads and variables necessary for the run
				startTime = System.currentTimeMillis();
				hostname = params[1];
				id = Integer.parseInt(params[2]);
				metadata = params[3];
				System.out.println("Received request to start " + hostname + " " + id + " " + metadata);

				// Set up new sensors with the newly received values
				sensors.clear();
				sensors.add(new NetworkSensor(startTime, metadata, id, hostname));
				sensors.add(new CpuSensor(startTime, metadata, id, hostname));
				sensors.add(new RamSensor(startTime, metadata, id, hostname));
				sensors.add(new DiskSensor(startTime, metadata, id, hostname));

				// Generate and start new threads
				sThreads.clear();
				for(Sensor s : sensors) {
					s.stop = false;
					Thread t = new Thread(s);
					sThreads.add(t);
					t.start();
				}
				
				{
					String resultsDir = "results/dataset_" + metadata + "/run_" + id + "/host_" + hostname + "/";
					String[] cmd = { "sudo", "tcpdump", "-i", "any", "udp", "-vv", ">", resultsDir+"tcpdump.log" };
					ProcessBuilder pb = new ProcessBuilder(cmd);
					tcpdumpProc = pb.start();
				}

				// If this machine is a client, also start the client process
				if(ControlMain.type == ControlType.CLIENT) {
					System.out.println("This is a client, starting client stuff");

					// Execute a shell command which will start the client process
					String[] cmd = { "./ClientStart.sh" };
					ProcessBuilder pb = new ProcessBuilder(cmd);
					pb.redirectErrorStream(true);
					clientProc = pb.start();
				} else if(ControlMain.type == ControlType.ATTACKER) {
					System.out.println("This is an attacker, starting attacker stuff");

					// Execute a shell command which will start the attacker process
					String[] cmd = { "./AttackerStart.sh" };
					ProcessBuilder pb = new ProcessBuilder(cmd);
					pb.redirectErrorStream(true);
					clientProc = pb.start();
				} else if(ControlMain.type == ControlType.BC) {
					System.out.println("This is an commander, starting commander stuff");

					// Execute a shell command which will start the attacker process
					String[] cmd = { "./ByzantineStart.sh" };
					ProcessBuilder pb = new ProcessBuilder(cmd);
					pb.redirectErrorStream(true);
					clientProc = pb.start();
				} else if(ControlMain.type == ControlType.AUTH) {
					System.out.println("This is an auth, starting auth stuff");

					// Execute a shell command which will start the attacker process
					String[] cmd = { "./AuthStart.sh", args[2] };
					ProcessBuilder pb = new ProcessBuilder(cmd);
					pb.redirectErrorStream(true);
					clientProc = pb.start();
				}

				if(clientProc != null) {
					// Connect to the process's input stream and write out the run's parameters
					PrintWriter out = new PrintWriter(clientProc.getOutputStream());
					out.write(hostname + " " + id + " " + metadata + "\n");
					out.flush();

					// Keep a thread running which will capture all messages from the client.
					// This prevents the client from waiting on us to read the messages
					// This is mostly for debugging purposes.
					System.out.println("Executing process: " + clientProc);
					clientReader = new Thread(new Runnable() {
						public void run() {
							InputStream in = clientProc.getInputStream();
							try {
								while(clientProc.isAlive() || in.available() > 0) {
									System.out.print((char)in.read());
								}
							} catch (IOException e) {
							}
						}
					});
					clientReader.start();
				}

				// Send the command server an acknowledgement that the command was received.
				toServer.writeBytes("ack\n");
				toServer.flush();
			}
			if(message.contains("metric")) {
				PrintWriter pw = new PrintWriter(clientProc.getOutputStream());
				pw.println(message);
				pw.flush();

				// Send an acknowledgement to the server
				toServer.writeBytes("ack\n");
				toServer.flush();
			}
			if(message.contains("stop")) {
				// This run is now stopping, clean up all running threads and processes

				// Alert all of the sensors to stop.
				for(Sensor s : sensors) {
					s.stop = true;
				}
				
				tcpdumpProc.destroy();

				// Alert and wait for the malware process to stop if it exists
				if(malwareProc != null) {
					PrintWriter pw = new PrintWriter(malwareProc.getOutputStream());
					pw.println("stop");
					pw.flush();
					malwareProc.waitFor();
				}

				// If this machine is a client and the process exists, alert and wait for the client process to stop.
				if(clientProc != null) {
					System.out.println("Telling client to stop...");
					PrintWriter pw = new PrintWriter(clientProc.getOutputStream());
					pw.println("stop");
					pw.flush();
					System.out.println("Waiting for client to stop...");
					clientProc.waitFor();
				}

				// Wait for the sensor threads to rejoin the main thread.
				System.out.println("Waiting for threads to rejoin");
				for(Thread t : sThreads) {
					t.join();
				}

				// Send the command server an acknowledgement, indicating that we have successfully stopped.
				System.out.println("Sending ack to command server");
				toServer.writeBytes("ack\n");
				toServer.flush();

				clientProc = null;
			}
		}

		// Properly close the connection.
		client.close();
	}
}
