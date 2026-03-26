package net.libraya.gobdarchive.service.web.routes;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.service.web.WebRoute;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.SessionHelper.MessageCategory;

public class WRLogout extends WebRoute {

	public WRLogout(WebServer ws) {
	        super(ws, "/logout", new String[] { "GET" } , null);
	    }

    public Response onRequest(IHTTPSession session, String body, SessionHelper sessionHelper) throws Exception {
    	sessionHelper.logout();
    	sessionHelper.addMessage(MessageCategory.INFO, "Du wurdest ausgeloggt.");
		
		// redirect to index
		return ws.redirect("/", session);
    }
}
