package com.epione.app.ui.screen.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epione.app.R
import com.epione.app.ui.components.EtablissementCard
import kotlin.system.exitProcess

private val DISTANCE_OPTIONS = listOf(5, 10, 20, 50, 100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEtablissementClick: (String) -> Unit,
    onAboutClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val etablissements    by viewModel.etablissements.collectAsStateWithLifecycle()
    val query             by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isUpdateInProgress by viewModel.isUpdateInProgress.collectAsStateWithLifecycle()
    val updateAvailable   by viewModel.updateAvailable.collectAsStateWithLifecycle()
    val hasLocation       by viewModel.hasLocation.collectAsStateWithLifecycle()
    val selectedDistanceKm by viewModel.selectedDistanceKm.collectAsStateWithLifecycle()
    val selectedCategorie by viewModel.selectedCategorie.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Lanceur de permission de localisation
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLastLocation(context, viewModel::updateUserLocation)
    }

    // Si permission déjà accordée, on récupère la position au démarrage
    LaunchedEffect(Unit) {
        val fineOk = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseOk = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineOk || coarseOk) fetchLastLocation(context, viewModel::updateUserLocation)
    }

    // SnackBar de mise à jour OTA
    val updateMsg    = stringResource(R.string.update_available)
    val updateAction = stringResource(R.string.update_restart)
    LaunchedEffect(updateAvailable) {
        if (!updateAvailable) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message     = updateMsg,
            actionLabel = updateAction,
            duration    = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            val intent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
            context.startActivity(intent)
            exitProcess(0)
        }
        viewModel.dismissUpdate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_home_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onAboutClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.screen_about_title),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Barre de progression (mise à jour OTA)
            if (isUpdateInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // ── Barre de recherche collante ──────────────────────────────────
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.search_clear),
                                    )
                                }
                            }
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                windowInsets = WindowInsets(0),
                content = {},
            )

            // ── Chips de filtre distance ─────────────────────────────────────
            DistanceChipsRow(
                hasLocation = hasLocation,
                selectedDistanceKm = selectedDistanceKm,
                onSelectDistance = viewModel::setDistanceFilter,
                onRequestLocation = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                },
            )

            // ── Chips de filtre catégorie ────────────────────────────────────
            CategoryChipsRow(
                selectedCategorie = selectedCategorie,
                onSelectCategorie = viewModel::setCategorie,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Liste défilable ──────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (etablissements.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (query.isBlank() && selectedDistanceKm == null && selectedCategorie == null) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text(
                                    text = stringResource(R.string.error_not_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = etablissements,
                        key = { it.etablissement.finessEt },
                    ) { item ->
                        EtablissementCard(
                            etablissement = item.etablissement,
                            distanceKm = item.distanceKm,
                            onClick = { onEtablissementClick(item.etablissement.finessEt) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DistanceChipsRow(
    hasLocation: Boolean,
    selectedDistanceKm: Int?,
    onSelectDistance: (Int?) -> Unit,
    onRequestLocation: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!hasLocation) {
            AssistChip(
                onClick = onRequestLocation,
                label = { Text(stringResource(R.string.location_enable)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        } else {
            FilterChip(
                selected = selectedDistanceKm == null,
                onClick = { onSelectDistance(null) },
                label = { Text(stringResource(R.string.distance_all)) },
            )
            DISTANCE_OPTIONS.forEach { km ->
                FilterChip(
                    selected = selectedDistanceKm == km,
                    onClick = { onSelectDistance(if (selectedDistanceKm == km) null else km) },
                    label = { Text("$km km") },
                )
            }
        }
    }
}

private val CATEGORIES_FILTRES = listOf(
    TypeCategorie.PHARMACIE,
    TypeCategorie.HOPITAL,
    TypeCategorie.LABO,
    TypeCategorie.MEDECIN,
    TypeCategorie.CENTRE,
    TypeCategorie.PMI,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipsRow(
    selectedCategorie: TypeCategorie?,
    onSelectCategorie: (TypeCategorie?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selectedCategorie == null,
            onClick = { onSelectCategorie(null) },
            label = { Text(stringResource(R.string.distance_all)) },
        )
        CATEGORIES_FILTRES.forEach { cat ->
            FilterChip(
                selected = selectedCategorie == cat,
                onClick = { onSelectCategorie(if (selectedCategorie == cat) null else cat) },
                label = { Text(cat.label) },
            )
        }
    }
}

/**
 * Récupère la dernière position connue via le LocationManager natif.
 * Si aucune position en cache, demande une mise à jour unique.
 * Ne requiert pas Google Play Services.
 */
@SuppressLint("MissingPermission")
private fun fetchLastLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    // 1) Essai avec la dernière position connue (instantané)
    for (provider in providers) {
        try {
            if (lm.isProviderEnabled(provider)) {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null) {
                    onLocation(loc.latitude, loc.longitude)
                    return
                }
            }
        } catch (_: Exception) {}
    }

    // 2) Aucune position en cache → demande une mise à jour unique
    for (provider in providers) {
        try {
            if (lm.isProviderEnabled(provider)) {
                lm.requestLocationUpdates(
                    provider, 0L, 0f,
                    object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            lm.removeUpdates(this)
                            onLocation(loc.latitude, loc.longitude)
                        }
                    },
                    Looper.getMainLooper(),
                )
                break
            }
        } catch (_: Exception) {}
    }
}


