package net.libraya.gobdarchive;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.ArrayList;

import net.libraya.gobdarchive.archive.ArchiveManager;
import net.libraya.gobdarchive.cli.CLI;
import net.libraya.gobdarchive.cli.Command;
import net.libraya.gobdarchive.service.ServiceHandler;
import net.libraya.gobdarchive.utils.Unicodes;
import net.libraya.gobdarchive.utils.config.Configurations;
import net.libraya.gobdarchive.utils.exception.ConfigurationException;

public class Main {
	
	private static final CLI commandInterface = new CLI();
	private static final ArchiveManager archiveManager = new ArchiveManager();
	private static final ServiceHandler serviceHandler = new ServiceHandler();
	private static Configurations configurations = new Configurations();
	
	public static void main(String[] args) {
		try {
			configurations.initialize();
		} catch (ConfigurationException e) {
			System.err.println(e.getMessage());
			return;
		}
		
		// interrupt if not compatible, bypass option for testing purposes.
		if (!isOSCompatible()) {
			if (args.length != 0 && !args[args.length -1].equals("--bypass")) { // else skip (bypass compatibility check)
				sendFeedback(new String[] {
						"Operating system is not fully compatible."
				});
				return;
			}
		}
		
		// ensure that log file is protected
		archiveManager.ensureLogFileProtected();
		
		if (args.length == 0) {
			sendCommands();
			return;
		}
		
		if (commandInterface.isCommand(args)) {
			if (!commandInterface.execute(args)) {
				Command cmd = commandInterface.getCommand(args);
				sendFeedback(new String[] {
						"Command: ",
						"    " + cmd.getName(),
						"Parameter: ",
						"    " + cmd.getUsageParameters(),
						"Description: ",
						"    " + cmd.getDescription()
				});
			}
			return;
		} 
		
		// send commands on invalid input
		sendCommands();
	}
	
	/**
	 * check os compatibility 
	 * */
	private static boolean isOSCompatible() {
	    String os = System.getProperty("os.name").toLowerCase();
	    
	    boolean isLinux = os.contains("linux");
	    boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
	    boolean hasShell = new File("/bin/sh").exists();

	    return isLinux && isPosix && hasShell;
	}

	
	private static void sendCommands() {
		ArrayList<String> feedback = new ArrayList<>();
		
		feedback.add(Unicodes.VERTICAL_BAR + " Command list");
		for (Command cmd : commandInterface.getCommands())  {
			feedback.add("    " + cmd.getName() + " " + Unicodes.EM_DASH + " " + cmd.getDescription());
		}
		
		sendFeedback(feedback.toArray(new String[0]));
	}
	
	public static void sendFeedback(String[] information) {
		for (int i = 0; i < 50; i++) {
			System.out.print(Unicodes.EM_DASH);
		}
		System.out.print("\n");
		System.out.println(Unicodes.BULLET + " Libraya.net " +  Unicodes.VERTICAL_BAR + " Billing Interface " + Unicodes.COPYRIGHT);
		System.out.println();
		
		
		for (String info : information) {
			System.out.println(info);
		}
		System.out.println();
		
		for (int i = 0; i < 50; i++) {
			System.out.print(Unicodes.EM_DASH);
		}
		System.out.print("\n");
	}
	
	public static CLI getCommandInterface() {
		return commandInterface;
	}
	
	public static ArchiveManager getArchiveManager() {
		return archiveManager;
	}
	
	public static ServiceHandler getServicehandler() {
		return serviceHandler;
	}
	
	public static Configurations getConfigurations() {
		return configurations;
	}
}