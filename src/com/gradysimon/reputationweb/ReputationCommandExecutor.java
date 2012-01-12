package com.gradysimon.reputationweb;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
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
			case "connection":
				return referralCommand(sender, args);

			default:
				// TODO: should I have a help command?
				return false;
			}
		}
		return false;
	}

	private boolean referralCommand(CommandSender sender, String[] args) {
		if (args.length == 2) {
			return selfReferralCommand(sender, args);
		}
		if (args.length == 3) {
			return otherReferralCommand(sender, args);
		}
		return false;
	}

	private boolean otherReferralCommand(CommandSender sender, String[] args) {
		Player startPlayer = getRealPlayer(args[1]);
		Player endPlayer = getRealPlayer(args[2]);
		if (startPlayer == null || endPlayer == null) {
			// TODO: At least one of the players doesn't exist error
			return true;
		}
		List<Player> path = reputationGraph
				.getReference(startPlayer, endPlayer);
		if (path == null) {
			// If this is null, there is no path between them
			// TODO: ensure that getReference returns null if there is no
			// path.
			// TODO: no path exists error.
			return true;
		}
		List<String> namesInPath = convertToNameList(path);
		String output = generateReferralOutput(namesInPath,
				startPlayer.getName(), endPlayer.getName());
		sender.sendMessage(output);
		return true;
	}

	private boolean selfReferralCommand(CommandSender sender, String[] args) {
		if (!isPlayer(sender)) {
			// TODO: must be a player or must supply 2 names error
		}
		Player senderPlayer = (Player) sender;
		Player otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			// TODO: other player doesn't exist error
			return true;
		}
		List<Player> path = reputationGraph.getReference(senderPlayer,
				otherPlayer);
		if (path == null) {
			// If this is null, there is no path between them
			// TODO: ensure that getReference returns null if there is no
			// path.
			// TODO: no path exists error.
		}
		List<String> namesInPath = convertToNameList(path);
		String output = generateReferralOutput(namesInPath,
				senderPlayer.getName(), otherPlayer.getName());
		return false;
	}

	private String generateReferralOutput(List<String> path, String startName,
			String endName)
	{
		// TODO Auto-generated method stub
		return null;
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
			// TODO: don't already trust error message
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

	private boolean isConsole(CommandSender sender) {
		return (sender instanceof ConsoleCommandSender)
				|| (sender instanceof RemoteConsoleCommandSender);
	}

	private Player getRealPlayer(String name) {
		OfflinePlayer potentialPlayer = server.getOfflinePlayer(name);
		if (potentialPlayer.hasPlayedBefore()) {
			return potentialPlayer.getPlayer();
		}
		return null;
	}

	private List<String> convertToNameList(List<Player> playerList) {
		List<String> nameList = new ArrayList<String>(playerList.size());
		for (Player player : playerList) {
			nameList.add(player.getName());
		}
		return nameList;
	}
}