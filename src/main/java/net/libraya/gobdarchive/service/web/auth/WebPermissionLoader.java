package net.libraya.gobdarchive.service.web.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;

import org.json.JSONObject;

import net.libraya.gobdarchive.utils.FilesUtil;
import net.libraya.gobdarchive.utils.HashUtil;
import net.libraya.gobdarchive.utils.SQL;
import net.libraya.gobdarchive.utils.exception.ServiceException;

public class WebPermissionLoader {
	
	private final Path tableColumnPath;
	private JSONObject tableColumns;
	
	private final Path permissionsPath;
	private JSONObject permissions;
	
	public WebPermissionLoader() throws IOException {
		this.tableColumnPath = Path.of(System.getProperty("user.dir"), "web",  "auth-table-columns.json");
		this.permissionsPath = Path.of(System.getProperty("user.dir"), "web",  "permissions.json");
		
		try {
			loadConfigurations();
		} catch (IOException e) {
			throw new IOException("Error while loading metadata requirements: ", e);
		}
	}
	
	/**
	 * Load configuration for permission handling, table structure etc.
	 * */
	private void loadConfigurations() throws IOException {
		// create defaults
        if (!Files.exists(tableColumnPath)) {
            Files.createDirectories(tableColumnPath.getParent());
            JSONObject table = new JSONObject();
            
            // add default column names
            table.put("table-name", "users");
            table.put("hashing-instance", "sha-256");
            table.put("primary-key", "id");
            table.put("email-column", "email");
            table.put("password-column", "password");
            table.put("permission-identifier", "group_id");
            
            // write data to column configuration.
            FilesUtil.writeString(tableColumnPath, table.toString(4), new String[] {
            		// commentaries
            		"Configuration for modifying authentication table column definitions."
            });
        }
        
        if (!Files.exists(permissionsPath)) {
            Files.createDirectories(permissionsPath.getParent());
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
            FilesUtil.writeString(permissionsPath, perms.toString(4), new String[] {
            		// commentaries
            		"A configuration to adjust user permissions via configured permission identifiers.",
            		"",
            		"default configuration:",
            		"	0 - auditor permission identifier",
            		"	1 - administrator permission identifier"
            });
        }
        
        
        // read from files
        this.tableColumns = new JSONObject(FilesUtil.readString(tableColumnPath));
        this.permissions = new JSONObject(FilesUtil.readString(permissionsPath));
    }
	
	/**
	 * return configured table which should contain an identifier and a permission-level column.
	 * */
	public String getTableName() {
		return tableColumns.getString("table-name");
	}
	
	/**
	 * return configured primary key 
	 * */
	public String getPrimaryKeyColumn() {
		return tableColumns.getString("primary-key");
	}
	
    public String getEmailColumn() {
		return tableColumns.getString("email-column");
	}
    
    public String getPasswordColumn() {
		return tableColumns.getString("password-column");
	}
    
    
	/**
	 * return configured column for permission level / which identifies the permission group.
	 * */
	public String getPermissionIdentifierColumn() {
		return tableColumns.getString("permission-identifier");
	}
	
	public String getHashingInstance() {
		return tableColumns.getString("hashing-instance");
	}
	
	/**
	 * returns if user is authorized to perform a specific action.
	 * @throws SQLException 
	 * */
	public boolean isAuthorized(String identifier, WebPermission webAction) throws SQLException {
		Map<String, Object> result = SQL.query(
		        "SELECT " + getPermissionIdentifierColumn() +
		        " FROM " + getTableName() +
		        " WHERE " + getPrimaryKeyColumn() + " = ?",
		        identifier
		    );
		
		if (result == null) {
			return false;
		}
		
		if (!result.containsKey(getPermissionIdentifierColumn())) {
			return false;
		}
		
		String permId = String.valueOf(result.get(getPermissionIdentifierColumn()));
		
		if (result != null && permissions.has(permId)) {
			JSONObject identifiedPerms = permissions.getJSONObject(permId);
			if (identifiedPerms.has(webAction.name().toLowerCase())) {
		    	return identifiedPerms.optBoolean(webAction.name().toLowerCase());
		    }
		}
		return false;
	}
	
	public Map<String, Object> authentication(String email, String password) throws SQLException, ServiceException {
		String hash;
		try {
			hash = HashUtil.hash(password, getHashingInstance());
		} catch (NoSuchAlgorithmException e) {
			throw new ServiceException("Such no hashing instance found: " + getHashingInstance());
		}
		
		Map<String, Object> result = SQL.query(
	        "SELECT * FROM " + getTableName() +
	        " WHERE " + getEmailColumn() + " = ? AND " + getPasswordColumn() + " = ?",
	        email.toLowerCase(), hash
	    );
		
		if (result != null) {
			return result;
		}
		return null;
	}
	
	/**
	 * returns configured authentication table columns.
	 * */
	public JSONObject getTableColumns() {
		return tableColumns;
	}
	
	public JSONObject getPermissions() {
		return permissions;
	}
}
