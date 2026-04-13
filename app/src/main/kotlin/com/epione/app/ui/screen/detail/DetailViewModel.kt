package com.epione.app.ui.screen.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epione.app.data.model.Etablissement
import com.epione.app.data.model.Qualite
import com.epione.app.data.repository.EtablissementRepository
import com.epione.app.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État de l'écran de détail. */
sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(val etablissement: Etablissement, val qualite: Qualite?) : DetailUiState()
    data object NotFound : DetailUiState()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EtablissementRepository,
) : ViewModel() {

    private val finessEt: String = checkNotNull(savedStateHandle[NavArgs.FINESS_ET])

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val etablissement = repository.getEtablissementById(finessEt)
            _uiState.value = if (etablissement == null) {
                DetailUiState.NotFound
            } else {
                val qualite = repository.getQualite(finessEt)
                DetailUiState.Success(etablissement, qualite)
            }
        }
    }
}
