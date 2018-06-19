package edu.ucdavis.cs.cra;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class TestAttacker {

	public static boolean stop = false;

	public static String host = "";
	public static int id = 0;
	public static String metadata = "";

	public static void main(String[] args) throws IOException {
		System.out.println("Waiting for attacker parameters");
		Scanner input = new Scanner(System.in);
		if(input.hasNextLine()) {
			String line = input.nextLine();
			String[] params = line.split(" ");
			if(params.length >= 3) {
				host = params[0];
				id = Integer.parseInt(params[1]);
				metadata = params[2];
				System.out.println("Starting Attacker: " + host + " " + id + " " + metadata);
			}
		}
		System.out.println("Finished receiving client parameters");
		
		DatagramSocket socket = new DatagramSocket();
		byte[] payload = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 
				           (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 
				           (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00 };

		while(input.hasNextLine()) {
			String line = input.nextLine();
			if(line.contains("stop")) {
				stop = true;
				break;
			} else {
				String[] params = line.split(" ");
				InetAddress address = InetAddress.getByName(params[1]);
				byte percent = Byte.parseByte(params[2]);
				payload[11] = percent;
				System.out.println("Sending attack to: " + params[1] + " " + params[2]);
				DatagramPacket packet = new DatagramPacket(payload, payload.length, address, 52018);
				socket.send(packet);
			}
		}
		input.close();
		socket.close();
		System.out.println("Attacker input read thread stopped");
	}
}
