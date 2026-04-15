package com.epione.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.epione.app.data.db.dao.EtablissementDao
import com.epione.app.data.db.dao.FavoriDao
import com.epione.app.data.db.dao.QualiteDao
import com.epione.app.data.model.Etablissement
import com.epione.app.data.model.Favori
import com.epione.app.data.model.Qualite

/**
 * Base de données Room.
 *
 * [version] : incrémenter lors de chaque migration de schéma.
 * [exportSchema] : true en production pour versionner le schéma JSON.
 */
@Database(
    entities = [Etablissement::class, Qualite::class, Favori::class],
    version = 2,
    exportSchema = false,
)
abstract class EpioneDatabase : RoomDatabase() {
    abstract fun etablissementDao(): EtablissementDao
    abstract fun qualiteDao(): QualiteDao
    abstract fun favoriDao(): FavoriDao

    companion object {
        /** Migration 1→2 : ajout de la table favoris (Lot 5). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS favoris " +
                    "(finessEt TEXT NOT NULL PRIMARY KEY, addedAt INTEGER NOT NULL)"
                )
            }
        }
    }
}
