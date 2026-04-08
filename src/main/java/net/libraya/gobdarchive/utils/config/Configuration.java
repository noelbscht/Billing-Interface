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
	
	public Configuration(Path path) {
		this.path = path;
	}
	
	public void load() throws ConfigurationException {
		try {
			if (Files.exists(path)) {
				String content = FilesUtil.readString(this.path);
				
				if (content.isBlank()) {
					throw new ConfigurationException("Unable to load empty configuration: " + path.toString());
				}
				
				this.content = new JSONObject(content);
			}
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
