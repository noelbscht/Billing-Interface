package net.libraya.gobdarchive.service.web.templating;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.libraya.gobdarchive.service.web.templating.cache.CacheHandler;
import net.libraya.gobdarchive.utils.exception.TemplatingException;

public class TemplatingHelper {
	
	/**
	 * return object itself if no method chaining, otherwise return last function output.
	 * @throws TemplatingException 
	 * */
	public static Object resolveObjectPath(Map<String, Object> context , Object obj, String potentialPath) throws TemplatingException {
		if (obj == null || potentialPath == null || potentialPath.isEmpty()) return obj;
	    
	    // split path
	    String[] parts = splitObjectPath(potentialPath);
	    Object current = obj;
	    
	    for (String part : parts) {
	        if (current == null) return null;
	        
	        // parse method
	        int bracketIndex = part.indexOf("(");
	        String methodName = (bracketIndex != -1) ? part.substring(0, bracketIndex).trim() : part.trim();
	        if (methodName.isEmpty()) continue;
	        
	        Object[] args = extractArgs(context, part);
	        try {
	        	 Method targetMethod = null;
	            
	        	 // search trigger method in object
	        	 List<Method> candidates = CacheHandler.getMethods(current.getClass(), methodName);
	        	 for (Method m : candidates) {
	        	     if (m.getParameterCount() == args.length) {
	        	         targetMethod = m;
	        	         break;
	        	     }
	        	 }
	            
	            if (targetMethod == null) {
	                throw new TemplatingException("No method '" + methodName + "' with " + args.length + " args in " + current.getClass().getSimpleName());
	            }
	                
	            current = targetMethod.invoke(current, args);
	        } catch (Exception e) {
	        	 throw new TemplatingException("Couldn't resolve '" + methodName + "' in " + current.getClass().getSimpleName());
	        }
	    }
	    return current;
	}
	
	/**
	 * divides chained methods by '.' while ignoring method parameters.
	 * */
	private static String[] splitObjectPath(String path) {
	    List<String> parts = new ArrayList<>();
	    String current = "";
	    int depth = 0;

	    for (char c : path.toCharArray()) {
	        if (c == '(') depth++;
	        if (c == ')') depth--;

	        if (c == '.' && depth == 0) {
	            parts.add(current);
	            current = "";
	        } else {
	            current += c;
	        }
	    }

	    parts.add(current);
	    return parts.toArray(new String[] {});
	}

	
	public static Object[] extractArgs(Map<String, Object> context, String method) throws TemplatingException {
		int bracketOpen = method.indexOf("(");
		int bracketClose = method.lastIndexOf(")");
		
		// return no args if no brackets found.
		if (bracketOpen == -1 || bracketClose == -1 || bracketClose < bracketOpen) {
			return new Object[] {};
		}
			
		String argsRaw = method.substring(bracketOpen + 1, bracketClose).trim();
		
		// if brackets empty
		if (argsRaw.isEmpty()) {
			return new Object[] {};
		}
		
		String[] raw = argsRaw.split(",");
	    Object[] args = new Object[raw.length];
	    for (int i = 0; i < args.length; i++) {
	    	String cleaned = raw[i].trim();
	    	
	    	// use variable if existence in context
	    	if (context.containsKey(cleaned)) {
	    	    args[i] = getValue(context, cleaned);
	    	    
	    	    continue;
	    	}
	    	
	    	boolean isQuoted = (cleaned.startsWith("\"") && cleaned.endsWith("\""))
	    	                || (cleaned.startsWith("'") && cleaned.endsWith("'"));

	    	if (isQuoted) {
	    	    args[i] = cleaned.substring(1, cleaned.length() - 1); 
	    	    continue;
	    	}
	    	
	    	// context lookup
	    	if (context.containsKey(cleaned)) {
	            args[i] = context.get(cleaned);
	            continue;
	    	}
	    	
	    	// cast to correct datatype
	        args[i] = cast(context, cleaned);

	    	
	    }
		
		return args;
	}
	
	public static Object getValue(Map<String, Object> context, String fullKey) throws TemplatingException {
		Object direct = context.get(fullKey);
		if (direct != null) return direct;
		
	    if (fullKey == null || fullKey.isEmpty()) return null;
	    
	    int firstBracket = fullKey.indexOf("(");
	    int lastBracket = fullKey.lastIndexOf(")");
	    int firstDot = fullKey.indexOf(".");

	    boolean isRootMethodCall = (firstBracket != -1 && lastBracket != -1 && (firstDot == -1 || firstBracket < firstDot));
	    
	    if (isRootMethodCall) {
	        String methodName = fullKey.substring(0, firstBracket);
	        Object contextObj = context.get(methodName);
	        
	        // methods from context
	        if (contextObj instanceof TemplateMethod) {
	        	TemplateMethod m = (TemplateMethod) contextObj;

		        Object[] args = extractArgs(context, fullKey);
		        
		        try {
					return m.method.invoke(m.owner, args);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new TemplatingException("Unable to invoke method: " + m.owner.getClass().getSimpleName() + "." + m.method + " (" + args.length + " args)");
				}
	        }
	    }
	    
	    // if method chaining
	    if (firstDot != -1) {
	    	String rootKey = fullKey.substring(0, firstDot);
	        String path = fullKey.substring(firstDot + 1);
	        Object rootObj = context.get(rootKey);
	        
	        return resolveObjectPath(context, rootObj, path);
	    }
	    
	    // return context
	    return context.get(fullKey);
	}
	
	public static boolean solveCondition(Map<String, Object> context, String headTag) throws TemplatingException {
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
	
	private static boolean solveOperators(Map<String, Object> context, String headTag) throws TemplatingException {
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
	private static String[] partsToContext(Map<String, Object> context, String[] parts) throws TemplatingException {
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object cast(Map<String, Object> context, Object ref) {
		String s = ref.toString();
		
		// number
		if (isNumeric(ref)) {
	        return s.contains(".") ? Float.parseFloat(s) : Integer.parseInt(s);
	    }
		
		// enum
		if (s.contains(".")) {
		    String[] parts = s.split("\\.");
		    if (parts.length == 2) {
		        String enumType = parts[0];
		        String enumKey = parts[1];

		        // check if type is in context
		        Object typeObj = context.get(enumType);
		        if (typeObj instanceof Class<?> clazz && clazz.isEnum()) {
		            return Enum.valueOf((Class<Enum>) clazz, enumKey);
		        }

		    }
		}
		
		// boolean
	    if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
	        return Boolean.parseBoolean(s);
	    }
	    
	    // just as passed (typically as string)
	    return ref;
	}
	
	private static boolean isNumeric(Object ref) {
		try {
			Float.parseFloat(ref.toString());
			return true;
		} catch (Exception e){
			return false;
		}
	}
	
	private static boolean containsComparison(String headTag) {
		return headTag.contains(">=") ||
			       headTag.contains("<=") ||
			       headTag.contains("==") ||
			       headTag.contains("!=") ||
			       headTag.contains(">")  ||
			       headTag.contains("<");

	}
}
