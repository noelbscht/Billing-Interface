package net.libraya.gobdarchive.cli.commands;

import java.util.Map;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.archive.ArchiveManager;
import net.libraya.gobdarchive.archive.VerificationResult;
import net.libraya.gobdarchive.archive.option.LogDetails;
import net.libraya.gobdarchive.cli.Command;

public class CommandVerify extends Command {

    public CommandVerify() {
        super(
            new String[] { "verify", "validate" },
            "Verify the integrity of an archived entry",
            "<archive_id, all, *> "
        );
    }

    @Override
    public boolean execute(String[] args) {
        if (args.length < 2) {
            System.out.println("Missing archive_id parameter.");
            return false;
        }

        String input = args[1];

        // verify all entries
        if (input.equalsIgnoreCase("all") || input.equalsIgnoreCase("*")) {
            ArchiveManager manager = Main.getArchiveManager();
            try {
                for (String entry : manager.listArchiveEntries()) {

                    // extract folder name: 000003-invoice
                    String folderName = entry.substring(entry.lastIndexOf("/") + 1);

                    // extract archiveId: 000003
                    String archiveId = folderName.split("-")[0];

                    VerificationResult r = manager.verify(archiveId, -1, new LogDetails(Map.of()));

                    System.out.println(archiveId + ": " + (r.isSuccess() ? "OK" : "FAILED"));
                }
            } catch (Exception e) {
                System.out.println("Error while performing verification command.");
                return false;
            }
            return true;
        }

        try {
            VerificationResult result = Main.getArchiveManager().verify(input, -1, new LogDetails(Map.of()));

            if (result.isSuccess()) {
                System.out.println("Verification successful for: " + input);
                return true;
            } else {
                System.out.println("Verification failed for: " + input);
                for (String error : result.getErrors()) {
                    System.out.println(" - " + error);
                }
                return false;
            }

        } catch (Exception e) {
            System.out.println("Verification error: " + e.getMessage());
            return false;
        }
    }
}