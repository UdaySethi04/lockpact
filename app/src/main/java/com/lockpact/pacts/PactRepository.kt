package com.lockpact.pacts

import com.lockpact.core.session.SessionManager
import com.lockpact.core.supabase.SupabaseClientProvider
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class Pact(
    val id: String,
    val name: String,
    val invite_code: String,
    val created_by: String? = null,
    val created_at: String? = null
)

@Serializable
data class PactMember(
    val id: String,
    val pact_id: String,
    val user_id: String,
    val role: String = "member",
    val joined_at: String? = null
)

@Serializable
data class MemberExposedApp(
    val id: String? = null,
    val user_id: String,
    val app_name: String,
    val package_name: String
)

@Serializable
data class UserProfile(
    val id: String,
    val username: String? = null,
    val full_name: String? = null,
    val streak_days: Int = 0,
    val total_clean_hours: Double = 0.0
)

@Serializable
data class ActiveLock(
    val id: String,
    val pact_id: String,
    val target_user_id: String,
    val locker_user_id: String,
    val package_name: String,
    val app_name: String,
    val starts_at: String? = null,
    val ends_at: String,
    val status: String,
    val clean_hours_awarded: Double = 0.0
)

@Serializable
data class PactActivityEvent(
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
    val event_type: String? = null,
    val created_at: String? = null
)

@Serializable
private data class CreateLockRequest(
    val pact_id: String,
    val target_user_id: String,
    val package_name: String,
    val app_name: String,
    val duration_minutes: Int
)

class PactRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getMyPacts(): Result<List<Pact>> {
        return try {
            val userId = SessionManager.currentUserId()
                ?: return Result.success(emptyList())

            val memberships = client.postgrest["pact_members"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<PactMember>()

            val pactIds = memberships.map { it.pact_id }
            if (pactIds.isEmpty()) return Result.success(emptyList())

            val pacts = client.postgrest["pacts"]
                .select { filter { isIn("id", pactIds) } }
                .decodeList<Pact>()

            Result.success(pacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPactById(pactId: String): Result<Pact> {
        return try {
            val pact = client.postgrest["pacts"]
                .select { filter { eq("id", pactId) } }
                .decodeSingleOrNull<Pact>()
                ?: return Result.failure(Exception("Pact not found"))
            Result.success(pact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyMembership(pactId: String): Result<PactMember?> {
        return try {
            val userId = SessionManager.currentUserId()
                ?: return Result.success(null)
            val membership = client.postgrest["pact_members"]
                .select { filter { eq("pact_id", pactId); eq("user_id", userId) } }
                .decodeSingleOrNull<PactMember>()
            Result.success(membership)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPactMembers(pactId: String): Result<List<PactMember>> {
        return try {
            val members = client.postgrest["pact_members"]
                .select { filter { eq("pact_id", pactId) } }
                .decodeList<PactMember>()
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfiles(userIds: List<String>): Result<List<UserProfile>> {
        return try {
            if (userIds.isEmpty()) return Result.success(emptyList())
            val profiles = client.postgrest["users"]
                .select { filter { isIn("id", userIds) } }
                .decodeList<UserProfile>()
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExposedAppsForUsers(userIds: List<String>): Result<List<MemberExposedApp>> {
        return try {
            if (userIds.isEmpty()) return Result.success(emptyList())
            val apps = client.postgrest["exposed_apps"]
                .select { filter { isIn("user_id", userIds) } }
                .decodeList<MemberExposedApp>()
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveLocksForPact(pactId: String): Result<List<ActiveLock>> {
        return try {
            val locks = client.postgrest["app_locks"]
                .select {
                    filter {
                        eq("pact_id", pactId)
                        eq("status", "active")
                    }
                }
                .decodeList<ActiveLock>()
            Result.success(locks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivityForPact(pactId: String): Result<List<PactActivityEvent>> {
        return try {
            val events = client.postgrest["activity_events"]
                .select {
                    filter { eq("pact_id", pactId) }
                }
                .decodeList<PactActivityEvent>()
                .sortedByDescending { it.created_at.orEmpty() }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Locks active ON the current user across ALL pacts (for Home screen)
    suspend fun getMyActiveLocks(): Result<List<ActiveLock>> {
        return try {
            val userId = SessionManager.currentUserId()
                ?: return Result.success(emptyList())
            val locks = client.postgrest["app_locks"]
                .select {
                    filter {
                        eq("target_user_id", userId)
                        eq("status", "active")
                    }
                }
                .decodeList<ActiveLock>()
            Result.success(locks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createLock(
        pactId: String,
        targetUserId: String,
        packageName: String,
        appName: String,
        durationMinutes: Int
    ): Result<Unit> {
        return try {
            if (SessionManager.currentUserId() == null) {
                return Result.failure(Exception("Please log in again."))
            }

            if (durationMinutes !in listOf(15, 30, 60, 90, 120, 180)) {
                return Result.failure(Exception("Choose a valid lock duration."))
            }

            client.functions(
                function = "create-lock",
                body = CreateLockRequest(
                    pact_id = pactId,
                    target_user_id = targetUserId,
                    package_name = packageName,
                    app_name = appName,
                    duration_minutes = durationMinutes
                ),
                headers = Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(friendlyLockError(e)))
        }
    }

    suspend fun createPact(name: String): Result<Pact> {
        return try {
            val userId = SessionManager.currentUserId()
                ?: return Result.failure(Exception("Not logged in"))
            val inviteCode = generateInviteCode()

            val pact = client.postgrest["pacts"]
                .insert(mapOf("name" to name, "invite_code" to inviteCode, "created_by" to userId)) { select() }
                .decodeSingle<Pact>()

            client.postgrest["pact_members"].insert(
                mapOf("pact_id" to pact.id, "user_id" to userId, "role" to "owner")
            )
            Result.success(pact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinPact(inviteCode: String): Result<Pact> {
        return try {
            val userId = SessionManager.currentUserId()
                ?: return Result.failure(Exception("Not logged in"))
            val pact = client.postgrest["pacts"]
                .select { filter { eq("invite_code", inviteCode.uppercase()) } }
                .decodeSingleOrNull<Pact>()
                ?: return Result.failure(Exception("Invalid invite code"))

            client.postgrest["pact_members"].insert(
                mapOf("pact_id" to pact.id, "user_id" to userId, "role" to "member")
            )
            Result.success(pact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun friendlyLockError(error: Throwable): String {
        val raw = error.message.orEmpty().lowercase()
        return when {
            raw.contains("unauthorized") || raw.contains("401") -> "Please log in again."
            raw.contains("not in pact") || raw.contains("not a member") -> "You can only lock members of this pact."
            raw.contains("not exposed") || raw.contains("exposed") -> "That app is not exposed by this member."
            raw.contains("already locked") -> "This app is already locked."
            raw.contains("duration") -> "Choose a valid lock duration."
            raw.contains("missing") -> "Some lock details are missing."
            else -> "Could not create lock. Please try again."
        }
    }
}

