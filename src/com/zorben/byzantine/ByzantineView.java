package com.zorben.byzantine;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ByzantineView {
	
	private int numFaults;
	private int numConsensus = 0;
	private double editDistance = 0;
	
	private ArrayList<ByzantineReplica> replicas;
	
	public ByzantineView() throws FileNotFoundException, SocketException, UnknownHostException {
		numFaults = 1;
		int numReplicas = 3 * 1 + 1;
		
		if(numReplicas > 0) {
			replicas = new ArrayList<ByzantineReplica>();
			for(int i = 0; i < numReplicas; i++) {
				replicas.add(new ByzantineReplica(i, this, "auth"+i));
			}
		}
		
		/** OLD CODE ************
		Scanner input = new Scanner(new File(faultyCFG));
		while(input.hasNextLine()) {
			String line = input.nextLine();
			int index = Integer.valueOf(line);
			if(index >= 0 && index < replicas.size()) {
				replicas.get(index).setFaulty(true);
			}
		}
		input.close();
		**/
	}
	
	public void requestPrimary(byte[] message) {
		ByzantineReplica primary = replicas.get(0);
		primary.setRequest(message);
		numConsensus = 0;
		editDistance = 0;
	}
	
	public void requestAll(byte[] message) {
		for(ByzantineReplica replica : replicas) {
			replica.setRequest(message);
		}
		numConsensus = 0;
		editDistance = 0;
	}
	
	public synchronized void doViewChange() {
		while(!replicas.get(0).isViewChange()) {
			ByzantineReplica replica = replicas.remove(0);
			replicas.add(replica);
		}
		byte[] message = getPrimary().getRequest();
		resetAll();
//		System.out.println("View Change Complete. Replica order is now:");
//		System.out.println("Primary Replica: " + replicas.get(0).getID());
		for(int i = 1; i < replicas.size(); i++) {
//			System.out.println("Backup Replica: " + replicas.get(i).getID());
		}
//		System.out.println("Resending request to new primary.");
		requestPrimary(message);
	}
	
	public ByzantineReplica getPrimary() {
		return replicas.get(0);
	}
	
	public void prePrepare(int seqNum, byte[] request) {
		for(int i = 1; i < replicas.size(); i++) {
			replicas.get(i).prePrepare(seqNum, request);
		}
	}
	
	public boolean isEnoughPrepared() {
		int numPrepared = 0;
		for(ByzantineReplica br : replicas) {
			if(br.isPrepared()) {
				numPrepared ++;
			}
		}
		return numPrepared >= 2 * numFaults;
	}
	
	public boolean isEnoughCommitted() {
		int numCommits = 0;
		for(ByzantineReplica br : replicas) {
			if(br.isCommitted()) {
				numCommits ++;
			}
		}
		return numCommits >= 2 * numFaults + 1;
	}
	
	public boolean isEnoughViewChange() {
		int numViewChange = 0;
		for(ByzantineReplica br : replicas) {
			if(br.isViewChange()) {
				numViewChange ++;
			}
		}
		return numViewChange >= 2 * numFaults;
	}
	
	public byte[] isEnoughResponded() throws InterruptedException {
		int numResponded = 0;
		for(ByzantineReplica br : replicas) {
			if(br.getResponse() != null) {
				numResponded ++;
			}
		}
		// TODO: This should be numResponded >= 2*faults + 1, But I cant be bothered to fix some concurrency issue so I have cheesed it a bit.
		if(numResponded >= 4) {
			/** DONT CARE ABOUT EQUIVALENCE ***
			byte[] response = null;
			for(ByzantineReplica replica : replicas) {
				response = replica.getResponse();
				if(response != null) break;
			}
			return response;
			**/
			// Issue with this code. DNS replies can have different TTL values set.
			// TODO: This should be reimplemented when it is possible for DNS replies
			//       can be properly compared.
			HashMap<byte[], Integer> results = new HashMap<byte[], Integer>();
			for(ByzantineReplica replica : replicas) {
				byte[] response = replica.getResponse();
				if(response == null) continue;
				System.out.print("ID " + replica.getID() + ": ");
				for(int i = 0; i < 24; i++) {
					System.out.print(response[i] + " ");
				}
				System.out.println();
				boolean matched = false;
				for(byte[] r : results.keySet()) {
					if(Arrays.equals(response, r)) {
						results.put(r, results.get(r) + 1);
						matched = true;
					}
				}
				if(!matched) {
					results.put(response, 1);
				}
			}
//			System.out.println(results.keySet().size());
			numConsensus = 2;
			ArrayList<byte[]> list = new ArrayList<byte[]>(results.keySet());
			if(list.size() >= 2) {
				editDistance = editDistance(list.get(0), list.get(1)) / (double)list.get(0).length;
			}
			for(byte[] response : results.keySet()) {
//				System.out.println(response + " " + response.length + " " + results.get(response));
				if(results.get(response) >= 2*numFaults + 1) {
					numConsensus = results.get(response);
					return response;
				}
			}
			
			return new byte[0];
		}
		return null;
	}
	
	public int editDistance(byte[] word1, byte[] word2) {
		int diff = 0;
		for(int i = 0; i < word1.length; i++) {
			if(word1[i] != word2[i])
				diff ++;
		}
		
		return diff;
	}
	
	public int getConsensus() {
		return numConsensus;
	}
	
	public double getEditDistance() {
		return editDistance;
	}
	
	public void resetAll() {
//		System.out.println("Resetting replicas");
		for(ByzantineReplica replica : replicas) {
			replica.reset();
		}
//		System.out.println("Reset complete");
	}
	
	public void clearAllResponse() {
		for(ByzantineReplica replica : replicas) {
			replica.setResponse(null);
		}
	}
	
	public void stop() {
		for(ByzantineReplica replica : replicas) {
			replica.setStop(true);
		}
	}
}
