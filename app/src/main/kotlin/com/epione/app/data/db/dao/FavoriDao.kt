package com.epione.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epione.app.data.model.Favori
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favori: Favori)

    @Query("DELETE FROM favoris WHERE finessEt = :finessEt")
    suspend fun delete(finessEt: String)

    @Query("SELECT * FROM favoris ORDER BY addedAt DESC")
    fun getAll(): Flow<List<Favori>>

    @Query("SELECT COUNT(*) > 0 FROM favoris WHERE finessEt = :finessEt")
    fun isFavori(finessEt: String): Flow<Boolean>
}
