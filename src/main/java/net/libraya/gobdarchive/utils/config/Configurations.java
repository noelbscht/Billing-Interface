package net.libraya.gobdarchive.utils.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.service.web.auth.WebPermission;
import net.libraya.gobdarchive.utils.FilesUtil;
import net.libraya.gobdarchive.utils.exception.ConfigurationException;

/**
 * A class to organize and summarize configuration files.
 * */
public class Configurations {
	
	private List<Configuration> configurations = new ArrayList<Configuration>();
	
	// metadata 
	public final Configuration customMetadataCfg;
	
	// web permission loader
	public final Configuration tableColumnsCfg;
	public final Configuration permissionsCfg;
	
	public Configurations() {
		// metadata configurations
		this.customMetadataCfg = addConfig(new Configuration(
				Path.of(System.getProperty("user.dir"), "requirements",  "custom-metadata.json")) {
			
			@Override
			public void writePreset() throws IOException {
				JSONObject obj = new JSONObject();
	            String[] requirements = new String[] {
	            		"stripe_bill_url" // default, example references
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
		});
		
		// permission loader configurations
		this.tableColumnsCfg = addConfig(new Configuration(Path.of(System.getProperty("user.dir"), "web",  "auth-table-columns.json")) {
			
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
		});
		
		this.permissionsCfg = addConfig(new Configuration(Path.of(System.getProperty("user.dir"), "web",  "permissions.json")) {
			
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
		});
	}
	
	/**
	 * initializes all configuration files by writing their preset data and creating missing directories.
	 * @throws ConfigurationException 
	 * */
	public void initialize() throws ConfigurationException {
		boolean interrupt = false;
		List<String> generated = new ArrayList<>();
		
		for (int i = 0; i < configurations.size(); i++) {
			Configuration cfg = configurations.get(i);
			Path path = cfg.getPath();
			
			if (!Files.exists(path)) {
				generated.add(cfg.getPath().toString());
				interrupt = true;
				
	            try {
	            	Files.createDirectories(path.getParent());
	            	cfg.writePreset();
	    		} catch (IOException e) {
	    			throw new ConfigurationException("An error occoured during configuration setup: " + e.getMessage());
	    		}
	        }
			cfg.load();
		}
		
		if (interrupt) {
			Main.sendFeedback(new String[] {
					"At least one configuration file was created.",
					"Look them up and restart.",
					"",
					"Configurations:",
					String.join(", ", generated).replace(System.getProperty("user.dir"), "")
					
			});
			System.exit(1);
		}
	}
	
	private Configuration addConfig(Configuration cfg) {
		this.configurations.add(cfg);
		
		return cfg;
	}
}
