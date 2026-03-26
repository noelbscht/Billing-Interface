package net.libraya.gobdarchive.service.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;
import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.service.Service;
import net.libraya.gobdarchive.service.web.auth.SessionHelper;
import net.libraya.gobdarchive.service.web.auth.SessionHelper.MessageCategory;
import net.libraya.gobdarchive.service.web.auth.WebPermission;
import net.libraya.gobdarchive.service.web.auth.WebPermissionLoader;
import net.libraya.gobdarchive.service.web.templating.SimpleTemplating;
import net.libraya.gobdarchive.utils.Environment;
import net.libraya.gobdarchive.utils.Unicodes;
import net.libraya.gobdarchive.utils.exception.TemplatingException;

public class WebServer extends Service {

	private WebRouter router;
	
	private WebPermissionLoader permissionLoader;
	
    private final Path webRoot;
    private final Path staticDir;
    private final Path templatesDir;

    public WebServer() {
    	super("WebInterface", Environment.WP_ENABLED, Environment.WP_PORT);
    	
    	try {
			this.permissionLoader = new WebPermissionLoader();
		} catch (IOException e) {
			log("Failed to load permission configuration.");
		}
    	
        this.webRoot = Path.of(System.getProperty("user.dir"), "web");
        this.staticDir = webRoot.resolve("static");
        this.templatesDir = webRoot.resolve("templates");
        
        try {
			loadWebroot();
		} catch (IOException e) {
			System.err.println("Error while loading webroot.");
			return;
		}
        // register routes
        this.router = new WebRouter(this);
    }
    
    /**
     * move frontend- template to web-root directory if it is empty or doesn't exist.
     * */
    public void loadWebroot() throws IOException {
    	if (!Files.exists(this.webRoot) || this.webRoot.toFile().listFiles(File::isDirectory).length == 0) {
    		log("awfawf aww wa f");
    		Files.createDirectories(this.webRoot);
    		
    		try {
				copyDefaultFolder("resources/web-default/static", this.staticDir);
				copyDefaultFolder("resources/web-default/templates", this.templatesDir);
				log("Default web template deployed to: " + this.webRoot.toAbsolutePath());
			} catch (URISyntaxException e) {
				log("Invalid webroot path.");
			} catch (Exception e) {
				log("Failed to fully load webroot.");
			}
    	}
    }
    
    private void copyDefaultFolder(String resourceFolder, Path targetDir) throws IOException, URISyntaxException {
        Files.createDirectories(targetDir);

        Path basePath = Paths.get(
        	    getClass().getProtectionDomain()
        	        .getCodeSource()
        	        .getLocation()
        	        .toURI()
        	);
        
        // file protocol (i.e. inside the ide)
        if (Files.isDirectory(basePath)) {
            Path source = basePath.resolve(resourceFolder);
            
            if (!Files.exists(source)) {
            	log("WARN: Resource folder not found: " + resourceFolder);
                return;
            }

            try (var stream = Files.walk(source)) {
                stream.forEach(path -> copyRecursive(source, path, targetDir));
            }

        } else { // jar protocol
        	try (FileSystem fs = FileSystems.newFileSystem(basePath, (ClassLoader) null)) {
        		Path source = fs.getPath("/" + resourceFolder);

                if (!Files.exists(source)) {
                    log("WARN: Resource folder not found in JAR: " + resourceFolder);
                    return;
                }
                
                try (var stream = Files.walk(source)) {
                    stream.forEach(path -> copyRecursive(source, path, targetDir));
                }
            }
        }
    }
    
