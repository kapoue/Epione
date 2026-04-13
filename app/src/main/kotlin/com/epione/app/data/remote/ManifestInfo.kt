package com.epione.app.data.remote

/**
 * Représentation du fichier manifest.json hébergé sur GitHub.
 * URL de référence : https://raw.githubusercontent.com/kapoue/Epione/main/manifest.json
 */
data class ManifestInfo(
    /** Numéro de version de la DB — incrémenté à chaque release. */
    val version: Int,
    /** URL de téléchargement de la DB SQLite (GitHub Release asset). */
    val dbUrl: String,
    /** SHA-256 hex de la DB pour vérification d'intégrité. */
    val dbSha256: String,
    /** Date de publication (ISO 8601). */
    val releasedAt: String,
    /** Nombre d'établissements dans cette version. */
    val recordCount: Int,
)
