package com.gradysimon.reputationweb;

import java.util.ArrayList;
import java.util.List;

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
	private final int numOfTopTrusters = 3;

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

	private boolean referralCommand(CommandSender sender, String[] args) {
		if (args.length == 2) {
			return selfReferralCommand(sender, args);
		}
		if (args.length == 3) {
			return otherReferralCommand(sender, args);
		}
		return false;
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
		List<String> output = generateReferralOutput(namesInPath,
				senderPlayer.getName(), otherPlayer.getName());
		sendMessage(sender, output);
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
		List<String> output = generateReferralOutput(namesInPath,
				startPlayer.getName(), endPlayer.getName());
		sendMessage(sender, output);
		return true;
	}

	private List<String> generateReferralOutput(List<String> path,
			String startName, String endName)
	{
		List<String> output = new ArrayList<String>();
		output.add("Chain of trust between player " + startName
				+ " and player " + endName + ":");
		String pathString = "";
		for (String name : path) {
			pathString += " -> " + name;
		}
		output.add(pathString);
		return output;
	}

	private boolean infoCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			return selfInfoCommand(sender);
		}
		if (args.length == 2) {
			return otherInfoCommand(sender, args);
		}
		return false;
	}

	private boolean selfInfoCommand(CommandSender sender) {
		if (!isPlayer(sender)) {
			// TODO: must be player error
			return true;
		}
		Player player = (Player) sender;
		return coreInfoCommand(sender, player);
	}

	private boolean otherInfoCommand(CommandSender sender, String[] args) {
		Player player = getRealPlayer(args[1]);
		if (player == null) {
			// TODO: player does not exist error
			return true;
		}
		return coreInfoCommand(sender, player);
	}
	
	private boolean coreInfoCommand(CommandSender sender, Player player) {
		double reputation = reputationGraph.getReputation(player);
		int numberOfTrusters = reputationGraph.trustersCount(player);
		int numberOfTrustees = reputationGraph.trusteesCount(player);
		List<Player> topTrusters = reputationGraph.getTopTrusters(player,
				numOfTopTrusters);
		List<String> output = generateInfoOutput(player.getName(), reputation,
				numberOfTrusters, numberOfTrustees, topTrusters);
		sendMessage(sender, output);
		return true;
	}

	private List<String> generateInfoOutput(String playerName,
			double reputation, int numberOfTrusters, int numberOfTrustees,
			List<Player> topTrusters)
	{
		List<String> output = new ArrayList<String>();
		output.add("Player: " + playerName);
		output.add("Reputation: " + reputation);
		String trustCountOutput = "Trusted by " + numberOfTrusters + " ";
		trustCountOutput += (numberOfTrusters == 1 ? "player" : "players")
				+ ".";
		trustCountOutput += "Trusts " + numberOfTrustees + " ";
		trustCountOutput += (numberOfTrusters == 1 ? "player" : "players")
				+ ".";
		output.add(trustCountOutput);
		String topTrustersOutput = "Most reputable players who trust "
				+ playerName + ": ";
		for (int i = 0; i < topTrusters.size(); i++) {

			Player truster = topTrusters.get(i);
			// TODO: should I use displayName() on the next line?
			topTrustersOutput += truster.getName();
			topTrustersOutput += "(" + reputationGraph.getReputation(truster)
					+ ")";
			if (i != topTrusters.size() - 1) {
				topTrustersOutput += ", ";
			}
		}
		output.add(topTrustersOutput);
		return output;

	}

	private void sendMessage(CommandSender sender, List<String> output) {
		for (String message : output) {
			sender.sendMessage(message);
		}
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

	private List<String> convertToNameList(List<Player> playerList) {
		List<String> nameList = new ArrayList<String>(playerList.size());
		for (Player player : playerList) {
			nameList.add(player.getName());
		}
		return nameList;
	}
}