    private void copyRecursive(Path root, Path current, Path targetDir) {
        try {
            Path relative = root.relativize(current);
            // JAR-paths: .toString() to correct path
            Path target = targetDir.resolve(relative.toString());

            if (Files.isDirectory(current)) {
                Files.createDirectories(target);
            } else if (!Files.exists(target)) {
                Files.copy(current, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log("Error copying: " + current);
        }
    }
    
	@Override
    public Response serve(IHTTPSession session) {		
		SessionHelper sessionHelper = new SessionHelper(permissionLoader, session);
        String uri = session.getUri();
        String datetimePrefix = "[" + 
        	    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        	        .withLocale(Locale.getDefault())
        	        .format(java.time.LocalDateTime.now()) 
        	    + "] ";
        
        log(datetimePrefix + session.getRemoteIpAddress() + " - " + session.getMethod() + " " + uri);
        
        // serve favicon
        if (uri.equals("/favicon.ico")) {
        	return serveStaticFile("favicon.ico");
        }
        
        // serve static files
        if (uri.startsWith("/static/")) {
            return serveStaticFile(uri.substring("/static/".length()));
        }

        // parse body
        Map<String, String> files = new HashMap<>();
        try {
        	if (session.getMethod() == Method.POST || session.getMethod() == Method.PUT) {
        		session.parseBody(files);
        	}
        } catch (Exception e) {
            return json(400, new JSONObject().put("error", "Invalid request body"));
        }
        String body = files.get("postData");

        // route matching
        WebRoute handler = this.router.match(uri);
        Response r;
        try {
        	
        	// not found
        	if (handler == null) {
        		String parameter = "?req=" + URLEncoder.encode(uri, "UTF-8"); 
        		r = notFound(uri + parameter, session, sessionHelper);
            } else if (!handler.supportsMethod(session.getMethod())) { // unsupported method
        		JSONObject response = new JSONObject();
        		response.put("error", "unsupported method");
        		
        		r = json(405, response);
        		
        	} else if (!isAuthorized(session, handler, sessionHelper)) { // not authorized
                // redirect to login
        		String loginPath = router.match("login").getRule();
        		if (!loginPath.startsWith("/")) loginPath = "/" + loginPath;
        		
        		if (!sessionHelper.isLoggedIn()) {
        			String targetURL = loginPath + "?req=" + URLEncoder.encode(uri, "UTF-8"); 
            		sessionHelper.addMessage(MessageCategory.INFO, "Logge dich ein, um weitergeleitet zu werden");
            		r = redirect(targetURL, session);
        		} else { // logged in but still not authorized
        			String parameter = "?req=" + URLEncoder.encode(uri, "UTF-8"); 
            		r = notAuthorized("/not_authorized" + parameter, session, sessionHelper);
        		}
            } else {
            	r = handler.onRequest(session, body, sessionHelper); // requested route
            }
        	
        	// serve response
        	sessionHelper.finalizeResponse(r);
            return r;
        } catch (Exception e) {
        	JSONObject response = new JSONObject();
        	if (e instanceof SQLException) {
        		response.put("error", "authentication error");
        	} else {
        		response.put("error", e.getMessage());
        	}
        	
        	log("[RESPONSE ERROR] " + session.getRemoteIpAddress() + " - " + response.toString() +  ":" + e.getMessage());
        	return json(400, response);
        }
    }
    
    /**
     * return if the UID cookie provides all required permissions.
     * */
    private boolean isAuthorized(IHTTPSession session, WebRoute handler, SessionHelper sessionHelper) throws IOException, SQLException {
    	WebPermission[] permissions = handler.getPermissions();
    	
    	if (permissions == null || permissions.length == 0) {
            return true; // no permissions required
        }
    	
    	// read uid from session cookie
    	JSONObject cookie = sessionHelper.getSessionData();
    	if (cookie == null) {
    	    return false;
    	}

    	String identifier = cookie.optString("uid", null);
    	if (identifier == null) {
    	    return false;
    	}
        
        // check if all permissions provided
        for (WebPermission action : permissions) {
            if (permissionLoader.isAuthorized(identifier, action)) {
                return true;
            }
        }
               	
    	return false;
    }
    
    protected static Response json(int status, JSONObject obj) {
        return newFixedLengthResponse(
            Response.Status.lookup(status),
            "application/json",
            obj.toString()
        );
    }
    
    public Response serveStaticFile(String relativePath) {
        Path path = staticDir.resolve(relativePath.replace("/", FileSystems.getDefault().getSeparator())).normalize();
        
        if (!Files.exists(path) || !path.startsWith(staticDir) || relativePath.isBlank()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Static file not found");
        }

        try {
        	String mime = Files.probeContentType(path);
        	File file = path.toFile();
            return newFixedLengthResponse(Response.Status.OK, mime, new FileInputStream(file), file.length());
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error reading static file");
        }
    }
    
    public Response serveTemplate(String name, IHTTPSession session, SessionHelper sessionHelper) throws Exception {
    	return serveTemplate(name, session, new SimpleTemplating(this, sessionHelper));// empty templating set (except for default-configurations)
    }

    
    /**
     * serve template file with specified template loader (including basics).
     * */
    public Response serveTemplate(String name, IHTTPSession session, SimpleTemplating loader) throws TemplatingException {
        Path path = templatesDir.resolve(name).normalize();
        
        if (!Files.exists(path) || !path.startsWith(templatesDir)) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Template not found: " + name);
        }
        
        // add defaults (always included)
 		try {
 			loader.addDefaults(session);
 		} catch (Exception e) {
 			throw new TemplatingException("Failed to load templating defaults: " + e.getMessage());
 		}
        
        try {
        	String html = loader.render(path); 
        	Response r = newFixedLengthResponse(Response.Status.OK, "text/html", html); // no content
     		//r.setData(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8))); // set content
     		
        	return r;
        } catch (IOException | TemplatingException e) {
        	log("[TEMPLATE ERROR] " + name +  ":" + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error reading template: " + name);
        }
    }
    
    public Response notFound(String url, IHTTPSession session, SessionHelper sessionHelper) throws Exception {
        Response resp = router.match("/not_found").onRequest(session, url, sessionHelper);
        
        resp.setStatus(Response.Status.NOT_FOUND);
        
        return resp;
    }

    public Response notAuthorized(String url, IHTTPSession session, SessionHelper sessionHelper) throws Exception {
    	 Response resp = router.match("/not_authorized").onRequest(session, url, sessionHelper);
         
         resp.setStatus(Response.Status.UNAUTHORIZED);
         
         return resp;
    }
    
    public Response redirect(String url, IHTTPSession session) {
        Response r = newFixedLengthResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "");
        
        r.addHeader("Location", url);
        
        // disable browser caching
        r.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        r.addHeader("Pragma", "no-cache");
        r.addHeader("Expires", "0");
        
        return r;
    }
    
    public WebRouter getRouter() {
		return router;
	}
    
    public WebPermissionLoader getPermissionLoader() {
		return permissionLoader;
	}
    
    public Path getWebRoot() {
		return webRoot;
	}
    
    public Path getStaticDir() {
		return staticDir;
	}
    
    public Path getTemplatesDir() {
		return templatesDir;
	}

    public void sendRoutes() {
    	ArrayList<String> feedback = new ArrayList<>();
		
		feedback.add(Unicodes.VERTICAL_BAR + " API Routes");
		for (WebRoute r : this.getRouter().getRoutes())  {
			feedback.add(r.getRule() + " " + Unicodes.EM_DASH + " permissions: " + r.getPermissions().toString().split(", "));
		}
		
		Main.sendFeedback(feedback.toArray(new String[0]));
    }
}

