package net.libraya.gobdarchive.cli.commands;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.cli.Command;
import net.libraya.gobdarchive.service.api.API;
import net.libraya.gobdarchive.service.web.WebServer;

public class CommandWeb extends Command {

    public CommandWeb() {
        super(
            new String[]{"service", "webservice", "wbs", "ws", "web"},
            "an command to handle web services.",
            """
            <mode> --serve, --routes
            
            modes:
            	full: run http + api (if enabled in .env)
            	http: run web interface only
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
        	case "api": {// application interface
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
        	}
        	case "http": {// web interface
        		WebServer http = new WebServer();
        		
        		if (parameter.equalsIgnoreCase("--serve")) {
        			try {
        				Main.getServicehandler().register(http);
                		Main.getServicehandler().initialize();
                	} catch (Exception e) {
                        System.err.println("Failed to serve web service: " + e.getMessage());
                        return false;
                    }
        		} else if (parameter.equalsIgnoreCase("--routes")) {
        			http.sendRoutes();
        			return true;
        		}
        		break;
        	}
        	case "full": {// api and web interface
        		API api = new API();
                WebServer http = new WebServer();

        		if (parameter.equalsIgnoreCase("--serve")) {
        			try {
        				Main.getServicehandler().register(api);
        				Main.getServicehandler().register(http);

        				Main.getServicehandler().initialize();
                        return true;

                    } catch (Exception e) {
                        System.err.println("Failed to start full web service: " + e.getMessage());
                        if (e.getMessage().startsWith("Address already in use")) {
                        	return true;
                        }
                        return false;
                    }

    			}
        		break;
        	}
    		default: {
    			System.out.println("Invalid web service selected.");
    			break;
    		}
        }
        return false;  
    }
}

