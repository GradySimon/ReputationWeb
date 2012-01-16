package com.gradysimon.reputationweb;

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
				+ player.getName();
		sendMessage(recipient, message);
	}

	void doesNotTrustPlayerError(CommandSender recipient, OfflinePlayer player)
	{
		String message = "You do not yet trust the specified player, so you cannot untrust: "
				+ player.getName();
		sendMessage(recipient, message);
	}

	void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer player)
	{
		String message = "There is no path of trust between you and player "
				+ player.getName();
		sendMessage(recipient, message);
	}

	void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer start,
			OfflinePlayer end)
	{
		String message = "There is no path of trust between player "
				+ start.getName() + " and player " + end.getName();
		sendMessage(recipient, message);
	}

	void trustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = "Success. You now trust player " + player.getName();
		sendMessage(recipient, message);
	}

	void untrustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = "Success. You no longer trust " + player.getName();
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
		String message = truster.getName()
				+ " now trusts you. Your new reputation is "
				+ formatRep(newReputation) + ".";
		sendMessage(player, message);
	}

	void playerUntrustedMessage(Player player, OfflinePlayer truster) {
		double newReputation = reputationGraph.getReputation(player);
		String message = truster.getName()
				+ " has stopped trusting you. Your new reputation is "
				+ formatRep(newReputation) + ".";
		sendMessage(player, message);
	}

	private static String formatRep(double reputation) {
		String formattedString = "(" + reputation + ")";
		return formattedString;
	}

	private static String formatPlayerName(OfflinePlayer player) {
		String formattedString = player.getName();
		return formattedString;
	}
}
