package com.epione.app.di

import android.content.Context
import androidx.room.Room
import com.epione.app.data.db.EpioneDatabase
import com.epione.app.data.db.dao.EtablissementDao
import com.epione.app.data.db.dao.QualiteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Fournit la base Room.
     *
     * [createFromAsset] charge la DB pré-remplie générée par scripts/init_db.py.
     * Si l'asset n'existe pas encore (premier développeur), Room crée une DB vide.
     */
    @Provides
    @Singleton
    fun provideEpioneDatabase(
        @ApplicationContext context: Context,
    ): EpioneDatabase = Room.databaseBuilder(
        context,
        EpioneDatabase::class.java,
        "epione.db",
    )
        .createFromAsset("epione.db")
        // fallbackToDestructiveMigration — acceptable en Lot 1 (données de test uniquement).
        // Remplacer par des migrations explicites en production (Lot 2+).
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideEtablissementDao(db: EpioneDatabase): EtablissementDao = db.etablissementDao()

    @Provides
    fun provideQualiteDao(db: EpioneDatabase): QualiteDao = db.qualiteDao()
}
