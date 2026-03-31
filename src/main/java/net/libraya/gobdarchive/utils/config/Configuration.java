package net.libraya.gobdarchive.utils.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONException;
import org.json.JSONObject;

import net.libraya.gobdarchive.utils.FilesUtil;
import net.libraya.gobdarchive.utils.exception.ConfigurationException;

public abstract class Configuration {
	
	private Path path;
	
	private JSONObject content;
	
	public Configuration(Path path) throws ConfigurationException {
		this.path = path;
		
		// create defaults
		if (!Files.exists(path)) {
            try {
            	Files.createDirectories(path.getParent());
            	writePreset();
    		} catch (IOException e) {
    			throw new ConfigurationException("An error occoured during configuration setup: " + e.getMessage());
    		}
        }
		
		try {
			this.content = new JSONObject(FilesUtil.readString(this.path));
		} catch (JSONException | IOException e) {
			throw new ConfigurationException("Unable to load configuration content: " + path.toString());
		}
	}
	
	public abstract void writePreset() throws IOException;
	
	public JSONObject getContent() {
		return this.content;
	}
	
	public Path getPath() {
		return path;
	}
}
