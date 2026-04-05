package net.libraya.gobdarchive.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.utils.Environment;
import net.libraya.gobdarchive.utils.HashUtil;
import net.libraya.gobdarchive.utils.exception.MetadataException;

public class Metadata {
	
	// custom metadata path
	private JSONObject customRequirements = Main.getConfigurations().customMetadataCfg.getContent();
	private JSONObject customMetadata = new JSONObject();
	
	// internal requirements (minimum for GoBD)
	protected final String archiveId;
	protected final EntryType type;
	protected final CommitType origin;
	protected final String filename;
	protected final String hash;
	protected final Path path;
	protected String timestamp;
	
	public Metadata(EntryType type,
			CommitType origin, 
			Path path, 
			JSONObject customMetadata, String filename) throws IOException, MetadataException {
		
		this.archiveId = ArchiveManager.generateNextArchiveId();
		
		this.type = type;
		this.origin = origin;
		this.filename = filename;
		this.path = path;
		this.customMetadata = customMetadata != null ? customMetadata : new JSONObject();
		
		this.hash = HashUtil.sha256(path); 
	}

	public Metadata(EntryType type,
            CommitType origin,
            File file,
            JSONObject customMetadata, String filename) throws IOException, MetadataException {

		this(type, origin, file.toPath(), customMetadata, filename);
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
	 * return necessary requirements
	 * */
	private JSONObject getNecessaryRequirements() {
		JSONObject requirements = new JSONObject();
		
		String finalFilePath = this.timestamp.substring(0, 7).replace("-", "/") + "/" +
			    	    this.archiveId + "-" + this.type.name().toLowerCase();
		
		requirements.put("archive_id", this.archiveId);
		requirements.put("entry_type", this.type.name());
		requirements.put("commit_type", this.origin.name()); // cli, api, sdk
		requirements.put("timestamp", this.timestamp);
		requirements.put("hash", this.hash);
		requirements.put("file_path", finalFilePath);
		requirements.put("original_file_name", filename);
		
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
