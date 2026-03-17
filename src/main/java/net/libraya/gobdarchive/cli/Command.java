package net.libraya.gobdarchive.cli;

public abstract class Command {

	private final String[] names;
	private final String description;
	private final String usage;

	public Command(String[] names, String description, String usage) {
		this.names = names;
		this.description = description;
		this.usage = usage;
	}

	public abstract boolean execute(String[] args);

	public String getName() {
		return names[0];
	}

	public String[] getNames() {
		return names;
	}

	public String getDescription() {
		return description;
	}

	public String getUsageParameters() {
		return usage;
	}
}
