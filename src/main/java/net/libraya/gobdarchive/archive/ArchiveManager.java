package net.libraya.gobdarchive.archive;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import net.libraya.gobdarchive.archive.option.ExportOptions;
import net.libraya.gobdarchive.archive.option.LogDetails;
import net.libraya.gobdarchive.archive.option.QueryFilterOptions;
import net.libraya.gobdarchive.utils.Environment;
import net.libraya.gobdarchive.utils.HashUtil;
import net.libraya.gobdarchive.utils.ZipUtil;
import net.libraya.gobdarchive.utils.exception.MetadataException;

public class ArchiveManager {
	
	public ArchiveManager() {
		
	}
	
	/**
	 * adds an entry to the archive including metadata.
	 * */
	public void commit(Metadata metadata, String actorUId, LogDetails details) throws Exception {
		// set final timestamp
	    metadata.timestamp = getISOTimestamp();
		
		if (metadata.isCustomForced()) {
			if (!metadata.isFilledUp()) {
				throw new MetadataException("Required metadata fields are missing. \n" + metadata.getCustomRequirementKeys());
			}
		}
		
		JSONObject requirements = metadata.getFinalMetadata();
		
	    // archive path
	    Path targetDir = generateArchivePath(metadata.archiveId, metadata.type, metadata.timestamp);
	    Files.createDirectories(targetDir);

	    // copy file
	    Path targetFile = targetDir.resolve(metadata.filename);
	    Files.copy(metadata.path, targetFile);
	    
	    // write metadata.json
	    Files.writeString(targetDir.resolve("metadata.json"), requirements.toString(2));

	    // write hash.txt
	    Files.writeString(targetDir.resolve("hash.txt"), metadata.hash);

	    // write log
	    if (details.getDetails().containsKey(LogDetails.RELATION_KEY)) {
	    	writeLogEntry(LogAction.COMMIT_UPDATE, metadata.timestamp, metadata.archiveId, actorUId, metadata.hash, LogActionStatus.SUCCESS, details);
	    } else {
	    	writeLogEntry(LogAction.COMMIT_CREATE, metadata.timestamp, metadata.archiveId, actorUId, metadata.hash, LogActionStatus.SUCCESS, details);
	    }

	    // set immutable
	    makeImmutable(targetDir);
	    makeImmutable(targetFile);
	    makeImmutable(targetDir.resolve("metadata.json"));
	    makeImmutable(targetDir.resolve("hash.txt"));
	}

