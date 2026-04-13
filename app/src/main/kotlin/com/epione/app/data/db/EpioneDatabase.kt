package com.epione.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.epione.app.data.db.dao.EtablissementDao
import com.epione.app.data.db.dao.QualiteDao
import com.epione.app.data.model.Etablissement
import com.epione.app.data.model.Qualite

/**
 * Base de données Room.
 *
 * [version] : incrémenter lors de chaque migration de schéma.
 * [exportSchema] : true en production pour versionner le schéma JSON.
 */
@Database(
    entities = [Etablissement::class, Qualite::class],
    version = 1,
    exportSchema = false,
)
abstract class EpioneDatabase : RoomDatabase() {
    abstract fun etablissementDao(): EtablissementDao
    abstract fun qualiteDao(): QualiteDao
}
