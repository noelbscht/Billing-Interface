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
import net.libraya.gobdarchive.service.web.templating.cache.CacheHandler;
import net.libraya.gobdarchive.utils.exception.TemplatingException;

public class SimpleTemplating {
	
	private WebServer ws;
	private SessionHelper sessionHelper;
	
	private final HashMap<String, Object> context;
	
	private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*(.*?)\\s*\\}\\}");

	public SimpleTemplating(WebServer ws, SessionHelper sessionHelper) throws TemplatingException {
		this.ws = ws;
		this.sessionHelper = sessionHelper;
		this.context = new HashMap<String, Object>();
		
	}
	
	public void addDefaults(IHTTPSession session) throws Exception {
		// session and authorization
		addVariable("messages", sessionHelper.getMessages());
		addVariable("loggedIn", sessionHelper.isLoggedIn());
		addVariable("uid", sessionHelper.getUserUId());
		addVariable("WebPermission", WebPermission.class);
		addMethod("isAuthorized", ws.getPermissionLoader(), "isAuthorized", String.class, WebPermission.class);
		
		// partials
		addPartial(ws, "headsection", "presets/headsection.html");
		addPartial(ws, "navigation", "presets/navigation.html");
		addPartial(ws, "footer", "presets/footer.html");
		addPartial(ws, "messagescript", "presets/messages.html");
	}
	
	/**
	 * add methods based on an object.
	 * */
	public void addMethod(String syntax, Object owner, String methodName, Class<?>... parameters) throws TemplatingException {
		try {
			Method method = owner.getClass().getMethod(methodName, parameters);
			
			addVariable(syntax, new TemplateMethod(owner, method));
		} catch (Exception e) {
			throw new TemplatingException("No such method: " + owner.getClass().getSimpleName() + "." + methodName + " (" + parameters.length + " args)");
		}
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
		
	    String partialContent = CacheHandler.loadTemplate(file);
	    this.addVariable(key, partialContent);
	}
	
	/**
	 * return final template
	 * @throws TemplatingException, IOException
	 * */
	public String render(Path templatePath) throws IOException, TemplatingException {
		String content = CacheHandler.loadTemplate(templatePath);
		
		// render partial content
		content = renderPartials(context, content);
		
		// render condition tags
		content = renderConditionTags(context, content);
		
		// render iterator tags
		content = renderIteratorTags(context, content);
		
		// render variables
		content = renderVariables(context, content);
		 
		 return content;
	}

	/**
	 * render condition tags
	 * @param itemScope 
	 * @throws TemplatingException 
	 * */
	private String renderConditionTags(Map<String, Object> scope, String content) throws TemplatingException {
	    String startTag = "{% if";
	    String elseTag = "{% else %}";
	    String endTag = "{% /if %}";

	    int searchPos = 0;

	    while (true) {
	        int startIndex = content.indexOf(startTag, searchPos);
	        if (startIndex == -1) break;

	        int tagEndIndex = content.indexOf("%}", startIndex);
	        if (tagEndIndex == -1) break;

	        String tagHead = content.substring(startIndex + startTag.length(), tagEndIndex).trim();

	        int innerStart = tagEndIndex + 2;
	        int pos = innerStart;
	        int depth = 1;

	        int elseIndex = -1;

	        while (depth > 0) {
	            int nextIf   = content.indexOf(startTag, pos);
	            int nextElse = content.indexOf(elseTag, pos);
	            int nextEnd  = content.indexOf(endTag, pos);

	            if (nextEnd == -1)
	                throw new TemplatingException("Missing {% /if %}");

	            if (nextElse != -1 && nextElse < nextEnd && (nextIf == -1 || nextElse < nextIf)) {
	                if (depth == 1) {
	                    elseIndex = nextElse;
	                }
	                pos = nextElse + elseTag.length();
	                continue;
	            }

	            if (nextIf != -1 && nextIf < nextEnd) {
	                depth++;
	                pos = nextIf + startTag.length();
	                continue;
	            }

	            depth--;
	            pos = nextEnd + endTag.length();
	        }

	        int closingIndex = pos - endTag.length();

	        boolean conditionMet = TemplatingHelper.solveCondition(scope, tagHead);

	        String finalContent;
	        if (elseIndex != -1) {
	            String beforeElse = content.substring(innerStart, elseIndex);
	            String afterElse  = content.substring(elseIndex + elseTag.length(), closingIndex);
	            finalContent = conditionMet ? beforeElse : afterElse;
	        } else {
	            String inner = content.substring(innerStart, closingIndex);
	            finalContent = conditionMet ? inner : "";
	        }
	        
	        finalContent = renderConditionTags(scope, finalContent);
	        
	        content = content.substring(0, startIndex)
	                + finalContent
	                + content.substring(pos);

	        searchPos = startIndex + finalContent.length();
	    }
	    
	    return content;
	}
	
	/**
	 * render for-each tags / iterator tags. ({% iterate item : list %} ... {% /iterate %}
	 * @param itemScope 
	 * @throws TemplatingException 
	 * */
	private String renderIteratorTags(Map<String, Object> scope, String content) throws TemplatingException {
	    String startTag = "{% iterate";
	    String endTag = "{% /iterate %}";

	    int searchPos = 0;

	    while (true) {
	        int startIndex = content.indexOf(startTag, searchPos);
	        if (startIndex == -1) break;

	        int tagEndIndex = content.indexOf("%}", startIndex);
	        if (tagEndIndex == -1) break;

	        // parse header (allow whitespace)
	        String tagHead = content.substring(startIndex + startTag.length(), tagEndIndex).trim();

	        // find matching closing tag (supports nested iterate blocks)
	        int innerStart = tagEndIndex + 2;
	        int pos = innerStart;
	        int depth = 1;

	        while (depth > 0) {
	            int nextOpen = content.indexOf(startTag, pos);
	            int nextClose = content.indexOf(endTag, pos);

	            if (nextClose == -1)
	                throw new TemplatingException("Missing {% /iterate %}");

	            if (nextOpen != -1 && nextOpen < nextClose) {
	                depth++;
	                pos = nextOpen + startTag.length();
	            } else {
	                depth--;
	                pos = nextClose + endTag.length();
	            }
	        }

	        int closingIndex = pos - endTag.length();
	        String innerTemplate = content.substring(innerStart, closingIndex);

	        // parse header syntax
	        String[] parts = tagHead.split(":");
	        if (parts.length != 2)
	            throw new TemplatingException("An iterator is using invalid syntax: " + tagHead);

	        String itemName = parts[0].trim();
	        String listKey = parts[1].trim();

	        Object listObj = TemplatingHelper.getValue(scope, listKey);

	        if (listObj instanceof Map<?, ?>)
	            listObj = ((Map<?, ?>) listObj).entrySet();
	        else if (!(listObj instanceof Iterable<?>))
	            throw new TemplatingException("An iterator contains an invalid object type: " + listKey);

	        // replace item inside the iterator
	        StringBuilder loopResult = new StringBuilder();

	        for (Object item : (Iterable<?>) listObj) {
	            Object old = scope.get(itemName);
	            scope.put(itemName, item);

	            String renderedInner = innerTemplate;
	            renderedInner = renderIteratorTags(scope, renderedInner); // render recursively 
	            renderedInner = renderConditionTags(scope, renderedInner);
	            renderedInner = renderVariables(scope, renderedInner);

	            loopResult.append(renderedInner);

	            if (old != null) scope.put(itemName, old);
	            else scope.remove(itemName);
	        }

	        // replace block (tag, inner, end-tag)
	        content = content.substring(0, startIndex)
	                + loopResult
	                + content.substring(pos);

	        searchPos = startIndex + loopResult.length();
	    }

	    return content;
	}

	private String renderVariables(Map<String, Object> scope, String content) throws TemplatingException {
	    Matcher m = VAR_PATTERN.matcher(content);
	    StringBuffer sb = new StringBuffer();

	    while (m.find()) {
	        String key = m.group(1).trim();
	        Object val = TemplatingHelper.getValue(scope, key);
	        String replacement = val != null ? val.toString() : "";
	        m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
	    }

	    m.appendTail(sb);
	    return sb.toString();
	}
	
	private String renderPartials(Map<String, Object> scope, String content) {
	    for (Map.Entry<String, Object> entry : scope.entrySet()) {
	        if (entry.getValue() instanceof String) {
	            String key = entry.getKey();
	            String val = (String) entry.getValue();
	            
	            Pattern p = Pattern.compile("\\{\\{\\s*" + Pattern.quote(key) + "\\s*\\}\\}");
	            Matcher m = p.matcher(content);

	            StringBuffer sb = new StringBuffer();
	            while (m.find()) {
	                m.appendReplacement(sb, Matcher.quoteReplacement(val));
	            }
	            m.appendTail(sb);
	            content = sb.toString();
	        }
	    }
	    return content;
	}
}
