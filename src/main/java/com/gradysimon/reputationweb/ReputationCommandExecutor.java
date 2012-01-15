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

	private final String trustPermissionNode = "reputationweb.trust";
	private final String infoSelfPermissionNode = "reputationweb.info.self";
	private final String infoAllPermissionNode = "reputationweb.info.all";
	private final String connectionSelfPermissionNode = "reputationweb.connection.self";
	private final String connectionAllPermissionNode = "reputationweb.connection.all";

	public ReputationCommandExecutor(ReputationWeb mainClass,
			ReputationGraph reputationGraph, Server server) {
		this.mainClass = mainClass;
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
		if (args.length > 0) {
			String firstArg = args[0].toLowerCase();
			if (firstArg.equals("trust")) {
				return trustCommand(sender, args);
			} else if (firstArg.equals("untrust")) {
				return untrustCommand(sender, args);
			} else if (firstArg.equals("info")) {
				return infoCommand(sender, args);
			} else if (firstArg.equals("connection")) {
				return referralCommand(sender, args);
			} else {
				// TODO: should I have a help command?
				return false;
			}
		}
		return false;
	}

	private boolean trustCommand(CommandSender sender, String[] args) {
		// TODO: DEBUGGING CODE
		System.out.println("Entered trustCommand()");
		// END DEBUG CODE

		if (!isPlayer(sender)) {
			sendMessage(sender, ReputationWebMessages.mustBePlayerError());
			return true;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, trustPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
			return true;
		}
		// TODO: DEBUGGING CODE
		System.out.println("trustCommand() passed permission check for player "
				+ senderPlayer.getName());
		// END DEBUG CODE
		OfflinePlayer otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
			return true; // if otherPlayer is null, the player was not found on
							// the server
		}
		if (senderPlayer == otherPlayer) {
			sendMessage(sender, ReputationWebMessages.cannotTrustSelfError());
			return true;
		}
		if (reputationGraph.trustRelationExists(senderPlayer, otherPlayer)) {
			sendMessage(sender,
					ReputationWebMessages.alreadyTrustsPlayerError(otherPlayer));
			return true;
		}
		reputationGraph.addTrustRelation(senderPlayer, otherPlayer);
		sendMessage(sender,
				ReputationWebMessages.trustSuccessMessage(otherPlayer));
		return true;
	}

	private boolean untrustCommand(CommandSender sender, String[] args) {
		// TODO: DEBUGGING CODE
		System.out.println("Entered untrustCommand()");
		// END DEBUG CODE

		if (!isPlayer(sender)) {
			sendMessage(sender, ReputationWebMessages.mustBePlayerError());
			return true;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, trustPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
			return true;
		}
		// TODO: DEBUGGING CODE
		System.out
				.println("untrustCommand() passed permission check for player "
						+ senderPlayer.getName());
		// END DEBUG CODE
		OfflinePlayer otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
			return true; // if otherPlayer is null, the player was not found on
							// the server
		}
		if (!reputationGraph.trustRelationExists(senderPlayer, otherPlayer)) {
			sendMessage(sender,
					ReputationWebMessages.doesNotTrustPlayerError(otherPlayer));
			return true;
		}
		reputationGraph.removeTrustRelation(senderPlayer, otherPlayer);
		// TODO: Untrust success message
		return true;
	}

	private boolean referralCommand(CommandSender sender, String[] args) {
		// TODO: DEBUGGING CODE
		System.out.println("Entered referralCommand()");
		// END DEBUG CODE
		if (args.length == 2) {
			return selfReferralCommand(sender, args);
		}
		if (args.length == 3) {
			return otherReferralCommand(sender, args);
		}
		return false;
	}

	private boolean selfReferralCommand(CommandSender sender, String[] args) {
		// TODO: DEBUGGING CODE
		System.out.println("Entered selfReferralCommand()");
		// END DEBUG CODE

		if (!isPlayer(sender)) {
			sendMessage(sender,
					ReputationWebMessages.mustBePlayerOrAddArgsError());
			return true;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, connectionSelfPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
			return true;
		}
		// TODO: DEBUGGING CODE
		System.out
				.println("selfReferralCommand() passed permission check for player "
						+ senderPlayer.getName());
		// END DEBUG CODE
		OfflinePlayer otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
			return true;
		}
		List<OfflinePlayer> path = reputationGraph.getReference(senderPlayer,
				otherPlayer);
		if (path == null) {
			// If this is null, there is no path between them
			// TODO: ensure that getReference returns null if there is no
			// path.
			sendMessage(sender,
					ReputationWebMessages.noTrustPathExistsMessage(otherPlayer));
			return true;
		}
		List<String> namesInPath = convertToNameList(path);
		// TODO: Move generation of this output to ReputationWebMessages.
		List<String> output = generateReferralOutput(namesInPath,
				senderPlayer.getName(), otherPlayer.getName());
		sendMessage(sender, output);
		return true;
	}

	private boolean otherReferralCommand(CommandSender sender, String[] args) {
		// TODO: DEBUGGING CODE
		System.out.println("Entered otherReferralCommand()");
		// END DEBUG CODE

		if (isPlayer(sender)) {
			if (!hasPermission((Player) sender, connectionAllPermissionNode)) {
				sendMessage(sender,
						ReputationWebMessages.lacksPermissionError());
				return true;
			}
		}
		// TODO: DEBUGGING CODE
		System.out.println("otherReferralCommand() passed permission check");
		// END DEBUG CODE
		OfflinePlayer startPlayer = getRealPlayer(args[1]);
		OfflinePlayer endPlayer = getRealPlayer(args[2]);
		if (startPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
			return true;
		}
		if (endPlayer == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[2]));
			return true;
		}
		List<OfflinePlayer> path = reputationGraph.getReference(startPlayer,
				endPlayer);
		if (path == null) {
			// If this is null, there is no path between them
			// TODO: ensure that getReference returns null if there is no
			// path.
			sendMessage(sender, ReputationWebMessages.noTrustPathExistsMessage(
					startPlayer, endPlayer));
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
		// TODO: DEBUGGING CODE
		System.out.println("Entered generateReferralOutput()");
		// END DEBUG CODE

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
		// TODO: DEBUGGING CODE
		System.out.println("Entered infoCommand()");
		// END DEBUG CODE

		if (args.length == 1) {
			return selfInfoCommand(sender);
		}
		if (args.length == 2) {
			return otherInfoCommand(sender, args);
		}
		return false;
	}

	private boolean selfInfoCommand(CommandSender sender) {
		// TODO: DEBUGGING CODE
		System.out.println("Entered selfInfoCommand()");
		// END DEBUG CODE
		if (!isPlayer(sender)) {
			sendMessage(sender,
					ReputationWebMessages.mustBePlayerOrAddArgsError());
			return true;
		}
		Player player = (Player) sender;
		if (!hasPermission(player, infoSelfPermissionNode)) {
			sendMessage(sender, ReputationWebMessages.lacksPermissionError());
			return true;
		}
		// TODO: DEBUGGING CODE
		System.out
				.println("selfInfoCommand() passed permission check for player "
						+ player.getName());
		// END DEBUG CODE
		return coreInfoCommand(sender, player);
	}

	private boolean otherInfoCommand(CommandSender sender, String[] args) {
		// TODO: DEBUGGING CODE
		System.out.println("Entered otherInfoCommand()");
		// END DEBUG CODE

		if (isPlayer(sender)) {
			if (!hasPermission((Player) sender, infoAllPermissionNode)) {
				sendMessage(sender,
						ReputationWebMessages.lacksPermissionError());
				return true;
			}
		}
		// TODO: DEBUGGING CODE
		System.out
				.println("otherInfoCommand() passed permission check for player");
		// END DEBUG CODE
		OfflinePlayer player = getRealPlayer(args[1]);
		if (player == null) {
			sendMessage(sender,
					ReputationWebMessages.playerDoesNotExistError(args[1]));
			return true;
		}
		return coreInfoCommand(sender, player);
	}

	private boolean coreInfoCommand(CommandSender sender, OfflinePlayer player)
	{
		// TODO: DEBUGGING CODE
		System.out.println("Entered coreInfoCommand()");
		// END DEBUG CODE

		double reputation = reputationGraph.getReputation(player);
		int numberOfTrusters = reputationGraph.trustersCount(player);
		int numberOfTrustees = reputationGraph.trusteesCount(player);

		List<OfflinePlayer> topTrusters = reputationGraph.getTopTrusters(
				player, numOfTopTrusters);
		List<String> output = generateInfoOutput(player.getName(), reputation,
				numberOfTrusters, numberOfTrustees, topTrusters);
		sendMessage(sender, output);
		return true;
	}

	private List<String> generateInfoOutput(String playerName,
			double reputation, int numberOfTrusters, int numberOfTrustees,
			List<OfflinePlayer> topTrusters)
	{
		// TODO: DEBUGGING CODE
		System.out.println("Entered generateInfoOutput()");
		// END DEBUG CODE

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

	private boolean isPlayer(CommandSender sender) {
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
		debugPrintOfflinePlayers();
		// TODO: DEBUGGING CODE
		System.out.println("Ingoing name to getRealPlayer(): " + name);
		// END DEBUGGING CODE
		OfflinePlayer potentialPlayer = server.getOfflinePlayer(name);
		if (potentialPlayer.hasPlayedBefore() || reputationGraph.playerIsInGraph(potentialPlayer)) {
			// TODO: DEBUGGING CODE
			System.out.println("Name of outgoing OfflinePlayer: "
					+ potentialPlayer.getName());
			// END DEBUGGING CODE
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
	// TODO DEBUGGING CODE
	private void debugPrintOfflinePlayers() {
		System.out.println("Printing all players who have ever played on this server:");
		for (OfflinePlayer player : server.getOfflinePlayers()) {
			System.out.println(player.getName());
		}
	}
}