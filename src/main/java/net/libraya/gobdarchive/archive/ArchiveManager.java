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
import net.libraya.gobdarchive.archive.option.QueryFilterOptions;
import net.libraya.gobdarchive.utils.Environment;
import net.libraya.gobdarchive.utils.HashUtil;
import net.libraya.gobdarchive.utils.ZipUtil;
import net.libraya.gobdarchive.utils.exception.MetadataException;

public class ArchiveManager {

	public ArchiveManager() {

	}
	
	public void commit(Metadata metadata) throws Exception {
		JSONObject requirements = metadata.getFinalMetadata();
		
		if (metadata.isCustomForced()) {
			if (!metadata.isFilledUp()) {
				throw new MetadataException("Required metadata fields are missing. \n" + metadata.getCustomRequirementKeys());
			}
		}
		
	    // archive path
	    Path targetDir = getArchivePath(metadata.archiveId, metadata.type);
	    Files.createDirectories(targetDir);

	    // copy file
	    Path targetFile = targetDir.resolve(metadata.path.getFileName());
	    Files.copy(metadata.path, targetFile);

	    // write metadata.json
	    Files.writeString(targetDir.resolve("metadata.json"), requirements.toString(2));

	    // write hash.txt
	    Files.writeString(targetDir.resolve("hash.txt"), metadata.hash);

	    // write log
	    logCommit(metadata.archiveId, metadata.hash);

	    // set immutable
	    makeImmutable(targetDir);
	    
	    // delete temp file
	    if (metadata.path.toString().contains("api_upload_")) {
	        Files.deleteIfExists(metadata.path);
	    }
	}

	public VerificationResult verify(String archiveId) throws Exception {
	    VerificationResult result = new VerificationResult();
	    
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
	    String originalFileName = metadata.getString("originalFile");
	    String archiveIdFromMeta = metadata.getString("archiveId");


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
            if (line.contains(" | " + archiveIdFromMeta + " | ")) {
                logEntryFound = true;

                if (!line.contains("sha256=" + recalculatedHash)) {
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

        return result;
    }
	
	public JSONObject show(String archiveId) throws Exception {
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
	            String type = meta.getString("type");
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
	
	
	public Path exportSingle(String archiveId, ExportOptions options) throws Exception {
	    JSONObject meta = show(archiveId);
	    Path folder = findArchiveFolder(archiveId);

	    if (folder == null) {
	        throw new Exception("Archive entry not found: " + archiveId);
	    }

	    Path tempDir = Files.createTempDirectory("export_single_");
	    Files.copy(folder.resolve(meta.getString("originalFile")), tempDir.resolve(meta.getString("originalFile")));
	    Files.writeString(tempDir.resolve("metadata.json"), meta.toString(2));

	    if (options.includeAuditLog) {
	        Files.copy(Environment.LOG_FILE, tempDir.resolve("audit.log"), StandardCopyOption.REPLACE_EXISTING);
	    }

	    Path zip = tempDir.resolve("export.zip");
	    ZipUtil.zipFolder(tempDir, zip);

	    return zip;
	}
	
	public Path exportFiltered(QueryFilterOptions filter, ExportOptions options) throws Exception {
	    List<JSONObject> entries = query(filter);

	    Path tempDir = Files.createTempDirectory("export_filtered_");

	    for (JSONObject meta : entries) {
	        String archiveId = meta.getString("archiveId");
	        Path folder = findArchiveFolder(archiveId);

	        Files.copy(folder.resolve(meta.getString("originalFile")),
	                   tempDir.resolve(meta.getString("originalFile")));

	        Files.writeString(tempDir.resolve(archiveId + "_metadata.json"), meta.toString(2));
	    }

	    if (options.includeAuditLog) {
	        Files.copy(Environment.LOG_FILE, tempDir.resolve("audit.log"), StandardCopyOption.REPLACE_EXISTING);
	    }

	    Path zip = tempDir.resolve("export.zip");
	    ZipUtil.zipFolder(tempDir, zip);

	    return zip;
	}
	
	public Path exportAll(ExportOptions options) throws Exception {
		QueryFilterOptions filter = new QueryFilterOptions(); // no filters set
	    return exportFiltered(filter, options);
	}

	
	public ArrayList<String> listArchiveEntries() throws IOException {
	    return listArchiveEntries(null);
	}

	
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

	
	private Path getArchivePath(String archiveId, EntryType type) {
        LocalDate now = LocalDate.now();
        return Environment.ARCHIVE_ROOT
            .resolve(String.valueOf(now.getYear()))
            .resolve(String.format("%02d", now.getMonthValue()))
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

    private void writeLogEntry(LocalDateTime timestamp, String archiveId, String hash) throws IOException {

        String prevHash = getLastLogHash();

        String entry = timestamp.format(DateTimeFormatter.ISO_DATE_TIME)
                + " | COMMIT | " + archiveId
                + " | sha256=" + hash
                + " | prevHash=" + prevHash
                + "\n";

        Files.writeString(
        	Environment.LOG_FILE,
            entry,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    private void logCommit(String archiveId, String hash) throws IOException {
        writeLogEntry(LocalDateTime.now(), archiveId, hash);
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
