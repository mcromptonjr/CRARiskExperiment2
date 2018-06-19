package com.zorben.byzantine;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ByzantineClient {
	
	private ByzantineView view;
	
	public ByzantineClient() throws FileNotFoundException, SocketException, UnknownHostException {
		view = new ByzantineView();
	}
	
	public byte[] request(byte[] message) throws InterruptedException {
//		System.out.println("Client: Sending request to primary replica. Waiting for up to 10 seconds for response.");
		view.requestAll(message);
		byte[] response = waitForResponse();
		if(response != null) {
			view.resetAll();
			view.clearAllResponse();
			return response;
		}
		view.resetAll();
//		System.out.println("Client: Primary replica took too long to reply. Sending request to all.  Waiting for up to 10 seconds for response.");
		view.requestAll(message);
		response = waitForResponse();
		if(response == null) {
//			System.err.println("Client: Satisfactory response not received. Request failed.");
		}
		return response;
	}
	
	public byte[] waitForResponse() throws InterruptedException {
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() < start + 10000) {
			byte[] response = view.isEnoughResponded();
			if(response != null && response.length == 0) {
//				System.out.println("Client: Response did not receive high enough consensus. Dropping request...");
				return response;
			}
			if(response != null) {
//				System.out.println("Client: Response received with high enough consensus. Returning response");
				return response;
			}
//			Thread.sleep(10);
		}
		return null;
	}
	
	public void reset() {
		view.resetAll();
	}
	
	public void stop() {
		view.stop();
	}
	
	public ByzantineView getView() {
		return view;
	}
}
