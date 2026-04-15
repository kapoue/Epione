package com.epione.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favori persisté localement.
 * Stocké dans la table "favoris" de la base Room (version 2+).
 */
@Entity(tableName = "favoris")
data class Favori(
    @PrimaryKey val finessEt: String,
    val addedAt: Long = System.currentTimeMillis(),
)
