package edu.ucdavis.cs.cra;
import java.io.File;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;


/**
 * Main class for execution of the command module of this experiment.
 * 
 * @author Mac Crompton
 *
 */
public class CommandMain {
	
	// List of all runs and their configuration parameters
	private static ArrayList<ExperimentRun> runs = new ArrayList<>();
	// The number of times we should run this experiment as a randomized duplicate
	private static final int NUM_DUPLICATES = 2;
	// If these values are non-negative, we should skip to a specific duplicate/run
	private static int skipDup = -1;
	private static int skipRun = -1;

	public static void main(String[] args) throws Exception {
		
		if(args.length >= 2) {
			try {
				skipDup = Integer.valueOf(args[0]);
				skipRun = Integer.valueOf(args[1]);
			} catch (Exception e) {
				System.err.println("Skip parameters are invalid.");
				System.err.println("Usage: java CommandMain [duplicate_num] [run_num]");
				System.err.println("Example: java CommandMain 0 15");
				System.err.println("Skips the original set up to but not including run id 15");
				System.exit(-1);
			}
		}
		
		runExperiment();
		
//		ServerSocket server = new ServerSocket(52017);
//		server.setSoTimeout(0);
//
//		Socket connection = server.accept();
//		System.out.println("Connection accepted");
//		
//		System.out.println(connection.getLocalAddress().getHostName());
//		System.out.println(connection.getLocalAddress().getHostAddress());
//		System.out.println(connection.getLocalAddress().getCanonicalHostName());
//
//		BufferedReader fromClient = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//		DataOutputStream toClient = new DataOutputStream(connection.getOutputStream());
		
//		toClient.writeBytes(message);
//		System.out.println("Written message");
//		toClient.flush();
//		System.out.println("Flushed");
//		String response = fromClient.readLine();
		
//		System.out.println(response);
		
//		server.close();
//		input.close();
	}
	
