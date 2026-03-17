package net.libraya.gobdarchive.cli.commands;

import java.util.ArrayList;

import net.libraya.gobdarchive.Main;
import net.libraya.gobdarchive.cli.Command;

public class CommandLog extends Command {

    public CommandLog() {
        super(
            new String[]{"log", "audit-log", "auditlog"},
            "Show the audit log",
            ""
        );
    }

    @Override
    public boolean execute(String[] args) {
        try {
            ArrayList<String> log = Main.getArchiveManager().readLog();

            if (log.isEmpty()) {
                System.out.println("Log is empty.");
                return true;
            }

            for (String line : log) {
                System.out.println(line);
            }

            return true;

        } catch (Exception e) {
            System.out.println("Error reading log: " + e.getMessage());
            return false;
        }
    }
}
