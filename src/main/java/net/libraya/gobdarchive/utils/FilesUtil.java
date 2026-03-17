package net.libraya.gobdarchive.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * An little extension to add commentaries and ignore them while reading files. (main purpose is for JSON-Files).
 * */
public class FilesUtil {
	
	/**
	 * Add commentaries above content
	 * */
	public static Path writeString(Path path, CharSequence csq, Charset cs, String [] comments, OpenOption... opt) throws IOException {
		String[] legalNotice = new String[] {
				"#		### This configuration is part of libraya.net " + Unicodes.COPYRIGHT + " content. ###\n",
				"# 		    All rights reserved\n",
				"#\n"
		};
 		
		// write legal notice and commentaries to content
		StringBuilder sb = new StringBuilder();
		for (String ln : legalNotice) {
			sb.append(ln);
		}
		for (int i = 0; i < comments.length + 2; i++) {
			if (i < comments.length) {
				sb.append("# " + comments[i] +"\n");
			}
			sb.append("#\n");
		}
		return Files.writeString(path, (sb.toString() + csq), cs, opt);
	}
	
	/**
	 * Add commentaries above content
	 * */
	public static Path writeString(Path path, CharSequence csq, String [] comments, OpenOption... opt) throws IOException {
		return writeString(path, csq, Charset.forName("utf-8"), comments, opt);
	}
	
	/**
	 * normally read, but ignore commentaries.
	 * @throws IOException 
	 * */
	public static String readString(Path path, Charset cs) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    for (String line : Files.readAllLines(path, cs)) {
	        if (!line.trim().startsWith("#")) {
	            sb.append(line).append("\n");
	        }
	    }
	    return sb.toString();
	}

	
	/**
	 * normally read, but ignore commentaries.
	 * @throws IOException 
	 * */
	public static String readString(Path path) throws IOException {
		return readString(path, Charset.defaultCharset());
	}
}
