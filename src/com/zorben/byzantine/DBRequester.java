package com.zorben.byzantine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DBRequester {
	
	DatagramSocket clientSocket;
	InetAddress ipAddress;
	
	public DBRequester(String authHost) throws SocketException, UnknownHostException {
		ipAddress = InetAddress.getByName(authHost);
		
		int index = Integer.valueOf(authHost.substring(4));
		
		clientSocket = new DatagramSocket(52180+index);
		clientSocket.setSoTimeout(100);
	}
	
	public byte[] makeRequest(byte[] request) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(request, request.length, ipAddress, 52018);
		clientSocket.send(sendPacket);
		
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket);
		
		return receiveData;
	}
}
