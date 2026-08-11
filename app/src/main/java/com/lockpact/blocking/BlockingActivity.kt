package com.lockpact.blocking

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lockpact.ui.theme.LockPactTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

class BlockingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val pactName = intent.getStringExtra(EXTRA_PACT_NAME) ?: "LockPact"
        val endsAt = intent.getStringExtra(EXTRA_ENDS_AT)

        setContent {
            LockPactTheme {
                BlockingScreen(
                    appName = appName,
                    pactName = pactName,
                    endsAt = endsAt,
                    onGoHome = { goToHomeScreen() },
                    onExpired = { finish() }
                )
            }
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_PACT_NAME = "extra_pact_name"
        const val EXTRA_ENDS_AT = "extra_ends_at"

        fun createIntent(activity: Activity, appName: String, pactName: String, endsAt: String?): Intent {
            return Intent(activity, BlockingActivity::class.java).apply {
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_PACT_NAME, pactName)
                putExtra(EXTRA_ENDS_AT, endsAt)
            }
        }
    }
}

@Composable
private fun BlockingScreen(
    appName: String,
    pactName: String,
    endsAt: String?,
    onGoHome: () -> Unit,
    onExpired: () -> Unit
) {
    var remainingSeconds by remember(endsAt) { mutableLongStateOf(secondsUntil(endsAt)) }

    LaunchedEffect(endsAt) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = secondsUntil(endsAt)
        }
        if (endsAt != null) onExpired()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "LOCKPACT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "$appName is locked",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This lock is active in $pactName.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = formatRemaining(remainingSeconds),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                text = "TIME LEFT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(36.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onGoHome
            ) {
                Text("Return home")
            }
        }
    }
}

private fun secondsUntil(endsAt: String?): Long {
    if (endsAt.isNullOrBlank()) return 0
    return try {
        ChronoUnit.SECONDS.between(Instant.now(), Instant.parse(endsAt)).coerceAtLeast(0)
    } catch (_: Exception) {
        0
    }
}

private fun formatRemaining(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%02d:%02d".format(minutes, secs)
    }
}
