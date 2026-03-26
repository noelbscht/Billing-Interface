package net.libraya.gobdarchive.service.web.auth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.utils.HashUtil;
import net.libraya.gobdarchive.utils.exception.ServiceException;

/**
 * handle session cookie, cookies in general and user feedback messages.
 * */
public class SessionHelper {
	
	private WebPermissionLoader permLoader;
	private List<String> deleteQueue = new ArrayList<>();
	
	private IHTTPSession session;
	private JSONObject currentSessionData = null;
	
	public SessionHelper(WebPermissionLoader permLoader, IHTTPSession session) {
		this.permLoader = permLoader;
		this.session = session;
	}
	
	/** write cookie configurations to response and set index path */
	public void finalizeResponse(Response response) {
		
		// write changes to response headers
		session.getCookies().unloadQueue(response);
		
		try {
	        if (currentSessionData != null) {
	            String encoded = URLEncoder.encode(currentSessionData.toString(), "UTF-8");
	            response.addHeader("Set-Cookie", "session=" + encoded + "; Path=/; Max-Age=31536000; HttpOnly");
	        }
	    } catch (Exception ignored) {}
		
		// set headers to let cookies expire
		for (String key : deleteQueue) {
            response.addHeader("Set-Cookie", key + "=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly");
        }
		
		// clear
		deleteQueue.clear();
	}
	
	public void deleteCookie(String key) {
		// delete from nano queue
	    if (!deleteQueue.contains(key)) {
	    	 deleteQueue.add(key);
	    }
	    
	    session.getCookies().delete(key);
	}
	
	public void setCookie(String key, String value, int expires) {
		session.getCookies().set(key, value, expires);
	}
	
	public JSONObject getSessionData() throws UnsupportedEncodingException {
		if (currentSessionData != null) return currentSessionData;
		
		String cookie = session.getCookies().read("session");
		
		if (cookie != null && !cookie.isEmpty()) {
			return new JSONObject(URLDecoder.decode(cookie, "UTF-8"));
		}
		return new JSONObject();
	}
	
	private void saveSession(JSONObject data) throws UnsupportedEncodingException {
		this.currentSessionData = data;
	    
	    try {
	    	String encoded = URLEncoder.encode(data.toString(), "UTF-8");
	        session.getCookies().set("session", encoded, 3650);
	    } catch (Exception ignored) {}
	}
	
	public boolean login(String email, String password) throws SQLException, ServiceException, UnsupportedEncodingException {
		Map<String, Object> result = permLoader.authentication(email, password);
		
		if (result != null) {
			JSONObject obj = getSessionData();
			
			obj.put("uid", result.get(permLoader.getPrimaryKeyColumn()));
			obj.put("email", result.get(permLoader.getEmailColumn()));
			obj.put("hash", HashUtil.sha256(password));
			
			saveSession(obj); 
			return true;
		}
		
		return false;
	}
	
	public void logout() throws UnsupportedEncodingException {
		this.currentSessionData = new JSONObject();
		deleteCookie("session"); // let cookie expire
	}
	
	
	public boolean isLoggedIn() throws UnsupportedEncodingException {
		JSONObject obj = getSessionData();
        return obj != null && obj.has("uid");
	}
	
	public void addMessage(MessageCategory category, String message) {
		if (message == null || message.isEmpty()) return;
		
		try {
	        JSONObject sessionData = getSessionData();
	        if (sessionData == null) sessionData = new JSONObject();

	        JSONObject msgsJson = sessionData.optJSONObject("messages");
	        if (msgsJson == null) msgsJson = new JSONObject();

	        msgsJson.put(message, category.name().toLowerCase());
	        
	        sessionData.put("messages", msgsJson);
	        saveSession(sessionData);
	    } catch (Exception ignored) {}
	}
	
	/**
	 * return messages and clear messsage history (format: [message, category])
	 * */
	public HashMap<String, String>  getMessages() {
		HashMap<String, String> result = new HashMap<>();
	    try {
	        JSONObject sessionData = getSessionData();
	        if (sessionData == null) return result;

	        JSONObject msgsJson = sessionData.optJSONObject("messages");
	        if (msgsJson != null) {
	            for (String message : msgsJson.keySet()) {
	                result.put(message, msgsJson.getString(message));
	            }

	            sessionData.remove("messages");
	            saveSession(sessionData);
	        }
	    } catch (Exception ignored) {}
	    return result;
	}
	
	public enum MessageCategory {
		INFO,
		ERROR,
		SUCCESS
	}

}
