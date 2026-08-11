package com.lockpact.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap?
)

class InstalledAppsScanner(
    private val context: Context
) {
    fun scanLaunchableApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val appName = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (appName.isBlank() || packageName == context.packageName) return@mapNotNull null

                InstalledApp(
                    appName = appName,
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager).toImageBitmap()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
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
}
