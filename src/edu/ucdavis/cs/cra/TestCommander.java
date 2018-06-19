package edu.ucdavis.cs.cra;

import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

import com.zorben.byzantine.ByzantineClient;
import com.zorben.byzantine.ByzantineConsensusException;

import edu.ucdavis.cs.cra.utils.Sys;

public class TestCommander {

	static boolean testMode = false;
	
	static boolean stop = false;

	// Metadata which identifies which duplicate we are running
	private static String metadata = "orig";
	// The hostname of this machine
	private static String host = "";
	// The ID of the run
	private static int id = -1;
	
	static ByzantineClient bc = null;
	
	static Object mutex = new Object();
	static double avgEditDistance = 0;
	static double avgConsensus = 0;
	static double avgRequestTime = 0;
	static int requests = 0;

	public static void main(String[] args) throws IOException {
		if(args.length > 0)
			testMode = true;
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

		// Start a thread which reads values sent from the controling process.
		Thread is = new Thread(new Runnable() {
			public void run() {
				Scanner input = new Scanner(System.in);
				while(input.hasNextLine()) {
					String line = input.nextLine();
					if(line.contains("stop")) {
						stop = true;
						bc.stop();
						break;
					}
				}
				input.close();
				System.out.println("Client input read thread stopped");
			}
		});
		is.start();

		String resultsDir = "results/dataset_" + metadata + "/run_" + id + "/host_" + host;
		String file = "/byzantine_";

		bc = new ByzantineClient();
		
		FileWriter bdist = new FileWriter(Sys.createFile(resultsDir, file+"bdist"));
		FileWriter btime = new FileWriter(Sys.createFile(resultsDir, file+"btime"));
		FileWriter bcons = new FileWriter(Sys.createFile(resultsDir, file+"bcons"));
		
		Thread sensors = new Thread(new Runnable() {
			public void run() {
				long startTime = System.currentTimeMillis();
				while(!stop) {
					long curTime = System.currentTimeMillis() - startTime;
					
					// Number of requests received per second
					try {
						bdist.write((curTime + " " + (avgEditDistance / requests)) + "\n");
						bdist.flush();
						
						btime.write((curTime + " " + (avgRequestTime / requests)) + "\n");
						btime.flush();
						
						bcons.write((curTime + " " + (avgConsensus / requests)) + "\n");
						bcons.flush();
						
						synchronized(mutex) {
							avgEditDistance = 0;
							avgRequestTime = 0;
							avgConsensus = 0;
							requests = 0;
						}
						
						Thread.sleep(1000);
					} catch (IOException e) {
					} catch (InterruptedException e) {
					}
				}
			}
		});
		sensors.start();

		System.out.println("Opening socket on 52017");
		DatagramSocket serverSocket = new DatagramSocket(52017);
		serverSocket.setSoTimeout(100);
		while(!stop) {
			long id = 0;
			InetAddress IPAddress = null;
			int port = 0;

			// Receive a UDP packet request from a client.
			System.out.println("Awaiting packet");
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			if(!testMode) {
				
				try {
					serverSocket.receive(receivePacket);
				} catch (IOException e) {
				}


				for(int i = 0; i < Long.BYTES; i++) {
					id = id << 8;
					id = id | receiveData[i];
				}

				// Get the client to send data back to.
				IPAddress = receivePacket.getAddress();
				if(IPAddress == null)  {
					System.out.println("Received null packet... Ignoring...");
					continue;
				}
				port = receivePacket.getPort();
				System.out.println("Packet received " + id + " " + IPAddress + " " + port);
			} else {
				id = input.nextLong();
				long idCopy = id;
				for(int i = Long.BYTES-1; i >= 0; i--) {
					receiveData[i] = (byte) (0xFF & idCopy);
					idCopy = idCopy >>> 8;
				}
				String ip = input.next();
				IPAddress = InetAddress.getByName(ip);
				port = 51234;
			}


			//			if(!requests.containsKey(IPAddress)) {
			//				requests.put(IPAddress, new HashSet<>());
			//			}

			//			requests.get(IPAddress).add(id);

			byte[] message = new byte[Long.BYTES+Integer.BYTES];
			for(int i = 0; i < Long.BYTES; i++) {
				message[i] = receiveData[i];
			}
			// TODO: This is not safe, if the address is IPv6!!!
			byte[] ipa = IPAddress.getAddress();
			for(int i = 0; i < ipa.length; i++) {
				message[i+Long.BYTES] = ipa[i];
			}

			for(int i = 0; i < message.length; i++) {
				System.out.print(message[i] + " ");
			}
			System.out.println();

			long time = 0;
			try {
				System.out.println("Sending request to DB");
				time = System.currentTimeMillis();
				receiveData = bc.request(message);
				time = System.currentTimeMillis() - time;
				synchronized(mutex) {
					requests++;
					avgEditDistance += bc.getView().getEditDistance();
					avgConsensus += bc.getView().getConsensus();
					avgRequestTime += time;
				}
				
				System.out.println("Received Response with consensus: " + bc.getView().getConsensus());
				if(receiveData == null || receiveData.length == 0) continue;
			} catch (InterruptedException e) {
				e.printStackTrace();
				continue;
			}

			// TODO: Respond to client!
			id = 0;
			for(int i = 0; i < Long.BYTES; i++) {
				id = id << 8;
				id = id | receiveData[i];
			}

			byte[] address = new byte[4];
			for(int i = Long.BYTES; i < Long.BYTES+Integer.BYTES; i++) {
				//				System.out.println("INDICES: " + (i-Long.BYTES) + " " + (i));
				address[i-Long.BYTES] = receiveData[i];
			}
			InetAddress resp = InetAddress.getByAddress(address);

			if(!testMode) {
				DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length, resp, port);
				serverSocket.send(packet);
			} else {
				System.out.println("COMMANDER RECEIVED: " + id + " " + resp.toString());
			}
		}
		
		bdist.close();
		btime.close();
		bcons.close();
		
		serverSocket.close();
		
		input.close();
	}
}
