package net.libraya.gobdarchive.service.web.routes;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.service.web.WebRoute;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.templating.SimpleTemplating;

public class WRNotAuthorized extends WebRoute {

	public WRNotAuthorized(WebServer ws) {
	        super(ws, "/not_authorized", new String[] { "GET" } , null);
    }


	@SuppressWarnings("deprecation")
	@Override
    public Response onRequest(IHTTPSession session, String body, SessionHelper sessionHelper) throws Exception {
		SimpleTemplating t = new SimpleTemplating(ws, sessionHelper);
		
		String requestedPath = session.getParms().get("req");
		
		t.addVariable("path", requestedPath != null ? requestedPath : "...");
		
		return ws.serveTemplate("not_authorized.html", session, t);
    }

}
