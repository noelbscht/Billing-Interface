package net.libraya.gobdarchive.service.web.templating;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.libraya.gobdarchive.utils.exception.TemplatingException;

public class CachedTemplate {
	
	private final String rawContent;
	private long lastModified;
	
	public CachedTemplate(Path path, String rawContent) throws TemplatingException {
		try {
			this.lastModified = Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			this.lastModified = 0;
			throw new TemplatingException("Error while determining file age.");
		}
		this.rawContent = rawContent;
	}
	
	public String getRawContent() {
		return rawContent;
	}
	
	public long getLastModified() {
		return lastModified;
	}
}
