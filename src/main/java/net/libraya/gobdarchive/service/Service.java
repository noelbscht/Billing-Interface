package net.libraya.gobdarchive.service;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import net.libraya.gobdarchive.utils.exception.ServiceException;

public class Service extends NanoHTTPD {
	
	private final String title;
	private final boolean allowed;
	
	protected final ServiceLogger logger;

	
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
	
	public String getTitle() {
		
		return title;
	}
	
	public boolean isAllowed() {
		return allowed;
	}
}
