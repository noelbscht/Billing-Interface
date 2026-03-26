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
	
	private static Connection getConnection() throws SQLException {
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

	        try (ResultSet rs = stmt.executeQuery()) {
	            if (rs.next()) {
	                Map<String, Object> row = new HashMap<>();
	                ResultSetMetaData metaData = rs.getMetaData();
	                int columnCount = metaData.getColumnCount();

	                for (int i = 1; i <= columnCount; i++) {
	                    row.put(metaData.getColumnName(i), rs.getObject(i));
	                }
	                return row;
	            }
	        }
	        return null;
	    }
	}
}
