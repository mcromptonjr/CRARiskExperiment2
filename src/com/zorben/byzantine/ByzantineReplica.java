package com.zorben.byzantine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ByzantineReplica extends Thread {

	private int id;
	private ByzantineView view;
	private DBRequester dbr;

	private boolean faulty;

	private boolean stop;
	private int lastSeqNum = 0;

	private byte[] request;
	private int seqNum;
	private boolean prepared;
	private boolean commit;
	private boolean viewChange;
	private byte[] response;

	public ByzantineReplica(int id, ByzantineView view, String authHost) throws FileNotFoundException, SocketException, UnknownHostException {
		this.id = id;
		this.view = view;
		dbr = new DBRequester(authHost);
		reset();
		start();
	}

	public synchronized void reset() {
		request = null;
		seqNum = -1;
		prepared = false;
		commit = false;
		viewChange = false;
	}
	
	public synchronized int getID() {
		return id;
	}

	public synchronized byte[] getRequest() {
		return request;
	}

	public synchronized void setRequest(byte[] request) {
		this.request = request;
	}

	public synchronized void prePrepare(int seqNum, byte[] request) {
		this.seqNum = seqNum;
		this.request = request;
	}

	public synchronized boolean isPrepared() {
		return prepared;
	}

	public synchronized void setPrepared(boolean prepared) {
		this.prepared = prepared;
	}

	public synchronized boolean isCommitted() {
		return commit;
	}

	public synchronized void setCommitted(boolean commit) {
		this.commit = commit;
	}

	public synchronized int getSeqNum() {
		return seqNum;
	}

	public synchronized void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}
	
	public synchronized boolean isViewChange() {
		return viewChange;
	}

	public synchronized void setViewChange(boolean viewChange) {
		this.viewChange = viewChange;
	}

	public synchronized byte[] getResponse() {
		return response;
	}

	public synchronized void setResponse(byte[] response) {
		this.response = response;
	}

	public synchronized void setFaulty(boolean faulty) {
		this.faulty = faulty;
	}

	public synchronized boolean isFaulty() {
		return faulty;
	}
	
	public void run() {
		while(!isStop()) {
			if(getRequest() != null) {
				try {
					setResponse(dbr.makeRequest(request));
//					printStatus("Received response!");
					if(response == null) continue;
					reset();
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
	}

	public void run_old() {
		while(!isStop()) {
			if(getResponse() != null) continue;
			if(getRequest() != null) {
				if(this == view.getPrimary() && getSeqNum() == -1) {
					setSeqNum(++lastSeqNum);
					printStatus("Primary received request. Setting sequence number to " + getSeqNum() + ". Sending pre-prepare message.");
					setPrepared(true);
					view.prePrepare(seqNum, request);
				} else if(this != view.getPrimary() && getSeqNum() == -1) {
					if(!isViewChange()) {
						printStatus("Request received without sequence number. Requesting view change.");
						setViewChange(true);
						addDelay();
					} else {
						if(view.isEnoughViewChange()) {
							printStatus("Enough view change requests received. Commencing view change.");
							view.doViewChange();
						}
					}
				} else if(this != view.getPrimary() && getSeqNum() >= 0 && !isPrepared()) {
					printStatus("Pre-prepare received. Sending prepare message.");
					setPrepared(true);
					addDelay();
				} 
				if(getSeqNum() >= 0 && isPrepared()) {
					if(!isCommitted()) {
						if(view.isEnoughPrepared()) {
							printStatus("Enough prepare messages received. Sending commit message.");
							setCommitted(true);
							addDelay();
						}
					} else {
						if(view.isEnoughCommitted() && getResponse() == null) {
							printStatus("Enough commit messages received. Commencing request.");
							try {
								setResponse(dbr.makeRequest(request));
								if(response == null) continue;
								printStatus("Received response!");
								reset();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	public static void addDelay() {
//		try {
//			Thread.sleep((long) (500+Math.random()*500));
//		} catch (InterruptedException e) { 
//			e.printStackTrace(); 
//		}
	}
	
	public void printStatus(String status) {
		System.out.println("ID " + id + ": " + status);
	}
	
	public synchronized boolean isStop() {
		return stop;
	}

	public synchronized void setStop(boolean stop) {
		this.stop = stop;
	}
}
