package com.epione.app.data.repository

import com.epione.app.data.db.dao.EtablissementDao
import com.epione.app.data.db.dao.FavoriDao
import com.epione.app.data.db.dao.QualiteDao
import com.epione.app.data.model.Etablissement
import com.epione.app.data.model.Favori
import com.epione.app.data.model.Qualite
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository unique pour les données établissements + qualité + favoris.
 * Point d'entrée pour les ViewModels — isole la couche Data.
 */
@Singleton
class EtablissementRepository @Inject constructor(
    private val etablissementDao: EtablissementDao,
    private val qualiteDao: QualiteDao,
    private val favoriDao: FavoriDao,
) {
    /** Stream réactif de tous les établissements. */
    fun getAllEtablissements(): Flow<List<Etablissement>> = etablissementDao.getAll()

    /** Recherche par nom / ville / CP, retourne un stream réactif. */
    fun searchEtablissements(query: String): Flow<List<Etablissement>> =
        etablissementDao.searchByName("%$query%")

    /** Récupère un établissement par son code FINESS. */
    suspend fun getEtablissementById(finessEt: String): Etablissement? =
        etablissementDao.getById(finessEt)

    /** Récupère les indicateurs qualité d'un établissement. */
    suspend fun getQualite(finessEt: String): Qualite? =
        qualiteDao.getByEtablissement(finessEt)

    // -------------------------------------------------------------------------
    // Favoris
    // -------------------------------------------------------------------------

    /** Stream réactif de tous les favoris (ordre anti-chronologique). */
    fun getAllFavoris(): Flow<List<Favori>> = favoriDao.getAll()

    /** Stream réactif indiquant si un établissement est en favori. */
    fun isFavori(finessEt: String): Flow<Boolean> = favoriDao.isFavori(finessEt)

    /** Ajoute un établissement aux favoris. */
    suspend fun addFavori(finessEt: String) = favoriDao.insert(Favori(finessEt))

    /** Retire un établissement des favoris. */
    suspend fun removeFavori(finessEt: String) = favoriDao.delete(finessEt)
}
