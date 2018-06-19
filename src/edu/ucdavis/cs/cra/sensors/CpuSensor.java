package edu.ucdavis.cs.cra.sensors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import edu.ucdavis.cs.cra.utils.Sys;

/**
 * Records cpu utilization data every second.
 * 
 * @author Mac Crompton
 * @see <a href=http://www.linuxhowtos.org/System/procstat.htm>/proc/stat</a>
 * @see <a href=https://stackoverflow.com/questions/23367857/accurate-calculation-of-cpu-usage-given-in-percentage-in-linux#23376195>/proc/stat - Discussion</a>
 *
 */
public class CpuSensor extends Sensor {
	
	public CpuSensor(long startTime, String metadata, int id, String hostname) {
		super(startTime, metadata, id, hostname);
	}

	public void run() {
		// Open up the cpu sensor file
		String resultsDir = "results/dataset_" + metadata + "/run_" + id + "/host_" + hostname + "/";
		try {
			FileWriter cpu = new FileWriter(Sys.createFile(resultsDir, "cpu"));

			// Keep track of the previous cpu values to determine utilization
			long PrevTotal = -1;
			long PrevIdle = -1;

			while(!stop) {
				// Open up the /proc/stat file which contains statistics on CPU utilization
				String[] params = null;
				Scanner input = new Scanner(new File("/proc/stat"));
				while(input.hasNextLine()) {
					String line = input.nextLine();
					if(line.contains("cpu")) {
						params = line.split(" +");
						break;
					}
				}
				input.close();

				if(params == null)
					continue;

				// Extract each of the cpu utilization parameters
				long user = Long.parseLong(params[1]);
				long nice = Long.parseLong(params[2]);
				long system = Long.parseLong(params[3]);
				long idle = Long.parseLong(params[4]);
				long iowait = Long.parseLong(params[5]);
				long irq = Long.parseLong(params[6]);
				long softirq = Long.parseLong(params[7]);
				long steal = Long.parseLong(params[8]);

				// Sum up the amount of time the CPU was idle
				long Idle = idle + iowait;
				// Sum up the amount of time the CPU was busy
				long NonIdle = user + nice + system + irq + softirq + steal;

				// Sum up the total time
				long Total = Idle + NonIdle;

				if(PrevTotal != -1) {
					// Find the difference between the current and last values
					long totalD = Total - PrevTotal;
					long idleD = Idle - PrevIdle;

					if(totalD != 0) {
						// Calculate the percentage of non-idle time to total time spent using the cpu
						double cpuP = (double)(totalD - idleD) / (double)totalD;
						long time = System.currentTimeMillis() - startTime;

//						System.out.println("CPU: " + cpuP);
						
						cpu.write(time + " " + cpuP + "\n");
						cpu.flush();
					}
				}

				PrevIdle = Idle;
				PrevTotal = Total;

				// Record only once every second
				Thread.sleep(1000);
			}
			cpu.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}
}
