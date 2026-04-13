package com.epione.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.epione.app.data.model.Etablissement
import kotlinx.coroutines.flow.Flow

@Dao
interface EtablissementDao {

    /** Retourne tous les établissements triés par nom. */
    @Query("SELECT * FROM etablissements ORDER BY nom ASC")
    fun getAll(): Flow<List<Etablissement>>

    /** Retourne un établissement par son numéro FINESS. */
    @Query("SELECT * FROM etablissements WHERE finess_et = :finessEt LIMIT 1")
    suspend fun getById(finessEt: String): Etablissement?

    /**
     * Recherche full-text simplifiée sur le nom, la ville et le code postal.
     * Le paramètre [query] doit inclure les wildcards SQL (ex. "%paris%").
     */
    @Query(
        """
        SELECT * FROM etablissements
        WHERE LOWER(nom) LIKE LOWER(:query)
           OR LOWER(ville) LIKE LOWER(:query)
           OR code_postal LIKE :query
        ORDER BY nom ASC
        """
    )
    fun searchByName(query: String): Flow<List<Etablissement>>
}
