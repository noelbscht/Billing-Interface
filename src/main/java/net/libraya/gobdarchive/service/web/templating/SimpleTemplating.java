package net.libraya.gobdarchive.service.web.templating;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.WebPermission;
import net.libraya.gobdarchive.utils.exception.TemplatingException;

public class SimpleTemplating {
	
	private WebServer ws;
	private SessionHelper sessionHelper;
	
	private final Map<String, Object> context;

	public SimpleTemplating(WebServer ws, SessionHelper sessionHelper) throws TemplatingException {
		this.ws = ws;
		this.sessionHelper = sessionHelper;
		this.context = new HashMap<String, Object>();
		//todo:: feat: recursive iteration
	}
	
	public void addDefaults(IHTTPSession session) throws Exception {
		// session and authorization
		addVariable("messages", sessionHelper.getMessages());
		addVariable("loggedIn", sessionHelper.isLoggedIn());
		addVariable("uid", sessionHelper.getSessionData().optString("uid", null));
		addVariable("WebPermission", WebPermission.class);
		addMethod("isAuthorized", ws.getPermissionLoader().getClass().getMethod("isAuthorized", String.class, WebPermission.class));
		
		// partials
		addPartial(ws, "headsection", "presets/headsection.html");
		addPartial(ws, "navigation", "presets/navigation.html");
		addPartial(ws, "footer", "presets/footer.html");
		addPartial(ws, "messagescript", "presets/messages.html");
	}
	
	public void addMethod(String syntax, Method method) {
		addVariable(syntax, method);
	}
	
	/**
	 * add context to replace with a specific value.
	 * */
	public void addVariable(String key, Object value) {
		if (this.context.containsKey(key)) {
			this.context.remove(key);
		}
		this.context.put(key, value);
	}
	
	/**
	 * set a keyword for file content to use is as content.
	 * */
	public void addPartial(WebServer ws, String key, String templateFileName) throws Exception, TemplatingException {
		Path file = ws.getTemplatesDir().resolve(templateFileName).normalize();

        if (!Files.exists(file) || !file.startsWith(ws.getTemplatesDir())) {
            throw new TemplatingException("Unallowed templating file provided: " + templateFileName);
        }
		
	    String partialContent = Files.readString(file);
	    this.addVariable(key, partialContent);
	}
	
	/**
	 * return final template
	 * @throws TemplatingException, IOException
	 * */
	public String render(Path templatePath) throws IOException, TemplatingException {
		String content = loadFromCache(templatePath);
		
		content = renderVariables(context, content);
		content = renderIteratorTags(context, content);
		content = renderConditionTags(context, content);
		
		// render variables (because of remaining context from loops and partial content)
		content = renderVariables(context, content);
		 
		 return content;
	}
	
	/**
	 * returns raw template content from cache while reloading outdated files.
	 * */
	private String loadFromCache(Path templatePath) throws IOException, TemplatingException {
		String cacheKey = templatePath.toAbsolutePath().normalize().toString();
		CachedTemplate cached = 
				TemplatingHelper.CACHE.get(cacheKey);
		long lastModified = Files.getLastModifiedTime(templatePath).toMillis();

		if (cached == null || cached.getLastModified() != lastModified) {
			ws.log("cache updated for: " + templatePath);
			cached = new CachedTemplate(templatePath, Files.readString(templatePath));
		    TemplatingHelper.CACHE.put(cacheKey, cached);
		}
		
		return cached.getRawContent();
	}
	
	/**
	 * render condition tags
	 * @param itemScope 
	 * @throws TemplatingException 
	 * */
	private String renderConditionTags(Map<String, Object> itemScope, String content) throws TemplatingException {
		String startTag = "{% if ";
		String endTag = "{% /if %}";
		String elseTag = "{% else %}";
		
		while (containsCondition(content)) {
			int startIndex = content.lastIndexOf(startTag);
			int tagEndIndex = content.indexOf("%}", startIndex); // %}
			int closingIndex = content.indexOf(endTag, tagEndIndex); // {% /if %}
	        
	        if (tagEndIndex == -1 || closingIndex == -1) {
	        	break; // interrupt if no tag available
	        }
			
	        // tag head
			String tagHead = content.substring(startIndex + startTag.length(), tagEndIndex).trim(); 
			
			// potential content
			String inner = content.substring(tagEndIndex + 2, closingIndex);
			
			// prove condition
			boolean conditionMet = TemplatingHelper.solveCondition(itemScope, tagHead);
			
	        String finalContent = "";
	        
	        // else tag
	        if (inner.contains(elseTag)) {
	            // split by else tag (true side, false side)
	            String[] parts = inner.split(Pattern.quote(elseTag));
	            finalContent = conditionMet ? parts[0] : (parts.length > 1 ? parts[1] : "");
	        } else {
	            // if no else implemented
	            finalContent = conditionMet ? inner : "";
	        }
			
			// replace block (tag, inner, end-tag)
			String fullBlock = content.substring(startIndex, closingIndex + endTag.length());
			
			// quote for special characters
			content = content.replaceFirst(Pattern.quote(fullBlock), 
                    Matcher.quoteReplacement(finalContent));
		}
		
		return content;
	}
	
