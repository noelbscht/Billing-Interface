package net.libraya.gobdarchive.utils.config;

import java.io.IOException;
import java.nio.file.Path;

import org.json.JSONObject;

import net.libraya.gobdarchive.service.web.auth.WebPermission;
import net.libraya.gobdarchive.utils.FilesUtil;
import net.libraya.gobdarchive.utils.exception.ConfigurationException;

/**
 * A class to organize and summarize configuration files.
 * */
public class Configurations {
	
	// metadata 
	public final Configuration customMetadataCfg;
	
	// web permission loader
	public final Configuration tableColumnsCfg;
	public final Configuration permissionsCfg;
	
	public Configurations() throws ConfigurationException {
		// metadata configurations
		this.customMetadataCfg =  new Configuration(
				Path.of(System.getProperty("user.dir"), "requirements",  "custom-metadata.json")) {
			
			@Override
			public void writePreset() throws IOException {
				JSONObject obj = new JSONObject();
	            String[] requirements = new String[] {
	            		"user_reference_id", // default, example references
	            		"stripe_bill_url"
	            };
	            
	            obj.put("requirements", requirements);
	            FilesUtil.writeString(getPath(), obj.toString(4), new String[] {
	            		// commentaries
	            		"A list to manually enforce requirements for commits.",
	            		"",
	            		"The following keys must be added manually, regardless if",
	            		"by api usage, or by typing them in the web interface."
	            });
			}
		};
		
		// permission loader configurations
		this.tableColumnsCfg = new Configuration(Path.of(System.getProperty("user.dir"), "web",  "auth-table-columns.json")) {
			
			@Override
			public void writePreset() throws IOException {
	            JSONObject table = new JSONObject();
	            
	            // add default column names
	            table.put("table-name", "users");
	            table.put("hashing-instance", "sha-256");
	            table.put("primary-key", "id");
	            table.put("email-column", "email");
	            table.put("password-column", "password");
	            table.put("permission-identifier", "group_id");
	            
	            // write data to column configuration.
	            FilesUtil.writeString(getPath(), table.toString(4), new String[] {
	            		// commentaries
	            		"Configuration for modifying authentication table column definitions."
	            });
			}
		};
		
		this.permissionsCfg = new Configuration(Path.of(System.getProperty("user.dir"), "web",  "permissions.json")) {
			
			@Override
			public void writePreset() throws IOException {
				JSONObject perms = new JSONObject();
	            
	            // add default permission levels (visitor, administrator)
	            JSONObject auditor = new JSONObject();
	            for (WebPermission wp : WebPermission.values()) {
	            	auditor.put(wp.name().toLowerCase(), false);
	            }
	            
	            JSONObject admin = new JSONObject();
	            for (WebPermission wp : WebPermission.values()) {
	            	admin.put(wp.name().toLowerCase(), true);
	            }
	            
	            perms.put("0", auditor);
	            perms.put("1", admin);
	            
	            // write data to permission configuration.
	            FilesUtil.writeString(getPath(), perms.toString(4), new String[] {
	            		// commentaries
	            		"A configuration to adjust user permissions via configured permission identifiers.",
	            		"",
	            		"default configuration:",
	            		"	0 - auditor permission identifier",
	            		"	1 - administrator permission identifier"
	            });
			}
		};
	}
}
