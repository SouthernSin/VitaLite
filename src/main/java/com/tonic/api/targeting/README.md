# Complex Targeting System

A sophisticated targeting system for the VitaLite bot that ensures only reachable targets are selected and automatically overrides targets when the player is attacked.

## Features

### ✅ Reachability Validation
- Uses `.isReachable()` method to validate targets before selection
- Prevents targeting NPCs/players that cannot be attacked due to obstacles
- Falls back to distance-based checks if pathfinding fails

### ✅ Attack Override System
- Automatically detects when the player is being attacked
- Overrides current target to prioritize attackers
- Maintains attack priority for 5 seconds after last attack

### ✅ Smart Target Selection
- Prioritizes targets based on threat level and distance
- Considers recent attackers as highest priority
- Validates targets are attackable before selection

### ✅ State Management
- Maintains current target state with continuous validation
- Automatically clears invalid or unreachable targets
- Tracks attack history for defensive targeting

## Usage Examples

### Basic Combat Loop
```java
// Update targeting system (handles attack detection)
ActorEx<?> target = ComplexTargetingSystem.updateTargeting();

if (target != null) {
    // Attack the validated, reachable target
    ComplexTargetingSystem.attackCurrentTarget();
} else {
    // No valid targets available
    System.out.println("No reachable targets found");
}
```

### Manual Target Setting
```java
// Find a specific NPC and validate reachability
NpcEx goblin = new NpcQuery()
    .nameContains("Goblin")
    .nearest();

if (goblin != null && ComplexTargetingSystem.isReachable(goblin)) {
    boolean success = ComplexTargetingSystem.setTarget(goblin);
    if (success) {
        System.out.println("Target set: " + goblin.getName());
    }
}
```

### Defensive Response
```java
// Check if under attack
if (ComplexTargetingSystem.isUnderAttack()) {
    ActorEx<?> attacker = ComplexTargetingSystem.getLastAttacker();
    System.out.println("Defensive mode: Attacked by " + attacker.getName());
    
    // System automatically targets attacker via updateTargeting()
}
```

## API Methods

### Core Methods
- `updateTargeting()` - Updates system and returns best target
- `setTarget(ActorEx<?> target)` - Manually set target with validation
- `getCurrentTarget()` - Get current validated target
- `attackCurrentTarget()` - Attack current target if valid

### Validation Methods
- `isReachable(ActorEx<?> target)` - Check if target is reachable
- `isValidTarget(ActorEx<?> target)` - Check if target can be attacked
- `findBestTarget()` - Find best available target

### State Methods
- `clearTarget()` - Clear current target
- `isUnderAttack()` - Check if player is being attacked
- `getLastAttacker()` - Get most recent attacker
- `overrideTarget(ActorEx<?> attacker)` - Force target override

## Implementation Details

### Reachability Checking
The system uses multiple layers of reachability validation:

1. **Distance Check**: Ensures target is within 20 tiles
2. **Pathfinding**: Uses `MovementAPI.canPathTo()` for accurate reachability
3. **Fallback**: Distance-based check if pathfinding fails

### Attack Detection
Monitors combat state through:
- `ActorEx.getInCombatWith()` to detect attackers
- Timeout-based tracking (5 seconds)
- Automatic target prioritization

### Target Scoring
Uses threat-based scoring system:
- Recent attackers: 0.5x distance multiplier
- Current combat targets: 0.7x distance multiplier  
- Normal targets: 1.0x distance multiplier

## Integration Notes

This targeting system integrates with existing VitaLite APIs:
- `MovementAPI` for reachability checking
- `CombatAPI` for combat state validation
- `ActorEx` wrapper for entity management
- `NpcQuery` and `PlayerQuery` for target filtering

## Error Handling

The system includes comprehensive error handling:
- Null checks for all actors and positions
- Graceful fallbacks for API failures
- Automatic target clearing on errors
- Exception-safe targeting operations

## Performance Considerations

- Cached target validation to reduce API calls
- Efficient query filtering
- Minimal computational overhead
- Optimized for real-time combat scenarios