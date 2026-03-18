package net.libraya.gobdarchive.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.libraya.gobdarchive.cli.commands.CommandCommit;
import net.libraya.gobdarchive.cli.commands.CommandList;
import net.libraya.gobdarchive.cli.commands.CommandLog;
import net.libraya.gobdarchive.cli.commands.CommandVerify;
import net.libraya.gobdarchive.cli.commands.CommandWeb;

public class CLI {
	
	private List<Command> commands = new ArrayList<Command>();
	
	public CLI() {
		this.commands.add(new CommandCommit());
		this.commands.add(new CommandVerify());
		this.commands.add(new CommandLog());
		this.commands.add(new CommandList());
		this.commands.add(new CommandWeb());
	}
	
	public boolean isCommand(String[] args) {
		for (Command cmd : commands)  {
			if (Arrays.asList(cmd.getNames()).contains(args[0])) {
				return true;
			}
		}
		return false;
	}
	
	public Command getCommand(String[] args) {
		for (Command cmd : commands)  {
			if (Arrays.asList(cmd.getNames()).contains(args[0])) {
				return cmd;
			}
		}
		return null;
	}
	
	public boolean execute(String[] args) {
		for (Command cmd : commands)  {
			if (Arrays.asList(cmd.getNames()).contains(args[0])) {
				return cmd.execute(args);
			}
		}
		return false;
	}
	
	public List<Command> getCommands() {
		return commands;
	}
}
