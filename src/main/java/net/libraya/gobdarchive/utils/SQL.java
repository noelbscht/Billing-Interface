package net.libraya.gobdarchive.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SQL {
	
	public static String getURL(String host, String database) {
		return "jdbc:mysql://" + host + "/" + database + "?useSSL=true&requireSSL=false&allowPublicKeyRetrieval=true";
	}
	
	public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
    		getURL(Environment.WP_MYSQL_HOST, Environment.WP_MYSQL_DATABASE),
            Environment.WP_MYSQL_USER,
            Environment.WP_MYSQL_PASSWORD
        );
    }

	public static Map<String, Object> query(String sql, Object... params) throws SQLException {
	    try (Connection conn = getConnection();
	         PreparedStatement stmt = conn.prepareStatement(sql)) {

	        for (int i = 0; i < params.length; i++) {
	            stmt.setObject(i + 1, params[i]);
	        }

	        boolean isSelect = sql.trim().toUpperCase().startsWith("SELECT");

	        if (isSelect) {
	            try (ResultSet rs = stmt.executeQuery()) {
	                if (!rs.next()) return null;

	                Map<String, Object> row = new HashMap<>();
	                ResultSetMetaData meta = rs.getMetaData();
	                for (int i = 1; i <= meta.getColumnCount(); i++) {
	                    row.put(meta.getColumnName(i), rs.getObject(i));
	                }
	                return row;
	            }
	        } else {
	            stmt.executeUpdate();
	            return null;
	        }
	    }
	}
}
