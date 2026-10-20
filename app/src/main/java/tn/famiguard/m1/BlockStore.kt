package tn.famiguard.m1

import android.content.Context

/**
 * Liste des applications bloquées par le parent, conservée sur l'appareil.
 * (Version démo : SharedPreferences locales. En production, les règles sont
 *  signées par le serveur et synchronisées — voir l'étude de faisabilité.)
 */
object BlockStore {
    private const val PREF = "famiguard_rules"
    private const val KEY = "blocked_packages"

    fun blocked(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()) ?: emptySet()

    fun isBlocked(ctx: Context, pkg: String): Boolean = blocked(ctx).contains(pkg)

    fun toggle(ctx: Context, pkg: String): Boolean {
        val set = blocked(ctx).toMutableSet()
        val nowBlocked = if (set.contains(pkg)) { set.remove(pkg); false } else { set.add(pkg); true }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, set).apply()
        return nowBlocked
    }
}
