package com.epione.app.ui.screen.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epione.app.R
import com.epione.app.data.model.Etablissement
import com.epione.app.data.model.Qualite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavori by viewModel.isFavori.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val etablissement = (uiState as? DetailUiState.Success)?.etablissement

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareEtablissement(context, etablissement) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.action_share),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    IconButton(onClick = viewModel::toggleFavori) {
                        Icon(
                            imageVector = if (isFavori) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavori) {
                                stringResource(R.string.action_remove_favori)
                            } else {
                                stringResource(R.string.action_add_favori)
                            },
                            tint = if (isFavori) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is DetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.error_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            is DetailUiState.Success -> {
                DetailContent(
                    etablissement = state.etablissement,
                    qualite = state.qualite,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    etablissement: Etablissement,
    qualite: Qualite?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val noPhone  = stringResource(R.string.action_no_phone)
    val noCoords = stringResource(R.string.action_no_coords)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // En-tête coloré avec le nom
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = etablissement.nom,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = etablissement.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }

        // ── Boutons d'action ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Appeler
            FilledTonalButton(
                onClick = {
                    val phone = etablissement.telephone
                    if (phone.isNullOrBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(noPhone) }
                    } else {
                        val digits = phone.filter { it.isDigit() }
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.action_call))
            }

            // Itinéraire
            FilledTonalButton(
                onClick = {
                    val lat = etablissement.latitude
                    val lon = etablissement.longitude
                    if (lat == null || lon == null) {
                        scope.launch { snackbarHostState.showSnackbar(noCoords) }
                    } else {
                        val nom = Uri.encode(etablissement.nom)
                        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($nom)")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.action_navigate))
            }

            // Site web
            if (!etablissement.siteWeb.isNullOrBlank()) {
                FilledTonalButton(
                    onClick = {
                        val url = etablissement.siteWeb.let {
                            if (it.startsWith("http")) it else "https://$it"
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_website))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Informations de contact
        DetailRow(label = stringResource(R.string.label_address), value = etablissement.adresse)
        DetailRow(
            label = stringResource(R.string.label_city),
            value = "${etablissement.ville} ${etablissement.codePostal}",
        )
        if (!etablissement.telephone.isNullOrBlank()) {
            DetailRow(label = stringResource(R.string.label_phone), value = etablissement.telephone)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Section qualité
        Text(
            text = stringResource(R.string.label_quality),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        val qualityText = when {
            qualite?.scoreGlobal != null -> "Score global : ${"%.1f".format(qualite.scoreGlobal)} / 5"
            else -> stringResource(R.string.quality_not_evaluated)
        }
        Text(
            text = qualityText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f),
        )
    }
}

private fun shareEtablissement(
    context: android.content.Context,
    etablissement: com.epione.app.data.model.Etablissement?,
) {
    if (etablissement == null) return
    val adresse = "${etablissement.adresse}, ${etablissement.codePostal} ${etablissement.ville}"
    val mapsUrl = if (etablissement.latitude != null && etablissement.longitude != null) {
        "https://www.google.com/maps/search/?api=1&query=${etablissement.latitude},${etablissement.longitude}"
    } else {
        val q = Uri.encode("${etablissement.nom} $adresse")
        "https://www.google.com/maps/search/?api=1&query=$q"
    }
    val text = buildList {
        add(etablissement.nom)
        add("📍 $adresse")
        if (!etablissement.telephone.isNullOrBlank()) add("📞 ${etablissement.telephone}")
        add("🗺️ $mapsUrl")
    }.joinToString("\n")
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(shareIntent, null))
}
