package net.libraya.gobdarchive.cli.commands;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.cli.Command;
import net.libraya.gobdarchive.service.api.API;

public class CommandWeb extends Command {

    public CommandWeb() {
        super(
            new String[]{"service", "webservice", "wbs", "ws", "web"},
            "an command to handle web services.",
            """
            <mode> --serve, --routes
            
            modes:
            	api:  run application interface only
            """
        );
    }

    @Override
    public boolean execute(String[] args) {
    	if (args.length < 3) {
            System.out.println("Missing parameter.");
            return false;
        }
    	
        String mode = args[1];
        String parameter = args[2];
        
        if (!parameter.equalsIgnoreCase("--serve") && !parameter.equalsIgnoreCase("--routes")) {
            System.out.println("Invalid parameter. Use --serve or --routes.");
            return false;
        }
        
        
        switch (mode) {
        	case "api":// application interface
        		API api = new API();
        		
        		if (parameter.equalsIgnoreCase("--serve")) {
                	try {
                		Main.getServicehandler().register(api);
                		Main.getServicehandler().initialize();
                	} catch (Exception e) {
                        System.err.println("Failed to serve web service: " + e.getMessage());
                        return false;
                    }
                } else if (parameter.equalsIgnoreCase("--routes")) {
                	api.sendRoutes();
            		return true;
                }
        		break;
    		default:
    			System.out.println("Invalid web service selected.");
    			break;
        }
        return false;  
    }
}

