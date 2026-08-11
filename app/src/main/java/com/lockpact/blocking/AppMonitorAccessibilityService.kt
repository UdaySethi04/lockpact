package com.lockpact.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.lockpact.pacts.ActiveLock
import com.lockpact.pacts.PactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

class AppMonitorAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = PactRepository()
    private var lastBlockedPackage: String? = null
    private var lastBlockAtMillis: Long = 0L
    private var isChecking = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        if (foregroundPackage == packageName || foregroundPackage == "com.android.systemui") return

        val now = System.currentTimeMillis()
        if (foregroundPackage == lastBlockedPackage && now - lastBlockAtMillis < 2500L) return
        if (isChecking) return

        isChecking = true
        serviceScope.launch {
            try {
                val activeLocks = repository.getMyActiveLocks().getOrDefault(emptyList())
                val matchingLock = activeLocks.firstOrNull { lock -> lock.isCurrentlyBlocking(foregroundPackage) }
                if (matchingLock != null) {
                    lastBlockedPackage = foregroundPackage
                    lastBlockAtMillis = System.currentTimeMillis()
                    openBlockingScreen(matchingLock)
                }
            } finally {
                isChecking = false
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun openBlockingScreen(lock: ActiveLock) {
        val intent = Intent(this, BlockingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BlockingActivity.EXTRA_APP_NAME, lock.app_name)
            putExtra(BlockingActivity.EXTRA_PACT_NAME, "LockPact")
            putExtra(BlockingActivity.EXTRA_ENDS_AT, lock.ends_at)
        }
        startActivity(intent)
    }

    private fun ActiveLock.isCurrentlyBlocking(foregroundPackage: String): Boolean {
        if (status != "active") return false
        if (package_name != foregroundPackage) return false
        return try {
            Instant.parse(ends_at).isAfter(Instant.now())
        } catch (_: Exception) {
            false
        }
    }
}
