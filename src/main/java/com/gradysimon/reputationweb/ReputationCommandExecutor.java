package com.gradysimon.reputationweb;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
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
	RWChatOutputManager output;
	Set<OfflinePlayer> havePlayedBefore = new HashSet<OfflinePlayer>();

	private final int numOfTopTrusters = 5;

	private final String trustPermissionNode = "reputationweb.trust";
	private final String infoSelfPermissionNode = "reputationweb.info.self";
	private final String infoAllPermissionNode = "reputationweb.info.all";
	private final String connectionSelfPermissionNode = "reputationweb.connection.self";
	private final String connectionAllPermissionNode = "reputationweb.connection.all";

	ReputationCommandExecutor(ReputationWeb plugin,
			ReputationGraph reputationGraph, Server server) {
		this.plugin = plugin;
		this.reputationGraph = reputationGraph;
		this.server = server;
		output = new RWChatOutputManager(reputationGraph);
		loadPlayersWhoHavePlayed();
	}

	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args)
	{
		label = label.toLowerCase();
		if (label.equals("rep") || label.equals("reputation")) {
			return dispatchReputationCommand(sender, args);
		} else if (label.equals("trust") && args.length > 0) {
			trustCommand(sender, args[0]);
			return true;
		} else if (label.equals("untrust") && args.length > 0) {
			untrustCommand(sender, args[0]);
			return true;
		}
		return false;
	}

	private void helpCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			output.generalHelp(sender);
		} else if (args.length > 1) {
			if (args[1].equals("trust")) {
				output.trustHelp(sender);
			} else if (args[1].equals("untrust")) {
				output.untrustHelp(sender);
			} else if (args[1].equals("info")) {
				output.infoHelp(sender);
			} else if (args[1].equals("connection")) {
				output.connectionHelp(sender);
			}
		}

	}

	private boolean dispatchReputationCommand(CommandSender sender,
			String[] args)
	{
		if (args.length > 0) {
			String firstArg = args[0].toLowerCase();
			if (firstArg.equals("trust") && args.length > 1) {
				trustCommand(sender, args[1]);
				return true;
			} else if (firstArg.equals("untrust") && args.length > 1) {
				untrustCommand(sender, args[1]);
				return true;
			} else if (firstArg.equals("info")) {
				infoCommand(sender, args);
				return true;
			} else if (firstArg.equals("connection") && args.length > 1) {
				referralCommand(sender, args);
				return true;
			} else if (firstArg.equals("help")) {
				helpCommand(sender, args);
				return true;
			}
		}
		return false;
	}

	private void trustCommand(CommandSender sender, String targetName) {
		if (!isOnlinePlayer(sender)) {
			output.mustBePlayerError(sender);
			return;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, trustPermissionNode)) {
			output.lacksPermissionError(sender);
			return;
		}
		OfflinePlayer otherPlayer = getRealPlayer(targetName);
		if (otherPlayer == null) {
			output.playerDoesNotExistError(sender, targetName);
			return;
		}
		if (senderPlayer == otherPlayer) {
			output.cannotTrustSelfError(sender);
			return;
		}
		if (reputationGraph.trustRelationExists(senderPlayer, otherPlayer)) {
			output.alreadyTrustsPlayerError(sender, otherPlayer);
			return;
		}
		addTrustToDatabase(senderPlayer.getName(), otherPlayer.getName());
		reputationGraph.addTrustRelation(senderPlayer, otherPlayer);
		output.trustSuccessMessage(sender, otherPlayer);
		if (otherPlayer.isOnline()) {
			output.playerTrustedMessage(otherPlayer.getPlayer(), senderPlayer);
		}
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
			output.mustBePlayerError(sender);
			return;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, trustPermissionNode)) {
			output.lacksPermissionError(sender);
			return;
		}
		OfflinePlayer otherPlayer = getRealPlayer(targetName);
		if (otherPlayer == null) {
			output.playerDoesNotExistError(sender, targetName);
			return;
		}
		if (!reputationGraph.trustRelationExists(senderPlayer, otherPlayer)) {
			output.doesNotTrustPlayerError(sender, otherPlayer);
			return;
		}
		removeTrustFromDatabase(senderPlayer.getName(), otherPlayer.getName());
		reputationGraph.removeTrustRelation(senderPlayer, otherPlayer);
		output.untrustSuccessMessage(sender, otherPlayer);
		if (otherPlayer.isOnline()) {
			output.playerUntrustedMessage(otherPlayer.getPlayer(), senderPlayer);
		}
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
			output.mustBePlayerOrAddArgsError(sender);
			return;
		}
		Player senderPlayer = (Player) sender;
		if (!hasPermission(senderPlayer, connectionSelfPermissionNode)) {
			output.lacksPermissionError(sender);
			return;
		}
		OfflinePlayer otherPlayer = getRealPlayer(args[1]);
		if (otherPlayer == null) {
			output.playerDoesNotExistError(sender, args[1]);
			return;
		}
		List<OfflinePlayer> path = reputationGraph.getReference(senderPlayer,
				otherPlayer);
		if (path == null) {
			output.noTrustPathExistsMessage(sender, otherPlayer);
			return;
		}
		output.referralCommandOutput(sender, path, senderPlayer, otherPlayer);
	}

	private void otherReferralCommand(CommandSender sender, String[] args) {
		if (isOnlinePlayer(sender)) {
			if (!hasPermission((Player) sender, connectionAllPermissionNode)) {
				output.lacksPermissionError(sender);
				return;
			}
		}
		OfflinePlayer startPlayer = getRealPlayer(args[1]);
		OfflinePlayer endPlayer = getRealPlayer(args[2]);
		if (startPlayer == null) {
			output.playerDoesNotExistError(sender, args[1]);
			return;
		}
		if (endPlayer == null) {
			output.playerDoesNotExistError(sender, args[2]);
			return;
		}
		List<OfflinePlayer> path = reputationGraph.getReference(startPlayer,
				endPlayer);
		if (path == null) {
			output.noTrustPathExistsMessage(sender, startPlayer, endPlayer);
			return;
		}
		output.referralCommandOutput(sender, path, startPlayer, endPlayer);
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
			output.mustBePlayerOrAddArgsError(sender);
			return;
		}
		Player player = (Player) sender;
		if (!hasPermission(player, infoSelfPermissionNode)) {
			output.lacksPermissionError(sender);
			return;
		}
		coreInfoCommand(sender, player);
	}

	private void otherInfoCommand(CommandSender sender, String[] args) {
		if (isOnlinePlayer(sender)) {
			if (!hasPermission((Player) sender, infoAllPermissionNode)) {
				output.lacksPermissionError(sender);
				return;
			}
		}
		OfflinePlayer player = getRealPlayer(args[1]);
		if (player == null) {
			output.playerDoesNotExistError(sender, args[1]);
			return;
		}
		coreInfoCommand(sender, player);
	}

	private void coreInfoCommand(CommandSender sender, OfflinePlayer player) {
		List<OfflinePlayer> topTrusters = reputationGraph.getTopTrusters(
				player, numOfTopTrusters);
		output.infoCommandOutput(sender, player, topTrusters);
	}

	private boolean hasPermission(Player player, String permissionNode) {
		return player.hasPermission(permissionNode);
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
	// The layout of this method seems funky, but it is like that to avoid as
	// much as possible the expensive hasPlayedBefore() method.
	private OfflinePlayer getRealPlayer(String name) {
		OfflinePlayer potentialPlayer = server.getOfflinePlayer(name);
		if (reputationGraph.playerIsInGraph(potentialPlayer)) {
			return potentialPlayer;
		}
		if (hasPlayedBefore(potentialPlayer)) {
			return potentialPlayer;
		}
		return null;
	}

	private boolean hasPlayedBefore(OfflinePlayer player) {
		if (havePlayedBefore.contains(player)) {
			return true;
		}
		String playerName = player.getName();
		List<World> worlds = server.getWorlds();
		for (World world : worlds) {
			File playersFolder = new File(world.getWorldFolder(), "players");
			if (!playersFolder.exists()) break;
			for (String fileName : playersFolder.list()) {
				String name = fileName.substring(0, fileName.length() - 4);
				if (name.equals(playerName)) {
					return true;
				}
			}
		}
		return false;
	}

	private void loadPlayersWhoHavePlayed() {
		List<World> worlds = server.getWorlds();
		for (World world : worlds) {
			File playersFolder = new File(world.getWorldFolder(), "players");
			if (!playersFolder.exists()) break;
			for (String fileName : playersFolder.list()) {
				String name = fileName.substring(0, fileName.length() - 4);
				havePlayedBefore.add(server.getOfflinePlayer(name));
			}
		}
	}
}