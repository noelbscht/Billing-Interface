package net.libraya.gobdarchive.archive.option;

import org.json.JSONObject;

public class ExportOptions {
	
    public boolean zip = true;
    public boolean includeAuditLog = true;
    
    public ExportOptions(JSONObject input) {
    	this.zip = input.optBoolean("zip");
    	this.zip = input.optBoolean("include_audit_log");
    }
}

