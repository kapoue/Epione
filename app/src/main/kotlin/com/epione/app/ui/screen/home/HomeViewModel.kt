package com.epione.app.ui.screen.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.epione.app.data.model.Etablissement
import com.epione.app.data.repository.EtablissementRepository
import com.epione.app.util.PrefsKeys
import com.epione.app.worker.DatabaseUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EtablissementRepository,
    @ApplicationContext private val context: Context,
    @Named("epione_prefs") private val prefs: SharedPreferences,
) : ViewModel() {

    // -------------------------------------------------------------------------
    // Recherche
    // -------------------------------------------------------------------------

    /** Terme de recherche saisi par l'utilisateur. */
    val searchQuery = MutableStateFlow("")

    /**
     * Liste réactive des établissements.
     * Bascule entre liste complète et résultats filtrés selon [searchQuery].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val etablissements: StateFlow<List<Etablissement>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllEtablissements()
            } else {
                repository.searchEtablissements(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    // -------------------------------------------------------------------------
    // État des mises à jour
    // -------------------------------------------------------------------------

    private val workManager = WorkManager.getInstance(context)

    /** True uniquement quand le Worker télécharge activement (RUNNING). */
    val isUpdateInProgress: StateFlow<Boolean> = workManager
        .getWorkInfosByTagFlow(DatabaseUpdateWorker.TAG)
        .map { list ->
            list.any { it.state == WorkInfo.State.RUNNING }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /** True si une nouvelle DB a été téléchargée et attend un redémarrage. */
    private val _updateAvailable = MutableStateFlow(
        prefs.getBoolean(PrefsKeys.UPDATE_DOWNLOADED, false)
    )
    val updateAvailable: StateFlow<Boolean> = _updateAvailable

    /** Appelé après que l'utilisateur a vu la SnackBar (ou déclenché le redémarrage). */
    fun dismissUpdate() {
        _updateAvailable.value = false
        prefs.edit().putBoolean(PrefsKeys.UPDATE_DOWNLOADED, false).apply()
    }
}

