package net.libraya.gobdarchive.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import fi.iki.elonen.NanoHTTPD;
import net.libraya.gobdarchive.utils.exception.ServiceException;

public class Service extends NanoHTTPD {
	
	private final String title;
	private final boolean allowed;
	
	protected final ServiceLogger logger;
	
	private final Path tempFileDir = Path.of(System.getProperty("user.dir")).resolve("temp");
	
	public Service(String title, boolean allowed, int port) {
		super(port);
		this.title = title;
		this.allowed = allowed;
		this.logger = new ServiceLogger();
	}
	
	protected void initialize() throws IOException, ServiceException {
		if (!this.allowed) {
        	throw new ServiceException("Service " + this.title + " is disabled. Configuration: .env");
        }
		
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        logger.log(this.title + " listening on http://127.0.0.1:" + this.getListeningPort());
	}
	
	public void log(String msg) {
		this.logger.log(msg);
	}
	
	public boolean hasChanged() {
	    return logger.hasChanged();
	}

	public void renderLog() {
	    logger.renderLog(this.title, this.getListeningPort());
	}

	public void markRendered() {
	    logger.markRendered();
	}
	
	/**
     * takes a NanoHTTPD temporary file, copies it into the temp-directory,
     * and returns the new path. (NanoHTTPD deletes the original temp file)
     * */
    public Path takeOverTempFile(Path tempFile) {
    	try {
    		
            // ensure file exists
            if (tempFile == null || !Files.exists(tempFile)) {
                throw new IllegalArgumentException("Temp file does not exist: " + tempFile);
            }

            // ensure this is a NanoHTTPD temporary file
            String filename = tempFile.getFileName().toString();
            String systemTempDir = System.getProperty("java.io.tmpdir");

            boolean isNanoTemp =
                    filename.startsWith("NanoHTTPD-") &&
                    tempFile.toAbsolutePath().toString().startsWith(systemTempDir);

            if (!isNanoTemp) {
                throw new SecurityException(
                    "Refusing to copy/delete non-NanoHTTPD temp file: " + tempFile
                );
            }
            
            // ensure temp directory exists
            Files.createDirectories(this.tempFileDir);
            
            // create target path
            Path target = this.tempFileDir.resolve(tempFile.getFileName().toString());

            // copy file
            Files.copy(tempFile, target);

            return target;

        } catch (Exception e) {
            throw new RuntimeException("Failed to copy temp file: " + tempFile, e);
        }
    }
	
	public String getTitle() {
		
		return title;
	}
	
	public boolean isAllowed() {
		return allowed;
	}
}
