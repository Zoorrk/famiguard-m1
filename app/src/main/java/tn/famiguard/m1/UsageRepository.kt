package tn.famiguard.m1

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/** Un poste d'usage : une application et son temps d'écran du jour. */
data class AppUsage(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val millisToday: Long,
)

/**
 * Lit le temps d'écran réel via UsageStatsManager (depuis minuit).
 * Nécessite l'autorisation spéciale "Accès aux données d'usage".
 */
object UsageRepository {

    fun hasUsageAccess(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(ctx: Context) {
        ctx.startActivity(
            android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Temps d'écran par application depuis le début de la journée, trié décroissant. */
    fun todayUsage(ctx: Context, minMinutes: Int = 1): List<AppUsage> {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = ctx.packageManager

        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()

        val totals = HashMap<String, Long>()
        for (st in usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)) {
            if (st.totalTimeInForeground <= 0) continue
            totals[st.packageName] = (totals[st.packageName] ?: 0L) + st.totalTimeInForeground
        }

        val self = ctx.packageName
        return totals.entries
            .filter { it.value >= minMinutes * 60_000L && it.key != self }
            .mapNotNull { (pkg, ms) ->
                val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
                if (!isLaunchable(pm, pkg)) return@mapNotNull null
                AppUsage(
                    packageName = pkg,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = runCatching { pm.getApplicationIcon(info) }.getOrNull(),
                    millisToday = ms,
                )
            }
            .sortedByDescending { it.millisToday }
    }

    private fun isLaunchable(pm: PackageManager, pkg: String): Boolean =
        pm.getLaunchIntentForPackage(pkg) != null

    fun formatDuration(ms: Long): String {
        val totalMin = ms / 60_000
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 -> "${h}h${m.toString().padStart(2, '0')}"
            else -> "${m} min"
        }
    }
}
