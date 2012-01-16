package com.gradysimon.reputationweb;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query;

public class ReputationCommandExecutor implements CommandExecutor {

	ReputationWeb plugin;
	ReputationGraph reputationGraph;
	Server server;
	private final int numOfTopTrusters = 3;

	private final String trustPermissionNode = "reputationweb.trust";
	private final String infoSelfPermissionNode = "reputationweb.info.self";
	private final String infoAllPermissionNode = "reputationweb.info.all";
	private final String connectionSelfPermissionNode = "reputationweb.connection.self";
	private final String connectionAllPermissionNode = "reputationweb.connection.all";

	public ReputationCommandExecutor(ReputationWeb plugin,
			ReputationGraph reputationGraph, Server server) {
		this.plugin = plugin;
		this.reputationGraph = reputationGraph;
		this.server = server;
	}

	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args)
	{
		// TODO: DEBUGGING CODE
		System.out.println("onCommand() recieved command: /" + label);
		for (int i = 0; i < args.length; i++) {
			System.out.println("Argument " + i + ": " + args[i]);
		}
		// END DEBUG CODE
		label = label.toLowerCase();
		if (label.equals("rep") || label.equals("reputation")) {
			return dispatchReputationCommand(sender, args);
		} else if (label.equals("trust")) {
			trustCommand(sender, args[0]);
		} else if (label.equals("untrust")) {
			untrustCommand(sender, args[0]);
		}
		return false;
	}

	// TODO: should I have a help command?
	private boolean dispatchReputationCommand(CommandSender sender,
			String[] args)
	{
		if (args.length > 0) {
			String firstArg = args[0].toLowerCase();
			if (firstArg.equals("trust")) {
				trustCommand(sender, args[1]);
				return true;
			} else if (firstArg.equals("untrust")) {
				untrustCommand(sender, args[1]);
			} else if (firstArg.equals("info")) {
				infoCommand(sender, args);
			} else if (firstArg.equals("connection")) {
				referralCommand(sender, args);
			}
		}
		return false;
	}

	private void trustCommand(CommandSender sender, String targetName) {
		if (!isOnlinePlayer(sender)) {
			sendMessage(sender, ReputationWebMessages.mustBePlayerError());
			return;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, trustPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
			return;
		}
		OfflinePlayer otherPlayer = getRealPlayer(targetName);
		if (otherPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(targetName));
			return;
		}
		if (senderPlayer == otherPlayer) {
			sendMessage(sender, ReputationWebMessages.cannotTrustSelfError());
			return;
		}
		if (reputationGraph.trustRelationExists(senderPlayer, otherPlayer)) {
			sendMessage(sender,
					ReputationWebMessages.alreadyTrustsPlayerError(otherPlayer));
			return;
		}
		addTrustToDatabase(senderPlayer.getName(), otherPlayer.getName());
		reputationGraph.addTrustRelation(senderPlayer, otherPlayer);
		sendMessage(sender,
				ReputationWebMessages.trustSuccessMessage(otherPlayer));
		return;
	}

	private void addTrustToDatabase(String truster, String trustee) {
		Trust trust = new Trust();
		trust.setTrusterName(truster);
		trust.setTrusteeName(trustee);
		EbeanServer database = plugin.getPluginDatabase();
		database.save(trust);
	}

	private void untrustCommand(CommandSender sender, String targetName) {
		if (!isOnlinePlayer(sender)) {
			sendMessage(sender, ReputationWebMessages.mustBePlayerError());
			return;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, trustPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
			return;
		}
		OfflinePlayer otherPlayer = getRealPlayer(targetName);
		if (otherPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(targetName));
			return;
		}
		if (!reputationGraph.trustRelationExists(senderPlayer, otherPlayer)) {
			sendMessage(sender,
					ReputationWebMessages.doesNotTrustPlayerError(otherPlayer));
			return;
		}
		removeTrustFromDatabase(senderPlayer.getName(), otherPlayer.getName());
		reputationGraph.removeTrustRelation(senderPlayer, otherPlayer);
		// TODO: Untrust success message
		return;
	}

	private void removeTrustFromDatabase(String truster, String trustee) {
		EbeanServer database = plugin.getPluginDatabase();
		Query<Trust> query = database.find(Trust.class);
		Trust trust = query.where().eq("trusterName", truster)
				.eq("trusteeName", trustee).findUnique();
		database.delete(trust);
	}

	private boolean referralCommand(CommandSender sender, String[] args) {
		if (args.length == 2) {
			selfReferralCommand(sender, args);
			return true;
		}
		if (args.length == 3) {
			otherReferralCommand(sender, args);
			return true;
		}
		return false;
	}

	private void selfReferralCommand(CommandSender sender, String[] args) {
		if (!isOnlinePlayer(sender)) {
			sendMessage(sender,
					ReputationWebMessages.mustBePlayerOrAddArgsError());
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, connectionSelfPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
		}
		OfflinePlayer otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
		}
		List<OfflinePlayer> path = reputationGraph.getReference(senderPlayer,
				otherPlayer);
		if (path == null) {
			sendMessage(sender,
					ReputationWebMessages.noTrustPathExistsMessage(otherPlayer));
		}
		List<String> namesInPath = convertToNameList(path);
		// TODO: Move generation of this output to ReputationWebMessages.
		List<String> output = generateReferralOutput(namesInPath,
				senderPlayer.getName(), otherPlayer.getName());
		sendMessage(sender, output);
	}

	private void otherReferralCommand(CommandSender sender, String[] args) {
		if (isOnlinePlayer(sender)) {
			if (!hasPermission((Player) sender, connectionAllPermissionNode)) {
				sendMessage(sender,
						ReputationWebMessages.lacksPermissionError());
				return;
			}
		}
		OfflinePlayer startPlayer = getRealPlayer(args[1]);
		OfflinePlayer endPlayer = getRealPlayer(args[2]);
		if (startPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
			return;
		}
		if (endPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[2]));
			return;
		}
		List<OfflinePlayer> path = reputationGraph.getReference(startPlayer,
				endPlayer);
		if (path == null) {
			sendMessage(sender, ReputationWebMessages.noTrustPathExistsMessage(
					startPlayer, endPlayer));
			return;
		}
		List<String> namesInPath = convertToNameList(path);
		List<String> output = generateReferralOutput(namesInPath,
				startPlayer.getName(), endPlayer.getName());
		sendMessage(sender, output);
		return;
	}

	private List<String> generateReferralOutput(List<String> path,
			String startName, String endName)
	{
		List<String> output = new ArrayList<String>();
		output.add("Chain of trust between " + startName + " and " + endName
				+ ":");
		String pathString = startName;
		for (String name : path) {
			pathString += " -> " + name;
		}
		output.add(pathString);
		return output;
	}

	private boolean infoCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			selfInfoCommand(sender);
			return true;
		}
		if (args.length == 2) {
			otherInfoCommand(sender, args);
			return true;
		}
		return false;
	}

	private void selfInfoCommand(CommandSender sender) {
		if (!isOnlinePlayer(sender)) {
			sendMessage(sender,
					ReputationWebMessages.mustBePlayerOrAddArgsError());
			return;
		}
		Player player = (Player) sender;
		if (!hasPermission(player, infoSelfPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
			return;
		}

		coreInfoCommand(sender, player);
	}

	private void otherInfoCommand(CommandSender sender, String[] args) {
		if (isOnlinePlayer(sender)) {
			if (!hasPermission((Player) sender, infoAllPermissionNode)) {
				sendMessage(sender,
						ReputationWebMessages.lacksPermissionError());
				return;
			}
		}
		OfflinePlayer player = getRealPlayer(args[1]);
		if (player == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
			return;
		}
		coreInfoCommand(sender, player);
	}

	private void coreInfoCommand(CommandSender sender, OfflinePlayer player) {
		double reputation = reputationGraph.getReputation(player);
		int numberOfTrusters = reputationGraph.trustersCount(player);
		int numberOfTrustees = reputationGraph.trusteesCount(player);

		List<OfflinePlayer> topTrusters = reputationGraph.getTopTrusters(
				player, numOfTopTrusters);
		List<String> output = generateInfoOutput(player.getName(), reputation,
				numberOfTrusters, numberOfTrustees, topTrusters);
		sendMessage(sender, output);
		return;
	}

	private List<String> generateInfoOutput(String playerName,
			double reputation, int numberOfTrusters, int numberOfTrustees,
			List<OfflinePlayer> topTrusters)
	{
		List<String> output = new ArrayList<String>();
		output.add("Player: " + playerName);
		output.add("Reputation: " + reputation);
		String trustCountOutput = "Trusted by " + numberOfTrusters + " ";
		trustCountOutput += (numberOfTrusters == 1 ? "player" : "players")
				+ ". ";
		trustCountOutput += "Trusts " + numberOfTrustees + " ";
		trustCountOutput += (numberOfTrustees == 1 ? "player" : "players")
				+ ".";
		output.add(trustCountOutput);
		String topTrustersOutput = "Most reputable players who trust "
				+ playerName + ": ";
		for (int i = 0; i < topTrusters.size(); i++) {

			OfflinePlayer truster = topTrusters.get(i);
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

	private boolean hasPermission(Player player, String permissionNode) {
		return player.hasPermission(permissionNode);
	}

	private void sendMessage(CommandSender sender, List<String> message) {
		for (String line : message) {
			sendMessage(sender, line);
		}
	}

	private void sendMessage(CommandSender sender, String message) {
		sender.sendMessage(message);
	}

	private boolean isOnlinePlayer(CommandSender sender) {
		return sender instanceof Player;
	}

	/**
	 * Returns an OfflinePlayer object if and only if the player has been on the
	 * server before or is presently online. Returns null otherwise.
	 * 
	 * @param name
	 *            The name of the player to return a Player object for.
	 * @return The Player object with the specified name. Returns null if the
	 *         player by that name has never been on the server before.
	 */
	private OfflinePlayer getRealPlayer(String name) {
		OfflinePlayer potentialPlayer = server.getOfflinePlayer(name);
		if (potentialPlayer.hasPlayedBefore()
				|| reputationGraph.playerIsInGraph(potentialPlayer)) {
			return potentialPlayer;
		}
		return null;
	}

	private List<String> convertToNameList(List<OfflinePlayer> playerList) {
		List<String> nameList = new ArrayList<String>(playerList.size());
		for (OfflinePlayer player : playerList) {
			nameList.add(player.getName());
		}
		return nameList;
	}
}