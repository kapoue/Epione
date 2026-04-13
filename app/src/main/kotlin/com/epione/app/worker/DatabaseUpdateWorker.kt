package com.epione.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.epione.app.data.update.UpdateCheckResult
import com.epione.app.data.update.UpdateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker chargé de vérifier et télécharger une nouvelle version de la DB.
 *
 * Planifié :
 * - Une fois immédiatement au premier démarrage.
 * - Périodiquement toutes les 24h en arrière-plan.
 *
 * Comportement :
 * - Succès sans update     → Result.success()
 * - Update téléchargé      → Result.success() + flag prefs UPDATE_DOWNLOADED = true
 * - Erreur réseau          → Result.retry() (WorkManager planifie une reprise)
 * - Erreur irrécupérable   → Result.failure()
 */
@HiltWorker
class DatabaseUpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateRepository: UpdateRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "epione_update_check"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when (val result = updateRepository.checkForUpdate()) {
            is UpdateCheckResult.UpToDate -> Result.success()

            is UpdateCheckResult.UpdateAvailable -> {
                val downloaded = updateRepository.downloadDatabase(
                    manifest = result.manifest,
                    destDir  = context.filesDir,
                )
                if (downloaded) Result.success() else Result.retry()
            }

            is UpdateCheckResult.Failure -> {
                // Erreur réseau transitoire → réessai automatique par WorkManager
                Result.retry()
            }
        }
    }
}