	/**
	 * returns current ISO timestamp.
	 * */
	private String getISOTimestamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
	}
	
	/**
	 * returns VerificationResult class, which provides possible errors during this function to test if the verification / validation succeed.
	 * */
	public VerificationResult verify(String archiveId, String actorUId, LogDetails details) throws Exception {
	    VerificationResult result = new VerificationResult();
	    
	   archiveId = formatArchiveId(archiveId);
	   
	    Path archiveFolder = findArchiveFolder(archiveId);
	    if (archiveFolder == null) {
	        result.addError("Archive entry not found: " + archiveId);
	        return result;
	    }

	    // metadata.json
	    Path metadataFile = archiveFolder.resolve("metadata.json");
	    if (!Files.exists(metadataFile)) {
	        result.addError("Missing metadata.json");
	        return result;
	    }

	    JSONObject metadata = new JSONObject(Files.readString(metadataFile));
	    String storedHash = metadata.getString("hash");
	    String originalFileName = metadata.getString("original_file_name");
	    String archiveIdFromMeta = metadata.getString("archive_id");


	    // hash.txt
        Path hashFile = archiveFolder.resolve("hash.txt");
        if (!Files.exists(hashFile)) {
            result.addError("Missing hash.txt");
            return result;
        }

        String hashTxt = Files.readString(hashFile).trim();

        // archived file
        Path archivedFile = archiveFolder.resolve(originalFileName);
        if (!Files.exists(archivedFile)) {
            result.addError("Archived file missing: " + archivedFile);
            return result;
        }

        String recalculatedHash = HashUtil.sha256(archivedFile);

        if (!storedHash.equals(recalculatedHash)) {
            result.addError("Hash mismatch: metadata.json != recalculated");
        }

        if (!hashTxt.equals(recalculatedHash)) {
            result.addError("Hash mismatch: hash.txt != recalculated");
        }

        // logfile: check entry for this archiveId
        boolean logEntryFound = false;
        for (String line : Files.readAllLines(Environment.LOG_FILE)) {
        	if (line.contains("| archiveId=" + archiveIdFromMeta + " |")) {
                logEntryFound = true;

                if (!line.contains("| sha256=" + recalculatedHash + " |")) {
                    result.addError("Log entry hash mismatch");
                }
                break;
            }
        }

        if (!logEntryFound) {
            result.addError("No log entry found for archiveId: " + archiveIdFromMeta);
        }

        // verify log hash chain
        String lastHash = "000000"; // Genesis-Hash

        for (String line : Files.readAllLines(Environment.LOG_FILE)) {

            int idx = line.lastIndexOf("prevHash=");
            if (idx == -1) {
                result.addError("Log entry missing prevHash: " + line);
                continue;
            }

            String prevHash = line.substring(idx + 9).trim();

            if (!prevHash.equals(lastHash)) {
                result.addError("Log hash chain broken at: " + line);
            }

            String contentWithoutPrev = line.substring(0, idx).trim();
            lastHash = HashUtil.sha256(contentWithoutPrev);
        }
        
        // write log entry
        LogActionStatus status = result.isSuccess() ? LogActionStatus.SUCCESS : LogActionStatus.FAILURE;
        
        writeLogEntry(LogAction.VERIFY, getISOTimestamp(), archiveId, actorUId, recalculatedHash, status, details);
        
        return result;
    }
	
	/**
	 * returns metadata based on archiveId.
	 * */
	public JSONObject show(String archiveId) throws Exception {
		archiveId = formatArchiveId(archiveId);
		
	    Path folder = findArchiveFolder(archiveId);
	    if (folder == null) {
	        throw new Exception("Archive entry not found: " + archiveId);
	    }

	    Path metadataFile = folder.resolve("metadata.json");
	    if (!Files.exists(metadataFile)) {
	        throw new MetadataException("Missing metadata.json for: " + archiveId);
	    }

	    return new JSONObject(Files.readString(metadataFile));
	}
	
	/**
	 * provides entry content paths including the file itself and its metadata file.
	 * 
	 * */
	public Path[] getEntryDataset(String archiveId, String actorUId, LogDetails details) throws Exception {
	    archiveId = formatArchiveId(archiveId);

	    Path folder = findArchiveFolder(archiveId);
	    if (folder == null) {
	        throw new Exception("Archive entry not found: " + archiveId);
	    }

	    Path metadataFile = folder.resolve("metadata.json");
	    if (!Files.exists(metadataFile)) {
	        throw new MetadataException("Missing metadata.json for: " + archiveId);
	    }

	    JSONObject meta = new JSONObject(Files.readString(metadataFile));
	    String originalFileName = meta.getString("original_file_name");
	    
	    Path archivedFile = folder.resolve(originalFileName);
	    if (!Files.exists(archivedFile)) {
	        throw new Exception("Archived file missing: " + archivedFile);
	    }
	    
	    // log entry
	    writeLogEntry(
    	    LogAction.VIEW_FILE,
    	    getISOTimestamp(),
    	    archiveId,
    	    actorUId,
    	    meta.getString("hash"),
    	    LogActionStatus.SUCCESS,
    	    details
    	);


	    return new Path[] {archivedFile, metadataFile};
	}
	
	/**
	 * returns a list of metadata, depending on chosen filtering options.
	 * */
	public List<JSONObject> query(QueryFilterOptions filter) throws Exception {
		List<JSONObject> results = new ArrayList<>();
		
		for (String entry : listArchiveEntries()) {
			String[] parts = entry.split("/");
			String archiveId = parts[2].split("-")[0];
			
			// validate existence
			Path folder = findArchiveFolder(archiveId);
			if (folder == null) continue;
			
			Path metadataFile = folder.resolve("metadata.json");
			if (!Files.exists(metadataFile)) continue;
			
			JSONObject meta = new JSONObject(Files.readString(metadataFile));
			
			// check filters
			if (filter.archiveId != null && !archiveId.equals(filter.archiveId)) 
				continue;
			
			if (filter.type != null) {
	            String type = meta.getString("entry_type");
	            if (!type.equalsIgnoreCase(filter.type.name())) 
	            	continue;
	        }
			
			if (filter.hash != null) {
	        	if (!meta.getString("hash").equals(filter.hash)) 
	            	continue;
	        }
			
			if (filter.from != null || filter.to != null) {
				LocalDate ts = LocalDate.parse(meta.getString("timestamp").substring(0, 10));
				if (filter.from != null && ts.isBefore(filter.from)) continue;
				if (filter.to != null && ts.isAfter(filter.to)) continue;
			}
			
			if (filter.customKey != null && filter.customValue != null) {
	            JSONObject custom = meta.optJSONObject("custom");
	            if (custom == null) continue;

	            if (!custom.has(filter.customKey)) continue;
	            if (!custom.getString(filter.customKey).equals(filter.customValue)) continue;
	        }
			
			// add to results
			results.add(meta);
		}
		
		return results;
	}
	
	
	public Path exportSingle(String archiveId, String actorUId, LogDetails details, ExportOptions options) throws Exception {
		archiveId = formatArchiveId(archiveId);
		
	    JSONObject meta = show(archiveId);
	    Path folder = findArchiveFolder(archiveId);

	    if (folder == null) {
	        throw new Exception("Archive entry not found: " + archiveId);
	    }

	    Path tempDir = Files.createTempDirectory("export_single_");
	    Files.copy(folder.resolve(meta.getString("original_file_name")), tempDir.resolve(meta.getString("original_file_name")));
	    Files.writeString(tempDir.resolve("metadata.json"), meta.toString(2));

	    if (options.includeAuditLog) {
	        Files.copy(Environment.LOG_FILE, tempDir.resolve("audit.log"), StandardCopyOption.REPLACE_EXISTING);
	    }

	    Path zip = Files.createTempFile("export_single_", ".zip");
	    ZipUtil.zipFolder(tempDir, zip);
	    
	    // log export
	    String hash = HashUtil.sha256(zip); // Hash des fertigen Export-Pakets
	    
	    writeLogEntry(LogAction.EXPORT_SINGLE, getISOTimestamp(), archiveId, actorUId, hash, LogActionStatus.SUCCESS, details);
	    
	    return zip;
	}
	
	/**
	 * an empty export initiates a full data export.
	 * */
	public Path exportFiltered(QueryFilterOptions filter, ExportOptions options, String actorUId, LogDetails details) throws Exception {
	    List<JSONObject> entries = query(filter);

	    Path tempDir = Files.createTempDirectory("export_filtered_");
	    
	    for (JSONObject meta : entries) {
	        String archiveId = meta.getString("archive_id");
	        String originalFileName = meta.getString("original_file_name");
	        String exportName = archiveId + "_" + originalFileName;

	        Path folder = findArchiveFolder(archiveId);

	        if (!options.nested) {
	        	Files.copy(folder.resolve(originalFileName),
		                   tempDir.resolve(exportName));
		        Files.writeString(tempDir.resolve(archiveId + "_metadata.json"), meta.toString(2));
	        } else {
	            Path nestedDir = tempDir.resolve(archiveId);
	            Files.createDirectories(nestedDir);

	            Files.copy(
	                folder.resolve(originalFileName),
	                nestedDir.resolve(originalFileName),
	                StandardCopyOption.REPLACE_EXISTING
	            );

	            Files.writeString(
	                nestedDir.resolve("metadata.json"),
	                meta.toString(2)
	            );

	        }
	    }
	    

	    if (options.includeAuditLog) {
	        Files.copy(Environment.LOG_FILE, tempDir.resolve("audit.log"), StandardCopyOption.REPLACE_EXISTING);
	    }

	    Path zip = Files.createTempFile("export_single_", ".zip");
	    ZipUtil.zipFolder(tempDir, zip);
	    
	    String hash = HashUtil.sha256(zip);
	    LogAction logAction = filter == null ? LogAction.EXPORT_ALL : LogAction.EXPORT_FILTERED;
	    writeLogEntry(logAction, getISOTimestamp(), "-", actorUId, hash, LogActionStatus.SUCCESS, details);
	    
	    return zip;
	}
	
	/**
	 * uses a filtered export with empty filtering option for a full data return.
	 * */
	public Path exportAll(ExportOptions options, String actorUID, LogDetails details) throws Exception {
		QueryFilterOptions filter = new QueryFilterOptions(null); // no filters set
		
	    return exportFiltered(filter, options, actorUID, details);
	}

	
	public ArrayList<String> listArchiveEntries() throws IOException {
	    return listArchiveEntries(null);
	}

	
	/**
	 * returns a listing of all entry-paths.
	 * */
	public ArrayList<String> listArchiveEntries(EntryType filterType) throws IOException {
	    ArrayList<String> entries = new ArrayList<>();

	    if (!Files.exists(Environment.ARCHIVE_ROOT)) {
	        return entries;
	    }

	    try (DirectoryStream<Path> years = Files.newDirectoryStream(Environment.ARCHIVE_ROOT)) {
	        for (Path year : years) {
	            if (!Files.isDirectory(year)) continue;

	            try (DirectoryStream<Path> months = Files.newDirectoryStream(year)) {
	                for (Path month : months) {
	                    if (!Files.isDirectory(month)) continue;

	                    try (DirectoryStream<Path> dirs = Files.newDirectoryStream(month)) {
	                        for (Path entry : dirs) {
	                            if (!Files.isDirectory(entry)) continue;

	                            String name = entry.getFileName().toString(); // z.B. "000001-invoice"

	                            // filter if not null
	                            if (filterType != null) {
	                                String typeLower = filterType.name().toLowerCase();
	                                if (!name.endsWith("-" + typeLower)) {
	                                    continue;
	                                }
	                            }

	                            entries.add(
	                                year.getFileName() + "/" +
	                                month.getFileName() + "/" +
	                                name
	                            );
	                        }
	                    }
	                }
	            }
	        }
	    }

	    return entries;
	}


	public ArrayList<String> readLog() throws IOException {
	    ArrayList<String> lines = new ArrayList<>();

	    if (!Files.exists(Environment.LOG_FILE)) {
	        return lines;
	    }

	    for (String line : Files.readAllLines(Environment.LOG_FILE)) {
	        lines.add(line);
	    }

	    return lines;
	}
	
	private Path findArchiveFolder(String archiveId) throws IOException {

        if (!Files.exists(Environment.ARCHIVE_ROOT)) {
            return null;
        }

        DirectoryStream<Path> years = null;
        DirectoryStream<Path> months = null;

        try {
            years = Files.newDirectoryStream(Environment.ARCHIVE_ROOT);

            for (Path year : years) {
                if (!Files.isDirectory(year)) continue;

                months = Files.newDirectoryStream(year);

                for (Path month : months) {
                    if (!Files.isDirectory(month)) continue;

                    try (DirectoryStream<Path> entries = Files.newDirectoryStream(month)) {
                        for (Path entry : entries) {
                            if (!Files.isDirectory(entry)) continue;
                            String name = entry.getFileName().toString(); // e.g. "000001-invoice"
                            if (name.startsWith(archiveId + "-")) {
                                return entry;
                            }
                        }
                    }
                }

                if (months != null) {
                    months.close();
                    months = null;
                }
            }

        } finally {
            if (years != null) years.close();
            if (months != null) months.close();
        }

        return null;
    }
	
	/**
	 * returns a formatted version of the provided archiveId.
	 * */
	private String formatArchiveId(String archiveId) {
		 // sender provided a path like 2026/02/000003-invoice
        if (archiveId.contains("/")) {
            String[] parts = archiveId.split("/");
            archiveId = parts[parts.length - 1];
        }

        // sender provided full folder name like 000003-invoice
        if (archiveId.matches("\\d{6}-[a-z]+")) {
        	archiveId = archiveId.split("-")[0];
        }

        // sender provided only a number like 3 or 000003
        if (archiveId.matches("\\d+")) {
        	archiveId = String.format("%06d", Integer.parseInt(archiveId));
        }
        
        return archiveId;
	}

	protected static String generateNextArchiveId() throws IOException {
        LocalDate now = LocalDate.now();
        Path monthDir = Environment.ARCHIVE_ROOT
            .resolve(String.valueOf(now.getYear()))
            .resolve(String.format("%02d", now.getMonthValue()));

        if (!Files.exists(monthDir)) {
            return "000001";
        }

        int max = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(monthDir)) {
            for (Path p : stream) {
                if (!Files.isDirectory(p)) continue;
                String name = p.getFileName().toString(); // "000001-invoice"
                String num = name.split("-")[0];          // "000001"
                max = Math.max(max, Integer.parseInt(num));
            }
        }

        return String.format("%06d", max + 1);
    }

	
	/**
	 * returns a path to store a commit.
	 * */
	private Path generateArchivePath(String archiveId, EntryType type, String timestamp) {
		LocalDateTime ts = LocalDateTime.parse(timestamp);
        return Environment.ARCHIVE_ROOT
            .resolve(String.valueOf(ts.getYear()))
            .resolve(String.format("%02d", ts.getMonthValue()))
            .resolve(archiveId + "-" + type.name().toLowerCase());
    }

    private String getLastLogHash() throws IOException {
        if (!Files.exists(Environment.LOG_FILE)) {
            return "000000"; // Genesis-Hash
        }

        String lastLine = null;
        for (String line : Files.readAllLines(Environment.LOG_FILE)) {
            lastLine = line;
        }

        if (lastLine == null) return "000000";

        int idx = lastLine.lastIndexOf("prevHash=");
        if (idx == -1) return "000000";

        String contentWithoutPrev = lastLine.substring(0, idx).trim();

        return HashUtil.sha256(contentWithoutPrev);
    }
    
    /**
     * appends a log entry to the auditlog file.
     * */
    private void writeLogEntry(LogAction action, String timestamp, String archiveId, String actorUId, String hash, LogActionStatus status, LogDetails details) throws IOException {
        String prevHash = getLastLogHash();
        
        String actor = actorUId != null ? "userId=" + actorUId : "actor=system";
        String finalDetails = details != null ? details.getDetailsFormatted() : "/";
        
        String entry = String.format("%s | action=%s | archiveId=%s | %s | details=%s | status=%s | sha256=%s | prevHash=%s\n",
        		timestamp, action, archiveId, actor, finalDetails, status, hash, prevHash);

        Files.writeString(
            Environment.LOG_FILE,
            entry,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND 
        );
    }
    
    protected enum LogAction {
    	EXPORT_FILTERED,
    	EXPORT_SINGLE,
    	EXPORT_ALL,
    	VERIFY,
    	COMMIT_CREATE,
    	COMMIT_UPDATE,
    	VIEW_FILE
    }
    
    protected enum LogActionStatus {
    	SUCCESS,
    	FAILURE
    }

    private void makeImmutable(Path file) {
        try {
            new ProcessBuilder("chattr", "+i", file.toString()).start().waitFor();
        } catch (Exception e) {
            System.err.println("Warning: Could not set immutable flag for " + file);
        }
    }

    private void makeAppendOnly(Path file) {
        try {
            new ProcessBuilder("chattr", "+a", file.toString()).start().waitFor();
        } catch (Exception e) {
            System.err.println("Warning: Could not set append-only flag for " + file);
        }
    }
    
    public void ensureLogFileProtected() {
        try {
            if (!Files.exists(Environment.LOG_FILE)) {
                Files.createFile(Environment.LOG_FILE);
            }
            makeAppendOnly(Environment.LOG_FILE);
        } catch (Exception e) {
            System.err.println("Warning: Could not set append-only flag for log file.");
        }
    }
}
