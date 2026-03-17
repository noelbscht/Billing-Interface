package net.libraya.gobdarchive.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.json.JSONArray;
import org.json.JSONObject;

import net.libraya.gobdarchive.utils.Environment;
import net.libraya.gobdarchive.utils.FilesUtil;
import net.libraya.gobdarchive.utils.HashUtil;
import net.libraya.gobdarchive.utils.exception.MetadataException;

public class Metadata {
	
	// custom metadata path
	private final Path customMetadataPath;
	private JSONObject customRequirements;
	private JSONObject customMetadata = new JSONObject();
	
	// internal requirements (minimum for GoBD)
	protected final String archiveId;
	protected final EntryType type;
	protected final CommitType origin;
	protected final String hash;
	protected final Path path;
	
	public Metadata(EntryType type,
			CommitType origin, 
			Path path, 
			JSONObject customMetadata) throws IOException, MetadataException {
		
		this.archiveId = ArchiveManager.generateNextArchiveId();
		
		this.type = type;
		this.origin = origin;
		this.path = path;
		this.customMetadata = customMetadata != null ? customMetadata : new JSONObject();
		
		this.hash = HashUtil.sha256(path); 
		
		// metadata paths
		this.customMetadataPath = Path.of(System.getProperty("user.dir"), "requirements",  "custom-metadata.json");
		try {
			loadConfigurations();
		} catch (IOException e) {
			throw new IOException("Error while loading metadata requirements: ", e);
		}
	}

	public Metadata(EntryType type,
            CommitType origin,
            File file,
            JSONObject customMetadata) throws IOException, MetadataException {

		this(type, origin, file.toPath(), customMetadata);
	}
	

	/**
	 * Loads configured, requested metadata keys.
	 * */
	private void loadConfigurations() throws IOException {
        if (!Files.exists(customMetadataPath)) {
        	// create defaults
            Files.createDirectories(customMetadataPath.getParent());
            JSONObject obj = new JSONObject();
            String[] requirements = new String[] {
            		"user_reference_id", // default, example references
            		"stripe_bill_reference"
            };
            
            obj.put("requirements", requirements);
            FilesUtil.writeString(customMetadataPath, obj.toString(4), new String[] {
            		// commentaries
            		"A list to customly force requirements for commits."
            });
        }

        this.customRequirements = new JSONObject(FilesUtil.readString(customMetadataPath));
    }
	
	/**
	 * return list of custom metadata keys that must be provided by the sender.
	 * */
	public JSONArray getCustomRequirementKeys() {
	    return customRequirements.getJSONArray("requirements");
	}
	
	/**
	 * return custom metadata, which is commonly provided by the sender by cli, api or sdk.
	 * */
	public JSONObject getCustomMetadata() {
		return customMetadata;
	}
	
	/*
	 * generate and return necessary requirements (should be only used once)
	 * */
	private JSONObject getNecessaryRequirements() {
		
		JSONObject requirements = new JSONObject();
		
		requirements.put("archiveId", this.archiveId);
		requirements.put("entryType", this.type.name());
		requirements.put("commitmentType", this.origin.name()); // cli, api, sdk
		requirements.put("timestamp", Instant.now().toString());
		requirements.put("hash", this.hash);
		requirements.put("filePath", this.path.toString());
		
		return requirements;
	}
	
	/**
	 * builds and returns the final metadata object containing:
	 * - all internal GoBD-required fields
	 * - all custom metadata fields provided by the sender

	 * */
	public JSONObject getFinalMetadata() {
		JSONObject requirements = getNecessaryRequirements();

		JSONArray keys = getCustomRequirementKeys();
	    for (int i = 0; i < keys.length(); i++) {
	        String key = keys.getString(i);
	        String value = customMetadata.optString(key, "");
	        requirements.put(key, value);
	    }

		
		return requirements;
	}

	
	/**
	 * return if custom requirements are enabled in the configuration.
	 * */
	public boolean isCustomForced() {
		return Environment.CUSTOM_REQUIREMENTS_FORCED;
	}


	/**
	 * checks if all required custom metadata fields have been provided and are non-empty.
	 * */
	public boolean isFilledUp() {
	    JSONArray keys = getCustomRequirementKeys();

	    for (int i = 0; i < keys.length(); i++) {
	        String key = keys.getString(i);

	        if (!customMetadata.has(key) || customMetadata.getString(key).isBlank()) {
	            return false;
	        }
	    }
	    return true;
	}
}