	/**
	 * render for-each tags / iterator tags. ({% iterate item : list %} ... {% /iterate %}
	 * @param itemScope 
	 * @throws TemplatingException 
	 * */
	private String renderIteratorTags(Map<String, Object> scope, String content) throws TemplatingException {
		String startTag = "{% iterate ";
		String endTag = "{% /iterate %}";
		
		while (containsIterator(content)) {
			int startIndex = content.lastIndexOf(startTag);
			int tagEndIndex = content.indexOf("%}", startIndex); // %}
			int closingIndex = content.indexOf(endTag, tagEndIndex); // {% /iterate %}
	        
	        if (tagEndIndex == -1 || closingIndex == -1) { // interrupt if no tag available
	        	break;
	        }
			
	        // parse header
			String tagHead = content.substring(startIndex + startTag.length(), tagEndIndex).trim(); 
			String[] parts = tagHead.split(":"); // split with separator
			
			 if (parts.length != 2) { // interrupt if tag-head is invalid
				 throw new TemplatingException("An iterator is using invalid syntax: " + tagHead);
			 }
			
			String itemName = parts[0].trim(); // item
			String listKey = parts[1].trim(); // list
			String innerTemplate = content.substring(tagEndIndex + 2, closingIndex);
			
			Object listObj = TemplatingHelper.getValue(scope, listKey);
			
			if (listObj instanceof Map<?, ?>) {
				listObj = ((Map<?, ?>) listObj).entrySet();
			} else if (listObj instanceof Iterable<?>) {
				listObj = (Iterable<?>) listObj;
			} else {
			    throw new TemplatingException("An iterator contains an invalid object type: " + listKey);
			}
	        
			// replace item inside the iterator
	        StringBuilder loopResult = new StringBuilder();
	        for (Object item : (Iterable<?>) listObj) {
	        	HashMap<String, Object> itemScope = new HashMap<>(scope); // copy of global context
	        	if (item instanceof Map.Entry) {
	                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
	                String[] varNames = itemName.split(",");
	                if (varNames.length == 2) {
	                    itemScope.put(varNames[0].trim(), entry.getKey());
	                    itemScope.put(varNames[1].trim(), entry.getValue());
	                } else {
	                    itemScope.put(itemName, entry.getValue());
	                }
	            } else {
	                itemScope.put(itemName, item);
	            }
	        	
	        	 String renderedInner = innerTemplate;
	        	 renderedInner = renderIteratorTags(itemScope, renderedInner); // render recursively 
	             renderedInner = renderConditionTags(itemScope, renderedInner);
	             renderedInner = renderVariables(itemScope, renderedInner);
	             
        	    loopResult.append(renderedInner);
	        }
	        
	        content = content.substring(0, startIndex) 
	                + loopResult.toString() 
	                + content.substring(closingIndex + endTag.length());
		}
		return content;
	}
	
	private String renderVariables(Map<String, Object> scope, String content) throws TemplatingException {
		int cursor = 0;
		while (containsVariable(content)) {
			int startIndex = content.indexOf("{{", cursor); // {{
			int endIndex = content.indexOf("}}", startIndex); // }}
			
			if (startIndex == -1 || endIndex == -1) break;
			 
			String fullKey = content.substring(startIndex + 2, endIndex).trim();
			Object val = TemplatingHelper.getValue(scope, fullKey);
			 
			if (val != null) {
				String replacement = String.valueOf(val);
	        	content = content.substring(0, startIndex) + replacement + content.substring(endIndex + 2);
	            // Cursor hinter die neue Stelle setzen
	        	cursor = startIndex + replacement.length();
			} else {
				cursor = endIndex + 2; 
			}
		}
		
		return content;
	}
	
	private boolean containsCondition(String content) {
		return content.contains("{% if") && content.contains("{% /if %}");
	}
	
	private boolean containsIterator(String content) {
		return content.contains("{% iterate") && content.contains("{% /iterate %}");
	}
	
	private boolean containsVariable(String content) {
		return content.contains("{{") && content.contains("}}");
	}
}
