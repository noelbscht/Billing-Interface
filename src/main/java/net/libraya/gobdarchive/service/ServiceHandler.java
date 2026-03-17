package net.libraya.gobdarchive.service;

import java.io.IOException;
import java.util.ArrayList;

import fi.iki.elonen.NanoHTTPD;
import net.libraya.gobdarchive.utils.Unicodes;
import net.libraya.gobdarchive.utils.exception.ServiceException;

public class ServiceHandler {
	
	private final ArrayList<Service> services;
	
	// tui
	private int selectedLogIndex = -1;
	private int lastLogIndex = 0;
	
	public ServiceHandler() {
		this.services = new ArrayList<Service>();
	}
	
	public void register(Service s) {
		this.services.add(s);
	}
	
	public Service getServiceByTitle(String title) {
		for (Service s : services) {
			if (s.getTitle().equalsIgnoreCase(title)) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * initialize all registered web services and run keep alive loop.
	 * */
	public void initialize() throws IOException, ServiceException {
		if (services.size() == 0) {
			System.err.println("No webservice selected.");
			return;
		}
		
		System.out.println("Running web services...\n");
		for (Service ws : services) {
			ws.initialize();
		}
		
		// shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    	    System.out.println("\nShutting down webservices...");
    	    try {
    	        for (Service ws : services) {
    	        	ws.stop();
    	        	System.out.println("	" + ws.getTitle() + " stopped.");
    	        }
    	        
    	        // reset input mode
    	        Runtime.getRuntime().exec(new String[] {"sh", "-c", "stty sane < /dev/tty"}).waitFor();
    	    } catch (Exception e) {
    	        System.err.println("Error during shutdown: " + e.getMessage());
    	    }
    	}));
		
		// handle tui input
		new Thread(this::handleInput).start();

		
		// run service loop
		while (allServicesAlive()) {

            // remove dead services safely
            services.removeIf(s -> !s.isAlive());

            // fix index if needed
            if (selectedLogIndex >= services.size()) {
                selectedLogIndex = Math.max(0, services.size() - 1);
            }

            if (services.isEmpty()) break;
            
            renderLog();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }

	}
	
	private void renderStatusOverview() {
	    System.out.println(Unicodes.BULLET + " Service Overview:\n");

	    for (Service s : services) {
	        String status = s.isAlive() ? "LISTENING" : "STOPPED";
	        String color = s.isAlive() ? "\033[32m" : "\033[31m"; // green/red

	        System.out.println(color + status + "  -  " + "\033[0m" + "127.0.0.1:" + s.getListeningPort() + "  -  " + s.getTitle());
	    }

	    System.out.println("\nUse A/D to switch logs or overview.");
	}
	
	private void renderLog() {
		if (selectedLogIndex == -1) {
			if (lastLogIndex != -1) {
				clearScreen();
				renderStatusOverview();
			}
			lastLogIndex = -1;
	        return;
	    }

		
		Service s = services.get(selectedLogIndex);

        boolean indexChanged = selectedLogIndex != lastLogIndex;
        boolean logChanged = s.hasChanged();

        if (indexChanged || logChanged) {
            clearScreen();
            s.renderLog();
            s.markRendered();
            lastLogIndex = selectedLogIndex;
        }
	}
	
	private void clearScreen() {
		System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }
	
	private void handleInput() {
	    try {
	    	// set input mode to directly 
	    	Runtime.getRuntime().exec(new String[]{"sh", "-c", "stty cbreak -echo < /dev/tty"}).waitFor();

	        while (allServicesAlive()) {
	            if (System.in.available() == 0) {
	                Thread.sleep(10);
	                continue;
	            }

	            int ch = System.in.read();

	            if (ch == 'd' || ch == 'D') {
	                selectedLogIndex++;
	                if (selectedLogIndex >= services.size()) {
	                    selectedLogIndex = -1;
	                }
	            }

	            if (ch == 'a' || ch == 'A') {
	                selectedLogIndex--;
	                if (selectedLogIndex < -1) {
	                    selectedLogIndex = services.size() - 1;
	                }
	            }

	        }

	    } catch (Exception ex) {}
	}
	
   
   /**
    * returns if all web services alive.
    * */
	private boolean allServicesAlive() {
   	for (NanoHTTPD ws : services) {
   		if (!ws.isAlive()) 
   			return false;
   	}
   	return true;
   }
	
	public ArrayList<Service> getServices() {
		return services;
	}
}
