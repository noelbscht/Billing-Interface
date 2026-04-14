package net.libraya.gobdarchive.service.web.auth;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import net.libraya.gobdarchive.utils.SQL;

/**
 * A simple addition to the SessionHelper class, to store and manage sessions
 * */
public class StoredSessions {
	
	/**
	 * auto-initialize to create table if needed.
	 * */
	static {
		try { 
			initialize();
		} catch (Exception e) {
			System.err.println("Couldn't create permission table: sessions");
		}
	}
	
	private static void initialize() throws SQLException {
		// create session table if missing
		try (Connection conn = SQL.getConnection(); 
				Statement st = conn.createStatement()) {

	            st.execute("""
	                CREATE TABLE IF NOT EXISTS sessions (
	                    uid VARCHAR(38) PRIMARY KEY NOT NULL,
	                    user_uid TEXT NOT NULL,
	                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	                    expires_at BIGINT NOT NULL,
	                    last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	                    last_ip TEXT NOT NULL
	                );
	            """);

		};
	}
	
	/**
	 * creates new session and returns sessionUId.
	 * */
	protected String set(String userUId, long expiresAt, String lastIP) throws SQLException {
		String sessionUId = UUID.randomUUID().toString();

		SQL.query("INSERT INTO sessions (uid, user_uid, expires_at, last_ip) VALUES (?, ?, ?, ?)", sessionUId, userUId, expiresAt, lastIP);
		
		return sessionUId;
	}
	
	/**
	 * updates current session.
	 * */
	protected void update(String sessionUId, String lastIP) throws SQLException {
		SQL.query("UPDATE sessions SET last_ip = ? WHERE uid = ?", lastIP, sessionUId);
	}
	
	/**
	 * delete session by sessionUId
	 * */
	protected void delete(String sessionUId) throws SQLException {
		SQL.query("DELETE FROM sessions WHERE uid = ?", sessionUId);
	}
	
	/**
	 * get session entry from table
	 * */
	protected Map<String, Object> getSession(String sessionUId) throws SQLException {
		return SQL.query("SELECT user_uid FROM sessions WHERE uid = ?", sessionUId);
	}
	
	/**
	 * get all sessions by userId
	 * */
	protected Map<String, Object> getUserSessions(String userUId) throws SQLException {
		return SQL.query("SELECT * FROM sessions WHERE user_uid = ?", userUId);
	}
	
	/**
	 * check by sessionUId if session is expired.
	 * */
	protected boolean isExpired(String sessionUId) throws SQLException {
        Map<String, Object> row = SQL.query("SELECT expires_at FROM sessions WHERE uid = ?", sessionUId);
        if (row == null) return true;

        long expiresAt = ((Number) row.get("expires_at")).longValue();
        return Instant.now().toEpochMilli() > expiresAt;
    }
}
