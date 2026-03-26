package net.libraya.gobdarchive.service.web;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.WebPermission;

public abstract class WebRoute {
	
	protected final WebServer ws;
	
    private final String rule;
    private final String[] methods;
    private final WebPermission[] permissions;
    
    public WebRoute(WebServer ws, String rule, String[] methods, WebPermission[] permissions) {
    	this.ws = ws;
        this.rule = rule;
        this.methods = methods;
        this.permissions = permissions;
    }

    public abstract NanoHTTPD.Response onRequest(IHTTPSession session, String body, SessionHelper sessionHelper) throws Exception;
    
    public File[] getFiles(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (Exception e) {
            return new File[0];
        }

        return files.values().stream()
                .map(File::new)
                .toArray(File[]::new);
    }

    
    public String getRule() {
        return rule;
    }

    public String[] getMethods() {
        return methods;
    }
    
    public boolean supportsMethod(Method method) {
    	for (String m : methods) {
    		if (method.toString().equalsIgnoreCase(m)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public WebPermission[] getPermissions() {
		return permissions;
	}
    
    public WebServer getWebServer() {
		return ws;
	}
}


