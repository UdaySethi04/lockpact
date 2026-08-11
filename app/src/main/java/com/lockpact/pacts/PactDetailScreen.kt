package com.lockpact.pacts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lockpact.core.session.SessionManager
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PactDetailScreen(
    pactId: String,
    onBack: () -> Unit,
    viewModel: PactViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = remember { SessionManager.currentUserId() }
    val snackbarHostState = remember { SnackbarHostState() }
    var lockTargetUserId by remember { mutableStateOf("") }
    var lockTargetApp by remember { mutableStateOf<MemberExposedApp?>(null) }
    var lockDurationMinutes by remember { mutableStateOf(30) }

    LaunchedEffect(pactId) {
        viewModel.loadPactDetail(pactId)
    }

    LaunchedEffect(uiState.lockError) {
        uiState.lockError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.lockSuccess) {
        if (uiState.lockSuccess) {
            snackbarHostState.showSnackbar("Lock created")
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.selectedPact?.name ?: "Pact",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.selectedPact == null -> {
                Text(
                    text = uiState.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(padding)
                        .padding(20.dp)
                )
            }

            uiState.selectedPact != null -> {
                val pact = uiState.selectedPact!!
                val profiles = uiState.selectedUserProfiles
                val members = uiState.selectedMembers
                val exposedApps = uiState.selectedExposedApps
                val activeLocks = uiState.selectedActiveLocks
                val activityEvents = uiState.selectedActivityEvents

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item {
                        LeaderboardCard(
                            leaderboard = profiles.sortedByDescending { it.total_clean_hours },
                            currentUserId = currentUserId
                        )
                    }

                    item {
                        PactSummary(
                            inviteCode = pact.invite_code,
                            members = members.size,
                            exposedApps = exposedApps.size,
                            activeLocks = activeLocks.size
                        )
                    }

                    item { SectionLabel("Ongoing locks") }
                    if (activeLocks.isEmpty()) {
                        item { InfoCard("No one is locked right now.") }
                    } else {
                        items(activeLocks, key = { it.id }) { lock ->
                            ActiveLockRow(lock = lock, profiles = profiles)
                        }
                    }

                    item { SectionLabel("Members and apps") }
                    if (members.isEmpty()) {
                        item { InfoCard("No other members are visible yet. If someone joined this pact, run the pact_members RLS fix in Supabase.") }
                    } else {
                        items(members, key = { it.id }) { member ->
                            val profile = profiles.find { it.id == member.user_id }
                            val apps = exposedApps.filter { it.user_id == member.user_id }
                            val isMe = member.user_id == currentUserId

                            MemberCard(
                                member = member,
                                profile = profile,
                                exposedApps = apps,
                                activeLocks = activeLocks,
                                isMe = isMe,
                                onLockApp = { app ->
                                    lockTargetUserId = member.user_id
                                    lockTargetApp = app
                                    lockDurationMinutes = 30
                                }
                            )
                        }
                    }

                    item { SectionLabel("Recent activity") }
                    if (activityEvents.isEmpty()) {
                        item { InfoCard("No activity yet.") }
                    } else {
                        items(activityEvents.take(8), key = { it.id }) { event ->
                            ActivityRow(event = event, profiles = profiles)
                        }
                    }
                }
            }
        }
    }

    lockTargetApp?.let { app ->
        LockDialog(
            app = app,
            durationMinutes = lockDurationMinutes,
            onDurationChange = { lockDurationMinutes = it },
            onConfirm = {
                viewModel.createLock(
                    pactId = pactId,
                    targetUserId = lockTargetUserId,
                    packageName = app.package_name,
                    appName = app.app_name,
                    durationMinutes = lockDurationMinutes
                )
                lockTargetApp = null
            },
            onDismiss = { lockTargetApp = null }
        )
    }
}

