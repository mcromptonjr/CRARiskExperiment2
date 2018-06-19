package edu.ucdavis.cs.cra.sensors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import edu.ucdavis.cs.cra.utils.Sys;

/**
 * Records statistics related to data sent over this node and its interfaces.
 * 
 * @author Mac Crompton
 * @see <a href=https://stackoverflow.com/questions/3521678/what-are-meanings-of-fields-in-proc-net-dev>/proc/net/dev</a>
 *
 */
public class NetworkSensor extends Sensor {
	
	public NetworkSensor(long startTime, String metadata, int id, String hostname) {
		super(startTime, metadata, id, hostname);
	}

	public void run() {
		// Open up files for packets received, sent, dropped, and data traversed over specific interfaces.
		String resultsDir = "results/dataset_" + metadata + "/run_" + id + "/host_" + hostname;
		String file = "/network_";
		try {
			FileWriter precv = new FileWriter(Sys.createFile(resultsDir, file+"precv"));
			FileWriter psend = new FileWriter(Sys.createFile(resultsDir, file+"psend"));
			FileWriter pdrop = new FileWriter(Sys.createFile(resultsDir, file+"pdrop"));
			FileWriter pdata = new FileWriter(Sys.createFile(resultsDir, file+"pdata"));

			// Keep track of individual sensor data by interface
			HashMap<String, Long> lastByteRec = new HashMap<String, Long>();
			HashMap<String, Long> lastPackRec = new HashMap<String, Long>();
			HashMap<String, Long> lastDropRec = new HashMap<String, Long>();

			HashMap<String, Long> lastByteTra = new HashMap<String, Long>();
			HashMap<String, Long> lastPackTra = new HashMap<String, Long>();
			HashMap<String, Long> lastDropTra = new HashMap<String, Long>();

			while(!stop) {
				HashMap<String, Long> byteRec = new HashMap<String, Long>();
				HashMap<String, Long> packRec = new HashMap<String, Long>();
				HashMap<String, Long> dropRec = new HashMap<String, Long>();

				HashMap<String, Long> byteTra = new HashMap<String, Long>();
				HashMap<String, Long> packTra = new HashMap<String, Long>();
				HashMap<String, Long> dropTra = new HashMap<String, Long>();
				// Open up /proc/net/dev which contains network statistics
				Scanner input = new Scanner(new File("/proc/net/dev"));
				while(input.hasNextLine()) {
					String line = input.nextLine();
					if(line.contains("eth")) {
						String[] params = line.split(" +");
						String interf = params[1].replace(":", "");

						// Data received over an interface
						byteRec.put(interf, Long.parseLong(params[2]));
						packRec.put(interf, Long.parseLong(params[3]));
						dropRec.put(interf, Long.parseLong(params[5]));

						// Data transmitted over an interface
						byteTra.put(interf, Long.parseLong(params[10]));
						packTra.put(interf, Long.parseLong(params[11]));
						dropTra.put(interf, Long.parseLong(params[13]));
					}
				}
				input.close();

				if(!lastByteRec.isEmpty()) {
					long packRecTot = 0;
					long packTraTot = 0;
					long packDroTot = 0;

					long time = (System.currentTimeMillis()-startTime);

					for(String interf : lastByteRec.keySet()) {
						// Sum up all of the totals
						packRecTot += packRec.get(interf)-lastPackRec.get(interf);
						packTraTot += packTra.get(interf)-lastPackTra.get(interf);
						packDroTot += (dropRec.get(interf)-lastDropRec.get(interf)) + (dropTra.get(interf)-lastDropTra.get(interf));

						// Keep track of the amount of data sent over an interface
						long byteInterf = (byteRec.get(interf)-lastByteRec.get(interf)+(byteTra.get(interf)-lastByteTra.get(interf)));
						// Write it out
						pdata.write((time + " " + byteInterf + " " + interf) + "\n");
						pdata.flush();
					}

					// Write out all of the totals
					precv.write((time + " " + packRecTot) + "\n");
					precv.flush();
					
					psend.write((time + " " + packTraTot) + "\n");
					psend.flush();
					
					pdrop.write((time + " " + packDroTot) + "\n");
					pdrop.flush();
				}

				lastByteRec = byteRec;
				lastPackRec = packRec;
				lastDropRec = dropRec;

				lastByteTra = byteTra;
				lastPackTra = packTra;
				lastDropTra = dropTra;

				// Only record once every second
				Thread.sleep(1000);
			}
			precv.close();
			psend.close();
			pdrop.close();
			pdata.close();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}
}
