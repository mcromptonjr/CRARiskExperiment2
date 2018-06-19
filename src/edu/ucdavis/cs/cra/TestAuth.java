package edu.ucdavis.cs.cra;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;

public class TestAuth {
	public static boolean stop = false;
	public static boolean fault = false;
	public static double faultPercent = 0;
	public static Object mutex = new Object();

	static InetAddress dbAddress;
	static InetAddress byzAddress;

	public static void main(String[] args) throws SocketException, UnknownHostException {
		int index = Integer.valueOf(args[0]);
		if(args.length >= 2 && args[1].toLowerCase().equals("true")) {
			fault = true;
		}
		dbAddress = InetAddress.getByName("DB"+index);
		byzAddress = InetAddress.getByName("BC");
		
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

		// From/To Byzantine Commander
		DatagramSocket fromByzSocket = new DatagramSocket(52018);
		fromByzSocket.setSoTimeout(100);
		DatagramSocket toByzSocket = new DatagramSocket();

		// From/To Database Server
		DatagramSocket fromDbSocket = new DatagramSocket(42018);
		fromDbSocket.setSoTimeout(100);
		DatagramSocket toDbSocket = new DatagramSocket();

		Thread byzThread = new Thread(new Runnable() {
			public void run() {
				while(!stop) {
					// Receive a request from the byzantine commander
					byte[] receiveData = new byte[1024];
					DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
					try {
						fromByzSocket.receive(packet);
						System.out.println("Received data from BC: " + receiveData);
					} catch (IOException e) {
						continue;
					}

					System.out.println("Received data, sending to DB");
					
					for(int i = 0; i < 12; i++) {
						System.out.print(receiveData[i] + " ");
					}
					
					boolean debug = true;
					for(int i = 0; i < 11; i++) {
						debug = debug && receiveData[i] == -1;
					}
					if(debug) {
						synchronized(mutex) {
							fault = true;
							byte percent = receiveData[11];
							faultPercent = percent / 100.0;
							continue;
						}
					}

					// Pass it along to the database
					packet = new DatagramPacket(receiveData, receiveData.length, dbAddress, 52017);
					System.out.println("Sending data to DB " + dbAddress);
					try {
						toDbSocket.send(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				fromByzSocket.close();
				toDbSocket.close();
			}
		});

		Thread dbThread = new Thread(new Runnable() {
			public void run() {
				while(!stop) {
					byte[] receiveData = new byte[1024];
					DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
					try {
						fromDbSocket.receive(packet);
					} catch (IOException e) {
						continue;
					}

					System.out.println("Received DB response, sending to BC");
					
					for(int i = 0; i < 12; i++) {
						System.out.print(receiveData[i] + " ");
					}
					// Modify the data here
					synchronized(mutex) {
						if(fault) {
							System.out.println("Creating faulty data...");
							long id = 0;
							for(int i = 0; i < Long.BYTES; i++) {
								id = id << 8;
								id = id | receiveData[i];
							}
							Random rand = new Random(-id);
							// Set a signal for the clients to know the data is bad (Clients would not normally know this, this is an "Evil Bit")
							receiveData[0] = (byte)0xFF;
							for(int i = 12; i < receiveData.length*faultPercent; i++) {
								receiveData[i] = (byte)rand.nextInt(Byte.MAX_VALUE);
							}
						}
					}

					// Pass it back to the byzantine commander
					packet = new DatagramPacket(receiveData, receiveData.length, byzAddress, 52180+index);
					try {
						toByzSocket.send(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				fromDbSocket.close();
				toByzSocket.close();
			}
		});

		System.out.println("Starting Auth Processing Threads");
		byzThread.start();
		dbThread.start();
		
		
	}
}
