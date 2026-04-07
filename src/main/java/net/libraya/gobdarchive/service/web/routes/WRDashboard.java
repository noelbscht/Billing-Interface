package net.libraya.gobdarchive.service.web.routes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.archive.ArchiveManager;
import net.libraya.gobdarchive.archive.CommitType;
import net.libraya.gobdarchive.archive.EntryType;
import net.libraya.gobdarchive.archive.Metadata;
import net.libraya.gobdarchive.archive.VerificationResult;
import net.libraya.gobdarchive.archive.option.ExportOptions;
import net.libraya.gobdarchive.archive.option.LogDetails;
import net.libraya.gobdarchive.archive.option.QueryFilterOptions;
import net.libraya.gobdarchive.service.web.WebRoute;
import net.libraya.gobdarchive.service.web.WebServer;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.WebPermission;
import net.libraya.gobdarchive.service.web.templating.SimpleTemplating;
import net.libraya.gobdarchive.utils.Environment;

public class WRDashboard extends WebRoute {
	
	public WRDashboard(WebServer ws) {
        super(ws, "/dashboard", new String[] { "GET", "POST" } , new WebPermission[] { WebPermission.ARCHIVE_READ });
    }

	@Override
    public Response onRequest(IHTTPSession session, String body, HashMap<String, String> files, SessionHelper sessionHelper) throws Exception {
		SimpleTemplating templating = new SimpleTemplating(ws, sessionHelper);
		ArchiveManager arch = Main.getArchiveManager();
		
		ArrayList<String> archiveEntries = arch.listArchiveEntries();
		List<String> entryTypes = Arrays.stream(EntryType.values()).map(e -> "\"" + e.name() + "\"").toList(); // entry types as string arguments
		JSONArray customMetadataRequirements = Main.getConfigurations().customMetadataCfg.getContent().optJSONArray("requirements");
		
		// add variables
		templating.addVariable("entries", archiveEntries);
		templating.addVariable("entryTypes", entryTypes);
		templating.addVariable("customRequirementsForced", Environment.CUSTOM_REQUIREMENTS_FORCED);
		templating.addVariable("customRequirements", customMetadataRequirements);
		
		// post requests
		if (session.getMethod() == Method.POST) {
			Map<String, String> args = sessionHelper.getParameters();
			JSONObject response = new JSONObject();
			
			// user info
			String uid = sessionHelper.getSessionData().optString("uid");
			String ipv4 = session.getRemoteIpAddress();
			String userAgent = session.getHeaders().getOrDefault("user-agent", "/");
			
			String action = args.containsKey("action") ? args.get("action") : "";
			String archiveId = args.containsKey("archive_id") ? args.get("archive_id") : null;
			String filter = args.containsKey("filter") ? args.get("filter") : "{}";
			String exportOptions = args.containsKey("export_options") ? args.get("export_options") : "{}";
			
			// check if all args are applicable
			if (!isApplicable(args)) {
				response.put("message", "Unapplicable args provided.");
				return ws.json(400, response);
			}
			
			switch (action) {
				case "commit": { // commit
					if (!ws.getPermissionLoader().isAuthorized(uid, WebPermission.ARCHIVE_WRITE)) {
						response.put("message", "not authorized");
						return ws.json(401, response);
					}
					
					String tempFilePath = files.get("file");
					String filename = sessionHelper.getParameters().get("file");
					Path storedFile = ws.takeOverTempFile(Path.of(tempFilePath));
					
				    // Metadata
				    JSONObject metaJSON = new JSONObject(args.get("metadata"));
				    String entryType = metaJSON.optString("entry_type");

			    	Metadata metadata = new Metadata(EntryType.valueOf(entryType), CommitType.WEBINTERFACE, storedFile, metaJSON, filename);
				    
				    arch.commit(metadata, uid, new LogDetails(Map.of(
							LogDetails.USER_IP, ipv4,
							LogDetails.USER_AGENT, userAgent)));
				    
				    // delete copy of temp file
				    Files.deleteIfExists(storedFile);
				    
				    return ws.json(200, new JSONObject().put("message", "ok"));
				}
				case "view": { // view
					Path[] fileset = arch.getEntryDataset(archiveId, uid, new LogDetails(Map.of(
							LogDetails.USER_IP, ipv4,
							LogDetails.USER_AGENT, userAgent)));
					
					Path originalFile = fileset[0];
					Path metadataFile = fileset[1];
					
					byte[] fileBytes = Files.readAllBytes(originalFile);
					byte[] metaBytes = Files.readAllBytes(metadataFile);

					String fileBase64 = Base64.getEncoder().encodeToString(fileBytes);
					String metaBase64 = Base64.getEncoder().encodeToString(metaBytes);

					String mime = Files.probeContentType(originalFile);

					response.put("filename", originalFile.getFileName().toString());
					response.put("mime", mime);
					response.put("file", fileBase64);
					response.put("metadata", metaBase64);

					return ws.json(200, response);
				}
				case "verify": { // verify
					VerificationResult vr = arch.verify(archiveId, uid, new LogDetails(Map.of(
							LogDetails.USER_IP, ipv4,
							LogDetails.USER_AGENT, userAgent)));
					
					response.put("status", vr.isSuccess() ? "success" : "error");
					if (!vr.isSuccess()) {
						response.put("errors", vr.getErrors().toString());
					}
					return vr.isSuccess() ? ws.json(200, response) : ws.json(400, response);
				}
				case "exportSingle": { // exportSingle
					if (!ws.getPermissionLoader().isAuthorized(uid, WebPermission.ARCHIVE_WRITE)) {
						response.put("message", "not authorized");
						return ws.json(401, response);
					}
					
					Path zipFile = arch.exportSingle(archiveId, uid, new LogDetails(Map.of(
							LogDetails.USER_IP, ipv4,
							LogDetails.USER_AGENT, userAgent,
							LogDetails.EXPORT_OPTIONS, exportOptions)), new ExportOptions(new JSONObject(exportOptions)));

					return WebServer.newChunkedResponse(Response.Status.OK, "application/zip", Files.newInputStream(zipFile));
				}
				case "export": { // export (filtered or all)
					if (!ws.getPermissionLoader().isAuthorized(uid, WebPermission.ARCHIVE_WRITE)) {
						response.put("message", "not authorized");
						return ws.json(401, response);
					}
				
					LogDetails details = new LogDetails(Map.of(
							LogDetails.USER_IP, ipv4,
							LogDetails.USER_AGENT, userAgent,
							LogDetails.EXPORT_OPTIONS, exportOptions,
							LogDetails.FILTER_OPTIONS, filter));
					Path zipFile = filter.equals("{}") ? zipFile = arch.exportAll(
							new ExportOptions(new JSONObject(exportOptions)), uid, details) 
							: arch.exportFiltered(new QueryFilterOptions(new JSONObject(filter)), 
							new ExportOptions(new JSONObject(exportOptions)), uid, details);
					return WebServer.newChunkedResponse(Response.Status.OK, "application/zip", Files.newInputStream(zipFile));
				}
				case "query": { // query
					try {
						List<JSONObject> result = arch.query(new QueryFilterOptions(new JSONObject(filter)));
						response.put("result", result);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					return ws.json(200, response);
				}
				default:
					// unavailable action
					response.clear();
					response.put("message", "Bad request");
					return ws.json(400, response);
			}
		}
		
		//todo:: add needed variables like 'backupsys', add ws.isAuthorized function to templating, adapt dashboard.html
		
		return ws.serveTemplate("dashboard.html", session, templating);
    }
	
	/**
	 * returns if all served arguments are applicable. (non-existent or blank arguments will return false) 
	 * */
	private boolean isApplicable(Object... args) {
		for (Object arg : args) {
			if (arg == null) return false;
			if (arg.toString().isBlank()) return false;
		}
		return true;
	}
}
