package com.zorben.byzantine;

public class ByzantineConsensusException extends Exception {

	private static final long serialVersionUID = 1L;
	private int numConsensus;
	
	public ByzantineConsensusException(int numConsensus) {
		this.numConsensus = numConsensus;
	}
	
	public int getNumConsensus() {
		return numConsensus;
	}
}
