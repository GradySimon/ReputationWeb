package com.gradysimon.reputationweb;

import java.util.HashMap;

import org.bukkit.entity.Player;

/**
 * The reputation web that drives this reputation plugin. Represents a graph, in
 * the computer science sense of the term, which is itself the reputation web
 * that underlies the plugin.
 * 
 * All players who have either vouched for another player or have been vouched
 * for by another player are represented as ReputationEntities within this
 * ReputationGraph.
 * 
 * A player gains reputation by being vouched for by another player. Vouching
 * for another player indicates that the voucher trusts the vouchee. A player's
 * reputation is determined not strictly by the number of players who vouch for
 * him, but rather by how trusted the players are who vouch for him. A player
 * with a high reputation who vouches for a vouchee will influence that
 * vouchee's reputation more than if a low-reputation player were to vouch for
 * that vouchee.
 * 
 * The calculation for reputation is fairly complicated. It is not simply the
 * numerical reputation of a player that sets how influential his vouches are.
 * If it were, this would mean that cycles (by which I mean loops) in the
 * reputation graph would mean that players can influence their own reputation
 * by vouching for those who vouch for them, and it would be possible to greatly
 * increase one's reputation by creating such cycles.
 * 
 * Much of the mechanics for the calculations of the reputation web are actually
 * found in ReputationEntity
 * 
 * @author Genre (Grady Simon)
 * 
 */
public class ReputationGraph {
	/*
	 * TODO: Decide if methods should be package-private Should these methods
	 * really be package-private? My rationale is that this will prevent other
	 * plugins from messing with them, thus providing assurance to users that
	 * reputations are legitmate.
	 */

	/**
	 * Determines how strongly the reputation of the voucher affects the
	 * reputation of the vouchee. Used to determine how much the reputation of a
	 * voucher affects the reputation of the vouchee. Should be less than 1 and
	 * greater than 0.
	 */
	public static final double REPUTATION_FLOW_MULTIPLIER = 0.5;

	/**
	 * Determines how many degrees away from a player in the reputation web
	 * another player can be while still influencing that player's reputation.
	 * If player1 vouches for player0, then they are 1 degree away from each
	 * other. If player2 then vouches for player1, player2 is 2 degrees from
	 * player0, and so on.
	 * 
	 * This number should be greater than 1. The higher it is, the greater the
	 * performance hit of each new vouch action will be. I recommend keeping it
	 * below 6.
	 */
	public static final int REPUTATION_CHAIN_LENGTH = 5;

	private HashMap<Player, ReputationEntity> playerEntityMap = new HashMap<Player, ReputationEntity>();

	void updateReputationForAll() {
		
	}

	boolean updateReputation(Player player) {
		
		return false;
	}

	double getReputation(Player player) {
		if (playerEntityMap.containsKey(player)) {
			return playerEntityMap.get(player).getReputation();
		}
		return Double.NaN;
	}

	public boolean addVouch(Player voucher, Player vouchee) {
		if (playerEntityMap.containsKey(voucher)
				&& playerEntityMap.containsKey(vouchee)) {
			ReputationEntity voucherEntity = playerEntityMap.get(voucher);
			ReputationEntity voucheeEntity = playerEntityMap.get(vouchee);
			if (voucherEntity.vouch(voucheeEntity)) {
				// Note that the call to ReputationEntity.vouch() actually adds
				// the vouch as well as checking if it was valid.
				return true;
			}
		}
		return false;
	}

	public boolean removeVouch(Player voucher, Player vouchee) {
		if (playerEntityMap.containsKey(voucher)
				&& playerEntityMap.containsKey(vouchee)) {
			ReputationEntity voucherEntity = playerEntityMap.get(voucher);
			ReputationEntity voucheeEntity = playerEntityMap.get(vouchee);
			if (voucherEntity.stopVouching(voucheeEntity)) {
				// Note that the call to ReputationEntity.stopvVouching()
				// actually removes the vouch as well as checking if it was
				// valid.
				return true;
			}
		}
		return false;
	}

}
