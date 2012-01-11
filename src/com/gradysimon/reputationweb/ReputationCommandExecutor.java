package com.gradysimon.reputationweb;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReputationCommandExecutor implements CommandExecutor {

	ReputationWeb mainClass;
	ReputationGraph reputationGraph;
	Server server;

	public ReputationCommandExecutor(ReputationWeb mainClass,
			ReputationGraph reputationGraph, Server server) {
		this.mainClass = mainClass;
		this.reputationGraph = reputationGraph;
		this.server = server;
	}

	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args)
	{
		if (args.length > 0) {
			String firstArgument = args[0].toLowerCase();
			switch (firstArgument) {

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
		// TODO Auto-generated method stub,
		return false;
	}

	private boolean untrustCommand(CommandSender sender, String[] args) {
		if (!isPlayer(sender)) {
			// TODO: Must be a player error message
			return true;
		}
		Player commandSender = (Player) sender;
		Player otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			// TODO: add error message for player not found
			return true; // if otherPlayer is null, the player was not found on
							// the server
		}
		if (!reputationGraph.trustRelationExists(commandSender, otherPlayer)) {
			// TODO: don't already trusts error message
			return true;
		}
		reputationGraph.removeTrustRelation(commandSender, otherPlayer);
		// TODO: Untrust success message
		return true;
	}

	private boolean trustCommand(CommandSender sender, String[] args) {
		if (!isPlayer(sender)) {
			// TODO: Must be a player error message
			return true;
		}
		Player commandSender = (Player) sender;
		Player otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			// TODO: add error message for player not found
			return true; // if otherPlayer is null, the player was not found on
							// the server
		}
		if (reputationGraph.trustRelationExists(commandSender, otherPlayer)) {
			// TODO: already trusts error message
			return true;
		}
		reputationGraph.addTrustRelation(commandSender, otherPlayer);
		// TODO: Trust success message
		return true;
	}

	private boolean isPlayer(CommandSender sender) {
		return sender instanceof Player;
	}

	private Player getRealPlayer(String name) {
		OfflinePlayer potentialPlayer = server.getOfflinePlayer(name);
		if (potentialPlayer.hasPlayedBefore()) {
			return potentialPlayer.getPlayer();
		}
		return null;
	}
}