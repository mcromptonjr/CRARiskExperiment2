package edu.ucdavis.cs.cra.utils;

public class ExperimentConfigGenerator {
	
	public static void main(String[] args) {
		String[] metrics = { "integrity" };
		String[] utilizations = { "25", "50", "100" };
		String[] scenarios = { 	"120 auth0", "120 auth1", "120 auth2", "120 auth3", 
								"120 auth0,150 auth1", "120 auth2,150 auth3",
								"120 auth0,150 auth1,180 auth2", "120 auth1,150 auth2,180 auth3",
								"120 auth0,150 auth1,180 auth2,210 auth3" };
		
		int experiment = 1;
		String endl = "\n";
		StringBuilder config = new StringBuilder();
		
		// For each of our scenarios
		for(String scenario : scenarios) {
			// Grab each of the events in the scenario (separated by a comma)
			String[] events = scenario.split(",");
			// Parse out the time and machine for the event
			String[] tam = new String[events.length*2];
			for(int i = 0; i < events.length; i++) {
				String[] mt = events[i].split(" ");
				if(mt.length >=2) {
					tam[2*i] = mt[0];
					tam[2*i+1] = mt[1];
				}
			}
			
			// For each of the metrics we're affecting
			for(String metric : metrics) {
				// For each of the utilization levels
				for(String utilization : utilizations) {
					config.append("{").append(endl);
					
					// Identify the experiment ID
					config.append("\t\"experiment\" : " + experiment + ",").append(endl);
					
					// Give a short description of the experiment
					config.append("\t\"description\" : \"" + events.length + " infected node(s), ");
					for(int i = 0; i < events.length; i++) {
						config.append(tam[2*i+1]).append(", ");
					}
					config.append(metric).append(" ").append(utilization).append("%\",");
					config.append(endl);
					
					// Generate the start event
					config.append("\t0 : {").append(endl).append("\t\t\"all\" : \"start\"").append(endl).append("\t},").append(endl);
					// Generate each of the infection events
					for(int i = 0; i < events.length; i++) {
						config.append("\t").append(tam[2*i]).append(" : {").append(endl);
						config.append("\t\t\"").append("attacker").append("\" : \"");
						config.append("metric ").append(tam[2*i+1]).append(" ").append(utilization).append("\"").append(endl);
						config.append("\t},").append(endl);
					}
					// Generate the stop event
					config.append("\t300 : {").append(endl).append("\t\t\"all\" : \"stop\"").append(endl).append("\t},").append(endl);
					
					config.append("},").append(endl);
					
					experiment++;
				}
			}
		}
		
		System.out.println(config);
	}
}
