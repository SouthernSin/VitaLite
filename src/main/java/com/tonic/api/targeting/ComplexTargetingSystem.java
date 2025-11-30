package com.tonic.api.targeting;

import com.tonic.Static;
import com.tonic.api.entities.ActorAPI;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Complex Targeting System with reachability validation and attack override functionality
 * 
 * Features:
 * - Only targets reachable actors using .isReachable() checks
 * - Automatically overrides current target when player is attacked
 * - Prevents targeting actors that cannot be attacked
 * - Smart target selection based on threat and distance
 */
public class ComplexTargetingSystem {
    
    private static ActorEx<?> currentTarget;
    private static ActorEx<?> lastAttacker;
    private static long lastAttackTime = 0;
    private static final long ATTACK_TIMEOUT = 5000; // 5 seconds to consider recent attack
    
    /**
     * Sets the target with full reachability validation
     * @param target The potential target
     * @return true if target was successfully set and is reachable
     */
    public static boolean setTarget(ActorEx<?> target) {
        if (target == null) {
            currentTarget = null;
            return false;
        }
        
        // Check if target is valid and reachable
        if (!isValidTarget(target)) {
            return false;
        }
        
        // Check reachability before setting target
        if (!isReachable(target)) {
            return false;
        }
        
        currentTarget = target;
        return true;
    }
    
    /**
     * Gets the current valid target
     * @return current target if still valid and reachable, null otherwise
     */
    public static ActorEx<?> getCurrentTarget() {
        if (currentTarget == null) {
            return null;
        }
        
        // Validate current target is still reachable and attackable
        if (!isValidTarget(currentTarget) || !isReachable(currentTarget)) {
            currentTarget = null;
            return null;
        }
        
        return currentTarget;
    }
    
