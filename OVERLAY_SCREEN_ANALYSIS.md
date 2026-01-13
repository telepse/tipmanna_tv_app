# Fundraising Milestone Overlay Screen - Comprehensive Analysis

## Table of Contents
1. [Screen Lifecycle Flow](#screen-lifecycle-flow)
2. [ViewModel Lifecycle Flow](#viewmodel-lifecycle-flow)
3. [Pusher Service Flow](#pusher-service-flow)
4. [ExoPlayer Lifecycle Management](#exoplayer-lifecycle-management)
5. [All Pusher Events](#all-pusher-events)
6. [Event Switching Flow](#event-switching-flow)
7. [Potential Crash Points](#potential-crash-points)
8. [Root Cause Analysis](#root-cause-analysis)
9. [Research Topics](#research-topics)

---

## Screen Lifecycle Flow

### 1. Screen Entry (`FundraisingMilestoneScreen` Composable)

**When:** User navigates to the overlay screen

**Sequence:**
1. **ExoPlayer Creation** (Screen Level)
   - `remember { ExoPlayer.Builder(context).build() }` creates a single ExoPlayer instance
   - This player persists across event switches within the same screen session
   - Player is configured with `repeatMode = Player.REPEAT_MODE_ONE`

2. **DisposableEffect(Unit)** - Screen Lifecycle
   - **onEnter:** Logs screen entry
   - **onDispose:** 
     - Stops ExoPlayer: `exoPlayer.stop()`
     - Releases ExoPlayer: `exoPlayer.release()`
     - **CRITICAL:** This only runs when the screen composable is completely removed from composition

3. **DisposableEffect(eventId)** - Event Change Tracking
   - **onEnter:** Logs event change
   - **onDispose:** Logs event changing (but ExoPlayer is NOT released here)

4. **LaunchedEffect(eventId)** - Event Loading
   - Triggers `viewModel.loadEvent(eventId)` when eventId changes
   - This is the entry point for loading event data

5. **UI State Collection**
   - `val uiState by viewModel.uiState.collectAsState()` observes ViewModel state
   - UI recomposes when state changes

### 2. VideoBackground Composable

**When:** Event is loaded and `event != null`

**Sequence:**
1. **Composable Entry**
   - Receives `exoPlayer` (shared instance from screen level)
   - Receives `uri` (background video URI)
   - Receives `resourceId` (background resource ID)

2. **LaunchedEffect(uri.toString())** - Media Update
   - **Trigger:** When URI changes (new event or background change)
   - **Actions:**
     - Checks ExoPlayer state: `exoPlayer.playbackState`
     - Stops player if not idle: `exoPlayer.stop()`
     - Creates new MediaItem: `MediaItem.fromUri(uri)`
     - Sets media item: `exoPlayer.setMediaItem(mediaItem)`
     - Prepares player: `exoPlayer.prepare()`
     - Starts playback: `exoPlayer.playWhenReady = true`

3. **AndroidView** - PlayerView Creation
   - **Factory:** Creates `PlayerView` and assigns `exoPlayer`
   - **Update:** Updates player reference if it changes
   - **Modifier:** Fills entire screen

---

## ViewModel Lifecycle Flow

### 1. loadEvent(eventId: String)

**Sequence:**
1. **State Management**
   - If no event exists: Sets `isLoading = true`
   - If event exists: Keeps old event visible (prevents null state crash)

2. **Network Check**
   - Checks `networkMonitor.isCurrentlyOnline()`
   - If offline: Sets error state and returns

3. **Event Switching Detection**
   - If `currentEventId != null && currentEventId != eventId`:
     - Calls `disconnectFromCurrentEvent()` (suspending function)
     - Waits for cleanup to complete

4. **Event Fetching**
   - Calls `eventRepository.getEventById(eventId)`
   - On success:
     - Updates `currentEventId = eventId`
     - Updates UI state with new event
     - Calls `initializePusher(eventId, event)` if online

### 2. disconnectFromCurrentEvent()

**Sequence:**
1. **Cancel Flow Jobs** (4 jobs total)
   - Iterates through `flowJobs` list
   - Calls `job.cancel()` for each job
   - Clears `flowJobs` list
   - **CRITICAL:** This triggers `awaitClose` in each flow

2. **Disconnect Pusher**
   - Calls `pusherService.disconnect()`
   - This unsubscribes from channels and disconnects Pusher instance

3. **Reset Flags**
   - Sets `isPusherInitialized = false`
   - Sets `currentEventId = null`

4. **Wait for Cleanup**
   - `delay(200)` to allow cleanup to complete

### 3. initializePusher(eventId: String, event: Event)

**Sequence:**
1. **Initialization Check**
   - If already initialized for same event: Returns early
   - If initialized for different event: Logs warning

2. **Pusher Connection**
   - Calls `pusherService.initialize(forceReconnect = ...)`
   - Waits 500ms for connection to establish

3. **Channel Subscriptions**
   - Subscribes to public channel: `event-{eventId}`
   - Subscribes to private channel: `private-event-{eventId}`
   - Waits 500ms for subscriptions to be ready

4. **Flow Collection Jobs** (4 separate coroutines)
   - **Job 1:** `observeTipAnimation()` - Collects tip-animation events
   - **Job 2:** `observeAmountUpdated()` - Collects event-amount-updated events
   - **Job 3:** `observeScreenSettingsUpdated()` - Collects screen-settings-updated events
   - **Job 4:** `observeLatestTipUpdated()` - Collects latest-tip-updated events
   - Each job is added to `flowJobs` list

### 4. onCleared()

**When:** ViewModel is being destroyed (user navigates away from screen)

**Sequence:**
1. Cancels all flow jobs
2. Disconnects Pusher
3. Resets flags

---

## Pusher Service Flow

### 1. initialize(forceReconnect: Boolean)

**Sequence:**
1. **Connection Check**
   - If Pusher exists and connected: Returns true
   - If Pusher exists but not connected: Disconnects and reinitializes

2. **PusherOptions Setup**
   - Sets cluster: `PusherConfig.CLUSTER`
   - Sets authorizer for private channels (HMAC-SHA256 signature)

3. **Pusher Instance Creation**
   - Creates new `Pusher` instance with API key and options
   - Sets up `ConnectionEventListener` for state changes
   - Calls `pusher.connect()`

4. **Connection States**
   - `CONNECTING` → `CONNECTED`
   - `DISCONNECTING` → `DISCONNECTED`
   - `RECONNECTING` (on network issues)

### 2. subscribeToPublicChannel(channelName: String)

**Sequence:**
1. **Previous Channel Cleanup**
   - If `publicChannel` exists and different: Unsubscribes from old channel

2. **Subscription**
   - Calls `pusher.subscribe(channelName)`
   - Stores channel in `publicChannel`

### 3. subscribeToPrivateChannel(channelName: String)

**Sequence:**
1. **Previous Channel Cleanup**
   - If `privateChannel` exists and different: Unsubscribes from old channel

2. **Subscription**
   - Calls `pusher.subscribePrivate(channelName)`
   - Stores channel in `privateChannel`

3. **Subscription Event Listeners**
   - Binds `pusher:subscription_succeeded` listener
   - Binds `pusher:subscription_error` listener

### 4. disconnect()

**Sequence:**
1. **Store References**
   - Stores `publicChannel`, `privateChannel`, `pusher` in local variables

2. **Clear References**
   - Sets `publicChannel = null`
   - Sets `privateChannel = null`
   - **CRITICAL:** This prevents flows from accessing channels during cleanup

3. **Unsubscribe**
   - Unsubscribes from public channel (using stored reference)
   - Unsubscribes from private channel (using stored reference)

4. **Disconnect Pusher**
   - Calls `pusher.disconnect()`

5. **Final Cleanup**
   - Sets all references to null

### 5. Flow Creation (observeTipAnimation, observeAmountUpdated, etc.)

**Each Flow:**
1. **Wait for Channel**
   - Waits up to 2 seconds (20 attempts × 100ms) for channel to be available
   - If channel not available: Closes flow

2. **Event Binding**
   - Creates `SubscriptionEventListener` or `PrivateChannelEventListener`
   - Binds to specific event name (e.g., "tip-animation", "event-amount-updated")
   - In `onEvent()`:
     - Checks if channel is still valid (`publicChannel != null`)
     - Parses JSON event data
     - Sends to flow: `trySend(event)`

3. **awaitClose**
   - **CRITICAL:** This is called when flow collection is cancelled
   - Unbinds event listener from channel
   - Checks if channel is still valid before unbinding

---

## ExoPlayer Lifecycle Management

### Current Architecture

**Screen Level:**
- Single ExoPlayer instance created with `remember { ... }`
- Persists across event switches
- Released only when screen composable is removed

**VideoBackground Level:**
- Receives shared ExoPlayer as parameter
- Updates media item when URI changes
- No cleanup logic (managed at screen level)

### Lifecycle States

1. **Player Creation**
   - Created once when screen enters
   - Configured with `repeatMode = Player.REPEAT_MODE_ONE`

2. **Media Updates**
   - When event changes: New URI triggers `LaunchedEffect`
   - Player is stopped, new media item is set, player is prepared and started

3. **Player Release**
   - Only when `DisposableEffect(Unit)` onDispose is called
   - This happens when screen composable is removed from composition

### Potential Issues

1. **Race Condition: Player Released While Updating**
   - If screen is disposed while `LaunchedEffect` is updating media
   - Player may be released while `setMediaItem()` is being called
   - Results in `IllegalStateException`

2. **Race Condition: Multiple Media Updates**
   - If event switches rapidly, multiple `LaunchedEffect` blocks may run concurrently
   - Multiple `setMediaItem()` calls may conflict

3. **Player State Check Timing**
   - `playbackState` check may pass, but player is released before `setMediaItem()`
   - No synchronization between state check and operation

---

## All Pusher Events

### 1. tip-animation (Public Channel)

**Event Name:** `"tip-animation"`

**Channel:** Public (`event-{eventId}`)

**Flow:** `observeTipAnimation()`

**Event Data Structure:**
```json
{
  "selectedTips": ["character_name_1", "character_name_2"],
  "amount": 10.50,
  "sender": "John Doe"
}
```

**Handler:** `handleTipAnimationEvent()`
- Finds characters by name in event's `characterSets`
- Maps character `styleName` to `AnimationType`
- Creates `AnimationTrigger` objects
- Adds to `pendingAnimations` list in UI state

**Flow Collection:**
- Waits for event to be loaded (up to 5 seconds)
- Queues events that arrive before event is ready
- Processes queued events once event is loaded

### 2. event-amount-updated (Public Channel)

**Event Name:** `"event-amount-updated"`

**Channel:** Public (`event-{eventId}`)

**Flow:** `observeAmountUpdated()`

**Event Data Structure:**
```json
{
  "amountRaised": 1500.75,
  "numOfTips": 42
}
```

**Handler:** `updateAmountRaised()`
- Updates `event.amountRaised` in UI state
- Updates `event.numOfTips` in UI state
- Triggers progress bar update

### 3. screen-settings-updated (Private Channel)

**Event Name:** `"screen-settings-updated"`

**Channel:** Private (`private-event-{eventId}`)

**Flow:** `observeScreenSettingsUpdated()`

**Event Data Structure:**
```json
{
  "eventBackground": "/backgrounds/islamic.mp4",
  "backgroundType": "islamic",
  "hideProgressBarAmount": false,
  "showLatestAmount": true,
  "moneyFor": "Charity",
  "tipUsageDisclosure": [...],
  "amountRaised": 1500.75,
  "numOfTips": 42,
  "latestTipAmount": 10.50
}
```

**Handler:** `updateScreenSettings()`
- Updates all event properties in UI state
- **CRITICAL:** Background change triggers `VideoBackground` `LaunchedEffect` via URI change
- This causes ExoPlayer to update media item

### 4. latest-tip-updated (Public Channel)

**Event Name:** `"latest-tip-updated"`

**Channel:** Public (`event-{eventId}`)

**Flow:** `observeLatestTipUpdated()`

**Event Data Structure:**
```json
{
  "latestTipAmount": 10.50
}
```

**Handler:** `updateLatestTipAmount()`
- Updates `event.latestTipAmount` in UI state
- Used when `showLatestAmount == true` to display latest tip instead of total

---

## Event Switching Flow

### Scenario: User switches from Event A to Event B

**Timeline:**

1. **User Action:** Clicks on Event B

2. **Screen Level:**
   - `LaunchedEffect(eventId)` triggers with new `eventId`
   - Calls `viewModel.loadEvent(newEventId)`

3. **ViewModel Level:**
   - Detects event switch: `currentEventId != newEventId`
   - Calls `disconnectFromCurrentEvent()` (suspending)

4. **Disconnect Process:**
   - **Step 1:** Cancels all 4 flow jobs
     - Each job's `awaitClose` is triggered
     - Flows try to unbind from channels
   - **Step 2:** Calls `pusherService.disconnect()`
     - Stores channel references
     - Clears channel references (`publicChannel = null`, `privateChannel = null`)
     - Unsubscribes from channels
     - Disconnects Pusher
   - **Step 3:** Resets flags
   - **Step 4:** Waits 200ms

5. **New Event Loading:**
   - Fetches event from repository
   - Updates UI state with new event
   - Calls `initializePusher(newEventId, newEvent)`

6. **Pusher Reinitialization:**
   - Initializes Pusher (may reuse existing connection)
   - Subscribes to new channels
   - Starts 4 new flow collection jobs

7. **VideoBackground Update:**
   - New event's background URI triggers `LaunchedEffect(uri.toString())`
   - ExoPlayer stops current playback
   - ExoPlayer sets new media item
   - ExoPlayer prepares and starts new video

### Potential Race Conditions

1. **Flow Collection vs Channel Disposal**
   - Flow's `onEvent()` may be called while channel is being unsubscribed
   - Flow may try to access `publicChannel` after it's been set to null
   - **Mitigation:** Checks `if (publicChannel == null) return` in event handlers

2. **Flow Unbinding vs Channel Disposal**
   - `awaitClose` may try to unbind from channel that's already unsubscribed
   - **Mitigation:** Checks channel validity before unbinding

3. **ExoPlayer Update vs Screen Disposal**
   - `LaunchedEffect` may update ExoPlayer while screen is being disposed
   - ExoPlayer may be released while `setMediaItem()` is executing
   - **Mitigation:** Try-catch blocks around ExoPlayer operations

4. **Multiple Event Switches**
   - User may switch events rapidly
   - Multiple `loadEvent()` calls may be in flight
   - Previous event's cleanup may not complete before new event loads
   - **Mitigation:** State management keeps old event visible until new one loads

---

## Potential Crash Points

### 1. ExoPlayer IllegalStateException

**Location:** `VideoBackground.kt` - `LaunchedEffect(uri.toString())`

**Cause:**
- ExoPlayer is released while `setMediaItem()` or `prepare()` is executing
- Player state check passes, but player is released before operation completes

**Symptoms:**
- `IllegalStateException: Player is released`
- Crash when switching events or leaving screen

**Current Mitigation:**
- Try-catch around ExoPlayer operations
- Logs error but doesn't crash

**Risk Level:** HIGH

### 2. Channel NullPointerException

**Location:** `PusherService.kt` - Flow event handlers

**Cause:**
- Flow's `onEvent()` is called after channel reference is cleared
- Channel is accessed after `publicChannel = null` or `privateChannel = null`

**Symptoms:**
- `NullPointerException` when accessing channel
- Crash when switching events

**Current Mitigation:**
- Null checks: `if (publicChannel == null) return`
- Channel references stored before clearing

**Risk Level:** MEDIUM

### 3. Flow Collection Cancellation Exception

**Location:** `FundraisingMilestoneViewModel.kt` - Flow collectors

**Cause:**
- Flow collection is cancelled while event is being processed
- `JobCancellationException` is thrown

**Symptoms:**
- Exception logged but not handled properly
- May cause state corruption

**Current Mitigation:**
- Catches `CancellationException` and ignores it
- Logs other exceptions

**Risk Level:** LOW

### 4. Race Condition: Multiple Media Updates

**Location:** `VideoBackground.kt` - `LaunchedEffect(uri.toString())`

**Cause:**
- Rapid event switches trigger multiple `LaunchedEffect` blocks
- Multiple `setMediaItem()` calls conflict

**Symptoms:**
- ExoPlayer in inconsistent state
- Video may not play or may show wrong video

**Current Mitigation:**
- None - relies on Compose's cancellation of previous `LaunchedEffect`

**Risk Level:** MEDIUM

### 5. UI State Update After Disposal

**Location:** `FundraisingMilestoneViewModel.kt` - State updates

**Cause:**
- UI state is updated after screen composable is disposed
- StateFlow emission after collection is cancelled

**Symptoms:**
- May cause memory leak
- State may not update correctly

**Current Mitigation:**
- ViewModel scope cancellation should prevent this
- But if update is in flight, it may still execute

**Risk Level:** LOW

### 6. Pusher Connection State Race

**Location:** `PusherService.kt` - `disconnect()`

**Cause:**
- Pusher is disconnected while connection state is changing
- `disconnect()` called while `CONNECTING` or `RECONNECTING`

**Symptoms:**
- Connection may not disconnect properly
- Channels may remain subscribed

**Current Mitigation:**
- Try-catch around disconnect operations
- But state may be inconsistent

**Risk Level:** MEDIUM

---

## Root Cause Analysis

### Primary Issue: ExoPlayer Lifecycle Race Condition

**The Problem:**
When switching events, the following sequence can occur:

1. User switches from Event A to Event B
2. `VideoBackground` `LaunchedEffect` for Event A is still running
3. New event loads, triggering `LaunchedEffect` for Event B
4. Event A's `LaunchedEffect` tries to update ExoPlayer
5. Event B's `LaunchedEffect` also tries to update ExoPlayer
6. Screen is disposed (user navigates away)
7. `DisposableEffect(Unit)` releases ExoPlayer
8. One of the `LaunchedEffect` blocks tries to use released player
9. **CRASH:** `IllegalStateException: Player is released`

**Why It's Difficult:**
- Compose's `LaunchedEffect` cancellation is not guaranteed to be immediate
- ExoPlayer operations are asynchronous
- Multiple coroutines may be accessing the same ExoPlayer instance
- No synchronization mechanism between `LaunchedEffect` blocks

**Current State:**
- Try-catch blocks prevent crash, but error is logged
- Player may be in inconsistent state
- Video may not play correctly

### Secondary Issue: Pusher Flow Cleanup Race

**The Problem:**
When disconnecting, flows may still receive events:

1. `disconnectFromCurrentEvent()` cancels flow jobs
2. `awaitClose` is triggered in each flow
3. `pusherService.disconnect()` clears channel references
4. But Pusher may still deliver events to listeners
5. Flow's `onEvent()` is called with `publicChannel == null`
6. Event is ignored (null check), but timing is unpredictable

**Why It's Difficult:**
- Pusher's event delivery is asynchronous
- Channel unsubscription may not be immediate
- Flow cancellation and channel disposal are not synchronized

**Current State:**
- Null checks prevent crashes
- But events may be lost or processed incorrectly

---

## Research Topics

### 1. ExoPlayer Lifecycle Management in Jetpack Compose

**Questions:**
- How to safely manage ExoPlayer lifecycle across composable recompositions?
- How to prevent `IllegalStateException` when player is released?
- Best practices for sharing ExoPlayer instance across multiple composables?

**Research Areas:**
- ExoPlayer documentation on lifecycle management
- Jetpack Compose side effects (`LaunchedEffect`, `DisposableEffect`)
- Coroutine cancellation and resource cleanup
- Synchronization mechanisms (Mutex, Semaphore)

**Potential Solutions:**
- Use `Mutex` to synchronize ExoPlayer access
- Check player state before every operation
- Use `isReleased` flag to prevent operations on released player
- Create new ExoPlayer instance per event instead of sharing

### 2. Flow Collection Cancellation and Cleanup

**Questions:**
- How to ensure flows are fully cancelled before resource cleanup?
- How to handle events that arrive during cleanup?
- Best practices for `awaitClose` in `callbackFlow`?

**Research Areas:**
- Kotlin Flow documentation on cancellation
- Coroutine job cancellation
- `awaitClose` best practices
- Pusher SDK cleanup patterns

**Potential Solutions:**
- Use `flowOn()` to control cancellation scope
- Add synchronization between flow cancellation and resource cleanup
- Use `SharedFlow` with replay to handle late events
- Implement event queue to buffer events during cleanup

### 3. Jetpack Compose State Management During Navigation

**Questions:**
- How to prevent state updates after composable disposal?
- How to handle rapid navigation between screens?
- Best practices for ViewModel state during screen transitions?

**Research Areas:**
- Jetpack Compose state hoisting
- ViewModel lifecycle and `viewModelScope`
- Navigation component lifecycle
- StateFlow and Compose state collection

**Potential Solutions:**
- Use `collectAsStateWithLifecycle()` for lifecycle-aware collection
- Cancel ViewModel operations on navigation
- Use `SavedStateHandle` for state persistence
- Implement state machine for event switching

### 4. Pusher Channel Management and Cleanup

**Questions:**
- How to ensure channels are fully unsubscribed before reinitializing?
- How to handle connection state changes during cleanup?
- Best practices for Pusher channel lifecycle?

**Research Areas:**
- Pusher SDK documentation on channel management
- Connection state machine
- Channel subscription/unsubscription timing
- Error handling in Pusher SDK

**Potential Solutions:**
- Wait for channel unsubscription confirmation
- Use connection state callbacks to coordinate cleanup
- Implement retry logic for failed unsubscriptions
- Use Pusher's built-in reconnection handling

### 5. Coroutine Concurrency and Race Conditions

**Questions:**
- How to prevent race conditions in concurrent coroutines?
- How to synchronize resource access across coroutines?
- Best practices for coroutine cancellation and cleanup?

**Research Areas:**
- Kotlin coroutines documentation
- Coroutine context and scope
- Mutex and Semaphore for synchronization
- Structured concurrency

**Potential Solutions:**
- Use `Mutex` to protect ExoPlayer operations
- Use `Semaphore` to limit concurrent operations
- Implement operation queue for sequential execution
- Use `SupervisorJob` for independent coroutine failure handling

---

## Summary

The crash when switching events is primarily caused by:

1. **ExoPlayer Lifecycle Race Condition:** Multiple `LaunchedEffect` blocks trying to update ExoPlayer simultaneously, with player being released during update
2. **Pusher Flow Cleanup Race:** Flows receiving events after channels are disposed
3. **Lack of Synchronization:** No coordination between ExoPlayer operations, Pusher cleanup, and screen disposal

**Key Research Areas:**
- ExoPlayer lifecycle management in Compose
- Flow cancellation and cleanup patterns
- Coroutine synchronization mechanisms
- Pusher channel management best practices

**Recommended Approach:**
1. Implement `Mutex` for ExoPlayer access synchronization
2. Add proper state checks before all ExoPlayer operations
3. Implement event queue for Pusher events during cleanup
4. Use lifecycle-aware state collection in Compose
5. Add comprehensive error handling and logging

