package com.gradysimon.reputationweb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
	 * Determines how strongly the reputation of the truster affects the
	 * reputation of the trustee. Should be less than 1 and greater than 0.
	 */
	public final double flowMultiplier;

	/**
	 * Determines how many degrees away from a player in the reputation web
	 * another player can be while still influencing that player's reputation.
	 * If player1 vouches for player0, then they are 1 degree away from each
	 * other. If player2 then vouches for player1, player2 is 2 degrees from
	 * player0, and so on.
	 */
	public final int maxChainLength;

	private HashMap<Player, ReputationEntity> playerEntityMap = new HashMap<Player, ReputationEntity>();

	public ReputationGraph(double flowMultiplier, int maxChainLength) {
		this.flowMultiplier = flowMultiplier;
		this.maxChainLength = maxChainLength;
	}

	double getReputation(Player player) {
		if (playerEntityMap.containsKey(player)) {
			return playerEntityMap.get(player).getReputation();
		}
		return Double.NaN;
	}

	public void addTrustRelation(Player truster, Player trustee) {
		if (!playerEntityMap.containsKey(truster)) {
			addPlayerToGraph(truster);
		}
		if (!playerEntityMap.containsKey(trustee)) {
			addPlayerToGraph(trustee);
		}
		ReputationEntity trusterEntity = playerEntityMap.get(truster);
		ReputationEntity trusteeEntity = playerEntityMap.get(trustee);
		trusterEntity.addTrustee(trusteeEntity);
		trusteeEntity.addTruster(trusterEntity);
		propagateTrustChange(trusteeEntity);
	}

	private void addPlayerToGraph(Player player) {
		playerEntityMap.put(player, new ReputationEntity());
	}

	public void removeTrustRelation(Player truster, Player trustee) {
		if (playerEntityMap.containsKey(truster)
				&& playerEntityMap.containsKey(trustee)) {
			ReputationEntity trusterEntity = playerEntityMap.get(truster);
			ReputationEntity trusteeEntity = playerEntityMap.get(trustee);
			trusterEntity.removeTrustee(trusteeEntity);
			trusteeEntity.removeTruster(trusterEntity);
			propagateTrustChange(trusteeEntity);
		}
	}

	public boolean trustRelationExists(Player truster, Player trustee) {
		if (playerEntityMap.containsKey(truster)
				&& playerEntityMap.containsKey(trustee)) {
			ReputationEntity trusterEntity = playerEntityMap.get(truster);
			ReputationEntity trusteeEntity = playerEntityMap.get(trustee);
			return trusterEntity.trustsEntity(trusteeEntity);
		}
		return false;
	}

	/**
	 * Flag the reputations of all players whose reputation could have been
	 * affected by the vouch change as being potentially inaccurate. Should be
	 * called on the trustee, not the truster.
	 * 
	 * @param trustee
	 *            The ReputationEntity to propagate out from.
	 */
	private void propagateTrustChange(ReputationEntity trustee) {
		/*
		 * Loop maxChainLength times, each iteration moving one degree further
		 * out from the voucher. Keep track of the players whose reputations
		 * have already been updated, to ensure that they are not re-updated.
		 */
		HashSet<ReputationEntity> todoSet = new HashSet<ReputationEntity>();
		HashSet<ReputationEntity> nextSet = new HashSet<ReputationEntity>();
		HashSet<ReputationEntity> updatedSet = new HashSet<ReputationEntity>();
		todoSet.add(trustee);
		for (int i = 1; i <= maxChainLength; i++) {
			boolean isLast = (i == maxChainLength);
			for (ReputationEntity current : todoSet) {
				current.reputationIsAccurate = false;
				updatedSet.add(current);
				if (isLast) continue;
				for (ReputationEntity trusteeOfCurrent : current.trustees) {
					if (!updatedSet.contains(trusteeOfCurrent))
						nextSet.add(trusteeOfCurrent);
				}
			}
			if (nextSet.isEmpty()) break;
			todoSet.clear();
			todoSet.addAll(nextSet);
			nextSet.clear();
		}
	}

	public List<Player> findPathBetween(ReputationEntity player, ReputationEntity otherPlayer) {
		
		Queue queue = new LinkedList();
		HashMap<ReputationEntity, ReputationEntity> pathBackMap = new HashMap<ReputationEntity, ReputationEntity>();
		
	}

	private class ReputationEntity {
		// TODO: Decide if these methods should be package-private.
		// See rationale in ReputationGraph.java

		/**
		 * The player's reputation as of last reputation update
		 */
		private double reputation;

		/**
		 * Whether the reputation field is up to date. Used to make sure that
		 * getReputation() only returns up-to-date values.
		 */
		private boolean reputationIsAccurate;

		/**
		 * The set of players who trust this player
		 */
		private HashSet<ReputationEntity> trusters = new HashSet<ReputationEntity>();

		/**
		 * The set of players who this player trusts
		 */
		private HashSet<ReputationEntity> trustees = new HashSet<ReputationEntity>();

		private ReputationEntity() {
			this.reputation = 0.0;
			this.reputationIsAccurate = false;
		}

		public boolean trustsEntity(ReputationEntity trusteeEntity) {
			return trustees.contains(trusteeEntity);
		}

		public void removeTruster(ReputationEntity trusterEntity) {
			this.trusters.remove(trusterEntity);
		}

		public void removeTrustee(ReputationEntity trusteeEntity) {
			this.trustees.remove(trusteeEntity);
		}

		private void addTruster(ReputationEntity truster) {
			this.trusters.add(truster);
		}

		private void addTrustee(ReputationEntity trustee) {
			this.trustees.add(trustee);
		}

		/**
		 * Recalculate reputation for this ReputationEntity.
		 */
		private void updateReputation() {
			// Don't need to do anything if reputation is already up-to-date.
			if (this.reputationIsAccurate) return;

			this.reputation = 0.0;
			/*
			 * Will hold all of the ReputationEntities that are the current
			 * number of steps away from the ReputationEntity being calculated
			 * for.
			 */
			HashSet<ReputationEntity> currentDegreeSet = new HashSet<ReputationEntity>();
			/*
			 * Will hold the ReputationEntities who will be in the next
			 * iteration. If the current degree away from the player being
			 * calculated for is i, they are i+1 away from the player.
			 */
			HashSet<ReputationEntity> nextSet = new HashSet<ReputationEntity>();
			/*
			 * Will hold the ReputationEntities that have already been involved
			 * in the calculation. They go into this set once they are used to
			 * ensure that they are never double counted.
			 */
			HashSet<ReputationEntity> ignoreSet = new HashSet<ReputationEntity>();
			currentDegreeSet.addAll(this.trusters);
			ignoreSet.add(this);
			for (int i = 0; i < maxChainLength; i++) {
				boolean isLast = (i == maxChainLength - 1);
				this.reputation += Math.pow(flowMultiplier, i)
						* currentDegreeSet.size();
				if (isLast) break;
				ignoreSet.addAll(currentDegreeSet);
				for (ReputationEntity truster : currentDegreeSet) {
					for (ReputationEntity potentialNext : truster.trusters) {
						if (!ignoreSet.contains(potentialNext)) {
							nextSet.add(potentialNext);
						}
					}
				}
				if (nextSet.isEmpty()) break;
				currentDegreeSet.clear();
				currentDegreeSet.addAll(nextSet);
				nextSet.clear();
			}
			this.reputationIsAccurate = true;
		}

		/**
		 * Returns the player's current reputation. Returns the player's current
		 * reputation. This returned reputation value is guaranteed to be up to
		 * date.
		 * 
		 * @return The player's reputation.
		 */
		double getReputation() {
			if (!reputationIsAccurate) this.updateReputation();
			return this.reputation;
		}

	}

}
