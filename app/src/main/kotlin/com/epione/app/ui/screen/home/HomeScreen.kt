package com.epione.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epione.app.R
import com.epione.app.ui.components.EtablissementCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEtablissementClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val etablissements by viewModel.etablissements.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_home_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Barre de recherche fixée en début de liste
            item {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    content = {},
                )
            }

            if (etablissements.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (query.isBlank()) {
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
                    key = { it.finessEt },
                ) { etablissement ->
                    EtablissementCard(
                        etablissement = etablissement,
                        onClick = { onEtablissementClick(etablissement.finessEt) },
                    )
                }
            }
        }
    }
}
