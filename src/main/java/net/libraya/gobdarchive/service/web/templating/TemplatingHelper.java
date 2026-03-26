package net.libraya.gobdarchive.service.web.templating;

import java.lang.reflect.Method;
import java.util.HashMap;

import net.libraya.gobdarchive.utils.exception.TemplatingException;

public class TemplatingHelper {
	
	/**
	 * return object itself if no method chaining, otherwise return last function output.
	 * @throws TemplatingException 
	 * */
	public static Object resolveObjectPath(Object obj, String potentialPath) throws TemplatingException {
		if (obj == null || potentialPath == null || potentialPath.isEmpty()) return obj;
	    
	    // split path
	    String[] parts = potentialPath.split("\\.");
	    Object current = obj;

	    for (String part : parts) {
	        if (current == null) return null;
	        
	        // parse method
	        int bracketIndex = part.indexOf("(");
	        String methodName = (bracketIndex != -1) ? part.substring(0, bracketIndex).trim() : part.trim();
	        if (methodName.isEmpty()) continue;
	        
	        Object[] args = extractArgs(part);
	        try {
	        	 Method targetMethod = null;
	            
	        	 // search trigger method in object
	            for (Method m : current.getClass().getMethods()) {
	                if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
	                    targetMethod = m;
	                    break;
	                }
	            }
	            
	            // fallback to getter
	            if (targetMethod == null) {
	                String getterName = "get" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
	                targetMethod = current.getClass().getMethod(getterName);
	                args = new Object[0];
	            }

	            current = targetMethod.invoke(current, args);
	        } catch (Exception e) {
	        	 throw new TemplatingException("[TEMPLATING ERROR] Couldn't resolve '" + methodName + "' in " + current.getClass().getSimpleName());
	        }
	    }
	    return current;
	}
	
	public static Object[] extractArgs(String method) {
		int bracketOpen = method.indexOf("(");
		int bracketClose = method.lastIndexOf(")");
		
		// return no args if no brackets found.
		if (bracketOpen == -1) {
			return new Object[] {};
		}
			
		String argsRaw = method.substring(bracketOpen + 1, bracketClose).trim();
		
		// if brackets empty
		if (argsRaw.isEmpty()) {
			return new Object[] {};
		}
		
		Object[] args = argsRaw.split(",");
	    for (int i = 0; i < args.length; i++) {
	    	// cast to correct datatype
	    	args[i] = cast(args[i].toString().trim().replace("'", "").replace("\"", ""));
	    	
	    }
		
		return args;
	}
	
	public static Object getValue(HashMap<String, Object> context, String fullKey) throws TemplatingException {
	    if (fullKey == null || fullKey.isEmpty()) return null;
	    
	    // if method chaining
	    if (fullKey.contains(".")) {
	        int dotIndex = fullKey.indexOf(".");
	        String rootKey = fullKey.substring(0, dotIndex);
	        String path = fullKey.substring(dotIndex + 1);
	        
	        Object rootObj = context.get(rootKey);
	        return resolveObjectPath(rootObj, path);
	    }
	    
	    // return context
	    return context.get(fullKey);
	}
	
	public static boolean solveCondition(HashMap<String, Object> context, String headTag) throws TemplatingException {
		// remove spacing
		headTag = headTag.trim();
		
		// negate head tag
		boolean negate = headTag.startsWith("!");
		if (negate) {
			headTag = headTag.substring(1).trim();
		}
		
		// if no operator found, try to solve as boolean
		if (!containsComparison(headTag)) {
			Object val = getValue(context, headTag);
			boolean result = false;
			
			if (val instanceof Boolean) {
				result = (Boolean) val;
			} else if (val != null) {
	            String s = String.valueOf(val);
	            result = !s.equalsIgnoreCase("false") && !s.isEmpty();
	        }
			
			return negate ? !result : result;
		}
		
		return solveOperators(context, headTag);
	}
	
	private static boolean solveOperators(HashMap<String, Object> context, String headTag) throws TemplatingException {
		if (headTag.contains("==")) {
	        String[] parts = partsToContext(context, headTag.split("=="));
	        return parts[0].toString().trim().equals(parts[1].toString().trim()); // without spacing
	    }
		
	    if (headTag.contains("!=")) {
	        String[] parts = partsToContext(context, headTag.split("!="));
	        return !parts[0].toString().trim().equals(parts[1].toString().trim());
	    }
	    
	    // number conditions (parse to float to make every numeric object type possible)
	   try {
		   if (headTag.contains(">=")) {
		        String[] parts = partsToContext(context, headTag.split(">="));
		        return Float.parseFloat(parts[0].trim()) >= Float.parseFloat(parts[1].trim());
		    }
		    
		    if (headTag.contains("<=")) {
		        String[] parts = partsToContext(context, headTag.split("<="));
		        return Float.parseFloat(parts[0].trim()) <= Float.parseFloat(parts[1].trim());
		    }
		   
		   if (headTag.contains(">")) {
		        String[] parts = partsToContext(context, headTag.split(">"));
		        return Float.parseFloat(parts[0].trim()) > Float.parseFloat(parts[1].trim());
		    }
		    
		    if (headTag.contains("<")) {
		        String[] parts = partsToContext(context, headTag.split("<"));
		        return Float.parseFloat(parts[0].trim()) < Float.parseFloat(parts[1].trim());
		    }
	   } catch (NullPointerException | NumberFormatException  ex) {
		   throw new TemplatingException("Invalid numeric condition in head tag: " + headTag);
	   }
	   throw new TemplatingException("Couldn't solve condition in head tag: " + headTag);
	}
	
	/**
	 * parse condition parts to context if needed
	 * */
	private static String[] partsToContext(HashMap<String, Object> context, String[] parts) throws TemplatingException {
		for (int i = 0; i < parts.length; i++) {
			
			if (parts[i] == null) continue;
			
			String part = parts[i].trim(); // without spacing
			Object val = getValue(context, part);
			
			 if (val != null) {
	            parts[i] = String.valueOf(val);
	        } else if (part.equalsIgnoreCase("null")) {
        		parts[i] = null;
	        } else if (!context.containsKey(part)) {
	            parts[i] = part.replace("'", "").replace("\"", "");
	        } else {
	        	parts[i] = null;
	        }
		}
		return parts;
	}
	
	private static Object cast(Object ref) {
		String s = ref.toString();
		if (isNumeric(ref)) {
	        return s.contains(".") ? Float.parseFloat(s) : Integer.parseInt(s);
	    }
	    if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
	        return Boolean.parseBoolean(s);
	    }
	    return ref;
	}
	
	private static boolean isNumeric(Object ref) {
		if (ref == null) return false;
		
		String s = ref.toString().trim();
		int startIndex = s.startsWith("-") ? 1 : 0;
		
		if (s.length() == startIndex) return false;
		
		for (int i = startIndex; i < s.length(); i++) {
	         if (!Character.isDigit(s.charAt(i))) {
	            return false;
	        }
	    }
	    return true;
	}
	
	private static boolean containsComparison(String headTag) {
		return headTag.contains("==") ||
				headTag.contains("!=") ||
				headTag.contains(">") ||
				headTag.contains("<") ||
				headTag.contains(">=") ||
				headTag.contains("<=");
	}
}
