package net.libraya.gobdarchive.archive.option;

import java.time.LocalDate;

import net.libraya.gobdarchive.archive.EntryType;

public class QueryFilterOptions {
	public EntryType type;
	public LocalDate from;
	public LocalDate to;
	public String customKey;
	public String customValue;
	public String hash;
	public String archiveId;
	
	public QueryFilterOptions() {
		
	}
}
