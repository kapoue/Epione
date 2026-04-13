package com.epione.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.epione.app.data.model.Qualite

@Dao
interface QualiteDao {

    /** Retourne les indicateurs qualité pour un établissement donné. */
    @Query("SELECT * FROM qualite WHERE finess_et = :finessEt LIMIT 1")
    suspend fun getByEtablissement(finessEt: String): Qualite?
}
