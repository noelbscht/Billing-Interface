package net.libraya.gobdarchive.service.web.routes;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.service.api.API;
import net.libraya.gobdarchive.service.web.WebRoute;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.WebPermission;
import net.libraya.gobdarchive.service.web.templating.SimpleTemplating;
import net.libraya.gobdarchive.utils.Environment;

public class WRAPIDoc extends WebRoute {

	public WRAPIDoc(WebServer ws) {
	        super(ws, "/api_documentation", new String[] { "GET" },  new WebPermission[] { WebPermission.VIEW_API_DOCS });
	    }


	@Override
    public Response onRequest(IHTTPSession session, String body, SessionHelper sessionHelper) throws Exception {
		SimpleTemplating t = new SimpleTemplating(ws, sessionHelper);
		
		t.addVariable("base_url", Environment.BASE_URL);
		t.addVariable("api_active", Main.getServicehandler().getServiceByTitle("API").isAlive());
		
		// add route list
		t.addVariable("routes", ((API)Main.getServicehandler().getServiceByTitle("API")).getRouter().getRoutes());
		// todo:: add recursive template iteration t.addPartial(ws, "routelist", "presets/api_routes.html");
		
		//todo:: stable version of api_doc.html
		return ws.serveTemplate("api_doc.html", session, t);
    }
}
