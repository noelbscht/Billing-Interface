package net.libraya.gobdarchive.service.web.routes;

import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.service.web.WebRoute;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.templating.SimpleTemplating;

public class WRIndex extends WebRoute {

	public WRIndex(WebServer ws) {
	        super(ws, "/", new String[] { "GET" } , null);
	    }


	@Override
    public Response onRequest(IHTTPSession session, String body, HashMap<String, String> files, SessionHelper sessionHelper) throws Exception {
		SimpleTemplating t = new SimpleTemplating(ws, sessionHelper);
		
		return ws.serveTemplate("index.html", session, t);
    }

}
