package com.epione.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.epione.app.data.update.DatabaseUpdateManager
import com.epione.app.worker.DatabaseUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Point d'entrée Hilt. Implémente Configuration.Provider pour injecter HiltWorkerFactory. */
@HiltAndroidApp
class EpioneApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        // Applique une éventuelle DB téléchargée AVANT que Room s'initialise.
        DatabaseUpdateManager.applyPendingUpdate(this)
        super.onCreate()
        scheduleUpdateCheck()
    }

    /**
     * Planifie une vérification périodique (toutes les 24h) via WorkManager.
     * KEEP = si déjà planifié, ne pas écraser (économie de batterie).
     */
    private fun scheduleUpdateCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<DatabaseUpdateWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .addTag(DatabaseUpdateWorker.TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DatabaseUpdateWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }
}

