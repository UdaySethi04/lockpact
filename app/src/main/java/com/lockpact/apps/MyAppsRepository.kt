package com.lockpact.apps

import com.lockpact.core.session.SessionManager
import com.lockpact.core.supabase.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class ExposedAppRow(
    val id: String? = null,
    val user_id: String? = null,
    val app_name: String? = null,
    val package_name: String
)

class MyAppsRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getExposedApps(): Result<Set<String>> {
        return try {
            val userId = SessionManager.currentUserId() ?: return Result.success(emptySet())

            val rows = client.postgrest["exposed_apps"]
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<ExposedAppRow>()

            Result.success(rows.map { it.package_name }.toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exposeApp(app: InstalledApp): Result<Unit> {
        return try {
            val userId = SessionManager.currentUserId() ?: return Result.failure(Exception("Not logged in"))

            client.postgrest["exposed_apps"].insert(
                mapOf(
                    "user_id" to userId,
                    "app_name" to app.appName,
                    "package_name" to app.packageName
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hideApp(packageName: String): Result<Unit> {
        return try {
            val userId = SessionManager.currentUserId() ?: return Result.failure(Exception("Not logged in"))

            client.postgrest["exposed_apps"].delete {
                filter {
                    eq("user_id", userId)
                    eq("package_name", packageName)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
