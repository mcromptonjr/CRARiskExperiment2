package edu.ucdavis.cs.cra.sensors;

/**
 * Sensor abstraction for parallelizing sensor recordings
 * 
 * @author Mac Crompton
 *
 */
public abstract class Sensor implements Runnable {
	public boolean stop = false;
	
	protected long startTime;
	protected String metadata;
	protected int id;
	protected String hostname;
	
	/**
	 * Constructs a sensor which records a set of data
	 * 
	 * @param startTime The start timestamp
	 * @param metadata Metadata for the run
	 * @param id The id of the run
	 * @param hostname The hostname of the machine running the sensor
	 */
	public Sensor(long startTime, String metadata, int id, String hostname) {
		super();
		this.startTime = startTime;
		this.metadata = metadata;
		this.id = id;
		this.hostname = hostname;
	}
	
	
}
