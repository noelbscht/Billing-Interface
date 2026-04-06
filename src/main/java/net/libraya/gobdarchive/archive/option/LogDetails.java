package net.libraya.gobdarchive.archive.option;

import java.util.Map;

/**
 * This class provides a structured way to store and format metadata as key-value pairs.
 * Includes static constants for commonly used keys.
 * */
public class LogDetails {
	
	private Map<String, String> details;
	
	public LogDetails(Map<String, String> map) {
		this.details = map;
	}
	
	public Map<String, String> getDetails() {
		return details;
	}
	
	/*
	 * return provided details in a specified format. ( related-docs=value;user-agent=value; )
	 * **/
	public String getDetailsFormatted() {
		StringBuilder sb = new StringBuilder();
		
		details.forEach((key, value) -> 
		sb.append(key)
		.append("=")
		.append(value)
		.append(";"));
		
		return sb.toString();
	}
	
	public void put(String key, String value) {
		details.put(key, value);
	}
	
	public void putAll(Map<String, String> m) {
		details.putAll(m);
	}
	
	/*
	 * default keys for multiple uses
	 * */
	public static final String EXPORT_OPTIONS = "export-options";
	public static final String FILTER_OPTIONS = "filter-options";
	public static final String ARCHIVE_ID = "archive-id";
	public static final String RELATION_KEY = "related-docs";
	public static final String USER_AGENT = "user-agent";
	public static final String USER_IP = "user-ip4";
	
}
