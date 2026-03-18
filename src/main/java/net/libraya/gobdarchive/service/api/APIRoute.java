package net.libraya.gobdarchive.service.api;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.archive.ArchiveManager;

public abstract class APIRoute {
	
	protected final ArchiveManager manager;
	
    private final String rule;
    private final String description;
    private final String[] methods;

    public APIRoute(ArchiveManager manager, String rule, String description, String[] methods) {
    	this.manager = manager;
        this.rule = rule;
        this.description = description;
        this.methods = methods;
    }

    public abstract NanoHTTPD.Response onRequest(IHTTPSession session, String body) throws Exception;
    
    public NanoHTTPD.Response error(int status, Object msg) {
        return NanoHTTPD.newFixedLengthResponse(
            Response.Status.lookup(status),
            "application/json",
            new JSONObject().put("error", msg).toString()
        );
    }

    public NanoHTTPD.Response success(JSONObject obj) {
        return NanoHTTPD.newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            obj.toString()
        );
    }
    
    public String getRule() {
        return rule;
    }

    public String getDescription() {
        return description;
    }

    public String[] getMethods() {
        return methods;
    }
}


