package com.epione.app.ui.screen.home

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.epione.app.data.model.Etablissement
import com.epione.app.data.repository.EtablissementRepository
import com.epione.app.util.PrefsKeys
import com.epione.app.util.haversineKm
import com.epione.app.worker.DatabaseUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Named

/** Établissement enrichi avec sa distance par rapport à la position de l'utilisateur. */
data class EtablissementItem(
    val etablissement: Etablissement,
    val distanceKm: Double? = null,
)

private data class FilterState(
    val query: String,
    val lat: Double?,
    val lon: Double?,
    val distKm: Int?,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EtablissementRepository,
    @ApplicationContext private val context: Context,
    @Named("epione_prefs") private val prefs: SharedPreferences,
) : ViewModel() {

    // -------------------------------------------------------------------------
    // Recherche
    // -------------------------------------------------------------------------

    val searchQuery = MutableStateFlow("")

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    // -------------------------------------------------------------------------
    // Géolocalisation
    // -------------------------------------------------------------------------

    private val _userLat = MutableStateFlow<Double?>(null)
    private val _userLon = MutableStateFlow<Double?>(null)

    val hasLocation: StateFlow<Boolean> = combine(_userLat, _userLon) { lat, lon ->
        lat != null && lon != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun updateUserLocation(lat: Double, lon: Double) {
        _userLat.value = lat
        _userLon.value = lon
    }

    // -------------------------------------------------------------------------
    // Filtre distance
    // -------------------------------------------------------------------------

    val selectedDistanceKm = MutableStateFlow<Int?>(null)

    fun setDistanceFilter(km: Int?) {
        selectedDistanceKm.value = km
    }

    // -------------------------------------------------------------------------
    // Liste (avec distance calculée + filtre + tri)
    // -------------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    val etablissements: StateFlow<List<EtablissementItem>> =
        combine(searchQuery, _userLat, _userLon, selectedDistanceKm, ::FilterState)
            .flatMapLatest { filter ->
                val rawFlow = if (filter.query.isBlank()) {
                    repository.getAllEtablissements()
                } else {
                    repository.searchEtablissements(filter.query)
                }
                rawFlow.map { list ->
                    var items = list.map { etab ->
                        val dist = if (filter.lat != null && filter.lon != null &&
                            etab.latitude != null && etab.longitude != null
                        ) {
                            haversineKm(filter.lat, filter.lon, etab.latitude, etab.longitude)
                        } else null
                        EtablissementItem(etab, dist)
                    }
                    if (filter.distKm != null) {
                        items = items.filter { it.distanceKm != null && it.distanceKm <= filter.distKm }
                    }
                    if (filter.lat != null) {
                        items = items.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
                    }
                    items
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    // -------------------------------------------------------------------------
    // État des mises à jour
    // -------------------------------------------------------------------------

    private val workManager = WorkManager.getInstance(context)

    val isUpdateInProgress: StateFlow<Boolean> = workManager
        .getWorkInfosByTagFlow(DatabaseUpdateWorker.TAG)
        .map { list -> list.any { it.state == WorkInfo.State.RUNNING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _updateAvailable = MutableStateFlow(
        prefs.getBoolean(PrefsKeys.UPDATE_DOWNLOADED, false)
    )
    val updateAvailable: StateFlow<Boolean> = _updateAvailable

    fun dismissUpdate() {
        _updateAvailable.value = false
        prefs.edit().putBoolean(PrefsKeys.UPDATE_DOWNLOADED, false).apply()
    }
}

