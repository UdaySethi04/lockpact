package com.lockpact.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lockpact.pacts.ActiveLock
import com.lockpact.pacts.PactViewModel
import com.lockpact.ui.navigation.MainBottomBar
import java.time.Instant
import java.time.temporal.ChronoUnit

private val DividerColor = Color(0xFF1A1A1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPacts: () -> Unit,
    onNavigateToMyApps: () -> Unit,
    onNavigateToActiveLocks: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToPactDetail: (String) -> Unit,
    currentRoute: String,
    onBottomNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: PactViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAccountPanel by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
        viewModel.loadPacts()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("LockPact", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { showAccountPanel = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Account and settings")
                    }
                },
                actions = {
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = { MainBottomBar(currentRoute = currentRoute, onNavigate = onBottomNavigate) }
    ) { padding ->
        if (uiState.isLoading && uiState.pacts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    StatusSurface(
                        activeLockCount = uiState.myActiveLocks.size,
                        cleanHours = uiState.myProfile?.total_clean_hours ?: 0.0,
                        streakDays = uiState.myProfile?.streak_days ?: 0
                    )
                }

                item { ThinDivider() }

                item {
                    LocksSurface(locks = uiState.myActiveLocks)
                }

                item { ThinDivider() }

                item {
                    AchievementsSurface(
                        cleanHours = uiState.myProfile?.total_clean_hours ?: 0.0,
                        streakDays = uiState.myProfile?.streak_days ?: 0,
                        pactCount = uiState.pacts.size
                    )
                }
            }
        }
    }

    if (showAccountPanel) {
        AlertDialog(
            onDismissRequest = { showAccountPanel = false },
            title = { Text("Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Settings", fontWeight = FontWeight.Bold)
                    Text(
                        "Profile, notification, and permission controls will live here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ThinDivider()
                    Text("${uiState.pacts.size} pacts")
                    Text("${"%.1f".format(uiState.myProfile?.total_clean_hours ?: 0.0)} clean hours")
                }
            },
            confirmButton = { TextButton(onClick = { showAccountPanel = false }) { Text("Close") } },
            dismissButton = {
                TextButton(onClick = {
                    showAccountPanel = false
                    showSignOutDialog = true
                }) { Text("Sign out") }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You will return to login. Your pacts and exposed apps stay saved.") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onLogout() }) {
                    Text("Sign out")
                }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatusSurface(activeLockCount: Int, cleanHours: Double, streakDays: Int) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Your status")
            Text(
                text = if (activeLockCount == 0) "Clear right now" else "$activeLockCount locked now",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatCleanTime(cleanHours),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "CLEAN TIME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${streakDays} day streak  ·  $activeLockCount locked now",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocksSurface(locks: List<ActiveLock>) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Active locks on you")
            if (locks.isEmpty()) {
                Text(
                    text = "No locks right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                locks.forEachIndexed { index, lock ->
                    if (index > 0) ThinDivider()
                    ActiveLockRow(lock)
                }
            }
        }
    }
}

@Composable
private fun AchievementsSurface(cleanHours: Double, streakDays: Int, pactCount: Int) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Achievements")
            AchievementRow("First pact", pactCount > 0)
            AchievementRow("One clean hour", cleanHours >= 1.0)
            AchievementRow("Three-day streak", streakDays >= 3)
        }
    }
}

@Composable
private fun AchievementRow(title: String, unlocked: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (unlocked) "✓" else "·",
            color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActiveLockRow(lock: ActiveLock) {
    val timeLeft = remember(lock.ends_at) {
        try {
            val minutes = ChronoUnit.MINUTES.between(Instant.now(), Instant.parse(lock.ends_at)).coerceAtLeast(0)
            if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m left" else "${minutes}m left"
        } catch (_: Exception) {
            "timer unavailable"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("🔒 ${lock.app_name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "${lock.pact_id.take(8)}  ·  $timeLeft",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(color = DividerColor, thickness = 1.dp)
}

private fun formatCleanTime(hours: Double): String {
    val wholeHours = hours.toInt()
    val minutes = ((hours - wholeHours) * 60).toInt().coerceAtLeast(0)
    return if (wholeHours == 0) "${minutes}m" else "${wholeHours}h ${minutes}m"
}
