package com.epione.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.epione.app.R
import com.epione.app.data.model.Etablissement
import com.epione.app.util.formatDistanceKm

private fun typeIcon(type: String): ImageVector = when {
    type.contains("Pharmacie", ignoreCase = true) ||
    type.contains("Propharmacie", ignoreCase = true)          -> Icons.Default.LocalPharmacy
    type.contains("Laboratoire", ignoreCase = true) ||
    type.contains("Biologie", ignoreCase = true)               -> Icons.Default.Science
    type.contains("Protection Maternelle", ignoreCase = true) ||
    type.contains("PMI", ignoreCase = true) ||
    type.contains("planification", ignoreCase = true)          -> Icons.Default.HealthAndSafety
    type.contains("Centre de Santé", ignoreCase = true) ||
    type.contains("Maison de santé", ignoreCase = true) ||
    type.contains("Maison médicale", ignoreCase = true) ||
    type.contains("Communautés profession", ignoreCase = true)  -> Icons.Default.MedicalServices
    else                                                       -> Icons.Default.LocalHospital
}

/**
 * Carte affichée dans la liste de l'écran Home.
 * Affiche le nom, le type, la ville et la distance si disponible.
 */
@Composable
fun EtablissementCard(
    etablissement: Etablissement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    distanceKm: Double? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = typeIcon(etablissement.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 0.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etablissement.nom,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${etablissement.type} — ${etablissement.ville} (${etablissement.codePostal})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                if (distanceKm != null) {
                    Text(
                        text = formatDistanceKm(distanceKm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.nav_back),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

