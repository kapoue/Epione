package com.epione.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epione.app.data.model.Etablissement
import com.epione.app.data.repository.EtablissementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EtablissementRepository,
) : ViewModel() {

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
}
