package net.libraya.gobdarchive.service.web;

import java.util.ArrayList;
import java.util.List;

import net.libraya.gobdarchive.service.web.routes.WRAPIDoc;
import net.libraya.gobdarchive.service.web.routes.WRDashboard;
import net.libraya.gobdarchive.service.web.routes.WRIndex;
import net.libraya.gobdarchive.service.web.routes.WRLogin;
import net.libraya.gobdarchive.service.web.routes.WRLogout;
import net.libraya.gobdarchive.service.web.routes.WRNotAuthorized;
import net.libraya.gobdarchive.service.web.routes.WRNotFound;

public class WebRouter {

    private final List<WebRoute> routes;
    
    private final WebServer ws;
    
    public WebRouter(WebServer ws) {
    	this.routes = new ArrayList<>();
    	this.ws = ws;
    	
    	register(new WRNotFound(ws));
    	register(new WRNotAuthorized(ws));
    	register(new WRIndex(ws));
    	register(new WRAPIDoc(ws));
    	register(new WRLogin(ws));
    	register(new WRLogout(ws));
    	register(new WRDashboard(ws));
    }
    
    public void register(WebRoute route) {
        routes.add(route);
    }

    public WebRoute match(String rule) {
        for (WebRoute r : routes) {
            if (r.getRule().equalsIgnoreCase(rule) || 
            		(r.getRule().startsWith("/") && r.getRule().substring(1).equalsIgnoreCase(rule))) {
                return r;
            }
        }
        return null;
    }
    
    public List<WebRoute> getRoutes() {
		return routes;
	}
    
    public WebServer getWebServer() {
		return ws;
	}
}
	
