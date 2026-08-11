package com.lockpact.locks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun LocksScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: PactViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
        viewModel.loadPacts()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Locks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = { MainBottomBar(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        when {
            uiState.isLoading && uiState.myActiveLocks.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionLabel("Lock status")
                                Text(
                                    text = if (uiState.myActiveLocks.isEmpty()) "Nothing running" else "${uiState.myActiveLocks.size} active",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "This screen shows locks active on you. To lock someone, open a pact.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item { ThinDivider() }

                    if (uiState.myActiveLocks.isNotEmpty()) {
                        item { SectionLabel("Active on you") }
                        items(uiState.myActiveLocks, key = { it.id }) { lock ->
                            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    LockRow(lock)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockRow(lock: ActiveLock) {
    val timeLeft = remember(lock.ends_at) {
        try {
            val minutes = ChronoUnit.MINUTES.between(Instant.now(), Instant.parse(lock.ends_at)).coerceAtLeast(0)
            if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m left" else "${minutes}m left"
        } catch (_: Exception) {
            "timer unavailable"
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("🔒 ${lock.app_name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
