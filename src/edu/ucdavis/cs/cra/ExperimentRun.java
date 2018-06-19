package edu.ucdavis.cs.cra;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Contains all of the information necessary for a single run of the experiment.
 * 
 * @author Mac Crompton
 *
 */
public class ExperimentRun {
	// Extra data to identify this specific run of the experiment
	protected String metadata;
	// A numeric id identifying this run
	protected int id;
	// An english text description of the run
	protected String description;
	// A list of all commands that will be executed during this run in chronological order
	protected ArrayList<ExperimentCommand> commands;
	
	/**
	 * Descriptor for a specific run of the experiment, including all commands to be executed during this run.
	 * 
	 * @param id A numeric identifier that must be unique to a specific run configuration
	 * @param description An english text description of the run (for debugging purposes)
	 */
	public ExperimentRun(int id, String description) {
		this.id = id;
		this.description = description;
		this.metadata = "";
		commands = new ArrayList<ExperimentCommand>();
	}
	
	/**
	 * Appends a command to the list of commands.<br>
	 * This command should come chronologically during or after all other added commands.<br>
	 * The command list sorts all commands in chronological order at the end of this function.<br>
	 * Appending a command in proper chronological order is faster than appending in random order.
	 * 
	 * @param command The {@link ExperimentCommand} that should be appended.
	 */
	public void addCommand(ExperimentCommand command) {
		commands.add(command);
		// This was a lambda expression to sort the ExperimentCommand's with, but it looks ugly...
		/** commands.sort((ExperimentCommand c1, ExperimentCommand c2) -> (int)(c1.getTime() - c2.getTime())); **/
		// Better to declare the method of comparison explicitly
		commands.sort(ExperimentCommand.ECComparator);
	}
	
	/**
	 * Executes the experimental run described by this experiment.<br>
	 * Handles communication of experiment commands to all appropriate nodes within the experiment.<br>
	 * As well as timing of all commands and receiving acknowledgments from experiment nodes.
	 * 
	 * @param sockets A hashmap containing EXPERIMENT_NAME to client {@link Socket} mappings.
	 * @param serverMap A hashmap containing EXPERIMENT_NAME to DETER_NAME mappings. Used for acquiring the correct sockets.
	 * @throws InterruptedException Thrown if this thread is interrupted outside of normal execution.
	 * @throws IOException Thrown if {@link Socket} streams encounter an error.
	 */
	public void run(HashMap<String, Socket> sockets, HashMap<String, String> serverMap) throws InterruptedException, IOException {
		// Keep track of the timestamp for when this run began (in milliseconds).
		long start = System.currentTimeMillis();
		System.out.println("About to sleep");
		// Execute each command in sequence
		for(ExperimentCommand command : commands) {
			// If it is not yet time to execute the command, sleep until it is.
			while((System.currentTimeMillis() - start)/1000 < command.getTime()) {
				// Prints out the amount of time remaining in seconds (every second).
				System.out.println(command.getTime() - (System.currentTimeMillis() - start)/1000);
				// Sleep for a quarter of a second.
				// Note: Thread.sleep will sleep for AT LEAST 250 milliseconds (may sleep for longer).
				// Therefore if command execution timing is more critical, decrease this value to gain better granularity.
				Thread.sleep(250);
			}
			System.out.println("Running Command:");
			System.out.println(command.getClient() + " " + command.getTime() + " " + command.getCommand());
			// If there is no client corresponding to this command, skip it. It is invalid. (This should never happen)
			if(command.getClient() == null)
				continue;
			// If this is an 'all' command. Broadcast it to all connected experiment nodes.
			if(command.getClient().equals("all")) {
				// Loop through all of the nodes.
				for(String host : serverMap.keySet()) {
					// Get the associated Socket
					Socket socket = sockets.get(serverMap.get(host));
					// If there is no socket, this is an invalid node.
					if(socket == null)
						continue;
					
					// Grab the input and output streams.
					BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					OutputStream toClient = socket.getOutputStream();
					
					// Keep sending the command until we receive an acknowledgement from the node.
					// This is a bit redundant as TCP guarantees the message will send, but it makes me feel better.
					String response = "";
					while(response == null || !response.equals("ack")) {
						// If it is the 'start' command, send some extra data to the node for reference.
						if(command.getCommand().contains("start"))
							toClient.write((command.getCommand() + " " + host + " " + id + " " + metadata + "\n").getBytes());	// Send the host name and run number when sending command to start
						else	// Otherwise just send the command.
							toClient.write((command.getCommand() + "\n").getBytes());
						System.out.println("Waiting for ack from: " + host);
						// Receive a response from the node.
						response = fromClient.readLine();
						System.out.println("Response from client: " + response);
					}
				}
			} else {
				// Get the client we're going to be sending the command to.
				String client = serverMap.get(command.getClient());
				System.out.println("Running command for: " + client);
				// Get that client's Socket
				Socket socket = sockets.get(client);
				
				// If the socket is null, skip this command
				if(socket == null)
					continue;
				// Get the socket's input and ouput streams
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				DataOutputStream toClient = new DataOutputStream(socket.getOutputStream());
				
				// Keep sending the command until we receive an acknowledgement from the node.
				// This is a bit redundant as TCP guarantees the message will send, but it makes me feel better.
				String response = "";
				while(!response.equals("ack")) {
					System.out.println("Sending command: " + command.getCommand());
					toClient.writeChars(command.getCommand() + "\n");
					response = fromClient.readLine();
				}
			}
		}
		
		// Once this run is done, sleep for 10 seconds to allow the machines to cool off
		Thread.sleep(10000);
	}

	/**
	 * @return Metadata pertaining to this particular run. Usually the duplicate identifier.
	 */
	public String getMetadata() {
		return metadata;
	}

	/**
	 * @param metadata Assign the metadata pertaining to this particular run a value.
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
}
