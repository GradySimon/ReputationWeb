package com.gradysimon.reputationweb;

import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

class RWChatOutput {	
	
	static void mustBePlayerError(CommandSender recipient) {
		String message = "You must be a player to execute that command.";
		sendMessage(recipient, message);
	}

	static void mustBePlayerOrAddArgsError(CommandSender recipient) {
		String message = "You must be a player to execute that command. Try adding another player name.";
		sendMessage(recipient, message);
	}

	static void lacksPermissionError(CommandSender recipient) {
		String message = "You lack the required permissions to execute that command.";
		sendMessage(recipient, message);
	}

	static void playerDoesNotExistError(CommandSender recipient, String name) {
		String message = "The specifed player has not played on this server or does not exist: " + name;
		sendMessage(recipient, message);
	}

	static void alreadyTrustsPlayerError(CommandSender recipient, OfflinePlayer player) {
		String message = "You already trust the specified player: " + player.getName();
		sendMessage(recipient, message);
	}

	static void doesNotTrustPlayerError(CommandSender recipient, OfflinePlayer player) {
		String message = "You do not yet trust the specified player, so you cannot untrust: " + player.getName();
		sendMessage(recipient, message);
	}

	static void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer player) {
		String message = "There is no path of trust between you and player " + player.getName();
		sendMessage(recipient, message);
	}

	static void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer start,
			OfflinePlayer end)
	{
		String message = "There is no path of trust between player " + start.getName()
				+ " and player " + end.getName();
		sendMessage(recipient, message);
	}

	static void trustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = "Success. You now trust player " + player.getName();
		sendMessage(recipient, message);
	}

	static void untrustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = "Success. You no longer trust " + player.getName();
		sendMessage(recipient, message);
	}

	static void cannotTrustSelfError(CommandSender recipient) {
		String message = "You can only trust other players.";
		sendMessage(recipient,message);
	}
	
	static void sendMessage(CommandSender recipient, List<String> message) {
		for (String line : message) {
			sendMessage(recipient, line);
		}
	}
	static void sendMessage(CommandSender recipient, String message) {
		recipient.sendMessage(message);
	}
}
