# Crash Fix Implementation Summary

## Date: 2026-01-12

## Problem
App crashes when switching between events, especially on the second event switch. Root causes identified:

1. **Pusher Instance State Corruption**: `IllegalArgumentException` during private channel subscription corrupts the Pusher instance, which `disconnect()` cannot recover from
2. **ExoPlayer Concurrent Modification**: Multiple `LaunchedEffect` blocks attempt to modify ExoPlayer simultaneously during rapid event switches
3. **Flow Collection Race Conditions**: Flows continue receiving events after channels are disposed due to asynchronous cleanup

## Solutions Implemented

### 1. PusherService - Instance Corruption Detection & Recreation

**File**: `app/src/main/java/com/example/telepse/data/realtime/PusherService.kt`

**Changes**:
- Added `isPusherCorrupted` flag to track corrupted state
- Added `pusherMutex` for thread-safe operations
- Changed `initialize()`, `subscribeToPublicChannel()`, `subscribeToPrivateChannel()`, and `disconnect()` to `suspend` functions with mutex locking
- Added `forceDestroy()` method for complete instance recreation
- Added `IllegalArgumentException` detection in subscription methods that sets `isPusherCorrupted = true`
- Modified `initialize()` to check for corrupted state and call `forceDestroy()` before creating new instance
- Modified `disconnect()` to preserve Pusher instance unless corrupted (allows reuse)

**Key Code**:
```kotlin
// Track corrupted state
private var isPusherCorrupted = false
private val pusherMutex = Mutex()

// In subscribeToPrivateChannel
try {
    privateChannel = pusherInstance.subscribePrivate(channelName)
} catch (e: IllegalArgumentException) {
    // CRITICAL: Marks instance as corrupted
    Log.e("PusherService", "❌ IllegalArgumentException - PUSHER INSTANCE CORRUPTED", e)
    isPusherCorrupted = true
    return false
}

// In initialize
if (isPusherCorrupted) {
    Log.w("PusherService", "⚠️ Pusher instance corrupted - forcing complete recreation")
    forceDestroy()
    isPusherCorrupted = false
}
```

**Why This Works**:
- Detects when Pusher instance enters invalid state
- Completely destroys and recreates instance instead of attempting recovery
- Mutex ensures no concurrent modifications during state transitions
- Preserves instance for reuse when not corrupted (performance optimization)

---

### 2. FundraisingMilestoneViewModel - Event Switching Serialization

**File**: `app/src/main/java/com/example/telepse/ui/viewmodel/FundraisingMilestoneViewModel.kt`

**Changes**:
- Added `eventSwitchMutex` to serialize all event switches
- Wrapped `loadEvent()` logic in `eventSwitchMutex.withLock { }`
- Extracted event loading logic into `performEventLoad()` method
- Changed flow job cancellation to use `cancelAndJoin()` instead of `cancel()` to wait for completion
- Increased cleanup delay from 200ms to 400ms in `disconnectFromCurrentEvent()`
- Added 500ms delay after disconnect before loading new event
- Added 300ms delays after error recovery cleanup

**Key Code**:
```kotlin
// Serialize event switches
private val eventSwitchMutex = Mutex()

fun loadEvent(eventId: String) {
    viewModelScope.launch {
        // CRITICAL: Only one event switch at a time
        eventSwitchMutex.withLock {
            performEventLoad(eventId)
        }
    }
}

// Wait for job cancellation to complete
job.cancelAndJoin() // Instead of just job.cancel()

// Longer delays for complete cleanup
delay(500) // After disconnect
delay(400) // In disconnectFromCurrentEvent
```

**Why This Works**:
- Mutex prevents overlapping event switches
- `cancelAndJoin()` ensures previous flows are fully stopped before starting new ones
- Longer delays allow asynchronous cleanup to complete
- Each event switch completes fully before the next one begins

---

### 3. FundraisingMilestoneScreen - ExoPlayer Synchronization

**File**: `app/src/main/java/com/example/telepse/ui/screen/FundraisingMilestoneScreen.kt`

**Changes**:
- Created `ExoPlayerManager` class with `Mutex` synchronization
- Added `updateMedia()` method with mutex locking
- Added URI tracking to prevent redundant updates (idempotency)
- Added `isReleased` flag for safety checks
- Replaced direct ExoPlayer usage with `ExoPlayerManager` wrapper
- Modified `VideoBackground` to accept `playerManager` instead of raw `exoPlayer`
- Simplified `LaunchedEffect` to just call `playerManager.updateMedia(uri)`

**Key Code**:
```kotlin
@Stable
private class ExoPlayerManager(context: Context) {
    private val mutex = Mutex()
    private var currentUri: Uri? = null
    private var isReleased = false
    
    suspend fun updateMedia(uri: Uri) = mutex.withLock {
        // Skip if same URI (idempotency)
        if (currentUri == uri) {
            Log.d("ExoPlayerManager", "⏭️ Skipping - same URI")
            return
        }
        
        if (isReleased) {
            Log.w("ExoPlayerManager", "⚠️ Cannot update - player released")
            return
        }
        
        // Safe, synchronized media update
        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        
        currentUri = uri
    }
}

// In VideoBackground
LaunchedEffect(uri.toString()) {
    // Synchronized update - only one at a time
    playerManager.updateMedia(uri)
}
```

**Why This Works**:
- Mutex prevents concurrent ExoPlayer modifications
- URI tracking prevents redundant updates when same background is applied
- `isReleased` flag prevents operations on released player
- Single responsibility: manager handles all player operations
- Multiple `LaunchedEffect` blocks can be triggered, but only one update executes at a time

