package edu.ucdavis.cs.cra;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.ucdavis.cs.cra.utils.Sys;


/**
 * Client software for interfacing with a UDP-based server.
 * 
 * @author Mac Crompton
 *
 */
public class TestClient {

	// How long in milliseconds to declare a request officially dead.
	private static final long TIMEOUT = 10000;	// Ten second timeout for request/response time
	// A mutex object for safe parallelized operations
	private static Object mutex = new Object();
	// The number of requests sent to the database server.
	private static long count = 0;
	// The number of requests successfully received
	private static long numReceived = 0;
	// The total amount of time packets spent in transit (for average time calculation)
	private static long totalTime = 0;
	// The last number of requests received over an interval
	private static long lastNumReceived = 0;

	// Whether or not to stop
	private static boolean stop = false;

	// Metadata which identifies which duplicate we are running
	private static String metadata = "orig";
	// The hostname of this machine
	private static String host = "";
	// The ID of the run
	private static int id = -1;

	public static void main(String[] args) throws IOException, InterruptedException {

		// Receive information about this machine and which duplicate/run we are on
		System.out.println("Waiting for client parameters");
		Scanner input = new Scanner(System.in);
		if(input.hasNextLine()) {
			String line = input.nextLine();
			String[] params = line.split(" ");
			if(params.length >= 3) {
				host = params[0];
				id = Integer.parseInt(params[1]);
				metadata = params[2];
				System.out.println("Starting Client: " + host + " " + id + " " + metadata);
			}
		}
		System.out.println("Finished receiving client parameters");

		// Get the hostname of the database server
		String server = "bc";
		if(args.length >=1 ) {
			server = args[0];
		}

		// Open up a UDP socket to the database server
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(1000);
		InetAddress IPAddress = InetAddress.getByName(server);
		byte[] sendData = new byte[Long.BYTES];
		byte[] receiveData = new byte[1024];


		// Use this to retain sent queries that are still waiting on a response
		HashMap<Long, Long> requests = new HashMap<Long, Long>();

		// Start a thread which receives and handles requests.
		Thread recv = new Thread(new Runnable() {
			public void run() {
				while(!stop) {
					try {
						// Receive a response from the database server
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
						clientSocket.receive(receivePacket);

						// Find the id of the request's response
						ByteBuffer bytes = ByteBuffer.allocate(Long.BYTES);
						bytes.put((byte)0);
						for(int i = 1; i < Long.BYTES; i++)
							bytes.put(receiveData[i]);
						bytes.flip();
						long id = bytes.getLong();
						System.out.println("Received request: " + id);
						// Remove the appropriate request from our list of waiting requests, and increment total time. (Do this in a thread-safe manner)
						synchronized(mutex) {
							if(requests.get(id) == null) continue;
							long time = requests.remove(id);
							totalTime += System.currentTimeMillis() - time;
						}

						// Increment our number of received responses, only if we dont receive the "Evil Bit"
						if(receiveData[0] == 0) {
							numReceived++;
							lastNumReceived++;
						}

					} catch (IOException e) {
						//						e.printStackTrace();
					}
				}
				System.out.println("Data receive thread stopping");
			}
		});
		recv.start();

		// Start a thread which reads values sent from the controling process.
		Thread is = new Thread(new Runnable() {
			public void run() {
				Scanner input = new Scanner(System.in);
				while(input.hasNextLine()) {
					String line = input.nextLine();
					if(line.contains("stop")) {
						stop = true;
						break;
					}
				}
				input.close();
				System.out.println("Client input read thread stopped");
			}
		});
		is.start();

		// Keep track of the current time and start time of this client.
		long time = System.currentTimeMillis();
		long startTime = System.currentTimeMillis();

		// Open up file handles to the client-specific metric files
		String resultsDir = "results/dataset_" + metadata + "/run_" + id + "/host_" + host;
		String file = "/success_";

		FileWriter ssucc = new FileWriter(Sys.createFile(resultsDir, file+"ssucc"));
		FileWriter sfail = new FileWriter(Sys.createFile(resultsDir, file+"sfail"));
		FileWriter sperc = new FileWriter(Sys.createFile(resultsDir, file+"sperc"));
		FileWriter stime = new FileWriter(Sys.createFile(resultsDir, file+"stime"));

		while(!stop) {
			// Add a new request to our request tracker, and remove entries that have timed out. (Do this in a thread-safe manner)
			synchronized(mutex) {
				requests.put(count, System.currentTimeMillis());
				requests.entrySet().removeIf((Map.Entry<Long, Long> e) -> (System.currentTimeMillis() - e.getValue() > TIMEOUT));
			}

			// Construct the UDP packet data
			ByteBuffer bytes = ByteBuffer.allocate(Long.BYTES);
			bytes.putLong(count++);
			for(int i = 0; i < Long.BYTES; i++)
				sendData[i] = bytes.get(i);

			System.out.println("Sending request: " + count);
			// Send the data in a UDP packet to the database
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 52017);
			clientSocket.send(sendPacket);

			// Record client-specific metrics once every second
			if(System.currentTimeMillis() - time > 1000) {
				//				System.out.println(count + " " + numReceived + " " + requests.size() + " " + (count - numReceived - requests.size()) + " " + ((double)totalTime / (double)lastNumReceived));
				long curTime = System.currentTimeMillis() - startTime;

				// Number of requests received per second
				ssucc.write((curTime + " " + numReceived) + "\n");
				ssucc.flush();

				// Number of requests not received per second
				sfail.write((curTime + " " + (count - numReceived - requests.size())) + "\n");
				sfail.flush();

				// Percent of requests successfully received per second
				sperc.write((curTime + " " + ((double)numReceived / (double)(count - requests.size()))) + "\n");
				sperc.flush();

				// Average amount of time each request takes over the last second
				stime.write((curTime + " " + ((double)totalTime / (double)lastNumReceived)) + "\n");
				stime.flush();

				time = System.currentTimeMillis();
				totalTime = 0;
				lastNumReceived = 0;
			}

			// Send a packet once every 4.5 milliseconds
			try {
				//				Thread.sleep(4, 500000);
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// Close all of the file handles
		System.out.println("Closing file writers");
		ssucc.close();
		sfail.close();
		sperc.close();
		stime.close();

		// Rejoin all open threads safely
		System.out.println("Rejoining client threads");
		recv.join();
		is.join();

		// Close the UDP socket opened
		System.out.println("Closing client socket");
		clientSocket.close();

		System.out.println("Terminating client");
	}
}
