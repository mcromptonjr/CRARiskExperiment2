package edu.ucdavis.cs.cra.sensors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import edu.ucdavis.cs.cra.utils.Sys;

/**
 * Record the RAM utilization as a percentage every second.
 * 
 * @author Mac Crompton
 * @see <a href=https://www.centos.org/docs/5/html/5.1/Deployment_Guide/s2-proc-meminfo.html>/proc/meminfo</a>
 *
 */
public class RamSensor extends Sensor {
	
	public RamSensor(long startTime, String metadata, int id, String hostname) {
		super(startTime, metadata, id, hostname);
	}

	public void run() {
		// Open up the file to record sensor data
		String resultsDir = "results/dataset_" + metadata + "/run_" + id + "/host_" + hostname;

		try {
			FileWriter ram = new FileWriter(Sys.createFile(resultsDir, "ram"));

			while(!stop) {
				// Keep track of the total and free memory
				long memTotal = 0;
				long memFree = 0;

				// Open up the /proc/meminfo file which contains memory statistic information
				Scanner input = new Scanner(new File("/proc/meminfo"));
				while(input.hasNextLine()) {
					String line = input.nextLine();
					if(line.contains("MemTotal")) {
						memTotal = Long.parseLong(line.split(" +")[1]);
					} else if(line.contains("MemFree")) {
						memFree = Long.parseLong(line.split(" +")[1]);
					}
				}
				input.close();
				
				// Calculate the amount of utilized memory as a percentage of the total
				double ramP = (double)(memTotal - memFree) / (double) memTotal;
				long time = System.currentTimeMillis() - startTime;
				
				ram.write(time + " " + ramP + "\n");
				ram.flush();
				
				// Record once every second
				Thread.sleep(1000);
			}
			
			ram.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
