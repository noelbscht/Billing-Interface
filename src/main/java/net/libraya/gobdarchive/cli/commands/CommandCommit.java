package net.libraya.gobdarchive.cli.commands;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.archive.CommitType;
import net.libraya.gobdarchive.archive.EntryType;
import net.libraya.gobdarchive.archive.Metadata;
import net.libraya.gobdarchive.cli.Command;

public class CommandCommit extends Command {

    public CommandCommit() {
        super(
            new String[] {"commit"},
            "Commit a new document to the system",
            """
            --file <file_path>,			Path to the file that should be archived 
            --type <type>, 				Entry type (e.g. document, invoice, contract)
            --metadata <meta_data>		Path to a JSON file containing custom metadata
            """
        );
    }

    @Override
    public boolean execute(String[] args) {
        String filePath = null;
        String metadataPath = null;
        EntryType type = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--file":
                    filePath = args[++i];
                    break;
                case "--type":
                	try {
                        type = EntryType.valueOf(args[++i].toUpperCase());
                    } catch (Exception e) {
                        System.out.println("Error: Invalid type. Allowed values:");
                        for (EntryType t : EntryType.values()) {
                            System.out.println(" - " + t.name().toLowerCase());
                        }
                        return false;
                    }

                    break;
                case "--metadata":
                    metadataPath = args[++i];
                    break;
                default:
                    System.out.println("Unknown parameter: " + args[i]);
                    return false;
            }
        }
    	
        // required parameters
        if (filePath == null) {
            System.out.println("Error: --file is required");
            return false;
        }
        
        if (type == null) {
            System.out.println("Error: --type is required");
            return false;
        }

        try {
        	// create customMetadata object if metadata is provided by sender.
            JSONObject customMetadata = new JSONObject();
            if (metadataPath != null) {
                customMetadata = new JSONObject(
                    Files.readString(Paths.get(metadataPath))
                );
            }

            Main.getArchiveManager().commit(new Metadata(
                    type,
                    CommitType.CLI,
                    Paths.get(filePath),
                    customMetadata));
            System.out.println("Successfully committed.");
            return true;
        } catch (Exception e) {
            System.err.println("Commit failed: " + e.getMessage());
            return false;
        }
    }
}