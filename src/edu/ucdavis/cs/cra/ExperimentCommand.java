package edu.ucdavis.cs.cra;
import java.util.Comparator;

/**
 * A container for all information pertaining to a specific command within an {@link ExperimentRun}
 * 
 * @author Mac Crompton
 *
 */
public class ExperimentCommand {
	
	/**
	 * A {@link java.util.Comparator} object for comparing two {@link ExperimentCommand} objects.
	 */
	public static final Comparator<ExperimentCommand> ECComparator = new Comparator<ExperimentCommand>() {

		/**
		 * Compare two {@link ExperimentCommand} objects based on their timestamps
		 */
		public int compare(ExperimentCommand ec0, ExperimentCommand ec1) {
			return (int)(ec0.getTime() - ec1.getTime());
		}
	};
	
	protected long time;	// The timestamp at which to run this command, in seconds
	protected String client;	// The string identifying the client
	protected String command;	// The command to send to the client
	
	/**
	 * Constructs an experiment command object given the timestamp, client name, and command string.
	 * 
	 * @param time The timestamp (in seconds) for which to execute this command.
	 * @param client A string uniquely identifying the client to send this command to ('all' will send it to every client)
	 * @param command The command to send to the client.
	 */
	public ExperimentCommand(long time, String client, String command) {
		this.time = time;
		this.client = client;
		this.command = command;
	}

	/**
	 * @return The timestamp in seconds.
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return The unique client name (EXPERIMENT_NAME)
	 */
	public String getClient() {
		return client;
	}

	/**
	 * @return The command to send to the client at the specified timestamp.
	 */
	public String getCommand() {
		return command;
	}
	
}
