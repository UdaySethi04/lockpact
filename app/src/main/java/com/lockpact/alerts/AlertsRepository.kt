package com.lockpact.alerts

import com.lockpact.core.supabase.SupabaseClientProvider
import com.lockpact.pacts.Pact
import com.lockpact.pacts.PactRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityEvent(
    val id: String,
    val pact_id: String? = null,
    val user_id: String? = null,
    val actor_id: String? = null,
    val actor_user_id: String? = null,
    val target_user_id: String? = null,
    val app_name: String? = null,
    val package_name: String? = null,
    val message: String? = null,
    val type: String? = null,
    @SerialName("event_type")
    val eventType: String? = null,
    val created_at: String? = null
)

data class AlertFeedItem(
    val id: String,
    val tone: AlertTone,
    val title: String,
    val meta: String,
    val createdAt: String?
)

enum class AlertTone {
    Warning,
    Success,
    Lock,
    Info
}

class AlertsRepository {
    private val client = SupabaseClientProvider.client
    private val pactRepository = PactRepository()

    suspend fun getAlerts(): Result<List<AlertFeedItem>> {
        return try {
            val pacts = pactRepository.getMyPacts().getOrThrow()
            val pactIds = pacts.map { it.id }
            if (pactIds.isEmpty()) return Result.success(emptyList())

            val events = client.postgrest["activity_events"]
                .select {
                    filter { isIn("pact_id", pactIds) }
                }
                .decodeList<ActivityEvent>()

            val pactById = pacts.associateBy { it.id }
            val alerts = events
                .sortedByDescending { it.created_at.orEmpty() }
                .map { event -> event.toFeedItem(pactById[event.pact_id]) }

            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ActivityEvent.toFeedItem(pact: Pact?): AlertFeedItem {
        val rawType = (eventType ?: type).orEmpty().lowercase()
        val pactName = pact?.name ?: "Pact"
        val app = app_name?.takeIf { it.isNotBlank() }
        val fallbackMessage = message?.takeIf { it.isNotBlank() }

        val tone = when {
            rawType.contains("tamper") || rawType.contains("bypass") -> AlertTone.Warning
            rawType.contains("expired") || rawType.contains("clean") -> AlertTone.Success
            rawType.contains("lock") -> AlertTone.Lock
            else -> AlertTone.Info
        }

        val title = fallbackMessage ?: when (tone) {
            AlertTone.Warning -> "Possible bypass detected${app?.let { " on $it" }.orEmpty()}"
            AlertTone.Success -> "${app ?: "Lock"} expired cleanly"
            AlertTone.Lock -> "${app ?: "App"} was locked"
            AlertTone.Info -> rawType.ifBlank { "Pact activity" }.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }

        return AlertFeedItem(
            id = id,
            tone = tone,
            title = title,
            meta = "$pactName  ·  ${relativeTime(created_at)}",
            createdAt = created_at
        )
    }

    private fun relativeTime(isoTime: String?): String {
        if (isoTime.isNullOrBlank()) return "recently"
        return try {
            val created = java.time.Instant.parse(isoTime)
            val now = java.time.Instant.now()
            val minutes = java.time.temporal.ChronoUnit.MINUTES.between(created, now).coerceAtLeast(0)
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
}
