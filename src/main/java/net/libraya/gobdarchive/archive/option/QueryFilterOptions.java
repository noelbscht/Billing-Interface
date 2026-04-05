package net.libraya.gobdarchive.archive.option;

import java.time.LocalDate;

import org.json.JSONObject;

import net.libraya.gobdarchive.archive.EntryType;

public class QueryFilterOptions {

    public EntryType type;
    public LocalDate from;
    public LocalDate to;
    public String customKey;
    public String customValue;
    public String hash;
    public String archiveId;

    public QueryFilterOptions(JSONObject input) {
        if (input != null) {

            String typeStr = normalize(input.optString("entry_type"));
            String fromStr = normalize(input.optString("from"));
            String toStr   = normalize(input.optString("to"));

            this.type = typeStr != null ? EntryType.valueOf(typeStr) : null;
            this.from = fromStr != null ? LocalDate.parse(fromStr) : null;
            this.to   = toStr   != null ? LocalDate.parse(toStr)   : null;

            this.customKey   = normalize(input.optString("custom_key"));
            this.customValue = normalize(input.optString("custom_value"));
            this.hash        = normalize(input.optString("hash"));
            this.archiveId   = normalize(input.optString("archive_id"));
        }
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
