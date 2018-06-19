package com.zorben.byzantine;

public class TimeoutException extends Exception {
	private static final long serialVersionUID = -8711059789437514428L;

	public TimeoutException() {
		super("Replica server has taken too long to respond.");
	}
}
