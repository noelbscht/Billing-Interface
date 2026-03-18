package net.libraya.gobdarchive.service.api;

import java.util.ArrayList;
import java.util.List;

import net.libraya.gobdarchive.archive.ArchiveManager;

public class APIRouter {

    private final List<APIRoute> routes;
    
    private final ArchiveManager manager;
    
    public APIRouter(ArchiveManager manager) {
    	this.routes = new ArrayList<>();
    	this.manager = manager;
    	
    }
    
    public void register(APIRoute route) {
        routes.add(route);
    }

    public APIRoute match(String rule) {
        for (APIRoute r : routes) {
            if (r.getRule().equals(rule)) {
                return r;
            }
        }
        return null;
    }
    
    public List<APIRoute> getRoutes() {
		return routes;
	}
    
    public ArchiveManager getArchiveManager() {
		return manager;
	}
}
	
