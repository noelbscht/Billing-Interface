package net.libraya.gobdarchive.service.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONObject;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.service.Service;
import net.libraya.gobdarchive.utils.Environment;
import net.libraya.gobdarchive.utils.Unicodes;

public class API extends Service {
    
    private APIRouter router;

    public API() {
        super("API", Environment.API_ENABLED, Environment.API_PORT);
        
        // register routes
        this.router = new APIRouter(Main.getArchiveManager());
    }

    @Override
    public Response serve(IHTTPSession session) {
        String method = session.getMethod().name();
        String uri = session.getUri();
        
        logger.log(session.getRemoteIpAddress() + " - " + session.getMethod() + " " + uri);

        // /api/<option>/<route?>
        String[] parts = uri.split("/");
        if (parts.length < 3 || !parts[1].equals("api")) {
            return json(404, new JSONObject().put("error", "Invalid API path"));
        }

        String rule = String.join("/", java.util.Arrays.copyOfRange(parts, 2, parts.length));
        APIRoute handler = this.router.match(rule);

        if (handler == null) {
            return json(404, new JSONObject().put("error", "Route not found"));
        }

        if (!Arrays.asList(handler.getMethods()).contains(method)) {
            return json(405, new JSONObject().put("error", "Method not allowed"));
        }
	
        // parse body
        Map<String, String> files = new java.util.HashMap<>();
        try {
            session.parseBody(files);
        } catch (Exception e) {
            return json(400, new JSONObject().put("error", "Invalid request body"));
        }

        String body = files.get("postData");

        // API key check
        String key = session.getHeaders().get("x-api-key");
        if (!authenticate(key)) {
            return json(401, new JSONObject().put("error", "Unauthorized"));
        }

        try {
            return handler.onRequest(session, body);
        } catch (Exception e) {
            e.printStackTrace();
            return json(500, new JSONObject().put("error", "Internal server error"));
        }
    }
    
    public static boolean authenticate(String authKey) {
    	return Environment.getString("API_AUTH_KEY", "").equals(authKey);
    }

    public Response json(int status, JSONObject obj) {
        return newFixedLengthResponse(
            Response.Status.lookup(status),
            "application/json",
            obj.toString()
        );
    }
    
    public void sendRoutes() {
    	ArrayList<String> feedback = new ArrayList<>();
		
		feedback.add(Unicodes.VERTICAL_BAR + " API Routes");
		for (APIRoute r : this.getRouter().getRoutes())  {
			feedback.add("    /api/" + r.getRule() + " " + Unicodes.EM_DASH + " " + r.getDescription());
		}
		
		Main.sendFeedback(feedback.toArray(new String[0]));
    }
    
    public APIRouter getRouter() {
		return router;
	}
}