package com.lockpact.core.session

import com.lockpact.core.supabase.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

object SessionManager {
    private val client = SupabaseClientProvider.client

    // Emits: NotAuthenticated, Authenticated, LoadingFromStorage, NetworkError
    val sessionStatus: StateFlow<SessionStatus> = client.auth.sessionStatus

    suspend fun logout() {
        client.auth.signOut()
    }

    fun currentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }

    fun currentUserEmail(): String? {
        return client.auth.currentUserOrNull()?.email
    }
}