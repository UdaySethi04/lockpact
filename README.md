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
