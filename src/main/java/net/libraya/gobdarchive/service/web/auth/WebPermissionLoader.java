package net.libraya.gobdarchive.service.web.auth;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;

import org.json.JSONObject;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.utils.HashUtil;
import net.libraya.gobdarchive.utils.SQL;
import net.libraya.gobdarchive.utils.exception.ServiceException;

public class WebPermissionLoader {
	
	private JSONObject tableColumns = Main.getConfigurations().tableColumnsCfg.getContent();
	
	private JSONObject permissions = Main.getConfigurations().permissionsCfg.getContent();
	
	public WebPermissionLoader() {}
	
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