	/**
	 * The top level function for the command module running the entire experiment.<br>
	 * <br>
	 * It opens connections to all of the experimental nodes and sends event messages<br>
	 * to each node when appropriate. All based off of the configuration file provided.<br>
	 * (ServerMappings.cfg).<br>
	 * <br>
	 * <strong>ServerMappings.cfg</strong><br>
	 * Contains a mapping of experimental node names to DETER pc names.
	 * Format is one node per line where each line has: EXPERIMENTAL_NAME DETER_NAME<br><br>
	 * There is a single space between the EXPERIMENTAL_NAME and DETER_NAME values<br>
	 * Example: ISP0 bpc207<br>
	 * Must be available in the working directory of the executing java file.<br>
	 * <br>
	 * <strong>Experiment.cfg</strong><br>
	 * Is a json formatted file containing the event details of every run of the experiment.
	 * The top level is a json array containing run objects.
	 * Each run object contains the following keys:<br>
	 * <ul>
	 * <li><em>experiment</em>: numeric identifier for the run</li>
	 * <li><em>description</em>: string containing english description of the run</li>
	 * <li><em>(timestamp)</em>: The key is a positive integer indicating a time in seconds
	 * 		<ul>
	 * 		<li>The value is an object containing event details</li>
	 * 		<li>The object has a set of keys containing EXPERIMENTAL_NAME's as keys</li>
	 * 		<li>And a string containing an event message as a value</li>
	 * 		<li>The key may also be 'all' if the message is to be sent to all nodes</li>
	 * 		<li>Must be available in the working directory of the executing java file.</li>
	 * 		</ul></li>
	 * </ul>
	 * 
	 * @throws Exception If something goes wrong, throw the exception to kill the experiment.
	 */
	public static void runExperiment() throws Exception {
		
		// Generate a mapping of EXPERIMENTAL_NAMEs to DETER_NAMEs from ServerMappings.cfg
		// The key is the EXPERIMENTAL_NAME and value is DETER_NAME
		HashMap<String, String> serverMap = new HashMap<>();
		Scanner smInput = new Scanner(new File("ServerMappings.cfg"));
		while(smInput.hasNextLine()) {
			String line = smInput.nextLine();
			String[] vals = line.split(" ");
			System.out.println(vals[0] + " " + vals[1]);
			if(vals.length >= 2)
				serverMap.put(vals[0].toLowerCase(), vals[1].toLowerCase());
		}
		smInput.close();
		
		// Open a server socket on port 52017
		ServerSocket server = new ServerSocket(52017);
		// Set the timeout to 0, which prevents reads from timing out.
		server.setSoTimeout(0);
		
		// Create a mapping of EXPERIMENTAL_NAMEs to client {@link Socket}
		HashMap<String, Socket> sockets = new HashMap<>();
		for(int i = 0; i < serverMap.size(); i++) {
			// Wait for experimental nodes to connect to the command server
			System.out.println("Accepting sockets");
			Socket client = server.accept();
			// This is how you retrieve the EXPERIMENTAL_NAME from the connection
			String clientName = client.getInetAddress().getHostName();
			
			// Clean the name up to reference it later
			clientName = clientName.substring(0, clientName.indexOf('.')).toLowerCase();
			System.out.println(clientName);
			sockets.put(clientName, client);
		}
		
		// Parse the Experiment.cfg file's JSON for each {@link ExperimentCommand} within each {@link ExperimentRun}
		System.out.println("Jsoning");
		JSONArray jsonExpArray = new JSONArray(new JSONTokener(new FileReader("Experiment.cfg")));
		for(int i = 0; i < jsonExpArray.length(); i++) {
			// Parse the details of each {@link ExperimentRun}
			JSONObject exp = jsonExpArray.getJSONObject(i);
			int expNum = exp.getInt("experiment");
			String description = exp.getString("description");
			ExperimentRun expRun = new ExperimentRun(expNum, description);
			runs.add(expRun);
			Iterator<String> times = exp.keys();
			while(times.hasNext()) {
				String time = times.next();
				// Skip the 'experiment' and 'description' keys (this is stupid)
				if(time.equals("experiment") || time.equals("description"))
					continue;
				
				// Parse each {@link ExperimentCommand} and add it to the appropriate {@link ExperimentRun}
				JSONObject commands = exp.getJSONObject(time);
				Iterator<String> servers = commands.keys();
				while(servers.hasNext()) {
					String serv = servers.next();
					String command = commands.getString(serv);
					expRun.addCommand(new ExperimentCommand(Long.parseLong(time), serv, command));
					
				}
			}
		}
		
		// If we're going to skip some runs, set this flag
		boolean skip = skipDup != -1;
		
		// Execute each run
		for(ExperimentRun run : runs) {
			// Stop skipping if we've reached the skip target
			if(skipDup != 0 && skipRun != run.id)
				skip = false;
			// Skip the run if we're still skipping
			if(skip)
				continue;
			
			// Set the run metadata to the original run set
			run.setMetadata("orig");
			// Run the current experiment run
			run.run(sockets, serverMap);
		}
		
		// This is safe as it is coming from clone...
		@SuppressWarnings("unchecked")
		// Create a clone of the experiment runs for us to randomize for the remaining runs
		ArrayList<ExperimentRun> randRuns = (ArrayList<ExperimentRun>)runs.clone();
		
		// Randomize this array with a static seed set to 2017 (so we can execute the same randomized set in the same order
		Random random = new Random(2017);
		// This is a comparator used to randomize the array of experiment runs (there is probably a better way of doing this
		Comparator<ExperimentRun> randCompare = (ExperimentRun r1, ExperimentRun r2) -> (2 * Math.round(random.nextFloat()) - 1);
		
		// Run the duplicates in random order
		for(int i = 1; i <= NUM_DUPLICATES; i++) {
			// Randomize the array
			randRuns.sort(randCompare);
			// Run the runs in the same way we ran the originals
			for(ExperimentRun run : randRuns) {
				System.out.println("Experiment run: " + i + "_" + run.id);
				if(skipDup != i && skipRun != run.id)
					skip = false;
				if(skip)
					continue;
				// Set the metadata to identify it as a duplicate and its id
				run.setMetadata("dup"+(i-1));
				run.run(sockets, serverMap);
			}
		}
		
		// Properly close the server
		server.close();
	}
}
