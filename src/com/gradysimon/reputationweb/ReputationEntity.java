package com.gradysimon.reputationweb;

import java.util.HashSet;

import org.bukkit.entity.Player;

/**
 * A node, representing one player, in the reputation web. Represents one player
 * in the reputation web. This class is the one primarily responsible for doing
 * the work that maintains the reputation web and keeps each player's reputation
 * up to date.
 * 
 * Details on how the reputation of a player is calculated and how the
 * reputation web works can be found in the documentation for the
 * ReputationGraph class.
 * 
 * @author Genre (Grady Simon)
 * 
 */
public class ReputationEntity {
	// TODO: Decide if these methods should be package-private.
	// See rationale in ReputationGraph.java

	/**
	 * The player represented by this ReputationEntity
	 */
	private Player player;

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
	 * The set of players who vouch for this player
	 */
	private HashSet<ReputationEntity> vouchers = new HashSet<ReputationEntity>();

	/**
	 * The set of players who this player vouches for
	 */
	private HashSet<ReputationEntity> vouchees = new HashSet<ReputationEntity>();

	public ReputationEntity(Player player) {
		this.player = player;
		this.reputation = 0.0;
		this.reputationIsAccurate = false;
	}

	/**
	 * Make this player vouch for another player. Vouches for the supplied
	 * player. This method updates this player's vouchee set and also the
	 * vouchee's voucher set. This method also propagates out from the vouchee
	 * to all other ReputationEntity objects whose reputation could be affected
	 * by this change. It sets the reputationIsAccurate flag to false for all
	 * ReputationEntity objects whose reputation calculations might no longer be
	 * accurate.
	 * 
	 * @param vouchee
	 *            The player that this player will vouch for.
	 * @return Returns true if the vouch was valid and successful. False
	 *         otherwise.
	 */
	boolean vouch(ReputationEntity vouchee) {
		// TODO: Should this return something more sophisticated to allow for
		// more specific error messages on failure?
		if (!this.vouchees.contains(vouchee)) {
			this.vouchees.add(vouchee);
			assert (!vouchee.vouchers.contains(this));
			vouchee.vouchers.add(this);
			propagateVouchChange(vouchee);
			return true;
		}
		return false;
	}

	/**
	 * Flag the reputations of all players whose reputation could have been
	 * affected by the vouch change as being potentially inaccurate.
	 * 
	 * @param vouchee
	 *            The ReputationEntity to propagate out from.
	 */
	private void propagateVouchChange(ReputationEntity vouchee) {
		/*
		 * Update all players who could have been affected by this vouch. The
		 * algorithm here is to loop REPUTATION_CHAIN_LENGTH times, each
		 * iteration moving one degree further out from the voucher. Keep track
		 * of the players whose reputations have already been updated, to ensure
		 * that they are not re-updated.
		 */
		HashSet<ReputationEntity> todoSet = new HashSet<ReputationEntity>();
		HashSet<ReputationEntity> nextSet = new HashSet<ReputationEntity>();
		HashSet<ReputationEntity> updatedSet = new HashSet<ReputationEntity>();
		todoSet.add(vouchee);
		for (int i = 1; i <= ReputationGraph.REPUTATION_CHAIN_LENGTH; i++) {
			boolean isLast = (i == ReputationGraph.REPUTATION_CHAIN_LENGTH);
			for (ReputationEntity current : todoSet) {
				/*
				 * Setting this to false means that next time the current
				 * player's reputation is checked, it has to be updated.
				 */
				current.reputationIsAccurate = false;
				updatedSet.add(current);
				if (isLast) continue; // The rest of this stuff is useless if
										// this is the last iteration in the
										// outer loop
				for (ReputationEntity voucheeOfCurrent : current.vouchees) {
					if (!updatedSet.contains(voucheeOfCurrent))
						nextSet.add(voucheeOfCurrent);
				}
			}
			if (nextSet.isEmpty()) break; // Don't do the next iteration if
											// there is nothing left to do.
			todoSet.clear();
			todoSet.addAll(nextSet);
			nextSet.clear();
		}
	}