@Composable
private fun ActivityRow(
    event: PactActivityEvent,
    profiles: List<UserProfile>
) {
    val actorName = (event.actor_id ?: event.actor_user_id)
        ?.let { id -> profiles.find { it.id == id } }
        ?.let { displayName(it) }
        ?: event.user_id
            ?.let { id -> profiles.find { it.id == id } }
            ?.let { displayName(it) }
        ?: "Someone"
    val type = (event.event_type ?: event.type).orEmpty().lowercase()
    val title = event.message?.takeIf { it.isNotBlank() } ?: when {
        type.contains("tamper") || type.contains("bypass") -> "$actorName may have bypassed ${event.app_name ?: "a lock"}"
        type.contains("expired") || type.contains("clean") -> "${event.app_name ?: "Lock"} expired cleanly"
        type.contains("lock") -> "$actorName locked ${event.app_name ?: "an app"}"
        type.contains("join") -> "$actorName joined the pact"
        else -> "Pact activity"
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = relativeTime(event.created_at),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaderboardCard(
    leaderboard: List<UserProfile>,
    currentUserId: String?
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "LEADERBOARD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Clean hours",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (leaderboard.isEmpty()) {
                Text(
                    text = "Members appear here once the pact loads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxHours = leaderboard.maxOfOrNull { it.total_clean_hours }?.coerceAtLeast(0.1) ?: 0.1
                leaderboard.forEachIndexed { index, profile ->
                    LeaderboardRow(
                        rank = index + 1,
                        profile = profile,
                        maxHours = maxHours,
                        isMe = profile.id == currentUserId
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    profile: UserProfile,
    maxHours: Double,
    isMe: Boolean
) {
    val name = displayName(profile)
    val barFraction = (profile.total_clean_hours / maxHours).toFloat().coerceIn(0.04f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$rank. ${if (isMe) "$name (you)" else name}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${"%.1f".format(profile.total_clean_hours)}h",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
            Surface(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .fillMaxHeight(),
                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            ) {}
        }
    }
}

@Composable
private fun PactSummary(
    inviteCode: String,
    members: Int,
    exposedApps: Int,
    activeLocks: Int
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Invite $inviteCode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniMetric("Members", members.toString(), Modifier.weight(1f))
                MiniMetric("Apps", exposedApps.toString(), Modifier.weight(1f))
                MiniMetric("Locks", activeLocks.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActiveLockRow(
    lock: ActiveLock,
    profiles: List<UserProfile>
) {
    val targetName = profiles.find { it.id == lock.target_user_id }?.let { displayName(it) } ?: "Member"
    val timeLeft = remember(lock.ends_at) {
        try {
            val ends = Instant.parse(lock.ends_at)
            val minutes = ChronoUnit.MINUTES.between(Instant.now(), ends).coerceAtLeast(0)
            if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
        } catch (_: Exception) {
            "-"
        }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(lock.app_name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "$targetName is locked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = timeLeft,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MemberCard(
    member: PactMember,
    profile: UserProfile?,
    exposedApps: List<MemberExposedApp>,
    activeLocks: List<ActiveLock>,
    isMe: Boolean,
    onLockApp: (MemberExposedApp) -> Unit
) {
    val name = profile?.let { displayName(it) } ?: if (member.role == "owner") "Owner" else "Member"

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMe) "$name (you)" else name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = member.role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${exposedApps.size} apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (exposedApps.isEmpty()) {
                Text(
                    text = "No exposed apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                exposedApps.forEach { app ->
                    val activeLock = activeLocks.find {
                        it.target_user_id == member.user_id && it.package_name == app.package_name
                    }
                    AppRow(
                        app = app,
                        activeLock = activeLock,
                        isMe = isMe,
                        onLock = { onLockApp(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: MemberExposedApp,
    activeLock: ActiveLock?,
    isMe: Boolean,
    onLock: () -> Unit
) {
    val context = LocalContext.current
    val timeLeft = remember(activeLock?.ends_at) {
        activeLock?.let {
            try {
                val ends = Instant.parse(it.ends_at)
                val minutes = ChronoUnit.MINUTES.between(Instant.now(), ends).coerceAtLeast(0)
                if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
            } catch (_: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(context = context, packageName = app.package_name)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.app_name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (timeLeft != null) {
                Text(
                    text = "$timeLeft left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!isMe) {
            if (activeLock == null) {
                TextButton(
                    onClick = onLock,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Lock", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LockDialog(
    app: MemberExposedApp,
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val durations = listOf(15 to "15 min", 30 to "30 min", 60 to "1 hr", 90 to "1.5 hrs", 120 to "2 hrs", 180 to "3 hrs")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lock ${app.app_name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { (minutes, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = durationMinutes == minutes,
                            onClick = { onDurationChange(minutes) }
                        )
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Create lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AppIcon(
    context: Context,
    packageName: String
) {
    val icon = remember(packageName) {
        context.loadAppIcon(packageName)
    }

    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(34.dp)
        )
    } else {
        Surface(
            modifier = Modifier.size(34.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}
    }
}

@Composable
private fun InfoCard(text: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun displayName(profile: UserProfile): String {
    return profile.full_name?.takeIf { it.isNotBlank() }
        ?: profile.username?.takeIf { it.isNotBlank() }
        ?: "Member"
}

private fun relativeTime(isoTime: String?): String {
    if (isoTime.isNullOrBlank()) return "recently"
    return try {
        val minutes = ChronoUnit.MINUTES.between(Instant.parse(isoTime), Instant.now()).coerceAtLeast(0)
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 1440 -> "${minutes / 60}h ago"
            else -> "${minutes / 1440}d ago"
        }
    } catch (_: Exception) {
        "recently"
    }
}

private fun Context.loadAppIcon(packageName: String): ImageBitmap? {
    return try {
        packageManager.getApplicationIcon(packageName).toImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap? {
    return try {
        if (this is BitmapDrawable && bitmap != null) {
            return bitmap.asImageBitmap()
        }

        val width = intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
