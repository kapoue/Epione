package com.epione.app.util

/** Clés SharedPreferences centralisées — évite les fautes de frappe. */
object PrefsKeys {
    const val PREFS_NAME        = "epione_prefs"
    /** Version de la DB actuellement installée (Int). */
    const val DB_VERSION        = "db_version"
    /** True si une nouvelle DB a été téléchargée et attend redémarrage. */
    const val UPDATE_DOWNLOADED = "update_downloaded"
}