---

## Testing Instructions

1. **Build the app**:
   ```bash
   ./gradlew assembleDebug
   ```

2. **Install and run**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **View logs**:
   ```powershell
   view-telepse-logs
   ```

4. **Test scenarios**:
   - **Scenario 1**: Load first event → wait 2 seconds → switch to second event → wait 2 seconds → go back
     - **Expected**: No crashes, smooth transitions
   
   - **Scenario 2**: Rapidly switch between events (click multiple events quickly)
     - **Expected**: Mutex prevents race conditions, only processes one switch at a time
   
   - **Scenario 3**: Change background settings via private channel
     - **Expected**: Background updates without crash
   
   - **Scenario 4**: Switch events 5+ times
     - **Expected**: No crashes even after many switches
   
   - **Scenario 5**: Switch event → trigger animation → switch event again before animation completes
     - **Expected**: Old animation stops cleanly, new event loads

5. **Look for these log messages**:
   - ✅ `"[LOAD_EVENT] Starting to load event (mutex acquired)"` - Event switch serialized
   - ✅ `"Skipping - same URI already loaded"` - Idempotency working
   - ✅ `"Updating media (mutex locked)"` - ExoPlayer synchronized
   - ✅ `"Job cancelled and joined"` - Complete flow cancellation
   - ❌ `"IllegalArgumentException - PUSHER INSTANCE CORRUPTED"` - Should NOT appear (if it does, recreation will happen)
   - ✅ `"Force destroying Pusher instance"` - Corrupted instance being recreated

---

## Key Improvements

### Before vs After

| Issue | Before | After |
|-------|--------|-------|
| **Pusher Corruption** | `disconnect()` called, instance still corrupted | `forceDestroy()` completely recreates instance |
| **Event Switching** | Multiple switches overlap, race conditions | Mutex serializes switches, one at a time |
| **ExoPlayer Updates** | Multiple `LaunchedEffect` blocks modify concurrently | Mutex ensures single update at a time |
| **Flow Cleanup** | `cancel()` doesn't wait, flows may still receive events | `cancelAndJoin()` waits for complete shutdown |
| **Cleanup Timing** | 200ms delays, insufficient | 400-500ms delays for thorough cleanup |
| **Idempotency** | Same URI triggers redundant updates | URI tracking skips redundant updates |

### Performance Optimizations

1. **Pusher Reuse**: Instance is preserved unless corrupted (avoids unnecessary reconnections)
2. **URI Idempotency**: Skips redundant ExoPlayer updates for same background
3. **State Preservation**: Old event remains visible during switch (better UX)

### Error Handling

1. **Defensive Logging**: Comprehensive logs at every critical point
2. **Graceful Degradation**: Errors are logged but don't crash the app
3. **State Flags**: `isPusherCorrupted`, `isReleased` prevent operations on invalid instances

---

## Files Modified

1. `app/src/main/java/com/example/telepse/data/realtime/PusherService.kt`
   - Added mutex synchronization
   - Added corruption detection
   - Added forced recreation logic

2. `app/src/main/java/com/example/telepse/ui/viewmodel/FundraisingMilestoneViewModel.kt`
   - Added event switch mutex
   - Improved job cancellation
   - Increased cleanup delays

3. `app/src/main/java/com/example/telepse/ui/screen/FundraisingMilestoneScreen.kt`
   - Created `ExoPlayerManager` wrapper
   - Added ExoPlayer synchronization
   - Simplified lifecycle management

---

## Expected Behavior

### Normal Operation
1. User loads Event A → Pusher connects → Flows start → UI displays
2. User switches to Event B → Mutex acquired → Old event cleaned up (wait) → New event loads
3. Background changes → ExoPlayer updates synchronized
4. User navigates away → Player released → Pusher disconnected

### Error Recovery
1. `IllegalArgumentException` during subscription → Instance marked corrupted
2. Next `initialize()` call → Detects corruption → Force destroys → Creates fresh instance
3. Cleanup errors → Logged but don't prevent next event from loading
4. ExoPlayer released during update → Operation skipped, next update will succeed

---

## Next Steps

1. **Test thoroughly** with the scenarios above
2. **Monitor logs** for any unexpected errors
3. **If crashes persist**:
   - Share the full logcat output from `view-telepse-logs`
   - Note the exact sequence of actions that caused the crash
   - Check for any new error messages not covered by current fixes

4. **Performance monitoring**:
   - Verify event switches are smooth (not too slow)
   - Check that animations still work correctly
   - Ensure background changes are responsive

---

## Technical Details

### Mutex Pattern
```kotlin
private val mutex = Mutex()

suspend fun criticalOperation() = mutex.withLock {
    // Only one execution at a time
    // All other callers wait in queue
}
```

### Corruption Detection Pattern
```kotlin
try {
    pusher.subscribePrivate(channelName)
} catch (e: IllegalArgumentException) {
    isPusherCorrupted = true // Mark for recreation
    return false
}

// Later, in initialize()
if (isPusherCorrupted) {
    forceDestroy() // Complete recreation
}
```

### Idempotency Pattern
```kotlin
private var currentUri: Uri? = null

suspend fun updateMedia(uri: Uri) = mutex.withLock {
    if (currentUri == uri) {
        return // Skip redundant update
    }
    // ... perform update ...
    currentUri = uri
}
```

---

## Credits

Based on research into:
- Pusher Java client error handling patterns
- Jetpack Compose lifecycle management best practices
- Kotlin coroutine synchronization primitives
- ExoPlayer lifecycle management in Compose

