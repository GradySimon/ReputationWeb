package com.gradysimon.reputationweb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.bukkit.OfflinePlayer;

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
 * numerical reputation of a player that sets how influential his trusts are. If
 * it were, this would mean that cycles (by which I mean loops) in the
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
	private final double flowMultiplier;

	/**
	 * Determines how many degrees away from a player in the reputation web
	 * another player can be while still influencing that player's reputation.
	 * If player1 vouches for player0, then they are 1 degree away from each
	 * other. If player2 then vouches for player1, player2 is 2 degrees from
	 * player0, and so on.
	 */
	private final int maxChainLength;

	private Map<OfflinePlayer, ReputationEntity> playerEntityMap = new HashMap<OfflinePlayer, ReputationEntity>();

	ReputationGraph(double flowMultiplier, int maxChainLength) {
		this.flowMultiplier = flowMultiplier;
		this.maxChainLength = maxChainLength;
	}

	/**
	 * Returns the reputation of the given player. If they player is not yet
	 * represented in the graph, which would mean that the player is not trusted
	 * by anyone and does not trust anyone, this method returns 0.
	 * 
	 * @param player
	 * @return
	 */
	public double getReputation(OfflinePlayer player) {
		if (playerIsInGraph(player)) {
			return getEntity(player).getReputation();
		}
		return 0;
	}

	/**
	 * Adds a trust relationship from OfflinePlayer truster to OfflinePlayer
	 * trustee. Updates the corresponding ReputationEntity objects for the two
	 * OfflinePlayer objects to reflect this change. If either of the players
	 * were not previously represented in the reputation graph, this method will
	 * ensure that they are added to it.
	 * 
	 * @param truster
	 *            The player who should now trust the trustee
	 * @param trustee
	 *            The player who should now be trusted by the truster
	 */
	void addTrustRelation(OfflinePlayer truster, OfflinePlayer trustee) {
		if (!playerIsInGraph(truster)) {
			addPlayerToGraph(truster);
		}
		if (!playerIsInGraph(trustee)) {
			addPlayerToGraph(trustee);
		}
		ReputationEntity trusterEntity = getEntity(truster);
		ReputationEntity trusteeEntity = getEntity(trustee);
		trusterEntity.addTrustee(trusteeEntity);
		trusteeEntity.addTruster(trusterEntity);
		propagateTrustChange(trusteeEntity);
	}

	/**
	 * Removes a trust relationship, if one exists, between the truster and the
	 * trustee. Updates the Reputation Graph to reflect this and each player's
	 * corresponding ReputationEntity.
	 * 
	 * @param truster
	 *            The player that should no longer trust the trustee
	 * @param trustee
	 *            The trustee that should no longer be trusted by the truster
	 */
	void removeTrustRelation(OfflinePlayer truster, OfflinePlayer trustee)
	{
		if (playerIsInGraph(truster) && playerIsInGraph(trustee)) {
			ReputationEntity trusterEntity = getEntity(truster);
			ReputationEntity trusteeEntity = getEntity(trustee);
			trusterEntity.removeTrustee(trusteeEntity);
			trusteeEntity.removeTruster(trusterEntity);
			propagateTrustChange(trusteeEntity);
		}
	}

	/**
	 * Returns true if the truster trusts the trustee.
	 * 
	 * @param truster
	 *            The OfflinePlayer object that potentially trusts the trustee
	 * @param trustee
	 *            The OfflinePlayer object that is potentially trusted by the
	 *            truster
	 * @return true if the truster does trust the trustee, false if the truster
	 *         does not trust the trustee or if one or both of the players have
	 *         not been entered into the reputation web yet.
	 */
	public boolean trustRelationExists(OfflinePlayer truster,
			OfflinePlayer trustee)
	{
		if (playerIsInGraph(truster) && playerIsInGraph(trustee)) {
			ReputationEntity trusterEntity = getEntity(truster);
			ReputationEntity trusteeEntity = getEntity(trustee);
			return trusterEntity.trustsEntity(trusteeEntity);
		}
		return false;
	}

	/**
	 * Returns a list of players that represents, in order, the path from the
	 * requestingPlayer to the otherPlayer. The first OfflinePlayer in the list
	 * is trusted by the requesting player. The second player is trusted by the
	 * first player and so on. The final player in the list is otherPlayer.
	 * 
	 * @param trustingPlayer
	 *            The player to find a connection to otherPlayer from
	 * @param otherPlayer
	 *            The player to find a connection to
	 * @return a list of players that represents a shortest path, in order, of
	 *         trust between trustingPlayer and otherPlayer.
	 */
	// TODO: make sure that this returns a list that contains the otherPlayer,
	// or update documentation to reflect that it does not.
	public List<OfflinePlayer> getReference(OfflinePlayer trustingPlayer,
			OfflinePlayer otherPlayer)
	{
		if (playerIsInGraph(trustingPlayer) && playerIsInGraph(otherPlayer)) {
			ReputationEntity requester = getEntity(trustingPlayer);
			ReputationEntity otherEntity = getEntity(otherPlayer);
			return convertToPlayerList(findPathBetween(requester, otherEntity));
		}
		return null;
	}

	/**
	 * Returns the top trusters of the specified player, in order of descending
	 * reputation.
	 * 
	 * @param player
	 *            The player to return the top trusters of.
	 * @param number
	 *            The number of top trusters to return. If the player does not
	 *            have at least this many trusters, the method will just return
	 *            as many as are available.
	 * @return A List of the top trusters of the specified player, in order of
	 *         descending reputation. Returns an empty List if the player has no
	 *         trusters or is not yet represented in the graph.
	 */
	public List<OfflinePlayer> getTopTrusters(OfflinePlayer player, int number)
	{
		if (!playerIsInGraph(player)) {
			return new ArrayList<OfflinePlayer>();
		}
		ReputationEntity entity = getEntity(player);
		PriorityQueue<ReputationEntity> priorityQueue = new PriorityQueue<ReputationEntity>();
		priorityQueue.addAll(entity.trusters);
		List<ReputationEntity> topTrusters = new ArrayList<ReputationEntity>(
				number);
		for (int i = 0; i < number; i++) {
			if (priorityQueue.isEmpty()) break;
			topTrusters.add(priorityQueue.poll());
		}
		return convertToPlayerList(topTrusters);
	}

	/**
	 * Returns the number of players who trust the supplied player. Be sure to
	 * use only if the player already exists in the graph. Check with
	 * playerIsInGraph().
	 * 
	 * @param player
	 * @return The number of players who trust the supplied player. If the
	 *         player doesn't exist, this method returns 0.
	 */
	public int trustersCount(OfflinePlayer player) {
		if (playerIsInGraph(player)) {
			return getEntity(player).getNumberOfTrusters();
		}
		return 0;
	}

	/**
	 * Returns the number of players the supplied player trusts. Be sure to use
	 * only if the player already exists in the graph. Check with
	 * playerIsInGraph().
	 * 
	 * @param player
	 * @return The number of players who the supplied player trusts. If the
	 *         player doesn't exist, this method returns 0.
	 */
	public int trusteesCount(OfflinePlayer player) {
		if (playerIsInGraph(player)) {
			return getEntity(player).getNumberOfTrustees();
		}
		return 0;
	}

	/**
	 * Returns a List of the players who trust the supplied player.
	 * 
	 * @param player
	 * @return A List of players who trust the supplied player. Returns an empty
	 *         List if the supplied player does not exist in the graph.
	 */
	public List<OfflinePlayer> getTrusters(OfflinePlayer player) {
		if (playerIsInGraph(player)) {
			return convertToPlayerList(getEntity(player).getTrusters());
		}
		return new ArrayList<OfflinePlayer>();
	}

	/**
	 * Returns a List of the players who the supplied player trusts.
	 * 
	 * @param player
	 * @return A List of players who the supplied player trusts. Returns an
	 *         empty List if the supplied player does not exist in the graph.
	 */
	public List<OfflinePlayer> getTrustees(OfflinePlayer player) {
		if (playerIsInGraph(player)) {
			return convertToPlayerList(getEntity(player).getTrustees());
		}
		return new ArrayList<OfflinePlayer>();
	}

	/**
	 * Returns true if the player is represented in the graph.
	 * 
	 * @param player
	 * @return True if player is already represented in the graph, false
	 *         otherwise.
	 */
	public boolean playerIsInGraph(OfflinePlayer player) {
		return playerEntityMap.containsKey(player);
	}

	private void addPlayerToGraph(OfflinePlayer player) {
		playerEntityMap.put(player, new ReputationEntity(player));
	}

	/**
	 * Gets the ReputationEntity for the supplied OfflinePlayer. Be sure to only
	 * use this method if you are sure the OfflinePlayer is in the graph
	 * already. Use playerIsInGraph() to check.
	 * 
	 * @param player
	 * @return
	 */
	private ReputationEntity getEntity(OfflinePlayer player) {
		return playerEntityMap.get(player);
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
		Set<ReputationEntity> todoSet = new HashSet<ReputationEntity>();
		Set<ReputationEntity> nextSet = new HashSet<ReputationEntity>();
		Set<ReputationEntity> updatedSet = new HashSet<ReputationEntity>();
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

	/**
	 * Returns a list of List of ReputationEntities that represents, in order, a
	 * shortest path of trust between the startEntity and the endEntity. This
	 * method is implemented with a breadth-first search, and as such, only
	 * returns a shortest path, with no guarantees about that path other than
	 * that is of minimum length.
	 * 
	 * @note I had intended to implement this to find the shortest path of trust
	 *       that also has the highest sum of path element reputations. This was
	 *       going to be implemented via Djikstra's algorithm, with path weights
	 *       being generated in such a way that the algorithm always selected
	 *       the path with the highest sum of player reputations, but this was
	 *       too complicated for this version of the plugin.
	 * @param startEntity
	 * @param endEntity
	 * @return a List of ReputationEntities that represents a shortest path from
	 *         the startEntity to the endEntity, which includes the endEntity
	 */
	// TODO: ensure that this method returns a list that actually contains the
	// endEntity, or update documentation to reflect that it does not.
	private List<ReputationEntity> findPathBetween(
			ReputationEntity startEntity, ReputationEntity endEntity)
	{
		Queue<ReputationEntity> queue = new LinkedList<ReputationEntity>();
		Map<ReputationEntity, ReputationEntity> pathBackMap = new HashMap<ReputationEntity, ReputationEntity>();
		queue.offer(startEntity);
		while (!queue.isEmpty()) {
			ReputationEntity current = queue.poll();
			if (current == endEntity) break;
			for (ReputationEntity trustee : current.trustees) {
				if (!pathBackMap.containsKey(trustee)) {
					pathBackMap.put(trustee, current);
				}
				queue.offer(trustee);
			}
		}
		List<ReputationEntity> pathToOtherEntity;
		pathToOtherEntity = buildPathBackList(pathBackMap, startEntity,
				endEntity);
		Collections.reverse(pathToOtherEntity);
		return pathToOtherEntity;
	}

	/**
	 * Given a map that maps each entity to a the previous entity in the path
	 * from the start entity to the end entity, this method returns a list that
	 * represents that path.
	 * 
	 * @param map
	 *            A map that maps each entity to the entity immediately before
	 *            it in the path from the start to the end entity.
	 * @param start
	 *            The first ReputationEntity in the path
	 * @param end
	 *            The last ReptuationEntity in the path
	 * @return A List that represents the path, in order
	 */
	private List<ReputationEntity> buildPathBackList(
			Map<ReputationEntity, ReputationEntity> map,
			ReputationEntity start, ReputationEntity end)
	{
		if (map.containsKey(end)) {
			List<ReputationEntity> pathBackList = new ArrayList<ReputationEntity>();
			ReputationEntity currentEntityInPath = end;
			while (currentEntityInPath != start) {
				pathBackList.add(currentEntityInPath);
				currentEntityInPath = map.get(currentEntityInPath);
			}
			return pathBackList;
		}
		return null;
	}

	/**
	 * Converts a list of ReputationEntity objects to a list of OfflinePlayer
	 * objects, preserving the order
	 * 
	 * @param listOfEntities
	 *            A List of ReputationEntity objects
	 * @return A List of OfflinePlayer objects, in the same order as the
	 *         corresponding listOfEntities
	 */
	private List<OfflinePlayer> convertToPlayerList(
			List<ReputationEntity> listOfEntities)
	{
		List<OfflinePlayer> convertedList = new ArrayList<OfflinePlayer>();
		for (ReputationEntity current : listOfEntities) {
			convertedList.add(current.player);
		}
		return convertedList;
	}

	private class ReputationEntity implements Comparable<ReputationEntity> {
		// TODO: Decide if these methods should be package-private.
		// See rationale in ReputationGraph.java

		/**
		 * The OfflinePlayer object represented by this ReputationEntity.
		 */
		private final OfflinePlayer player;
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
		private Set<ReputationEntity> trusters = new HashSet<ReputationEntity>();

		/**
		 * The set of players who this player trusts
		 */
		private Set<ReputationEntity> trustees = new HashSet<ReputationEntity>();

		private ReputationEntity(OfflinePlayer player) {
			this.player = player;
			this.reputation = 0.0;
			this.reputationIsAccurate = false;
		}

		/**
		 * Returns true if this ReputationEntity trusts the trusteeEntity
		 * 
		 * @param trusteeEntity
		 *            the entity to check for trust of.
		 * @return true if this ReputationEntity trusts the trusteeEntity, false
		 *         otherwise.
		 */
		boolean trustsEntity(ReputationEntity trusteeEntity) {
			return trustees.contains(trusteeEntity);
		}

		void removeTruster(ReputationEntity trusterEntity) {
			this.trusters.remove(trusterEntity);
		}

		void removeTrustee(ReputationEntity trusteeEntity) {
			this.trustees.remove(trusteeEntity);
		}

		void addTruster(ReputationEntity truster) {
			this.trusters.add(truster);
		}

		void addTrustee(ReputationEntity trustee) {
			this.trustees.add(trustee);
		}

		private int getNumberOfTrusters() {
			return this.trusters.size();
		}

		private int getNumberOfTrustees() {
			return this.trustees.size();
		}

		private List<ReputationEntity> getTrusters() {
			return new ArrayList<ReputationEntity>(trusters);
		}

		private List<ReputationEntity> getTrustees() {
			return new ArrayList<ReputationEntity>(trustees);
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
			Set<ReputationEntity> currentDegreeSet = new HashSet<ReputationEntity>();
			/*
			 * Will hold the ReputationEntities who will be in the next
			 * iteration. If the current degree away from the player being
			 * calculated for is i, they are i+1 away from the player.
			 */
			Set<ReputationEntity> nextSet = new HashSet<ReputationEntity>();
			/*
			 * Will hold the ReputationEntities that have already been involved
			 * in the calculation. They go into this set once they are used to
			 * ensure that they are never double counted.
			 */
			Set<ReputationEntity> ignoreSet = new HashSet<ReputationEntity>();
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

		/**
		 * Compares two ReputationEntity objects according to their reputation.
		 * Not consistent with equals().
		 * 
		 * This is not really the best way to do this, but this method makes
		 * higher reputation entities look "lower" than lower ones. This is so
		 * that I can use Java's default PriorityQueue, which is actually a min
		 * PQ when I need a max PQ.
		 */
		// TODO: double check that this does what it's supposed to
		public int compareTo(ReputationEntity other) {
			ReputationEntity otherEntity = (ReputationEntity) other;
			double diff = this.getReputation() - otherEntity.getReputation();
			if (diff > 0) {
				return -1;
			}
			if (diff < 0) {
				return 1;
			}
			return 0;
		}
	}
}