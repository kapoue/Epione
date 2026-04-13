package com.epione.app.data.update

import android.content.SharedPreferences
import com.epione.app.data.remote.ManifestInfo
import com.epione.app.util.PrefsKeys
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** Résultat d'une vérification de mise à jour. */
sealed class UpdateCheckResult {
    data object UpToDate : UpdateCheckResult()
    data class UpdateAvailable(val manifest: ManifestInfo) : UpdateCheckResult()
    data class Failure(val message: String) : UpdateCheckResult()
}

/**
 * Repository chargé de :
 * 1. Récupérer le manifest.json distant.
 * 2. Comparer la version avec celle installée.
 * 3. Télécharger la nouvelle DB si nécessaire.
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @Named("epione_prefs") private val prefs: SharedPreferences,
) {

    companion object {
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/kapoue/Epione/main/manifest.json"
        /** Nom du fichier en attente d'application (dans filesDir). */
        const val PENDING_DB_NAME = "epione_pending.db"
    }

    // -------------------------------------------------------------------------
    // Vérification de version
    // -------------------------------------------------------------------------

    /**
     * Récupère le manifest distant et détermine si une mise à jour est disponible.
     * Bloquant — à appeler depuis un thread background (ex. Worker).
     */
    fun checkForUpdate(): UpdateCheckResult {
        return try {
            val manifest = fetchManifest() ?: return UpdateCheckResult.Failure("Manifest invalide")
            val installedVersion = prefs.getInt(PrefsKeys.DB_VERSION, 1)
            if (manifest.version > installedVersion) {
                UpdateCheckResult.UpdateAvailable(manifest)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failure(e.message ?: "Erreur réseau inconnue")
        }
    }

    // -------------------------------------------------------------------------
    // Téléchargement
    // -------------------------------------------------------------------------

    /**
     * Télécharge la DB pointée par [manifest] vers [destDir]/epione_pending.db.
     * Vérifie l'intégrité SHA-256 et marque la mise à jour comme téléchargée.
     *
     * @return true si le téléchargement et la vérification ont réussi.
     */
    fun downloadDatabase(manifest: ManifestInfo, destDir: File): Boolean {
        if (manifest.dbUrl.isBlank()) return false

        val tempFile = File(destDir, "${PENDING_DB_NAME}.tmp")
        val pendingFile = File(destDir, PENDING_DB_NAME)

        return try {
            val request = Request.Builder().url(manifest.dbUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                // Écriture en streaming pour ne pas saturer la RAM
                FileOutputStream(tempFile).use { out ->
                    body.byteStream().use { input -> input.copyTo(out) }
                }
            }

            // Vérification intégrité
            if (manifest.dbSha256.isNotBlank()) {
                val actualSha = computeSha256(tempFile)
                if (!actualSha.equals(manifest.dbSha256, ignoreCase = true)) {
                    tempFile.delete()
                    return false
                }
            }

            // Renommage atomique
            tempFile.renameTo(pendingFile)

            // Sauvegarde de la version + flag téléchargé
            prefs.edit()
                .putInt(PrefsKeys.DB_VERSION, manifest.version)
                .putBoolean(PrefsKeys.UPDATE_DOWNLOADED, true)
                .apply()

            true
        } catch (e: Exception) {
            tempFile.delete()
            false
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires privés
    // -------------------------------------------------------------------------

    private fun fetchManifest(): ManifestInfo? {
        val request = Request.Builder().url(MANIFEST_URL).build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            parseManifest(body)
        }
    }

    private fun parseManifest(json: String): ManifestInfo? = try {
        val obj = JSONObject(json)
        ManifestInfo(
            version     = obj.getInt("version"),
            dbUrl       = obj.optString("db_url", ""),
            dbSha256    = obj.optString("db_sha256", ""),
            releasedAt  = obj.optString("released_at", ""),
            recordCount = obj.optInt("record_count", 0),
        )
    } catch (_: Exception) { null }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(8192)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