	/**
	 * Recalculate reputation for this ReputationEntity.
	 */
	private void updateReputation() {
		// Don't need to do anything if reputation is already up-to-date.
		if (this.reputationIsAccurate) return;

		this.reputation = 0.0;
		/*
		 * Will hold all of the ReputationEntities that are the current number
		 * of steps away from the ReputationEntity being calculated for.
		 */
		HashSet<ReputationEntity> currentDegreeSet = new HashSet<ReputationEntity>();
		/*
		 * Will hold the ReputationEntities who will be in the next iteration.
		 * If the current degree away from the player being calculated for is i,
		 * they are i+1 away from the player.
		 */
		HashSet<ReputationEntity> nextSet = new HashSet<ReputationEntity>();
		/*
		 * Will hold the ReputationEntities that have already been involved in
		 * the calculation. They go into this set once they are used to ensure
		 * that they are never double counted.
		 */
		HashSet<ReputationEntity> alreadySeenSet = new HashSet<ReputationEntity>();
		currentDegreeSet.addAll(this.vouchers);
		alreadySeenSet.add(this); // To make sure the player being calculated
									// for isn't counted at all.
		/*
		 * Here we loop once for each level of distance (or degrees) away from
		 * the ReputationEntity being calculated for. We do this up to
		 * ReputationGraph.REPUTATION_CHAIN_LENGTH times. The number of times we
		 * loop reflects how far away from the ReputationEntity being calculated
		 * for we allow vouch changes to have an effect.
		 * 
		 * At each iteration of the loop, we add (REPUTATION_FLOW_MULTIPLIER ^
		 * i) * (number of vouchers at this distance from the ReputationEntity
		 * being calculated for) to the ReputationEntity's reputation. Then,
		 * loop through the ReputationEntities at this distance from the one
		 * being calculated for, and add each voucher of those
		 * ReputationEntities to the set of ReputationEntities on the next level
		 * of distance, to be used in the next iteration.
		 */
		for (int i = 0; i < ReputationGraph.REPUTATION_CHAIN_LENGTH; i++) {
			boolean isLast = (i == ReputationGraph.REPUTATION_CHAIN_LENGTH - 1);
			this.reputation += Math.pow(
					ReputationGraph.REPUTATION_FLOW_MULTIPLIER, i)
					* currentDegreeSet.size();
			if (isLast) break; // No need to do the rest if last iteration
			alreadySeenSet.addAll(currentDegreeSet);
			for (ReputationEntity voucher : currentDegreeSet) {
				for (ReputationEntity potentialNext : voucher.vouchers) {
					if (!alreadySeenSet.contains(potentialNext)) {
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
	 * Make this player stop vouching for another player. Updates the reputation
	 * graph to reflect that this player no longer vouches for the supplied
	 * player. Updates this player's vouchee set and also the vouchee's voucher
	 * set. This method also propagates out from the vouchee to all other
	 * ReputationEntity objects whose reputation could be affected by this
	 * change. It sets the reputationIsAccurate flag to false for all
	 * ReputationEntity objects whose reputation calculations might no longer be
	 * accurate.
	 * 
	 * @param vouchee
	 *            The player that this player will no longer vouch for.
	 * @return Returns true if the vouch removal was valid and successful. False
	 *         otherwise.
	 */
	boolean stopVouching(ReputationEntity vouchee) {
		// TODO: Should this return something more sophisticated to allow for
		// more specific error messages on failure?
		if (this.vouchees.contains(vouchee)) {
			this.vouchees.remove(vouchee);
			assert (vouchee.vouchers.contains(this));
			vouchee.vouchers.remove(this);
			propagateVouchChange(vouchee);
			return true;
		}
		return false;
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
