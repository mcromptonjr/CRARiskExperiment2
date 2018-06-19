package edu.ucdavis.cs.cra.utils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;


public class ExperimentCSVConverter {
	
	public static class DataPoint {
		public double cpu = -1;
		public double disk = -1;
		public long[] network_pdata = new long[10];
		public long network_pdrop = -1;
		public long network_precv = -1;
		public long network_psend = -1;
		public double ram = -1;
		public long success_sfail = -1;
		public double success_sperc = -1;
		public long success_ssucc = -1;
		public double success_stime = -1;
		public double byzantine_bcons = -1;
		public double byzantine_bdist = -1;
		public double byzantine_btime = -1;
		
		String host;
		int time;
		
		public DataPoint(String host, int time) {
			this.host = host;
			this.time = time;
			for(int i = 0; i < network_pdata.length; i++) {
				network_pdata[i] = -1;
			}
		}
		
		public void setMetric(String key, String value) {
			switch(key) {
			case "cpu":
				cpu = Double.parseDouble(value);
				break;
			case "disk":
				disk = Double.parseDouble(value);
				break;
			case "network_pdata":
				String[] params = value.split(" ");
				int index = Integer.parseInt(params[1]);
				network_pdata[index] = Long.parseLong(params[0]);
				break;
			case "network_pdrop":
				network_pdrop = Long.parseLong(value);
				break;
			case "network_precv":
				network_precv = Long.parseLong(value);
				break;
			case "network_psend":
				network_psend = Long.parseLong(value);
				break;
			case "ram":
				ram = Double.parseDouble(value);
				break;
			case "success_sfail":
				success_sfail = Long.parseLong(value);
				break;
			case "success_sperc":
				success_sperc = Double.parseDouble(value);
				break;
			case "success_ssucc":
				success_ssucc = Long.parseLong(value);
				break;
			case "success_stime":
				success_stime = Double.parseDouble(value);
				break;
			case "byzantine_bcons":
				byzantine_bcons = Double.parseDouble(value);
				break;
			case "byzantine_bdist":
				byzantine_bdist = Double.parseDouble(value);
				break;
			case "byzantine_btime":
				byzantine_btime = Double.parseDouble(value);
				break;
			}
		}
		
		public String toString() {
			String ret = "";
			ret += (cpu==-1?"":cpu) + ",";
			ret += (disk==-1?"":disk) + ",";
			for(int i = 0; i < network_pdata.length; i++) {
				ret += (network_pdata[i]==-1?"":network_pdata[i]) + ",";
			}
			ret += (network_pdrop==-1?"":network_pdrop) + ",";
			ret += (network_precv==-1?"":network_precv) + ",";
			ret += (network_psend==-1?"":network_psend) + ",";
			ret += (ram==-1?"":ram) + ",";
			ret += (success_sfail==-1?"":success_sfail) + ",";
			ret += (success_sperc==-1?"":success_sperc) + ",";
			ret += (success_ssucc==-1?"":success_ssucc) + ",";
			ret += (success_stime==-1?"":success_stime) + ",";
			ret += (byzantine_bcons==-1?"":byzantine_bcons) + ",";
			ret += (byzantine_bdist==-1?"":byzantine_bdist) + ",";
			ret += (byzantine_btime==-1?"":byzantine_btime);
			
			return ret;
		}
	}

	public static void main(String[] args) throws IOException {
		File top = new File("results/");
		File dir = new File("csv_results/");
		boolean printHostOrder = true;
		dir.mkdirs();
		File f = new File(dir, "CombinedResults.csv");
		if(!f.exists())
			f.createNewFile();
		FileWriter fw = new FileWriter(f);
		for(File dataset : top.listFiles()) {
			for(File run : dataset.listFiles()) {
				HashMap<String, DataPoint[]> data = new HashMap<String, DataPoint[]>();
				for(File host : run.listFiles()) {
					String hostname = host.getName();
					DataPoint[] dps = new DataPoint[400];
					data.put(hostname, dps);
					for(File metric : host.listFiles()) {
						Scanner input = new Scanner(metric);
						while(input.hasNextLine()) {
							String line = input.nextLine();
							String[] params = line.split(" ");
							int time = (int) (Long.parseLong(params[0]) / 1000);
							if(dps[time] == null) {
								dps[time] = new DataPoint(hostname, time);
							}
							String value = params[1];
							if(params.length >= 3) {
								value += " " + params[2].substring(3);
							}
							dps[time].setMetric(metric.getName(), value);
						}
						input.close();
					}
				}
				
				int time = 0;
				while(time <= 299) {
					DataPoint[] dps = new DataPoint[data.keySet().size()];
					int index = 0;
					for(String host : data.keySet()) {
						if(printHostOrder) {
							System.out.println(index + " " + host + " " + data.keySet().size());
						}
						DataPoint dp = data.get(host)[time];
						if(dp == null) {
							dp = new DataPoint(host, time);
						}
						dps[index++] = dp;
					}
					printHostOrder = false;
					
					fw.append(dataset.getName()+"__"+run.getName()+","+time+",");
					int i = 0;
					for(DataPoint dp : dps) {
						fw.append(dp.toString());
						if(i+1 < dps.length) {
							fw.append(",");
						}
						i++;
					}
					fw.append("\n");
					time++;
				}
			}
		}
		fw.close();
	}
}
