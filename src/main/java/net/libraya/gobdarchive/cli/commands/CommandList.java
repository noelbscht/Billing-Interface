package net.libraya.gobdarchive.cli.commands;

import java.util.ArrayList;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.archive.EntryType;
import net.libraya.gobdarchive.cli.Command;

public class CommandList extends Command {

    public CommandList() {
        super(
            new String[]{"list"},
            "List all archived entries",
            "--filter <entry_type>"
        );
    }

    @Override
    public boolean execute(String[] args) {
        try {
        	// search filter argument
        	EntryType filter = null;
        	for (int i = 1; i < args.length; i++) {
        		if (args[i].equalsIgnoreCase("--filter")) {
        			try {
                        filter = EntryType.valueOf(args[++i].toUpperCase());
                    } catch (Exception e) {
                        System.out.println("Error: Invalid entry type. Allowed values:");
                        for (EntryType t : EntryType.values()) {
                            System.out.println(" - " + t.name().toLowerCase());
                        }
                        return false;
                    }
        			break;
        		}
        	}
        	
            ArrayList<String> entries = Main.getArchiveManager().listArchiveEntries(filter);
            if (entries.isEmpty()) {
                System.out.println("No entries found.");
                return true;
            }

            for (String inv : entries) {
                System.out.println(inv);
            }

            return true;

        } catch (Exception e) {
            System.out.println("Error listing invoices: " + e.getMessage());
            return false;
        }
    }
}

