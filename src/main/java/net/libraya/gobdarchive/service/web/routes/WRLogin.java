package net.libraya.gobdarchive.service.web.routes;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.service.web.WebRoute;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.SessionHelper.MessageCategory;
import net.libraya.gobdarchive.service.web.templating.SimpleTemplating;

public class WRLogin extends WebRoute {

	public WRLogin(WebServer ws) {
        super(ws, "/login", new String[]{ "GET", "POST" } , null);
    }


	@Override
	@SuppressWarnings("deprecation")
    public Response onRequest(IHTTPSession session, String body, SessionHelper sessionHelper) throws Exception {
		SimpleTemplating t = new SimpleTemplating(ws, sessionHelper);
		
		String requestedPath = session.getParms().containsKey("req") ? session.getParms().get("req") : null;
		
		if (sessionHelper.isLoggedIn()) {
			if (requestedPath != null) {
				return ws.redirect(requestedPath, session); // send to requested path
			}
			return ws.redirect("/dashboard", session);
		}
		
		// login performed
		if (session.getMethod() == Method.POST) {
			String email = session.getParms().get("email");
			String password = session.getParms().get("password");
			
			if (email != null && password != null) {
				// if authentication was successful
				if (sessionHelper.login(email, password)) {
					// if req-parameter is given
					if (requestedPath != null) {
						return ws.redirect(requestedPath, session); // send to requested path
					}
					
					// send to dashboard
					sessionHelper.addMessage(MessageCategory.SUCCESS, "Erfolgreich eingeloggt.");
					return ws.redirect("/dashboard", session); // dashboard
				} else {
					sessionHelper.addMessage(MessageCategory.ERROR, "Login fehlgeschlagen.");
				}
			}
		}
		
		// serve login page
		return ws.serveTemplate("login.html", session, t);
    }
}
