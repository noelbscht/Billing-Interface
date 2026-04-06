package net.libraya.gobdarchive.service.web.routes;

import java.util.ArrayList;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.archive.ArchiveManager;
import net.libraya.gobdarchive.service.web.WebRoute;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.WebPermission;
import net.libraya.gobdarchive.service.web.templating.SimpleTemplating;

public class WRDashboard extends WebRoute {
	
	public WRDashboard(WebServer ws) {
        super(ws, "/dashboard", new String[] { "GET", "POST" } , new WebPermission[] { WebPermission.ARCHIVE_READ });
    }

	@Override
    public Response onRequest(IHTTPSession session, String body, HashMap<String, String> files, SessionHelper sessionHelper) throws Exception {
		SimpleTemplating templating = new SimpleTemplating(ws, sessionHelper);
		ArchiveManager arch = Main.getArchiveManager();
		
		ArrayList<String> archiveEntries = arch.listArchiveEntries();
		
		templating.addVariable("entries", archiveEntries);
		
		//todo:: add needed variables like 'backupsys', add ws.isAuthorized function to templating, adapt dashboard.html
		
		return ws.serveTemplate("dashboard.html", session, templating);
    }
}