    /**
     * Forces target override (used when attacked)
     * @param attacker The actor that attacked us
     */
    public static void overrideTarget(ActorEx<?> attacker) {
        if (attacker != null && isValidTarget(attacker) && isReachable(attacker)) {
            currentTarget = attacker;
            lastAttacker = attacker;
            lastAttackTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Updates targeting system - checks for attacks and returns best target
     * @return Best available target or null
     */
    public static ActorEx<?> updateTargeting() {
        // Check if we're being attacked and override if necessary
        checkForAttacks();
        
        // If we have a current target, validate it
        ActorEx<?> current = getCurrentTarget();
        if (current != null) {
            return current;
        }
        
        // Find best new target
        return findBestTarget();
    }
    
    /**
     * Checks if the player is being attacked and overrides target if needed
     */
    private static void checkForAttacks() {
        Client client = Static.getClient();
        if (client == null || client.getLocalPlayer() == null) {
            return;
        }
        
        // Check if local player is under attack
        ActorEx<?> localPlayer = ActorEx.fromActor(client.getLocalPlayer());
        ActorEx<?> attacker = localPlayer.getInCombatWith();
        
        if (attacker != null && isValidTarget(attacker)) {
            // Check if this is a new attacker or recent attack
            if (!attacker.equals(lastAttacker) || 
                (System.currentTimeMillis() - lastAttackTime) < ATTACK_TIMEOUT) {
                overrideTarget(attacker);
            }
        }
    }
    
    /**
     * Finds the best available target based on reachability and threat
     * @return Best target or null
     */
    public static ActorEx<?> findBestTarget() {
        List<ActorEx<?>> potentialTargets = new ArrayList<>();
        
        // Get attackable NPCs
        NpcQuery npcQuery = new NpcQuery()
            .keepIf(ComplexTargetingSystem::isValidTarget)
            .keepIf(ComplexTargetingSystem::isReachable);
        
        for (NpcEx npc : npcQuery) {
            potentialTargets.add(npc);
        }
        
        // Get attackable Players (if in PVP)
        if (CombatAPI.isInPvpArea()) {
            PlayerQuery playerQuery = new PlayerQuery()
                .keepIf(ComplexTargetingSystem::isValidTarget)
                .keepIf(ComplexTargetingSystem::isReachable);
            
            for (PlayerEx player : playerQuery) {
                potentialTargets.add(player);
            }
        }
        
        if (potentialTargets.isEmpty()) {
            return null;
        }
        
        // Sort by threat level and distance
        return potentialTargets.stream()
            .min(Comparator.comparingDouble(ComplexTargetingSystem::getThreatScore))
            .orElse(null);
    }
    
    /**
     * Checks if a target is valid for attacking
     * @param target The target to validate
     * @return true if valid target
     */
    private static boolean isValidTarget(ActorEx<?> target) {
        if (target == null || target.isDead()) {
            return false;
        }
        
        return target.canAttack();
    }
    
    /**
     * Checks if a target is reachable from current position
     * @param target The target to check reachability for
     * @return true if reachable
     */
    public static boolean isReachable(ActorEx<?> target) {
        if (target == null) {
            return false;
        }
        
        Client client = Static.getClient();
        if (client == null || client.getLocalPlayer() == null) {
            return false;
        }
        
        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        WorldPoint targetPos = target.getWorldPoint();
        
        if (playerPos == null || targetPos == null) {
            return false;
        }
        
        // Check if target is within reasonable distance
        double distance = playerPos.distanceTo(targetPos);
        if (distance > 20) { // Max reasonable combat distance
            return false;
        }
        
        // Use MovementAPI to check pathfinding reachability
        try {
            return MovementAPI.canPathTo(targetPos);
        } catch (Exception e) {
            // Fallback to basic distance check if MovementAPI fails
            return distance <= 8; // Typical max attack range
        }
    }
    
    /**
     * Calculates threat score for target (lower = higher priority)
     * @param target The target to score
     * @return threat score
     */
    private static double getThreatScore(ActorEx<?> target) {
        if (target == null) {
            return Double.MAX_VALUE;
        }
        
        Client client = Static.getClient();
        if (client == null || client.getLocalPlayer() == null) {
            return Double.MAX_VALUE;
        }
        
        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        WorldPoint targetPos = target.getWorldPoint();
        
        if (playerPos == null || targetPos == null) {
            return Double.MAX_VALUE;
        }
        
        double distance = playerPos.distanceTo(targetPos);
        
        // Prioritize targets that are attacking us
        if (target.equals(lastAttacker) && 
            (System.currentTimeMillis() - lastAttackTime) < ATTACK_TIMEOUT) {
            return distance * 0.5; // Lower score for recent attackers
        }
        
        // Prioritize targets that are in combat with us
        ActorEx<?> interacting = target.getInteracting();
        if (interacting != null && interacting.equals(ActorEx.fromActor(client.getLocalPlayer()))) {
            return distance * 0.7; // Slightly lower score for current combat target
        }
        
        return distance;
    }
    
    /**
     * Clears current target
     */
    public static void clearTarget() {
        currentTarget = null;
    }
    
    /**
     * Attacks current target if valid and reachable
     * @return true if attack was initiated
     */
    public static boolean attackCurrentTarget() {
        ActorEx<?> target = getCurrentTarget();
        if (target == null) {
            return false;
        }
        
        try {
            target.interact("Attack");
            return true;
        } catch (Exception e) {
            // Attack failed, clear target
            clearTarget();
            return false;
        }
    }
    
    /**
     * Gets the last actor that attacked us
     * @return last attacker or null
     */
    public static ActorEx<?> getLastAttacker() {
        // Clear attacker if timeout expired
        if (System.currentTimeMillis() - lastAttackTime > ATTACK_TIMEOUT) {
            lastAttacker = null;
        }
        return lastAttacker;
    }
    
    /**
     * Checks if we are currently being attacked
     * @return true if under attack
     */
    public static boolean isUnderAttack() {
        return getLastAttacker() != null;
    }
}