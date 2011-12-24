package com.gradysimon.reputationweb;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReputationCommandExecutor implements CommandExecutor {

	ReputationWeb mainClass;
	ReputationGraph reputationGraph;
	
	public ReputationCommandExecutor(ReputationWeb mainClass,
			ReputationGraph reputationGraph) {
		this.mainClass = mainClass;
		this.reputationGraph = reputationGraph;
	}

	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (args.length > 0) {
			switch (args[0]) {

			case "trust":
			case "vouch":
				return trustCommand(sender, args);

			case "untrust":
			case "unvouch":
			case "distrust":
				return untrustCommand(sender, args);

			case "player":
			case "info":
				return infoCommand(sender, args);

			case "referral":
			case "reference":
			case "ref":
				return referralCommand(sender, args);
			
			default:
				// TODO: should I have a help command?
				return false;
			}
		}
		return false;
	}

	private boolean referralCommand(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean infoCommand(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean untrustCommand(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean trustCommand(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return false;
	}

}