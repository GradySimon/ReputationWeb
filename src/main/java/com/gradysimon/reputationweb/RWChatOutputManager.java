package com.gradysimon.reputationweb;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class RWChatOutputManager {

	private ReputationGraph reputationGraph;

	RWChatOutputManager(ReputationGraph reputationGraph) {
		this.reputationGraph = reputationGraph;
	}

	void mustBePlayerError(CommandSender recipient) {
		String message = formatError("You must be a player to execute that command.");
		sendMessage(recipient, message);
	}

	void mustBePlayerOrAddArgsError(CommandSender recipient) {
		String message = formatError("You must be a player to execute that command. Try adding another player name.");
		sendMessage(recipient, message);
	}

	void lacksPermissionError(CommandSender recipient) {
		String message = formatError("You lack the required permissions to execute that command.");
		sendMessage(recipient, message);
	}

	void playerDoesNotExistError(CommandSender recipient, String name) {
		String message = formatError("The specifed player has not played on this server or does not exist: "
				+ name);
		sendMessage(recipient, message);
	}

	void alreadyTrustsPlayerError(CommandSender recipient, OfflinePlayer player)
	{
		String message = formatError("You already trust the specified player: ")
				+ formatPlayer(player);
		sendMessage(recipient, message);
	}

	void doesNotTrustPlayerError(CommandSender recipient, OfflinePlayer player)
	{
		String message = formatError("You do not yet trust the specified player, so you cannot untrust: ")
				+ formatPlayer(player);
		sendMessage(recipient, message);
	}

	void cannotTrustSelfError(CommandSender recipient) {
		String message = formatError("You can only trust other players.");
		sendMessage(recipient, message);
	}

	void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer player)
	{
		String message = formatNorm("There is no path of trust between you and player ")
				+ formatPlayer(player);
		sendMessage(recipient, message);
	}

	void noTrustPathExistsMessage(CommandSender recipient, OfflinePlayer start,
			OfflinePlayer end)
	{
		String message = formatNorm("There is no path of trust between player ")
				+ formatPlayer(start)
				+ formatNorm(" and player ")
				+ formatPlayer(end);
		sendMessage(recipient, message);
	}

	void trustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = formatNorm("Success. You now trust ")
				+ formatPlayer(player) + formatNorm(".");
		sendMessage(recipient, message);
	}

	void untrustSuccessMessage(CommandSender recipient, OfflinePlayer player) {
		String message = formatNorm("Success. You no longer trust ")
				+ formatPlayer(player) + formatNorm(".");
		sendMessage(recipient, message);
	}

	void playerTrustedMessage(Player player, OfflinePlayer truster) {
		double newReputation = reputationGraph.getReputation(player);
		String message = formatPlayer(truster)
				+ formatNorm(" now trusts you. Your new reputation is ")
				+ formatRep(newReputation) + formatNorm(".");
		sendMessage(player, message);
	}

	void playerUntrustedMessage(Player player, OfflinePlayer truster) {
		double newReputation = reputationGraph.getReputation(player);
		String message = formatPlayer(truster)
				+ formatNorm(" has stopped trusting you. Your new reputation is ")
				+ formatRep(newReputation) + ".";
		sendMessage(player, message);
	}

	void referralCommandOutput(CommandSender sender, List<OfflinePlayer> path,
			OfflinePlayer startPlayer, OfflinePlayer endPlayer)
	{
		List<String> output = new ArrayList<String>();
		output.add(formatNorm("Chain of trust between ")
				+ formatPlayer(startPlayer) + formatNorm(" and ")
				+ formatPlayer(endPlayer) + formatNorm(":"));
		String pathString = formatPlayer(startPlayer);
		for (OfflinePlayer player : path) {
			pathString += formatNorm(" -> ") + formatPlayer(player);
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
		List<String> output = new ArrayList<String>();
		output.add(formatHeader("==== Reputation Information ===="));
		output.add(formatNorm("Player: ") + formatPlayerNoRep(player));
		output.add(formatNorm("Reputation: ") + formatRep(reputation));
		String trustCountOutput = formatNorm("Trusted by ")
				+ formatNum(numberOfTrusters) + " ";
		trustCountOutput += formatNorm((numberOfTrusters == 1 ? "player"
				: "players") + ". ");
		trustCountOutput += formatNorm("Trusts ") + formatNum(numberOfTrustees)
				+ " ";
		trustCountOutput += formatNorm((numberOfTrustees == 1 ? "player"
				: "players") + ".");
		output.add(trustCountOutput);
		if (numberOfTrusters > 0) {
			String topTrustersOutput = formatNorm("Most reputable players who trust ")
					+ formatPlayerNoRep(player) + formatNorm(": ");
			output.add(topTrustersOutput);
			topTrustersOutput = "";
			for (int i = 0; i < topTrusters.size(); i++) {
				OfflinePlayer truster = topTrusters.get(i);
				topTrustersOutput += formatPlayer(truster);
				if (i != topTrusters.size() - 1) {
					topTrustersOutput += formatNorm(", ");
				}
			}
			output.add(topTrustersOutput);
		}
		output.add(formatHeader("============================"));
		sendMessage(sender, output);
	}

	void generalHelp(CommandSender sender) {
		List<String> output = new ArrayList<String>();
		String message = formatHelp("Type ") + formatCommand("/rep help <command>");
		message += formatHelp(" for command-specfic help, e.g. ");
		message += formatCommand("/rep help trust");
		output.add(message);
		message = formatHelp("Available commands: ");
		output.add(message);
		output.add(formatCommand("/rep trust <player>"));
		output.add(formatCommand("/rep untrust <player>"));
		output.add(formatCommand("/rep info <player>"));
		output.add(formatCommand("/rep connection <player>"));
		sendMessage(sender, output);
	}

	void trustHelp(CommandSender sender) {
		List<String> output = new ArrayList<String>();
		String message = "";
		message = formatHelp("Usage: ");
		message += formatCommand("/trust <player>");
		message += formatHelp(" or ");
		message += formatCommand("/rep trust <player>");
		output.add(message);
		message = formatHelp("Trust the specified player.");
		output.add(message);
		sendMessage(sender, output);
	}

	void untrustHelp(CommandSender sender) {
		List<String> output = new ArrayList<String>();
		String message = "";
		message = formatHelp("Usage: ");
		message += formatCommand("/untrust <player>");
		message += formatHelp(" or ");
		message += formatCommand("/rep untrust <player>");
		output.add(message);
		message = formatHelp("Stop trusting the specified player.");
		output.add(message);
		sendMessage(sender, output);
	}

	void infoHelp(CommandSender sender) {
		List<String> output = new ArrayList<String>();
		String message = "";
		message = formatHelp("Usage: ");
		message += formatCommand("/rep info");
		message += formatHelp(" or ");
		message += formatCommand("/rep info <player>");
		output.add(message);
		message = formatHelp("Display reputation information about the "
				+ "specified player.");
		output.add(message);
		message = formatHelp("If no player is specified, display "
				+ "reputation information about yourself.");
		output.add(message);
		sendMessage(sender, output);
	}

	void connectionHelp(CommandSender sender) {
		List<String> output = new ArrayList<String>();
		String message = "";
		message = formatHelp("Usage: ");
		message += formatCommand("/rep connection <player>");
		message += formatHelp(" or ");
		message += formatCommand("/rep connection <player> <player>");
		output.add(message);
		message = formatHelp("Find a path of trust between you and the "
				+ "specified player.");
		output.add(message);
		message = formatHelp("If two players are specified, find a path "
				+ "between them.");
		output.add(message);
		sendMessage(sender, output);
	}

	void sendMessage(CommandSender recipient, List<String> message) {
		for (String line : message) {
			sendMessage(recipient, line);
	
		}
	}

	void sendMessage(CommandSender recipient, String message) {
		if (!(recipient instanceof Player)) {
			message = ChatColor.stripColor(message);
		}
		recipient.sendMessage(message);
	}

	private static String formatHeader(String string) {
		return formatNorm(string);
	}

	private static String formatRep(double reputation) {
		String formattedString = ChatColor.BLUE + "(" + reputation + ")";
		return formattedString;
	}

	private String formatRep(OfflinePlayer player) {
		return formatRep(reputationGraph.getReputation(player));
	}

	// TODO: consider using getDisplayName().
	private String formatPlayer(OfflinePlayer player) {
		return formatPlayerNoRep(player) + formatRep(player);
	}

	private String formatPlayerNoRep(OfflinePlayer player) {
		String formattedString = ChatColor.AQUA + player.getName();
		return formattedString;
	}

	private static String formatNum(int num) {
		return "" + ChatColor.YELLOW + num;
	}

	private static String formatNum(double num) {
		return "" + ChatColor.YELLOW + num;
	}

	private static String formatError(String message) {
		return ChatColor.RED + message;
	}

	private static String formatNorm(String message) {
		return ChatColor.GREEN + message;
	}

	private String formatCommand(String string) {
		return ChatColor.YELLOW + string;
	}

	private String formatHelp(String string) {
		return formatNorm(string);
	}
}
