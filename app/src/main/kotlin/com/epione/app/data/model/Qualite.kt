package com.epione.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entité Room représentant les indicateurs qualité HAS d'un établissement.
 * Relation 1-1 avec [Etablissement] via finessEt.
 */
@Entity(
    tableName = "qualite",
    foreignKeys = [
        ForeignKey(
            entity = Etablissement::class,
            parentColumns = ["finess_et"],
            childColumns = ["finess_et"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("finess_et")]
)
data class Qualite(
    @PrimaryKey
    @ColumnInfo(name = "finess_et") val finessEt: String,
    // Score global HAS de 0.0 à 5.0 ; null = non évalué
    @ColumnInfo(name = "score_global") val scoreGlobal: Double?,
    // Catégories évaluées (Lot 4 uniquement — champs préparés)
    @ColumnInfo(name = "score_patient") val scorePatient: Double?,
    @ColumnInfo(name = "score_securite") val scoreSecurite: Double?,
    @ColumnInfo(name = "annee_evaluation") val anneeEvaluation: Int?,
)
