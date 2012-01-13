package com.gradysimon.reputationweb;
import org.bukkit.entity.Player;

class ReputationWebMessages {
	public static String mustBePlayerError() {
		return "You must be a player to execute that command.";
	}

	public static String mustBePlayerOrAddArgsError() {
		return "You must be a player to execute that command. Try adding another player name.";
	}

	public static String lacksPermissionError() {
		return "You lack the required permissions to execute that command.";
	}

	public static String playerDoesNotExistError(String name) {
		return "The specifed player has not played on this server or does not exist: "
				+ name;
	}

	public static String alreadyTrustsPlayerError(Player player) {
		return "You already trust the specified player: " + player.getName();
	}

	public static String doesNotTrustPlayerError(Player player) {
		return "You do not yet trust the specified player, so you cannot untrust: "
				+ player.getName();
	}

	public static String noTrustPathExistsMessage(Player player) {
		return "There is no path of trust between you and player "
				+ player.getName();
	}

	public static String noTrustPathExistsMessage(Player start, Player end) {
		return "There is no path of trust between player " + start.getName()
				+ " and player " + end.getName();
	}
	
	public static String trustSuccessMessage(Player player) {
		return "Success. You now trust player " + player.getName();
	}
	
	public static String untrustSuccessMessage(Player player) {
		return "Success. You no longer trust " + player.getName();
	}
	
	public static String cannotTrustSelfError() {
		return "You can only trust other players.";
	}
}
