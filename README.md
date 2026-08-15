# LockPact

LockPact is an Android accountability app that helps friends stay focused by letting them lock each other's chosen distracting apps for a fixed time.

Users create pacts, expose only the apps they are comfortable sharing, lock pact members' exposed apps, and track activity through alerts, active locks, clean hours, streaks, and leaderboard-style progress.

## Features

- User signup and login with Supabase Authentication
- Create and join pacts using invite codes
- Scan installed Android apps
- Choose which apps are exposed to pact members
- Lock a friend's exposed app for a selected duration
- View active locks on the Home and Locks screens
- View pact members, exposed apps, leaderboard data, and recent activity
- Alerts feed for lock and pact activity
- Android Accessibility Service prototype for detecting opened apps
- Blocking screen that appears when a locked app is opened
- Dark, minimal Jetpack Compose UI

## Tech Stack

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Supabase Auth
- Supabase PostgreSQL
- Supabase Edge Functions
- Supabase Realtime
- Firebase Cloud Messaging
- DataStore
- WorkManager
- Room
- Kotlin Serialization

## Project Structure

```text
app/src/main/java/com/lockpact/
  auth/          Login, signup, and auth state
  home/          Main dashboard
  pacts/         Pact list, pact details, members, locks, leaderboard
  apps/          Installed app scanner and exposed apps screen
  locks/         Active locks screen
  alerts/        Activity and alert feed
  blocking/      Accessibility service and blocking screen
  core/
    session/     Current user/session helpers
    supabase/    Supabase client setup
  ui/
    navigation/  App routes and bottom navigation
    theme/       Colors, typography, and shapes
```

## Backend Overview

LockPact uses Supabase for the backend.

Main database tables:

- `users`
- `pacts`
- `pact_members`
- `exposed_apps`
- `app_locks`
- `activity_events`
- `heartbeats`

Main Edge Function:

- `create-lock`

The `create-lock` function validates that both users are in the same pact, checks that the target user exposed the selected app, creates the active lock, and records an activity event.

## Setup

1. Clone or open the project in Android Studio.

2. Add your Supabase values in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "SUPABASE_URL", "\"https://YOUR_PROJECT_REF.supabase.co\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"YOUR_SUPABASE_ANON_KEY\"")
```

3. Add Firebase config:

Place `google-services.json` inside:

```text
app/google-services.json
```

4. Build the app:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

5. Install on a connected Android device:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installDebug
```

## Accessibility Service

To test real app blocking:

1. Install the app on a physical Android phone.
2. Open phone Settings.
3. Go to Accessibility.
4. Enable `LockPact App Monitor`.
5. Create an active lock from another pact member.
6. Open the locked app on the target phone.

Expected result: LockPact should show a blocking screen with the locked app name and remaining time.

## Current Status

Working:

- Authentication
- Pact creation and joining
- Installed app scanning
- Exposed apps
- Lock creation through Supabase Edge Function
- Alerts/activity frontend
- Active locks display
- Basic Accessibility Service blocking prototype

In progress:

- Local active lock cache
- Background lock sync
- Heartbeat reporting
- Tamper detection
- Firebase push notification handling
- More accurate achievements and leaderboard scoring

## Roadmap

- Cache active locks locally for faster blocking
- Add WorkManager sync for active locks
- Add heartbeat checks to confirm enforcement is running
- Report tamper events when Accessibility Service is disabled
- Improve achievement system
- Add push notifications for new locks
- Polish onboarding for Android permissions
- Strengthen production security and privacy controls

## Security Notes

- The Supabase anon key is safe to use in the Android app when Row Level Security is configured correctly.
- The Supabase service role key must never be placed in the Android app.
- Sensitive write operations should go through Edge Functions.
- Row Level Security should control access to pacts, members, exposed apps, locks, and activity events.

## License

This project is currently built as a college/demo project. Add a license before public release.
