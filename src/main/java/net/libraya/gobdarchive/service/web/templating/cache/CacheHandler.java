package net.libraya.gobdarchive.service.web.templating.cache;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.libraya.gobdarchive.utils.exception.TemplatingException;

public class CacheHandler {
	
	public static final ConcurrentHashMap<String, CachedTemplate> TEMPLATE_CACHE = new ConcurrentHashMap<String, CachedTemplate>();
	public static final Map<Class<?>, Map<String, List<Method>>> METHOD_CACHE = new ConcurrentHashMap<>();

	/**
	 * returns raw template content from cache while reloading outdated files.
	 * */
	public static String loadTemplate(Path templatePath) throws IOException, TemplatingException {
	    String cacheKey = templatePath.toAbsolutePath().normalize().toString();

	    CachedTemplate cached = CacheHandler.TEMPLATE_CACHE.compute(cacheKey, (key, old) -> {
	    	try {
	            long lastModified = Files.getLastModifiedTime(templatePath).toMillis();
	            if (old == null || old.getLastModified() != lastModified) {
	                return new CachedTemplate(templatePath, Files.readString(templatePath));
	            }
	        } catch (Exception ignored) {}
	        return old;
	    });

	    return cached.getRawContent();
	}
	
	/**
	 * returns methods from cache.
	 * */
	public static List<Method> getMethods(Class<?> clazz, String name) {
	    return CacheHandler.METHOD_CACHE
	        .computeIfAbsent(clazz, c -> {
	            Map<String, List<Method>> map = new HashMap<>();
	            for (Method m : c.getMethods()) {
	                map.computeIfAbsent(m.getName(), k -> new ArrayList<>()).add(m);
	            }
	            return map;
	        })
	        .getOrDefault(name, List.of());
	}

}
