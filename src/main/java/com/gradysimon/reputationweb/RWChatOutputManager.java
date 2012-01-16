package com.gradysimon.reputationweb;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class RWChatOutputManager {

	private ReputationGraph reputationGraph;

	RWChatOutputManager(ReputationGraph reputationGraph) {
		this.reputationGraph = reputationGraph;
	}

	void mustBePlayerError(CommandSender recipient) {
		String message = "You must be a player to execute that command.";
		sendMessage(recipient, message);
	}

	void mustBePlayerOrAddArgsError(CommandSender recipient) {
		String message = "You must be a player to execute that command. Try adding another player name.";
		sendMessage(recipient, message);
	}

	void lacksPermissionError(CommandSender recipient) {
		String message = "You lack the required permissions to execute that command.";
		sendMessage(recipient, message);
	}

	void playerDoesNotExistError(CommandSender recipient, String name) {
		String message = "The specifed player has not played on this server or does not exist: "
				+ name;
		sendMessage(recipient, message);
	}

	void alreadyTrustsPlayerError(CommandSender recipient, OfflinePlayer player)
	{
		String message = "You already trust the specified player: "
				+ formatPlayer(player);
		sendMessage(recipient, message);
	}

	void doesNotTrustPlayerError(CommandSender recipient, OfflinePlayer player)
	{
		String message = "You do not yet trust the specified player, so you cannot untrust: "
				+ formatPlayer(player);
		sendMessage(recipient, message);
	}

	void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer player)
	{
		String message = "There is no path of trust between you and player "
				+ formatPlayer(player);
		sendMessage(recipient, message);
	}

	void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer start,
			OfflinePlayer end)
	{
		String message = "There is no path of trust between player "
				+ formatPlayer(start) + " and player "
				+ formatPlayer(end);
		sendMessage(recipient, message);
	}

	void trustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = "Success. You now trust player " + formatPlayer(player);
		sendMessage(recipient, message);
	}

	void untrustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = "Success. You no longer trust " + formatPlayer(player);
		sendMessage(recipient, message);
	}

	void cannotTrustSelfError(CommandSender recipient) {
		String message = "You can only trust other players.";
		sendMessage(recipient, message);
	}

	void sendMessage(CommandSender recipient, List<String> message) {
		for (String line : message) {
			sendMessage(recipient, line);
		}
	}

	void sendMessage(CommandSender recipient, String message) {
		recipient.sendMessage(message);
	}

	void playerTrustedMessage(Player player, OfflinePlayer truster) {
		double newReputation = reputationGraph.getReputation(player);
		String message = formatPlayer(truster)
				+ " now trusts you. Your new reputation is "
				+ formatRep(newReputation) + ".";
		sendMessage(player, message);
	}

	void playerUntrustedMessage(Player player, OfflinePlayer truster) {
		double newReputation = reputationGraph.getReputation(player);
		String message = formatPlayer(truster)
				+ " has stopped trusting you. Your new reputation is "
				+ formatRep(newReputation) + ".";
		sendMessage(player, message);
	}

	void referralCommandOutput(CommandSender sender, List<String> path,
			OfflinePlayer startPlayer, OfflinePlayer endPlayer)
	{
		List<String> output = new ArrayList<String>();
		output.add("Chain of trust between " + formatPlayer(startPlayer)
				+ " and " + formatPlayer(endPlayer) + ":");
		String pathString = formatPlayer(startPlayer);
		for (String name : path) {
			pathString += " -> " + name;
		}
		output.add(pathString);
		sendMessage(sender, output);
	}

	void infoCommandOutput(CommandSender sender, OfflinePlayer player,
			List<OfflinePlayer> topTrusters)
	{
		double reputation = reputationGraph.getReputation(player);
		int numberOfTrusters = reputationGraph.trustersCount(player);
		int numberOfTrustees = reputationGraph.trusteesCount(player);
		String playerName = formatPlayer(player);
		List<String> output = new ArrayList<String>();
		output.add("Player: " + formatPlayer(player));
		output.add("Reputation: " + reputation);
		String trustCountOutput = "Trusted by " + formatNum(numberOfTrusters)
				+ " ";
		trustCountOutput += (numberOfTrusters == 1 ? "player" : "players")
				+ ". ";
		trustCountOutput += "Trusts " + formatNum(numberOfTrustees) + " ";
		trustCountOutput += (numberOfTrustees == 1 ? "player" : "players")
				+ ".";
		output.add(trustCountOutput);
		String topTrustersOutput = "Most reputable players who trust "
				+ playerName + ": ";
		for (int i = 0; i < topTrusters.size(); i++) {
			OfflinePlayer truster = topTrusters.get(i);
			topTrustersOutput += truster.getName();
			topTrustersOutput += "(" + reputationGraph.getReputation(truster)
					+ ")";
			if (i != topTrusters.size() - 1) {
				topTrustersOutput += ", ";
			}
		}
		output.add(topTrustersOutput);
		sendMessage(sender, output);
	}

	private static String formatRep(double reputation) {
		String formattedString = "(" + reputation + ")";
		return formattedString;
	}

	// TODO: consider using getDisplayName().
	private static String formatPlayer(OfflinePlayer player) {
		String formattedString = player.getName();
		return formattedString;
	}

	private static String formatNum(int num) {
		return "" + num;
	}

	private static String formatNum(double num) {
		return "" + num;
	}
}
