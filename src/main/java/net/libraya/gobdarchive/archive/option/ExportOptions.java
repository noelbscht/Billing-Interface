package net.libraya.gobdarchive.archive.option;

import org.json.JSONObject;

public class ExportOptions {
	
	public boolean zip = true;
    public boolean includeAuditLog = true;
    public boolean interpretDATEV; 
    
    public ExportOptions(JSONObject input) {
    	this.zip = input.optBoolean("zip");
    	this.includeAuditLog = input.optBoolean("include_audit_log");
    	this.interpretDATEV = false; //todo:: optString interpret_datev
    }
}

