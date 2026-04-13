package com.epione.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un établissement sanitaire français.
 * Source : données FINESS (data.gouv.fr).
 */
@Entity(tableName = "etablissements")
data class Etablissement(
    @PrimaryKey
    @ColumnInfo(name = "finess_et") val finessEt: String,   // Numéro FINESS entité (9 chiffres)
    @ColumnInfo(name = "nom") val nom: String,
    @ColumnInfo(name = "type") val type: String,             // ex. "Hôpital", "Clinique", "EHPAD"
    @ColumnInfo(name = "adresse") val adresse: String,
    @ColumnInfo(name = "code_postal") val codePostal: String,
    @ColumnInfo(name = "ville") val ville: String,
    @ColumnInfo(name = "telephone") val telephone: String?,
    @ColumnInfo(name = "site_web") val siteWeb: String?,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
)
