package com.epione.app.data.update

import android.content.Context
import android.content.SharedPreferences
import com.epione.app.util.PrefsKeys
import java.io.File

/**
 * Applique une mise à jour de base de données en attente lors du démarrage à froid.
 *
 * Doit être appelé AVANT toute initialisation de Room (avant le premier accès DAO).
 * Room est lazy : il n'ouvre le fichier DB qu'au premier appel de requête.
 *
 * Logique :
 * - Si `filesDir/epione_pending.db` existe → ferme/supprime la DB actuelle → copie le fichier.
 * - Room ouvrira ensuite la nouvelle DB.
 */
object DatabaseUpdateManager {

    fun applyPendingUpdate(context: Context) {
        val pendingFile = File(context.filesDir, UpdateRepository.PENDING_DB_NAME)
        if (!pendingFile.exists()) return

        try {
            val dbFile  = context.getDatabasePath("epione.db")
            val dbShmFile = File(dbFile.path + "-shm")
            val dbWalFile = File(dbFile.path + "-wal")

            // Suppression des anciens fichiers (DB + WAL/SHM)
            dbShmFile.delete()
            dbWalFile.delete()
            dbFile.parentFile?.mkdirs()
            dbFile.delete()

            // Copie atomique du fichier en attente
            pendingFile.copyTo(dbFile, overwrite = true)
            pendingFile.delete()

            // Efface le flag "update downloaded"
            context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PrefsKeys.UPDATE_DOWNLOADED, false)
                .apply()

        } catch (_: Exception) {
            // En cas d'échec : ne pas crasher, la DB existante reste intacte.
            pendingFile.delete()
        }
    }
}
