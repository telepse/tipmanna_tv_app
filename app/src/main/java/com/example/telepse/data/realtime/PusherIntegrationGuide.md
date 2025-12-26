# Pusher Real-time Integration Guide

## Overview

This document describes the Pusher real-time event integration for the Telepse fundraising milestone screen. The integration handles three types of Pusher events and triggers character animations based on payment notifications.

## Architecture

### Components

1. **PusherService** (`data/realtime/PusherService.kt`)
   - Manages Pusher connection and channel subscriptions
   - Provides Flow-based event streams for reactive UI updates
   - Handles all Pusher event types

2. **PusherEvent Models** (`data/model/PusherEvent.kt`)
   - `TipAnimationEvent`: Contains selected tips with character information
   - `AmountUpdatedEvent`: Updates amount raised and tip count
   - `ScreenSettingsUpdatedEvent`: Updates UI settings

3. **FundraisingMilestoneViewModel**
   - Manages event state and Pusher subscriptions
   - Handles event processing and animation triggers
   - Updates UI state based on Pusher events

4. **AnimationTypeMapper** (`ui/animations/AnimationTypeMapper.kt`)
   - Maps backend styleName to AnimationType enum
   - Handles case-insensitive matching and variations

## Setup

### 1. Configure Pusher Credentials

Update `PusherConfig.kt` with your actual Pusher credentials:

```kotlin
object PusherConfig {
    const val API_KEY = "your-actual-pusher-api-key"
    const val CLUSTER = "your-cluster" // e.g., "mt1", "eu"
}
```

### 2. Event Flow

1. **Initial Load**: 
   - Screen loads → `loadEvent(eventId)` is called
   - Event is fetched from API
   - Pusher is initialized and subscribed to `event-{eventId}` channel

2. **Tip Animation Event**:
   - Pusher receives `tip-animation` event
   - ViewModel finds characters by `characterId` in event's `characterSets`
   - Maps `styleName` to `AnimationType` using `AnimationTypeMapper`
   - Adds animation triggers to UI state
   - Screen displays animations

3. **Amount Updated Event**:
   - Pusher receives `event-amount-updated` event
   - ViewModel updates `amountRaised` and `numOfTips`
   - UI automatically reflects changes

4. **Screen Settings Updated Event**:
   - Pusher receives `screen-settings-updated` event
   - ViewModel updates various UI settings
   - Background, progress bar visibility, etc. are updated

## Event Payloads

### tip-animation Event

```json
{
  "selectedTips": [
    {
      "characterId": "abc123",
      "styleName": "Confetti",
      "name": "Rose",
      "price": 5.0,
      "time": "2025-01-01T10:00:00Z"
    }
  ],
  "amount": 12.0,
  "sender": "user123"
}
```

### event-amount-updated Event

```json
{
  "amountRaised": 120.00,
  "numOfTips": 34
}
```

### screen-settings-updated Event

```json
{
  "amountRaised": 1250,
  "numOfTips": 42,
  "hideProgressBarAmount": false,
  "showLatestAmount": true,
  "tipUsageDisclosure": [...],
  "backgroundType": "gaming"
}
```

## Style Name Mapping

The following style names are mapped to animation types:

- `"Confetti"` → `AnimationType.CONFETTI`
- `"Pop and Shoot"` or `"Shoot and Pop"` → `AnimationType.POP_AND_SHOOT`
- `"Fall"` → `AnimationType.FALL`
- `"Burst"` → `AnimationType.BURST`
- Unknown styles default to `CONFETTI`

## Character Lookup

Characters are found by `characterId` in the event's `characterSets`:

```kotlin
event.characterSets
    ?.flatMap { it.characters ?: emptyList() }
    ?.firstOrNull { it.id == characterId }
```

## Usage

The integration is automatic once the screen loads. No manual intervention is required:

1. User navigates to `FundraisingMilestoneScreen`
2. Event is loaded from API
3. Pusher subscription is established
4. Events are processed automatically
5. Animations are displayed when tips are received
6. UI updates in real-time

## Cleanup

The ViewModel automatically disconnects from Pusher when cleared:

```kotlin
override fun onCleared() {
    super.onCleared()
    pusherService.disconnect()
}
```

## Error Handling

- Pusher connection errors are logged but don't crash the app
- Invalid event payloads are caught and logged
- Missing characters result in skipped animations (not added to pending list)
- All Flow streams use `.catch { }` to handle errors gracefully

