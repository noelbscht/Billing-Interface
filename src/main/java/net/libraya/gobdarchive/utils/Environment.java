package net.libraya.gobdarchive.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.cdimascio.dotenv.Dotenv;

public class Environment {
	
	// enviroment file
	private static final Dotenv DOTENV = Dotenv.load();
	
	/*
	 * STATIC CONFIG VALUES
	 * */
	
	// ArchiveManager
	public static final Path ARCHIVE_ROOT = getPath("ARCHIVE_ROOT_PATH", "/var/lib/libraya-billing/archive");
	public static final Path LOG_FILE = getPath("LOG_FILE_PATH", "/var/lib/libraya-billing" + "/audit.log");
	
	// Metadata
	public static final boolean CUSTOM_REQUIREMENTS_FORCED = getBoolean("CUSTOM_REQUIREMENTS_FORCED", true);
	
	// API (auth_key not declared as static value for safety reasons)
	public static final boolean API_ENABLED = getBoolean("API_ENABLED", false);
	public static final int API_PORT = getInt("API_PORT", 8080);
	
	// WEB PAGE (front end interface)
	public static final String BASE_URL = getString("BASE_URL", "http://127.0.0.1");
	public static final boolean WP_ENABLED = getBoolean("WEBPAGE_ENABLED", false);
	public static final int WP_PORT = getInt("WEBPAGE_PORT", 9191);
	public static final String WP_MYSQL_DATABASE = getString("WEBPAGE_MYSQL_DATABASE", "billinginterface");
	public static final String WP_MYSQL_HOST = getString("WEBPAGE_MYSQL_HOST", "127.0.0.1");
	public static final String WP_MYSQL_USER = getString("WEBPAGE_MYSQL_USER", "root");
	public static final String WP_MYSQL_PASSWORD = getString("WEBPAGE_MYSQL_PASSWORD", "");
	
	// private constructor to prevent double initialization 
	private Environment() {}
	
	/*
	 * helper functions
	 * */
	
	
	private static String getenv(String name, String def) {
		String result = DOTENV.get(name);
		
		return result != null ? result : def;
	}
	
	public static String getString(String name, String def) {
		return getenv(name, def);
	}
	
	public static boolean getBoolean(String name, boolean def) {
		return Boolean.parseBoolean(getenv(name, String.valueOf(def)));
	}
	
	public static int getInt(String name, int def) {
		return Integer.parseInt(getenv(name, String.valueOf(def)));
	}
	
	public static float getFloat(String name, float def) {
		return Float.parseFloat(getenv(name, String.valueOf(def)));
	}
	
	public static Path getPath(String name, String def) {
		return Paths.get(getenv(name, def));
	}
}
