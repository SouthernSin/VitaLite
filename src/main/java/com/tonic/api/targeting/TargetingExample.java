package com.tonic.api.targeting;

import com.tonic.api.entities.ActorAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;

/**
 * Example usage of the Complex Targeting System
 */
public class TargetingExample {
    
    /**
     * Example of how to use the targeting system in a combat script
     */
    public static void combatLoopExample() {
        while (true) {
            // Update targeting system - this handles attack overrides
            ActorEx<?> target = ComplexTargetingSystem.updateTargeting();
            
            if (target != null) {
                // We have a valid, reachable target
                System.out.println("Targeting: " + target.getName());
                
                // Attack the target
                if (ComplexTargetingSystem.attackCurrentTarget()) {
                    System.out.println("Attacking target...");
                }
            } else {
                // No valid target found
                System.out.println("No reachable targets available");
                ComplexTargetingSystem.clearTarget();
            }
            
            // Check if we're under attack
            if (ComplexTargetingSystem.isUnderAttack()) {
                ActorEx<?> attacker = ComplexTargetingSystem.getLastAttacker();
                System.out.println("Under attack by: " + attacker.getName());
            }
            
            // Sleep/wait loop
            try {
                Thread.sleep(600); // Typical game tick
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    /**
     * Example of manually setting a target with reachability check
     */
    public static void manualTargetingExample() {
        // Find a specific NPC type
        NpcEx goblin = new NpcQuery()
            .nameContains("Goblin")
            .keepIf(ComplexTargetingSystem::isReachable)
            .nearest();
        
        if (goblin != null) {
            boolean success = ComplexTargetingSystem.setTarget(goblin);
            if (success) {
                System.out.println("Successfully set target to: " + goblin.getName());
            } else {
                System.out.println("Failed to set target - not reachable");
            }
        }
    }
    
    /**
     * Example of defensive targeting when attacked
     */
    public static void defensiveTargetingExample() {
        // This method would be called when you detect you're being attacked
        ActorEx<?> localPlayer = ActorAPI.getInCombatWith();
        
        if (localPlayer != null) {
            // Force override to target the attacker
            ComplexTargetingSystem.overrideTarget(localPlayer);
            System.out.println("Defensive targeting: Now attacking " + localPlayer.getName());
        }
    }
    
    /**
     * Example of checking target validity before actions
     */
    public static void validateTargetExample() {
        ActorEx<?> currentTarget = ComplexTargetingSystem.getCurrentTarget();
        
        if (currentTarget != null) {
            // Target is valid and reachable
            System.out.println("Current target " + currentTarget.getName() + " is valid");
            
            // You can now safely perform actions that require a valid target
            // For example: eat food, use special attack, etc.
        } else {
            System.out.println("No valid current target - need to find new target");
            
            // Find a new target
            ActorEx<?> newTarget = ComplexTargetingSystem.findBestTarget();
            if (newTarget != null) {
                ComplexTargetingSystem.setTarget(newTarget);
                System.out.println("Found new target: " + newTarget.getName());
            }
        }
    }
}