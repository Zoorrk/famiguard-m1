package tn.famiguard.m1

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent

/**
 * Surveille l'application au premier plan. Si elle fait partie des applications
 * bloquées par le parent, affiche l'écran de blocage par-dessus.
 *
 * Cette approche fonctionne sans remise à zéro du téléphone (offre "Simple").
 * L'offre "Complète" (Device Owner) permet un blocage système plus fort — voir l'étude.
 */
class BlockAccessibilityService : AccessibilityService() {

    private var lastBlockedShownFor: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg == packageName) return // ne pas se bloquer soi-même

        if (BlockStore.isBlocked(this, pkg)) {
            if (pkg != lastBlockedShownFor) {
                lastBlockedShownFor = pkg
                showBlockScreen(pkg)
            }
        } else {
            lastBlockedShownFor = null
        }
    }

    private fun showBlockScreen(blockedPkg: String) {
        // Retour à l'accueil, puis affichage de l'écran de blocage.
        performGlobalAction(GLOBAL_ACTION_HOME)
        val label = runCatching {
            val info = packageManager.getApplicationInfo(blockedPkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(blockedPkg)

        startActivity(
            Intent(this, BlockActivity::class.java)
                .putExtra(BlockActivity.EXTRA_APP, label)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun onInterrupt() {}

    companion object {
        /** L'utilisateur a-t-il activé ce service dans Réglages > Accessibilité ? */
        fun isEnabled(ctx: android.content.Context): Boolean {
            val expected = "${ctx.packageName}/${BlockAccessibilityService::class.java.name}"
            val enabled = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            while (splitter.hasNext()) {
                if (splitter.next().equals(expected, ignoreCase = true)) return true
            }
            return false
        }

        fun openAccessibilitySettings(ctx: android.content.Context) {
            ctx.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
