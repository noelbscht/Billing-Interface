package net.libraya.gobdarchive.service.web.auth;

public enum WebPermission {
	// archive access
	ARCHIVE_READ, 		// list, log, show, query, export
	ARCHIVE_WRITE,		// commit, verify (+ READ)
	
	// system
	SYSTEM_MANAGE_USERS,
	SYSTEM_MANAGE_PERMISSIONS,
	
	// documentation
	VIEW_GOBD_DOCS,
    VIEW_API_DOCS,

}
