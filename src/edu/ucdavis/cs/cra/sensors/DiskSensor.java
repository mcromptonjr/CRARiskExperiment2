package edu.ucdavis.cs.cra.sensors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import edu.ucdavis.cs.cra.utils.Sys;

/**
 * Calculates the disk utilization and records it every second.
 * 
 * @author Mac Crompton
 * @see <a href=https://www.kernel.org/doc/Documentation/ABI/testing/procfs-diskstats>/proc/diskstats</a>
 * @see <a href=https://www.linuxquestions.org/questions/suse-opensuse-60/interpreting-proc-diskstats-360350/>/proc/diskstats - Discussion</a>
 *
 */
public class DiskSensor extends Sensor {
	
	public DiskSensor(long startTime, String metadata, int id, String hostname) {
		super(startTime, metadata, id, hostname);
	}

	public void run() {
		// Open up the file for writing sensor data
		String resultsDir = "results/dataset_" + metadata + "/run_" + id + "/host_" + hostname;
		try {
			FileWriter disk = new FileWriter(Sys.createFile(resultsDir, "disk"));
			
			// Keep track of the last IO timestamps
			long pastIOTime = -1;
			long lastTime = -1;
			
//			long totalIOTime = 0;
			
			while(!stop) {
				// Open the /proc/diskstats file which contains statistics on disk utilization
				Scanner input = new Scanner(new File("/proc/diskstats"));
				while(input.hasNextLine()) {
					String line = input.nextLine();
					if(line.contains("sda")) {
						String[] params = line.split(" +");
						if(params.length < 12)
							break;
						// Get the disk IO utilization time
						long ioTime = Long.parseLong(params[13]); 
//						long ioTime = Long.parseLong(params[14]);
						long time = System.currentTimeMillis() - startTime;
						if(pastIOTime != -1) {
							// Calculate the amount of disk time utilized since the last time as a percentage of the total time
							double diskP = (double)(ioTime - pastIOTime) / (double)(time - lastTime);
							disk.write(time + " " + diskP + "\n");
							disk.flush();
//							totalIOTime += (ioTime - pastIOTime);
//							System.out.println(diskP + " " + (ioTime - pastIOTime) + " " + (time-lastTime) + " " + (totalIOTime / (double)time));
						}
						pastIOTime = ioTime;
						lastTime = time;
						
						break;
					}
				}
				input.close();
				
				// Record a value once every second
				Thread.sleep(1000);
			}
			
			disk.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
