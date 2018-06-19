package edu.ucdavis.cs.cra.utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * A set of utility functions for easily running experiment features.
 * 
 * @author Mac Crompton
 *
 */
public class Sys {
	
	/**
	 * Executes a simple shell command and returns the output of the command.
	 * 
	 * @param command The shell command to run
	 * @return A string which contains the entire output of the command. Each line separated by a newline character.
	 * @throws IOException Thrown if there is an error executing the command or the process's input stream.
	 * @throws InterruptedException Thrown if there is an error executing the command.
	 */
	public static String executeCommand(String command) throws IOException, InterruptedException {
		Runtime r = Runtime.getRuntime();
		String[] cmd = { "/bin/sh", "-c", command };
		Process p = r.exec(cmd);
		p.waitFor();
		BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = "";
		StringBuilder sb = new StringBuilder();

		while ((line = b.readLine()) != null) {
		  sb.append(line).append("\n");
		}

		b.close();
		
		return sb.toString();
	}
	
	/**
	 * Guarantees the creation of a file in a specified directory.
	 * 
	 * @param dir The relative or absolute path to a directory for which a file should be created.
	 * @param filename The name of the file to be created.
	 * @return A handle to the file that was created.
	 * @throws IOException Thrown if there was an error in file creation.
	 */
	public static File createFile(String dir, String filename) throws IOException {
		File df = new File(dir);
		df.mkdirs();
		File file = new File(dir, filename);
		return file;
	}
}
